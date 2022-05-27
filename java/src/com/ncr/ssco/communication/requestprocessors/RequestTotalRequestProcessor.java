package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.TenderType;
import com.ncr.ssco.communication.entities.TenderTypeEnum;
import com.ncr.ssco.communication.entities.pos.SscoCashier;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoTender;
import com.ncr.ssco.communication.entities.pos.SscoTotalAmount;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.manager.TenderTypeManager;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class RequestTotalRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(RequestTotalRequestProcessor.class);
    private SscoTender sscoTender;

    public RequestTotalRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.info("Enter");
        String tenderType = requestFromSsco.getStringField("TenderType").replaceAll(" ", "");
        sscoTender = new SscoTender(TenderTypeEnum.valueOf(tenderType.length() > 0 ? tenderType : "None"), 0);
        if (TenderTypeManager.getInstance().getActionPOSByName(sscoTender.getTenderType()).isRounded()) {
            getManager().tenderInquiryRequest(sscoTender);
        } else {
            sendResponses(new SscoError());
        }
        logger.info("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.info("Enter");

        sendTotalsResponse();
        getMessageHandler().getResponses().add(addEndResponse());
        logger.info("Exit");
    }

    protected void sendTotalsResponse() {
        logger.info("Enter");
        boolean rounded = false;
        int rounding = 0;

        TenderType tenderType = TenderTypeManager.getInstance().getActionPOSByName(sscoTender.getTenderType());
        SscoTotalAmount totalAmount = getManager().getTotalsAmount();

        int balance = totalAmount.getBalanceDue();
        int amount = totalAmount.getTotalAmount();
        logger.info("Balance: " + balance + " Amount: " + amount + " Tender Rounding: " + getManager().getTenderRounding());
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Totals");
        if (tenderType != null) {
            rounded = tenderType.isRounded();
            if (rounded) {
                rounding = getManager().getTenderRounding();
            }
        }
        responseToSsco.setIntField("RoundedAmount", amount - rounding);
        responseToSsco.setIntField("RoundedBalanceDue", balance - rounding);
        responseToSsco.setIntField("DisplayRoundedTotals", rounded ? 1 : 0);
        responseToSsco.setIntField("BalanceDue", balance);
        responseToSsco.setIntField("TotalAmount", amount);

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.info("Exit");
    }
}
