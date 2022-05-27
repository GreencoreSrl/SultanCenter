package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoCashier;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.manager.SscoPosManager;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class SignOffRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(SignOffRequestProcessor.class);
    private String laneNumber = "";

    public SignOffRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        laneNumber = requestFromSsco.getStringField("LaneNumber");
        SscoCashier sscoCashier = getManager().getCashier();
        logger.info("User " + sscoCashier.getUserId() + ", with LaneNumber " + laneNumber + " is performing logoff");

        if (getManager().transactionHasStarted()) {
            sendResponses(new SscoError(SscoError.CANT_SIGNOFF_NOW));
        } else {
            if (!getManager().signOffRequest()) logger.warn("-- Warning ");
        }

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("SignOff");
        responseToSsco.setStringField("LaneNumber", laneNumber);
        if( sscoError.OK != sscoError.getCode() ){
            logger.info("Response SignOff KO - Enter - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());
            responseToSsco.setStringField("Message.1", sscoError.getMessage());
        }
        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit ");
    }
}