package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

/**
 * Created by Umberto on 09/05/2017.
 */
public class PickupRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(InitializeRequestProcessor.class);
    private int amount=0;
    private String denomination;
    private String currency;
    private String count;

    public PickupRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter process");

        amount = requestFromSsco.getIntField("Amount");
        denomination = requestFromSsco.getStringField("Denomination.1");
        currency = requestFromSsco.getStringField("Currency.1");
        count = requestFromSsco.getStringField("Count.1");

        logger.info("Pickup Response -- START ");
        logger.info("- amount: " + amount);
        logger.info("- denomination: " + denomination);
        logger.info("- currency: " + currency);
        logger.info("- count: " + count);
        logger.info("Pickup Response -- END ");

        sendResponses(new SscoError());
        logger.debug("Exit process");
    }

    @Override
    public void additionalProcess() {
        logger.info("EnterExit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Pickup");
        responseToSsco.setIntField("Amount", this.amount);
        responseToSsco.setStringField("Message.1", "prova");
        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDenomination() {
        return denomination;
    }

    public void setDenomination(String denomination) {
        this.denomination = denomination;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }
}
