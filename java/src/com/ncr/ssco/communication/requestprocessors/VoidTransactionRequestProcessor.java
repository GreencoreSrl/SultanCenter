package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.manager.SscoStateManager;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;


public class VoidTransactionRequestProcessor extends TransactionProcessor {
    private String id;

    public VoidTransactionRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        this.id = requestFromSsco.getStringField("Id");
        logger.info("VoidTransaction - Id: " + id + " State: " + SscoStateManager.getInstance().getCurrentState().getName());
        if ("Total".equals(SscoStateManager.getInstance().getCurrentState().getName())) {
            logger.info("Amount: " + getManager().getTotalsAmount().getTotalAmount());
            if (getManager().getTotalsAmount().getTotalAmount() == 0) {
                sendResponses(new SscoError());
                return;
            }
        }
        getManager().voidTransactionRequest(id);
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("TransactionVoided");
        responseToSsco.setStringField("Id", this.id);

        if (sscoError.OK != sscoError.getCode()) {
            logger.info("Response VoidTransaction KO - Enter  - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());
            responseToSsco.setStringField("Message.1", sscoError.getMessage());
        }

        getMessageHandler().sendResponseToSsco(responseToSsco);
        sendEndTransactionResponseToSsco(id);
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit");
    }
}