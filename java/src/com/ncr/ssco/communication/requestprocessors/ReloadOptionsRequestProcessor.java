package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;

public class ReloadOptionsRequestProcessor extends DefaultRequestProcessor {

    public ReloadOptionsRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        sendResponses();
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ReloadComplete");

        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add( addEndResponse());

        logger.info("Exit");
    }
}