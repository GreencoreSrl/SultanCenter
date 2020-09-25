package com.ncr.ssco.communication.manager;

import com.ncr.ssco.communication.entities.DataNeeded;
import com.ncr.ssco.communication.entities.DataNeededType;
import com.ncr.ssco.communication.hook.AutomationMessage;
import com.ncr.ssco.communication.requestdecoder.RequestDecoderFactory;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.requestprocessors.*;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import com.ncr.ssco.communication.util.Utilities;
import com.ncr.ssco.communication.util.UtilitySscoMessageField;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.*;

import static com.ncr.ssco.communication.requestprocessors.ProcessorConstants.*;

/**
 * Created by stefanobertarello on 28/02/17.
 */
public class SscoMessageHandler {
    private static final Logger logger = Logger.getLogger(SscoMessageHandler.class);
    private SscoStateManager stateManager;
    private Vector<String> responses;
    private Vector<String> pendingResponses;
    private SscoLanguageHandler languageManager;
    private Map<String, RequestProcessorInterface> requestProcessorsMap;
    private RequestProcessorInterface defaultRequestProcessor;
    private Properties processorsProperties;
    private Properties dataneededProperties;
    private DataNeeded pendingDataNeeded;
    private boolean dataNeededPending = false;

    public synchronized Vector<String> peekResponses() {
        Vector<String> ret = new Vector<String>(responses);
        responses.clear();
        return ret;
    }

    public synchronized Vector<String> getResponses() {
        return responses;
    }

    public synchronized void setResponses(Vector<String> responses) { // AMZ-FLANE#ADD -- SYNC
        this.responses = responses;
    }

    public SscoMessageHandler(String configurationFilePath, String processorsConfigFilePath, String dataneededConfigFilePath) {
        stateManager = SscoStateManager.getInstance();
        responses = new Vector<String>();
        pendingResponses = new Vector<String>();
        languageManager = SscoLanguageHandler.getInstance();
        requestProcessorsMap = new HashMap<String, RequestProcessorInterface>();
        try {
            processorsProperties = new Properties();
            processorsProperties.load(new FileInputStream(processorsConfigFilePath));
        } catch (Exception e) {
            logger.error("Error loading Properties: " + e);
        }

        try {
            dataneededProperties = new Properties();
            dataneededProperties.load(new FileInputStream(dataneededConfigFilePath));
        } catch (Exception e) {
            logger.error("Error loading Properties: " + e);
        }

        setDefaultProcessor(new DefaultRequestProcessor(this));

        buildProcessorsList();
    }

    private synchronized void buildProcessorsList() {
        addProcessor(INITIALIZE, new InitializeRequestProcessor(this));
        addProcessor(REQUEST_POS_STATE, new RequestPosStateRequestProcessor(this));
        addProcessor(VALIDATE_USER_ID, new ValidateUserIdRequestProcessor(this));
        addProcessor(SIGN_ON, new SignOnRequestProcessor(this));
        addProcessor(SIGN_OFF, new SignOffRequestProcessor(this));
        addProcessor(SHUTTING_DOWN, new ShuttingDownRequestProcessor(this));
        addProcessor(VOID_TRANSACTION, new VoidTransactionRequestProcessor(this));
        addProcessor(SUSPEND_TRANSACTION, new SuspendTransactionRequestProcessor(this));
        addProcessor(PRINT_REPORT, new PrintReportRequestProcessor(this));
        addProcessor(TENDER, new TenderRequestProcessor(this));
        addProcessor(VOID_ITEM, new VoidItemRequestProcessor(this));
        addProcessor(ITEM, new ItemRequestProcessor(this));
        addProcessor(COUPON, new CouponRequestProcessor(this));
        addProcessor(LOAN_CONST, new LoanRequestProcessor(this));
        addProcessor(BALANCE_CONST, new BalanceRequestProcessor(this));
        addProcessor(PICKUP_CONST, new PickupRequestProcessor(this));
        addProcessor(LANGUAGE, new LanguageRequestProcessor(this));
        addProcessor(LOYALTY_CARD, new LoyaltyRequestProcessor(this));
        addProcessor(AIR_MILES, new AirMilesRequestProcessor(this));
        addProcessor(ENTER_TENDER_MODE, new EnterTenderModeRequestProcessor(this));
        addProcessor(EXIT_TENDER_MODE, new ExitTenderModeRequestProcessor(this));
        addProcessor(DATA_NEEDED_REPLY, new DataNeededReplyProcessor(this));
        addProcessor(CUSTOM_ASSIST_MODE, new CustomAssistModeRequestProcessor(this));
        addProcessor(CUSTOM_ASSIST_TENDER_MODE, new CustomAssistModeTenderRequestProcessor(this));
        addProcessor(REQUEST_TOTAL, new RequestTotalRequestProcessor(this));
        addProcessor(RELOAD_OPTIONS, new ReloadOptionsRequestProcessor(this));
        addProcessor(COMMAND, new CommandRequestProcessor(this));
        addProcessor(EFT_SETTLE, new EftSettleRequestProcessor(this));   //EFT-SETTLE-CGA#A
    }

    public synchronized void handleMessage(String message) {
        handleCommand(message);
    }

    public synchronized void setDefaultProcessor(RequestProcessorInterface defaultRequestProcessor) {
        this.defaultRequestProcessor = defaultRequestProcessor;
    }

    public synchronized void addProcessor(String requestMessageName, RequestProcessorInterface requestProcessor) {
        requestProcessorsMap.put(requestMessageName, requestProcessor);
        logger.debug("addProcessor(\"" + requestMessageName + "\", " + requestProcessor.getClass().getSimpleName() + ");");
        logger.info("Add - Processor: " + requestProcessor.getClass().getSimpleName());
    }

    public synchronized RequestProcessorInterface getProcessor(String requestMessageName) {
        return requestProcessorsMap.get(requestMessageName);
    }

    public synchronized void handleCommand(String message) {
        Set<String> bypassMessages = new HashSet<String>(Arrays.asList(
                ProcessorConstants.BALANCE_CONST,
                ProcessorConstants.RELOAD_OPTIONS,
                ProcessorConstants.COMMAND,
                ProcessorConstants.ENTER_ASSIST_MODE,
                ProcessorConstants.EXIT_ASSIST_MODE
        ));

        RequestFromSsco requestFromSsco = RequestDecoderFactory.decodeRequest(message);
        if (requestFromSsco == null) {
            logger.error("ERROR, CANNOT DECODE XML MESSAGE FROM SSCO!!");
            return;
        }
        if (bypassMessages.contains(requestFromSsco.getMessageName())) {
            logger.info("Bypassing message " + requestFromSsco.getMessageName());
            return;
        }

        RequestProcessorInterface posProcessor = (RequestProcessorInterface) requestProcessorsMap.get(requestFromSsco.getMessageName());
        if (posProcessor == null) {
            if (defaultRequestProcessor == null) {
                logger.error("ERROR, CANNOT MAP THE XML MESSAGE FROM SSCO WITH A PROCESSOR!!");
                return;
            }
            posProcessor = defaultRequestProcessor;
            logger.info("USING DEFAULT  PROCESSOR FOR MESSAGE \"" + requestFromSsco.getMessageName() + "\"");
        } else {
            logger.info("USING SPECIFIC PROCESSOR FOR MESSAGE \"" + requestFromSsco.getMessageName() + "\"");
        }

        posProcessor.setProcessRequest(requestFromSsco);
        if (stateManager.isValid(requestFromSsco.getMessageName())) {
            logger.debug("isValid OK");
            posProcessor.process(requestFromSsco);
        } else {
        }
    }

    public synchronized ResponseToSsco createResponseToSsco(String messageName) {
        return new ResponseToSsco(messageName);
    }

    public synchronized void sendResponseToSsco(ResponseToSsco responseToSsco) {
        sendResponseToSsco(responseToSsco, true);
    }

    public synchronized void sendResponseToSsco(ResponseToSsco responseToSsco, boolean changeState) {
        String responseXml = prepareResponseToSsco(responseToSsco);

        logger.info("POS->FLA: MAPPED  RESPONSE: " + Utilities.rightFillWithSpaces(responseToSsco.getMessageName()) + "    [" + responseXml + "]");

        if (responseXml == "") {
            logger.info("ERROR, Prepared response xml for SSCO is \"\" - skipping cal to posHook.sendResponse(...)");
            return;
        }

        if (changeState) {
            logger.info("Changing state");
            this.stateManager.setCurrentState(this.stateManager.getFutureState().getName());
            this.stateManager.setFutureState(null);
        }
        logger.debug("metto responseXml in responses");
        responses.add(responseXml);
    }

    public synchronized boolean isPendingResponses() {
        return pendingResponses.size() > 0;
    }

    public synchronized void pendingResposeToResponse() {
        responses.addAll(pendingResponses);
        pendingResponses = new Vector<String>();
        dataNeededPending = false;
    }

    public synchronized String prepareResponseToSsco(ResponseToSsco responseToSsco) {
        String responseXml = "";
        String messageName = responseToSsco.getMessageName();
        AutomationMessage automationMessage = new AutomationMessage(responseToSsco.getMessageName());

        Map<String, Object> fieldsMap = responseToSsco.getFieldsMap();

        for (String fieldName : fieldsMap.keySet()) {
            Object fieldValue = fieldsMap.get(fieldName);
            UtilitySscoMessageField.convertAndAddToAutomationMessage(automationMessage, fieldName, fieldValue);
        }

        responseXml = automationMessage.xml();
        return responseXml;
    }

    public synchronized boolean isRunningOnFastLane() {
        return true;
    }

    public void destroy() {
        logger.fatal("------------------------------------------------------------------------------");
    }

    public synchronized Properties getProcessorsProperties() {
        return processorsProperties;
    }

    public synchronized Properties getDataneededProperties() {
        return dataneededProperties;
    }

    private synchronized void setListField(ResponseToSsco responseToSsco, String name, List<String> values) {
        for (int index = 1; index <= values.size(); index++) {
            responseToSsco.setStringField(name + "." + index, values.get(index - 1));
        }
    }

    public synchronized boolean isDataNeededPending() {
        return dataNeededPending;
    }

    public synchronized void setDataNeededPending(boolean dataNeededPending) {
        this.dataNeededPending = dataNeededPending;
    }

    public synchronized void sendDataNeeded(DataNeeded dataNeeded) {
        logger.debug("Enter");
        pendingDataNeeded = dataNeeded;
        ResponseToSsco responseToSsco = createResponseToSsco("DataNeeded");

        if (dataNeeded.getType() == DataNeededType.Unknown) {
            logger.error("Wrong DataNeeded Type");
        } else {
            setDataNeededPending(true);

            responseToSsco.setIntField("Type", dataNeeded.getType().getCode());
            responseToSsco.setIntField("Id", dataNeeded.getId());
            responseToSsco.setIntField("Mode", dataNeeded.getMode());

            setListField(responseToSsco, "TopCaption", dataNeeded.getTopCaptionLines());
            setListField(responseToSsco, "TopCaptionSubstitutions", dataNeeded.getTopCaptionSubstitutionsLines());
            setListField(responseToSsco, "TopCaptionTableName", dataNeeded.getTopCaptionTableNameLines());
            setListField(responseToSsco, "TopCaptionDestination", dataNeeded.getTopCaptionDestinationLines());
            setListField(responseToSsco, "SummaryInstruction", dataNeeded.getSummaryInstructionLines());
            setListField(responseToSsco, "SummaryInstructionSubstitutions", dataNeeded.getSummaryInstructionSubstitutionsLines());
            setListField(responseToSsco, "SummaryInstructionTableName", dataNeeded.getSummaryInstructionTableNameLines());
            setListField(responseToSsco, "SummaryInstructionDestination", dataNeeded.getSummaryInstructionDestinationLines());
            setListField(responseToSsco, "BottomCaption", dataNeeded.getBottomCaptionLines());
            setListField(responseToSsco, "BottomCaptionSubstitutions", dataNeeded.getBottomCaptionSubstitutionsLines());
            setListField(responseToSsco, "BottomCaptionTableName", dataNeeded.getBottomCaptionTableNameLines());
            setListField(responseToSsco, "BottomCaptionDestination", dataNeeded.getBottomCaptionDestinationLines());
            setListField(responseToSsco, "DetailedInstruction", dataNeeded.getDetailedInstructionLines());
            setListField(responseToSsco, "DetailedInstructionSubstitutions", dataNeeded.getDetailedInstructionSubstitutionsLines());
            setListField(responseToSsco, "DetailedInstructionTableName", dataNeeded.getDetailedInstructionTableNameLines());
            setListField(responseToSsco, "DetailedInstructionDestination", dataNeeded.getDetailedInstructionDestinationLines());
            responseToSsco.setIntField("EnableSecurity", dataNeeded.getEnableSecurity());
            setListField(responseToSsco, "ButtonData", dataNeeded.getButtonDataLines());
            setListField(responseToSsco, "ButtonText", dataNeeded.getButtonTextLines());

            if (dataNeeded.getMinInputLength() != null) {
                responseToSsco.setIntField("MinInputLength", dataNeeded.getMinInputLength());
            }
            if (dataNeeded.getKeypad() != null) {
                responseToSsco.setIntField("Keypad", dataNeeded.getKeypad());
            }
            if (dataNeeded.getInputMask() != null) {
                responseToSsco.setStringField("InputMask", dataNeeded.getInputMask());
            }
            if (dataNeeded.getEnableScanner() != null) {
                responseToSsco.setIntField("EnableScanner", dataNeeded.getEnableScanner());
            }
            if (dataNeeded.getHideTotal() != null) {
                responseToSsco.setIntField("HideTotal", dataNeeded.getHideTotal());
            }
            if (dataNeeded.getHideGoBack() != null) {
                responseToSsco.setIntField("HideGoBack", dataNeeded.getHideGoBack());
            }
            if (dataNeeded.getHideHelp() != null) {
                responseToSsco.setIntField("HideHelp", dataNeeded.getHideHelp());
            }

            //TODO: Add remaining fields
            sendResponseToSsco(responseToSsco);
        }
        logger.debug("Exit");
    }

    public synchronized DataNeeded getPendingDataNeeded() {
        return pendingDataNeeded;
    }

    public synchronized void setPendingDataNeeded(DataNeeded pendingDataNeeded) {
        this.pendingDataNeeded = pendingDataNeeded;
    }

    public synchronized void setPrimaryLanguage(String lang) {
        languageManager.setPrimaryLanguage(lang);
    }

    public synchronized void setCustomerLanguage(String lang) {
        languageManager.setCustomerLanguage(lang);
    }
}