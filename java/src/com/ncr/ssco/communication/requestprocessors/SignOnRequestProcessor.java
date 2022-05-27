package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoCashier;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.manager.SscoPosManager;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class SignOnRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(SignOnRequestProcessor.class);
    private String laneNumber = "";

    public SignOnRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        laneNumber = requestFromSsco.getStringField("LaneNumber");
        logger.info("Process SignOn - laneNumber: " + laneNumber);

        SscoCashier sscoCashier = new SscoCashier(requestFromSsco.getStringField("UserId"), requestFromSsco.getStringField("Password"));
        getManager().signOnRequest(sscoCashier);
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("SignOn");
        responseToSsco.setStringField("LaneNumber", laneNumber);
        if( sscoError.OK != sscoError.getCode() ){
            logger.info("Response SignOn KO - Enter  - error code: " + sscoError.getCode());
            responseToSsco.setStringField("Message.1", sscoError.getMessage());
        }

        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add( addEndResponse() ); // fine sequenza

        logger.debug("Exit");
    }
}