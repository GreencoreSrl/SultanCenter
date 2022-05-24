package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class EnterTrainingModeRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(EnterTrainingModeRequestProcessor.class);

    public EnterTrainingModeRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        getManager().enterExitTrainingModeRequest();
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("TrainingModeEntered");
        if (sscoError.OK != sscoError.getCode()) {
            responseToSsco.setStringField("Message.1", sscoError.getMessage());
        }
        getMessageHandler().sendResponseToSsco(responseToSsco);

        getMessageHandler().getResponses().add(addEndResponse());
        logger.debug("Exit");
    }
}