package com.ncr.ssco.communication.manager;

import com.ncr.Itemdata;
import com.ncr.ssco.communication.SscoGSPSocketServer;
import com.ncr.ssco.communication.entities.*;
import com.ncr.ssco.communication.entities.pos.*;
import com.ncr.ssco.communication.requestprocessors.*;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import static com.ncr.ssco.communication.requestprocessors.ProcessorConstants.NO;
import static com.ncr.ssco.communication.requestprocessors.ProcessorConstants.NOWEIGHT;

/**
 * Created by stefanobertarello on 28/02/17.
 */
public class SscoPosManager implements SscoPosManagerInterface {
    private static final Logger logger = Logger.getLogger(SscoPosManager.class);
    public static final String PROPERTIES = "conf/ArsSsco.properties";
    public static final String PROCESSORS_PROPERTIES = "conf/ArsSscoProcessors.properties";
    public static final String DATANEEDED_PROPERTIES = "conf/DataNeeded.properties";
    public static final String LOG_PROPERTIES = "conf/ArsSscoLog4j.properties";
    public static final String EOD_INPUT = "3";
    public static final int OK_CANCEL_ENTER = 2;
    public static final int OK_CANCEL_CLEAR = 1;


    private static SscoPosManager instance = null;
    private ActionPOSManager actionPosManager = null;
    private RequestProcessorInterface processor;
    private SscoMessageHandler messageHandler;
    private Component component;
    private EventQueue queue;
    private boolean isEnabled = false;
    private boolean loadCashiers = false;
    private boolean testEnvironment = false;
    private Vector<Itemdata> currentItemPromotions = new Vector<Itemdata>();
    private ArrayList<Itemdata> transactionalRewards = new ArrayList<Itemdata>();
    private int transactionalDiscount;
    private int roundDiscount;
    private int donation;
    private static Object dataneededReplyMutex = new Object();
    private int dataneededOkCancelKey = 0;
    private int dataneededCloseWait = 0;
    private int tenderRounding = 0;
    private SscoTender currentSscoTender = new SscoTender(TenderTypeEnum.Cash, 0);
    private SscoItem currentSscoItem = new SscoItem();
    private SscoTerminal terminal;
    private Properties processorProperties;

    private int savedAmt;
    private int savedBal;
    private int savedCont;
    private boolean shutdownRequested = false;

    private SscoPosManager() {
    }

    public static SscoPosManager getInstance() {
        if (instance == null) {
            instance = new SscoPosManager();
        }

        return instance;
    }

    public void initialize(Component component, EventQueue queue, String reg_nbr) {
        this.component = component;
        this.queue = queue;

        try {
            PropertyConfigurator.configure(LOG_PROPERTIES);
        } catch (Exception e) {
            e.printStackTrace();
        }

        actionPosManager = ActionPOSManager.getInstance();
        terminal = new SscoTerminal(reg_nbr);
        messageHandler = new SscoMessageHandler(PROPERTIES, PROCESSORS_PROPERTIES, DATANEEDED_PROPERTIES);
        processorProperties = messageHandler.getProcessorsProperties();

        Thread thread = new Thread(new SscoGSPSocketServer(6696, messageHandler, processorProperties.getProperty("Encoding", "UTF-8")));
        thread.start();
    }

    public boolean isTestEnvironment() {
        return testEnvironment;
    }

    public void addTransactionalReward(Itemdata rew) {
        logger.info("addTransactionalReward text=" + rew.text);
        logger.info("addTransactionalReward amt =" + rew.amt);
        //TODO: Check this field logger.info("addTransactionalReward entryId=" + rew.entryId);
        logger.info("addTransactionalReward number=" + rew.number);

        for (Itemdata itm : transactionalRewards) {
            if (itm.number.compareTo(rew.number) == 0) {
                transactionalRewards.remove(itm);
                logger.info("addTransactionalReward duplicate found ... removing " + rew.number);
                break;
            }
        }
        transactionalRewards.add(rew);
    }

    public ArrayList<Itemdata> getTransactionalRewards() {
        return transactionalRewards;
    }

    public void resetTransactionalRewards() {
        logger.info("resetTransactionalRewards called");
        transactionalRewards.clear();
    }

    public int rigaDescrttivaSuScontrinoSSCO(String riga, int itemAssociato) {
        ResponseToSsco responseToSsco = messageHandler.createResponseToSsco("ItemSold");

        responseToSsco.setStringField("UPC", "0");

        if (itemAssociato > 0) {
            responseToSsco.setStringField("DiscountDescription.1", riga);
            responseToSsco.setIntField("AssociatedItemNumber", itemAssociato);
        } else {
            responseToSsco.setStringField("Description", riga);
            responseToSsco.setIntField("Price", 0);
        }

        responseToSsco.setIntField("RequiresSubsCheck", NO);

        responseToSsco.setIntField("RequiresSecurityBagging", NOWEIGHT);

        responseToSsco.setIntField("ShowRewardPoints", 0);

        int itmno = itemNumberSetting();
        responseToSsco.setIntField("ItemNumber", itmno);

        messageHandler.sendResponseToSsco(responseToSsco);
        return itmno;
    }

    public String stringaDaLanguageProperties(int code, String scrittaDefault) {
        return new SscoError(code, scrittaDefault).getMessage();
    }

    public void sendDataneededEnter() {
        synchronized (dataneededReplyMutex) {
            dataneededOkCancelKey = OK_CANCEL_ENTER;
        }
    }

    public void sendDataneededCorrettore() {
        synchronized (dataneededReplyMutex) {
            dataneededOkCancelKey = OK_CANCEL_CLEAR;
        }
    }

    public int waitForDataneededReply(int timeout) {
        final int TICK = 100;
        logger.info("waitForDataneededReply called. Timeout: " + timeout);
        synchronized (dataneededReplyMutex) {
            dataneededOkCancelKey = 0;
        }

        while (timeout > 0) {
            timeout -= TICK;
            try {
                Thread.sleep(TICK);
            } catch (Exception e) {
            }
            synchronized (dataneededReplyMutex) {
                if (dataneededOkCancelKey != 0) {
                    logger.info("waitForDataneededReply returning " + dataneededOkCancelKey);
                    return dataneededOkCancelKey;
                }
            }
        }
        logger.info("driiin!! Returning " + OK_CANCEL_CLEAR);
        return OK_CANCEL_CLEAR;
    }

    public void sendDataneededClose() {
        synchronized (dataneededReplyMutex) {
            dataneededCloseWait = 1;
        }
    }

    public void waitForDataneededClose() {
        logger.info("waitForDataneededClose called");
        synchronized (dataneededReplyMutex) {
            dataneededCloseWait = 0;
        }
        while (true) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            synchronized (dataneededReplyMutex) {
                if (dataneededCloseWait != 0) {
                    break;
                }
            }
        }
        logger.info("waitForDataneededClose return");
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isUsed() {
        return isEnabled && (terminal.getCashier().getUserId() != null);
    }

    public void addCustomer(SscoCustomer customer) {
        terminal.getTransaction().setCustomer(customer);
    }

    public void addItem(SscoItem item) {
        terminal.getTransaction().getItems().add(item);
    }

    public void addPromotion(SscoItemPromotion promo) {
        terminal.getTransaction().getPromotions().add(promo);
    }

    public String getRegNUmber() {
        return terminal.getRegNumber();
    }

    public void addTender(SscoTender tender) {
        tender.setTenderType(currentSscoTender.getTenderType());
        terminal.getTransaction().getTenders().add(tender);
    }

    public void updateTransactionalDiscount(int transDiscount) {
        transactionalDiscount = transDiscount;
    }

    public int getTenderRounding() {
        return tenderRounding;
    }

    public void setTenderRounding(int tenderRounding) {
        this.tenderRounding = tenderRounding;
    }

    public void updateRoundDiscount(int roundDisc) {
        logger.info("Updating round discount to: " + roundDisc);
        roundDiscount = roundDisc;
    }

    public void setDonation(int amt) {
        if (!isEnabled) return;

        logger.info("Updating donation to: " + amt);
        donation = amt;
    }

    public void updateTotalAmountFromSave() {
        updateTotalAmount(savedAmt, savedBal, savedCont, 0);
    }

    public void setTicketAmount(int amount) {
        this.terminal.getTransaction().getTotalAmount().setTicketAmount(amount);
    }

    public void setGlutenFreeAmount(int amount) {
        this.terminal.getTransaction().getTotalAmount().setGlutenFreeAmount(amount);
    }

    public void updateTotalAmount(int amt, int bal, int cont, int changeDue) {
        logger.info("amt=" + amt + ",bal=" + bal + ",cont=" + cont);

        savedAmt = amt;
        savedBal = bal;
        savedCont = cont;
        terminal.getTransaction().getTotalAmount().setTotalAmount(amt - transactionalDiscount);
        terminal.getTransaction().getTotalAmount().setBalanceDue(bal);

        if (bal == 0 && changeDue != 0) {
            terminal.getTransaction().getTotalAmount().setChangeDue(Math.abs(changeDue + roundDiscount) - donation);
        }

        terminal.getTransaction().getTotalAmount().setItemCount((int) (long) cont);

        logger.info("changeDue=" + changeDue);

        logger.info("roundDiscount=" + roundDiscount + ",transactionalDiscount=" + transactionalDiscount);
        logger.info("getTotalAmount=" + terminal.getTransaction().getTotalAmount().getTotalAmount());
        logger.info("getBalanceDue=" + terminal.getTransaction().getTotalAmount().getBalanceDue());
        logger.info("getChangeDue=" + terminal.getTransaction().getTotalAmount().getChangeDue());
    }

    public DataNeeded sendDataNeeded(String dataNeededInfo) {
        if (dataNeededInfo.equals("Clear")) {
            if (messageHandler.isPendingResponses()) {
                logger.info("Clear DataNeeded rejected due pending responses ");
                messageHandler.pendingResposeToResponse();
                return null;
            }
        }
        DataNeeded dataNeeded = new DataNeeded(dataNeededInfo, messageHandler.getDataneededProperties());
        processor.sendDataNeeded(dataNeeded);
        return dataNeeded;
    }

    public void sendShutdownRequested() {
        processor.sendShutdownRequested();
        shutdownRequested = true;
    }

    public void abortDataNeeded() {
        logger.info("abortDataNeeded");
        messageHandler.setPendingDataNeeded(null);
        messageHandler.pendingResposeToResponse();
        return;
    }

    public void sendDataNeeded(String dataNeededInfo, String message) {
        processor.sendDataNeeded(new DataNeeded(dataNeededInfo, messageHandler.getDataneededProperties(), message));
    }

    public void sendDataNeeded(String dataNeededInfo, ArrayList<String> messages) {
        processor.sendDataNeeded(new DataNeeded(dataNeededInfo, messageHandler.getDataneededProperties(), messages));
    }

    public int getPriceInserted() {
        if (processor instanceof ItemRequestProcessor) {
            ItemRequestProcessor itm = (ItemRequestProcessor) processor;
            Integer priceRequest = itm.getPrice();
            return priceRequest;
        }
        return 0;
    }

    public int getWeightInserted() {
        if (processor instanceof ItemRequestProcessor) {
            ItemRequestProcessor itm = (ItemRequestProcessor) processor;
            Integer weightInserted = itm.getWeight();
            return weightInserted;
        }
        return 0;
    }

    public void setCashierSupervisor(boolean supervisor) {
        terminal.getCashier().setSupervisor(supervisor);
    }

    public SscoCashier getCashier() {
        return terminal.getCashier();
    }

    public SscoTotalAmount getTotalsAmount() {
        return terminal.getTransaction().getTotalAmount();
    }

    public List<SscoItem> getItems() {
        return terminal.getTransaction() == null ? null : terminal.getTransaction().getItems();
    }

    public SscoCustomer getCustomer() {
        return terminal.getTransaction() == null ? null : terminal.getTransaction().getCustomer();
    }

    public SscoTransaction getTransaction() {
        return terminal.getTransaction();
    }

    public SscoTender getLastTender() {
        int size = -1;
        if (terminal.getTransaction().getTenders() != null) {
            size = terminal.getTransaction().getTenders().size();
            if (size == 0) {
                return null;
            }
            return terminal.getTransaction().getTenders().get(size - 1);
        } else
            return null;
    }

    public SscoItem getLastItem() {
        int size = -1;
        if (terminal.getTransaction().getItems() != null) {
            size = terminal.getTransaction().getItems().size();
            if (size != 0)
                return terminal.getTransaction().getItems().get(size - 1);
        }
        return null;
    }

    public int itemNumberSetting() {
        return terminal.getTransaction().addItemNumber();
    }

    public int getLastItemNumberSetting() {
        return terminal.getTransaction().getLastItemNumber();
    }

    public void setItemResponse(SscoItem responseBySSCO) {
        getProcessor().setItemResponse(responseBySSCO);
    }

    public boolean transactionHasStarted() {
        return (getItems() != null && getItems().size() != 0) || getCustomer() != null;
    }

    @Override
    public SscoTransaction startTransaction(int id) {
        logger.debug("Enter");

        terminal.initTransaction(id);
        updateTransactionalDiscount(0);
        updateRoundDiscount(0);
        setTenderRounding(0);
        setDonation(0);
        resetCurrentItemPromotions();
        logger.info("Exit " + terminal.getTransaction().getTransactionId());
        return terminal.getTransaction();
    }

    @Override
    public void endTransaction() {
        logger.debug("Enter");

        terminal.setTransaction(null);

        logger.debug("Exit");
    }

    public void voidTransaction() {
        terminal.voidTransaction();
    }

    @Override
    public void tenderRequest(SscoTender sscoTender) {
        logger.debug("Enter");
        TenderType tender = TenderTypeManager.getInstance().getActionPOSByName(sscoTender.getTenderType());
        currentSscoTender = sscoTender;

        String cmd = "SSCO::" + tender.getActionPOS();
        postAction(cmd);

        ActionPOS action = actionPosManager.getActionPOSByName("ENTER");
        if (sscoTender.getUpc() != null) {
            logger.info("Sending UPC: " + sscoTender.getUpc());
            cmd = "SSCO:" + sscoTender.getUpc() + ":" + action.getCommand().get(0).getEvent();
            postAction(cmd);
        }
        if (sscoTender.getAmount() != 0) {
            logger.info("Sending amount: " + sscoTender.getAmount());
            cmd = "SSCO:" + sscoTender.getAmount() + ":" + action.getCommand().get(0).getEvent();
            postAction(cmd);
        }
        logger.debug("Exit");
    }

    @Override
    public void tenderInquiryRequest(SscoTender sscoTender) {
        logger.debug("Enter");
        TenderType tender = TenderTypeManager.getInstance().getActionPOSByName(sscoTender.getTenderType());
        String cmd = "SSCO::" + tender.getActionPOS();
        postAction(cmd);

        clearRequest();
        logger.debug("Exit");
    }

    @Override
    public void enterExitTrainingModeRequest() {
        ActionPOS action = actionPosManager.getActionPOSByName("TRAINING_MODE");
        postAction("SSCO::" + action.getCommand().get(0).getEvent());
    }

    public void postAction(String cmd) {
        ActionEvent e = new ActionEvent(component, ActionEvent.ACTION_PERFORMED, cmd);
        queue.postEvent(e);
        logger.info("postAction " + cmd);
    }

    @Override
    public boolean signOnRequest(SscoCashier sscoCashier) {
        logger.debug("Enter");

        terminal.setCashier(sscoCashier);
        ActionPOS action = actionPosManager.getActionPOSByName("ENTER");
        String cmd = "SSCO:" + String.format("%03d", Integer.parseInt(sscoCashier.getUserId())) + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);

        logger.debug("Exit");
        return true;
    }

    @Override
    public boolean signOffRequest() {
        logger.debug("Enter");

        if (isUsed()) {
            ActionPOS action = actionPosManager.getActionPOSByName("SIGNOFF");
            postAction("SSCO::" + action.getCommand().get(0).getEvent());

            logger.debug("Exit");
            return true;
        }
        logger.info("No logged user to process signOff!");
        return false;
    }

    @Override
    public boolean shuttingDownRequest() {
        logger.debug("Enter");

        if (isUsed()) {
            logger.info("Shutdown - Cashier is open");
            if (transactionHasStarted()) {
                logger.info("Shutdown - Transaction is open");
                ActionPOS action = actionPosManager.getActionPOSByName("ABORT");
                postAction("SSCO::" + action.getCommand().get(0).getEvent());
            }
            ActionPOS action = actionPosManager.getActionPOSByName("SIGNOFF");
            postAction("SSCO::" + action.getCommand().get(0).getEvent());
        }

        ActionPOS action = actionPosManager.getActionPOSByName("ABORT");

        String data = shutdownRequested ? EOD_INPUT : "";
        postAction("SSCO:" + data + ":" + action.getCommand().get(0).getEvent());

        logger.debug("Exit");
        return true;
    }

    private void itemPriceChange(SscoItem item) {
        item.setPriceChanged(true);

        ActionPOS action = actionPosManager.getActionPOSByName("PRICE_CHANGE");
        postAction("SSCO::" + action.getCommand().get(0).getEvent());

        action = actionPosManager.getActionPOSByName("ENTER");
        postAction("SSCO:" + item.getPrice() + ":" + action.getCommand().get(0).getEvent());

        action = actionPosManager.getActionPOSByName(item.isScanned() == 0 ? "ENTER" : "SCANNER");
        postAction("SSCO:" + item.getUpc() + ":" + action.getCommand().get(0).getEvent());
    }

    @Override
    public boolean itemRequest(SscoItem item) {
        logger.debug("Enter");

        if (isUsed()) {
            if (item.getUpc() != null) {
                if (item.getPrice() != 0 && !currentSscoItem.isZeroPriced()) {
                    itemPriceChange(item);
                } else {
                    if (item.getQty() <= 1) {
                        sendItemCode(item);
                    } else {
                        sendQuantity(item);
                    }
                }
            } else if (item.getDepartment() != null) {
                sendDepartment(item);
            }
            currentSscoItem = item;

            logger.debug("Exit");
            return true;
        }

        logger.info("No user logged to process item");
        return false;
    }

    @Override
    public void voidItemRequest(SscoItem item) {
        sendVoid();

        if (item.getUpc() != null && item.getUpc().length() > 0) {
            if (item.isPriceChanged()) {
                itemPriceChange(item);
            } else {
                sendItemCode(item);
            }
        } else {
            sendDepartment(item);
        }
    }

    private void sendVoid() {
        ActionPOS action = actionPosManager.getActionPOSByName("VOID");
        String cmd = "SSCO::" + action.getCommand().get(0).getEvent();
        postAction(cmd);
    }

    private void sendItemCode(SscoItem item) {
        ActionPOS action = actionPosManager.getActionPOSByName(item.isScanned() == 0 ? "ENTER" : "SCANNER");
        String cmd = "SSCO:" + item.getUpc() + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);
    }

    private void sendDepartment(SscoItem item) {
        ActionPOS action = actionPosManager.getActionPOSByName("DEPT");
        String cmd = "SSCO:" + item.getDepartment() + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);
    }

    private void sendQuantity(SscoItem item) {
        ActionPOS action = actionPosManager.getActionPOSByName("QUANTITY");
        String cmd = "SSCO:" + item.getQty() + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);
    }

    @Override
    public boolean eftSettleRequest() {
        logger.debug("Enter");

        if (isUsed()) {
            ActionPOS action = actionPosManager.getActionPOSByName("SETTLE");
            String cmd = "SSCO::" + action.getCommand().get(0).getEvent();
            postAction(cmd);

            logger.debug("Exit");
            return true;
        }
        logger.info("No user logged to process suspend");
        return false;
    }
    //EFT-SETTLE-CGA#A END

    @Override
    public void voidTransactionRequest(String id) {
        logger.debug("Enter");
        terminal.setIdTransactionVoided(id);
        ActionPOS action = actionPosManager.getActionPOSByName("ABORT");
        postAction("SSCO::" + action.getCommand().get(0).getEvent());

        logger.debug("Exit");
    }

    @Override
    public void suspendTransactionRequest(String id) {
        logger.debug("Enter");
        ActionPOS action = actionPosManager.getActionPOSByName("SUSPEND");
        postAction("SSCO::" + action.getCommand().get(0).getEvent());
        logger.debug("Exit");
    }

    @Override
    public void enterTenderModeRequest() {
        logger.debug("Enter");
        ActionPOS action = actionPosManager.getActionPOSByName("TOTAL");
        postAction("SSCO::" + action.getCommand().get(0).getEvent());
        logger.debug("Exit");
    }

    @Override
    public void exitTenderModeRequest() {
        logger.debug("Enter");
        ActionPOS action = actionPosManager.getActionPOSByName("CLEAR");
        postAction("SSCO::" + action.getCommand().get(0).getEvent());
        logger.debug("Exit");
    }

    private String loyaltyInfoCompose(SscoCustomer sscoCustomer) {
        String loyaltyInfo = sscoCustomer.getCountryCode();

        if (loyaltyInfo.startsWith("+")) {
            loyaltyInfo = loyaltyInfo.substring(1);
        }
        return loyaltyInfo + sscoCustomer.getAccountNumber();
    }

    @Override
    public void loyaltyRequest(SscoCustomer sscoCustomer) {
        logger.debug("Enter");
        ActionPOS action = actionPosManager.getActionPOSByName(sscoCustomer.getCountryCode().length() == 0 ? "CUSTOMER_CARD" : "CUSTOMER_MOBILE");
        String cmd = "SSCO:" + loyaltyInfoCompose(sscoCustomer) + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);
        logger.debug("Exit");
    }

    @Override
    public void airMilesRequest(SscoCustomer sscoCustomer) {
        logger.debug("Enter");
        ActionPOS action = actionPosManager.getActionPOSByName("IDENT");
        String cmd = "SSCO::" + action.getCommand().get(0).getEvent();
        postAction(cmd);

        action = actionPosManager.getActionPOSByName("ENTER");
        cmd = "SSCO:" + loyaltyInfoCompose(sscoCustomer) + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);
        logger.debug("Exit");
    }

    @Override
    public void clearRequest() {
        logger.debug("Enter");
        ActionPOS action = actionPosManager.getActionPOSByName("CLEAR");
        String cmd = "SSCO::" + action.getCommand().get(0).getEvent();
        postAction(cmd);
        logger.debug("Exit");
    }

    public String findUPCbyItemNumber(SscoItem item) {
        String upc = "";
        for (SscoItem itm : terminal.getTransaction().getItems()) {
            if (itm.getItemNumber() == item.getItemNumber()) {
                upc = itm.getUpc();
                break;
            }
        }

        return upc;
    }

    public SscoItem findItembyItemNumber(int entryId) {

        SscoItem itemReturned = null;
        for (SscoItem itm : terminal.getTransaction().getItems()) {
            if (itm.getItemNumber() == entryId) {
                itemReturned = itm;
                break;
            }
        }

        return itemReturned;
    }

    public void sendAdditionalProcess(String info, String key) {
        ActionPOS action = actionPosManager.getActionPOSByName(key);
        String cmd = "SSCO:" + info + ":" + action.getCommand().get(0).getEvent();
        postAction(cmd);
    }

    @Override
    public void tenderResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void tenderSelectedResponse() {
        if (processor instanceof RequestTotalRequestProcessor) {
            processor.sendResponses(new SscoError());
        }
    }

    @Override
    public void enterExitTrainingModeResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void signOnResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void signOffResponse() {
        terminal = new SscoTerminal();
        processor.sendResponses(new SscoError());
    }

    //EFT-SETTLE-CGA#A BEG
    @Override
    public void eftSettleResponse() {
        processor.sendResponses(new SscoError());
    }
    //EFT-SETTLE-CGA#A END

    @Override
    public void shuttingDownResponse() {
        if (processor != null) processor.sendResponses(new SscoError());
    }

    @Override
    public void itemResponse() {
        if (processor instanceof EnterTenderModeRequestProcessor || processor instanceof VoidTransactionRequestProcessor) {
            logger.info("EnterTender mode workaround");
        } else {
            processor.sendResponses(new SscoError());
        }
    }

    @Override
    public void voidTransactionResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void enterTenderModeResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void exitTenderModeResponse() {
        if (processor.getClass() == ExitTenderModeRequestProcessor.class) {
            processor.sendResponses(new SscoError());
        }
    }

    @Override
    public void suspendTransactionResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void voidItemResponse() {
        processor.sendResponses(new SscoError());
    }

    @Override
    public void loyaltyResponse() {
        processor.sendResponses(new SscoError());
    }

    public synchronized void error(SscoError error) {
        processor.sendResponses(error);
    }

    public void loadSSCOParameters(String line, int nrOfLine) {
        logger.debug("Enter");

        switch (nrOfLine) {
            case 0:
                isEnabled = line.substring(0, 2).equals("01");
                loadCashiers = line.substring(2, 3).equals("1");
                testEnvironment = line.substring(3, 4).equals("1");
                break;
        }
        logger.debug("Exit");
    }

    public RequestProcessorInterface getProcessor() {
        return processor;
    }

    public void setProcessor(RequestProcessorInterface processor) {
        this.processor = processor;
    }

    public Vector<Itemdata> getCurrentItemPromotions() {
        return currentItemPromotions;
    }

    public Vector<Itemdata> resetCurrentItemPromotions() {
        return currentItemPromotions = new Vector<Itemdata>();
    }

    public void addCurrentItemPromotion(Itemdata currentItemPromotion) {
        currentItemPromotions.add(currentItemPromotion);
    }

    public List<TableElement> getDepartmentsTable() {
        return terminal.getDepartmentsTable();
    }

    public SscoItem getCurrentSscoItem() {
        return currentSscoItem;
    }

    public void setCurrentSscoItem(SscoItem currentSscoItem) {
        this.currentSscoItem = currentSscoItem;
    }

    public SscoTender getCurrentSscoTender() {
        return currentSscoTender;
    }

    public void setCurrentSscoTender(SscoTender currentSscoTender) {
        this.currentSscoTender = currentSscoTender;
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    public void setShutdownRequested(boolean shutdownRequested) {
        this.shutdownRequested = shutdownRequested;
    }
    public void forceProcessor(String name) {
        setProcessor(messageHandler.getProcessor(name));
    }

    public int getMaxDonationAmount() {
        try {
            return Integer.parseInt(processorProperties.getProperty("MaxCharityAmount"));
        } catch (Exception e) {
            logger.error("Error getting max Charity amount: " + e);
            return 0;
        }
    }
}
