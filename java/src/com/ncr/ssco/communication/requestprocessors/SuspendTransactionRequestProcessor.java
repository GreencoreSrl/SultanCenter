package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;

public class SuspendTransactionRequestProcessor extends TransactionProcessor {
    private String id;

    public SuspendTransactionRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        this.id = requestFromSsco.getStringField("Id");
        logger.info("Id: " + id);
        getManager().suspendTransactionRequest(id);
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("TransactionSuspended");
        responseToSsco.setStringField("Id", this.id);

        if (sscoError.OK != sscoError.getCode()) {
            logger.info("KO - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());
            responseToSsco.setStringField("Message.1", sscoError.getMessage());
        }

        getMessageHandler().sendResponseToSsco(responseToSsco);
        sendEndTransactionResponseToSsco(id);
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit");
    }
}