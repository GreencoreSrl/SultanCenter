package com.ncr.ssco.communication.requestprocessors;

import com.ncr.Itemdata;
import com.ncr.ssco.communication.entities.TenderTypeEnum;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoTender;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;

public class EnterTenderModeRequestProcessor extends TransactionProcessor{

    public EnterTenderModeRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        getManager().enterTenderModeRequest();
        getManager().resetTransactionalRewards();
        logger.debug("Exit");
    }

    private void sendTenderAcceptedResponseToSsco(SscoTender sscoTender) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("TenderAccepted");
        responseToSsco.setIntField("Amount", sscoTender.getAmount());
        responseToSsco.setStringField("TenderType", sscoTender.getTenderType().toString());
        responseToSsco.setStringField("Description", sscoTender.getDescription());

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        int itmno = 10000;
        for (Itemdata itmd : getManager().getTransactionalRewards()) {
            ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemSold");
            responseToSsco.setIntField("ItemNumber", itmno++);
            responseToSsco.setStringField("Description", "Transaction Discount");
            responseToSsco.setIntField("DiscountAmount", (int) -itmd.amt);
            responseToSsco.setStringField("DiscountDescription.1", itmd.text);
            responseToSsco.setIntField("AssociatedItemNumber", 0);
            responseToSsco.setIntField("RewardLocation", 1);
            getMessageHandler().sendResponseToSsco(responseToSsco);
        }

        logger.info("Transactional discounts END");

        if (getManager().getTotalsAmount().getBalanceDue() == 0 && getManager().getTransactionalRewards().size() > 0) {
            SscoTender tender = new SscoTender(TenderTypeEnum.Cash, 0);
            sendTenderAcceptedResponseToSsco(tender);
            sendTotalsResponse(sscoError);
            sendEndTransactionResponseToSsco("1");
        } else {
            sendTotalsResponse(sscoError);
        }
        //getManager().resetTransactionalRewards();

        getMessageHandler().getResponses().add(addEndResponse());
        logger.debug("Exit");
    }
}
