package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoCashier;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class ShuttingDownRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(ShuttingDownRequestProcessor.class);

    public ShuttingDownRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        SscoCashier sscoCashier = getManager().getCashier();
        logger.info("User " + sscoCashier.getUserId() + " is performing shutdown");

        if (!getManager().shuttingDownRequest()) logger.warn("-- Warning ");

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ShutDownComplete");
        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit");
    }
}