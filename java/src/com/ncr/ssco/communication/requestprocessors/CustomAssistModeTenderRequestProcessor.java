package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import org.apache.log4j.Logger;

public class CustomAssistModeTenderRequestProcessor extends ItemRequestProcessor {
    private static final Logger logger = Logger.getLogger(CustomAssistModeTenderRequestProcessor.class);

    public CustomAssistModeTenderRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        logger.info("Apertura dataneeded per funzionalità estese");
        getManager().sendDataNeeded("ExtendedTender");
        logger.debug("Exit");
    }

}

