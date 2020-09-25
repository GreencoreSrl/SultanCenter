package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.PosState;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class RequestPosStateRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(RequestPosStateRequestProcessor.class);
    private PosState posState = PosState.Inactive;

    public RequestPosStateRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        posState = PosState.Inactive;
        logger.info("posState: " + posState);
        sendResponses(new SscoError());

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("PosState");
        responseToSsco.setStringField("State", posState.toString());

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }
}

