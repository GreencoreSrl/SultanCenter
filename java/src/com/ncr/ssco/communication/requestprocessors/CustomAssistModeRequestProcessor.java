package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import org.apache.log4j.Logger;

public class CustomAssistModeRequestProcessor extends ItemRequestProcessor {
    private static final Logger logger = Logger.getLogger(CustomAssistModeRequestProcessor.class);

    public CustomAssistModeRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        logger.info("Apertura dataneeded per funzionalità estese");
        getManager().sendDataNeeded("ExtendedSold");
        logger.debug("Exit");
    }

}

