package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class BalanceRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(InitializeRequestProcessor.class);
    private int amount = 0;


    public BalanceRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter process");

        amount = requestFromSsco.getIntField("Amount.1");

        logger.info("Balance from SSCO");
        logger.info("amount: " + amount);

        sendResponses(new SscoError());
        logger.debug("Exit");
    }

    @Override
    public void additionalProcess() {
        logger.info("EnterExit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Balance");
        responseToSsco.setIntField("Amount", amount);

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }
}
