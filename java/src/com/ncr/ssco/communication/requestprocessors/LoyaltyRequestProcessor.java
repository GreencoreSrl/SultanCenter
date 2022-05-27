package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoCustomer;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class LoyaltyRequestProcessor extends ItemRequestProcessor {
    private static final Logger logger = Logger.getLogger(LoyaltyRequestProcessor.class);
    private SscoCustomer sscoCustomer;
    private String cardType = "";
    private String responseName = "";

    public LoyaltyRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        setLoyaltyInfo(new SscoCustomer(
                requestFromSsco.getStringField("AccountNumber"),
                requestFromSsco.getStringField("CountryCode"),
                requestFromSsco.getStringField("EntryMethod"),
                0  //INTEGRATION-PHILOSHOPIC-CGA#A
        ));
        getManager().loyaltyRequest(getSscoCustomer());
        logger.debug("Exit");
    }

    public void setLoyaltyInfo(SscoCustomer sscoCustomer) {
        logger.debug("Enter");

        setSscoCustomer(sscoCustomer);
        setCardType("Loyalty");
        setResponseName("LoyaltyCard");
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        if (!getManager().transactionHasStarted()) {
            sendStartTransaction();
            if (SscoError.OK == sscoError.getCode()) {
                getManager().addCustomer(getSscoCustomer());
            }
        } else {
            sendLoyaltyResponse(mapErrorToStatus(sscoError));
            sendTotalsResponse(sscoError);
            getMessageHandler().getResponses().add(addEndResponse());
        }
        logger.debug("Exit");
    }

    private int mapErrorToStatus(SscoError sscoError) {
        switch (sscoError.getCode()) {
            case SscoError.OK:
                return 1;
            case 117:
                return 2;
            default:
                return 0;
        }
    }

    private void sendLoyaltyResponse(int status) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco(getResponseName());
        responseToSsco.setStringField("AccountNumber", getSscoCustomer().getAccountNumber());
        responseToSsco.setStringField("CardType", getCardType());
        responseToSsco.setIntField("Status", status);

        getMessageHandler().sendResponseToSsco(responseToSsco);
        logger.debug("Exit");
    }

    public SscoCustomer getSscoCustomer() {
        return sscoCustomer;
    }

    public void setSscoCustomer(SscoCustomer sscoCustomer) {
        this.sscoCustomer = sscoCustomer;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getResponseName() {
        return responseName;
    }

    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }
}