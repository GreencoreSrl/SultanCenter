package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.DataNeeded;
import com.ncr.ssco.communication.entities.AdditionalProcessType;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.manager.SscoPosManager;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class DefaultRequestProcessor implements RequestProcessorInterface {
    protected static final Logger logger = Logger.getLogger(DefaultRequestProcessor.class);

    private SscoPosManager manager;
    private SscoMessageHandler messageHandler;
    private RequestFromSsco request;

    public DefaultRequestProcessor(SscoMessageHandler messageHandler) {
        this.manager = SscoPosManager.getInstance();
        this.messageHandler = messageHandler;
    }

    @Override
    public void setProcessRequest(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        manager.setProcessor(this);
        request = requestFromSsco;
        logger.debug("Exit");
    }

    @Override
    public void setAdditionalProcessType(AdditionalProcessType info) {
        logger.debug("EnterExit");
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        sendResponses();
        logger.debug("Exit");
    }

    @Override
    public void additionalProcess() {
        logger.info("EnterExit");
    }

    @Override
    public void sendResponses() {
        logger.debug("Enter sendResponses DefaultProcessor");
        sendResponses(new SscoError(SscoError.DEFAULT));
        logger.debug("Exit sendResponses DefaultProcessor");
    }

    @Override
    public void sendResponses(SscoError sscoError) {

        if( sscoError.getCode() == SscoError.DEFAULT ) {
            getMessageHandler().getResponses().add(addEndResponse());
        }
    }

    public void sendDataNeeded(DataNeeded dataNeeded) {
        logger.debug("Enter");

        getMessageHandler().setDataNeededPending(true);
        getMessageHandler().sendDataNeeded(dataNeeded);
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit");
    }

    @Override
    public void sendShutdownRequested() {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ShutdownRequested");
        responseToSsco.setStringField("Action", "RebootRequired");
        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit");
    }

    @Override
    public void setItemResponse(SscoItem responseBySSCO) {
        logger.info("Not used");
    }

    public RequestFromSsco getRequest() {
        return request;
    }

    public void setRequest(RequestFromSsco request) {
        this.request = request;
    }

    public SscoMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(SscoMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public SscoPosManager getManager() {
        return manager;
    }

    public void setManager(SscoPosManager manager) {
        this.manager = manager;
    }

    @Override
    public String addEndResponse() {
        return "EndProcess";
    }
}
