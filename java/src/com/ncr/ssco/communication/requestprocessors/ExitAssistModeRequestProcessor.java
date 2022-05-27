package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import org.apache.log4j.Logger;

public class ExitAssistModeRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(ExitAssistModeRequestProcessor.class);

    public ExitAssistModeRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        sendResponses(new SscoError());
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");
        getMessageHandler().getResponses().add(addEndResponse());
        logger.debug("Exit");
    }
}