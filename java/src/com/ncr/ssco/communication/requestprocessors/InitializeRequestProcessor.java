package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.TableElement;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class InitializeRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(InitializeRequestProcessor.class);
    private String version = "";
    private int timeout = 0;
    private int heartbeatTimeout = 3000;
    private String laneNumber = "";
    private String primaryLanguage = "";
    private Properties processorProperties;

    public InitializeRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
        processorProperties = messageHandler.getProcessorsProperties();
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        version = requestFromSsco.getStringField("Version");
        timeout = requestFromSsco.getIntField("HeartbeatTimeout");
        primaryLanguage = requestFromSsco.getStringField("PrimaryLanguage");
        laneNumber = getManager().getRegNUmber();

        logger.info("Initialize Response -- START ");
        logger.info("- version: " + version);
        logger.info("- timeout: " + timeout);
        logger.info("- laneNumber: " + laneNumber);
        logger.info("- primaryLanguage: " + primaryLanguage);
        logger.info("Initialize Response -- END ");

        if (primaryLanguage == null) {
            getMessageHandler().setPrimaryLanguage("0409");
        } else {
            getMessageHandler().setPrimaryLanguage(primaryLanguage);
        }

        sendResponses(new SscoError());
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        sendPOSMessagesTableMessage();
        sendElementsTableMessage("Vouchers", getVouchersTable());
        sendElementsTableMessage("Departments", getDepartmentsTable());
        sendInitializationCompleteMessage();

        getMessageHandler().getResponses().add(addEndResponse());
        logger.debug("Exit");
    }

    private void sendInitializationCompleteMessage() {
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("InitializationComplete");
        responseToSsco.setStringField("Version", version);
        responseToSsco.setStringField("LaneNumber", "1");
        responseToSsco.setIntField("HeartbeatTimeout", heartbeatTimeout);
        getMessageHandler().sendResponseToSsco(responseToSsco);
    }

    private void sendPOSMessagesTableMessage() {
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Table");
        responseToSsco.setStringField("Name", "POSMessages");
        responseToSsco.setStringField("URL", "file://c:/scot/config/POSMessages.xml");
        getMessageHandler().sendResponseToSsco(responseToSsco);
    }

    private void sendElementsTableMessage(String tableName, List<TableElement> table) {
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Table");
        responseToSsco.setStringField("Name", tableName);
        int row = 1;
        for (TableElement element : table) {
            responseToSsco.setStringField("Cell." + row + ".1", element.getName());
            responseToSsco.setStringField("Cell." + row + ".2", element.getId());
            row++;
        }
        getMessageHandler().sendResponseToSsco(responseToSsco);
    }

    private void sendDepartmentsTableMessage() {
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Table");
        responseToSsco.setStringField("Name", "Departments");
        getMessageHandler().sendResponseToSsco(responseToSsco);
    }

    private List<TableElement> getDepartmentsTable() {
        return getManager().getDepartmentsTable();
    }

    private List<TableElement> getVouchersTable() {
        List<TableElement> tableList = new ArrayList<TableElement>();

        for (int index = 1; index <= 9999; index++) {
            String ret = processorProperties.getProperty("Voucher." + index);
            if (ret == null) {
                break;
            } else {
                String[] tokens = ret.split(";");
                if (tokens[0] != null && tokens[0].length() > 0 && tokens[1] != null && tokens[1].length() > 0) {
                    tableList.add(new TableElement(tokens[0], tokens[1]));
                }
            }
        }
        return tableList;
    }
}

