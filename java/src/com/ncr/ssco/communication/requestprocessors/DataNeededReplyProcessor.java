package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;

public class DataNeededReplyProcessor extends DefaultRequestProcessor {

    public DataNeededReplyProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    private static int delayedConfirmation;
    private static boolean waitReplyOfCloseDateneeded;
    private String dataNeededInputData;

    @Override
    public void setProcessRequest(RequestFromSsco requestFromSsco) {
    }

    private void sendCloseDataNeeded() {
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("DataNeeded");
        responseToSsco.setIntField("Type", 0);
        responseToSsco.setIntField("Id", 0);
        responseToSsco.setIntField("Mode", 0);
        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add(addEndResponse());
    }

    private boolean detectCancelAndClose(RequestFromSsco requestFromSsco) {
        Integer repCancel = requestFromSsco.getIntField("Cancel");
        if (repCancel != null) {
            logger.info("Cancel detected =" + repCancel);
            if (repCancel == 1) {
                logger.info("Closing dataneeded");
                sendCloseDataNeeded();
                return true;
            }
        }
        return false;
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        int type = requestFromSsco.getIntField("Type");

        if (type == 0) {
            getMessageHandler().pendingResposeToResponse();
            getMessageHandler().getResponses().add(addEndResponse());

            getManager().sendDataneededClose();
            return;
        }

        String originalDataneededName = getMessageHandler().getPendingDataNeeded().getName();
        if (originalDataneededName.equalsIgnoreCase("LoyaltyNotFound") ||
                originalDataneededName.equalsIgnoreCase("LoyaltyError") ||
                originalDataneededName.equalsIgnoreCase("RiepilogoBuoni") ||
                originalDataneededName.equalsIgnoreCase("QrCodeUnsoldItems") ||
                originalDataneededName.equalsIgnoreCase("UPBError")) {
            sendCloseDataNeeded();
        }

        if (originalDataneededName.equalsIgnoreCase("CashierDisplayMessage") ||
                originalDataneededName.equalsIgnoreCase("CashierDisplayMessageStore") ||
                originalDataneededName.equalsIgnoreCase("SceltaOkCancel") ||
                originalDataneededName.equalsIgnoreCase("SceltaOkCancelStore") ||
                originalDataneededName.equalsIgnoreCase("UPBConfirmError") ||
                originalDataneededName.equalsIgnoreCase("UPBConfirm")) {
            logger.info("Dataneeded SceltaOkCancel o CashierDisplayMessage o CashierDisplayMessageStore o UPBConfirm reply o UPBConfirmError");
            logger.info("Type=" + type);

            int confirmation = requestFromSsco.getIntField("Confirmation");
            logger.info("confirmation=" + confirmation);

            if (confirmation == 1) {
                getManager().sendDataneededEnter();
            } else {
                getManager().sendDataneededCorrettore();
            }
            return;
        }
    }
}
