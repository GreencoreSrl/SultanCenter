package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.AdditionalProcessType;
import com.ncr.ssco.communication.entities.pos.SscoCustomer;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;

public class VoidItemRequestProcessor extends TransactionProcessor {
    private SscoItem itemRequest;
    private AdditionalProcessType additionalProcessType = AdditionalProcessType.NONE;

    public VoidItemRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void additionalProcess() {
        logger.debug("Enter");
        String info = "";
        String key = "ENTER";

        if (additionalProcessType == AdditionalProcessType.DEPT) {
            info = "" + this.itemRequest.getPrice();
        } else if (additionalProcessType == AdditionalProcessType.QTY) {
            info = "" + this.itemRequest.getUpc();
        } else if (additionalProcessType == AdditionalProcessType.PRICE) {
            if (itemRequest.getPrice() > 0) {
                info = "" + itemRequest.getPrice();
            } else {
                key = "CLEAR";
                sendResponses(new SscoError(SscoError.ZERO_PRICED, "Missing Price"));
            }
        }
        getManager().sendAdditionalProcess(info, key);
        additionalProcessType = AdditionalProcessType.NONE;
        logger.debug("Exit");
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        String upc = requestFromSsco.getStringField("UPC");
        int itemNumber = requestFromSsco.getIntField("ItemNumber");
        Integer qta = requestFromSsco.getIntField("Quantity");
        Integer scanned = requestFromSsco.getIntField("Scanned");

        if (itemNumber > ITEM_ASSOCIATED_DELTA) {
            logger.debug("itemNumber before : " + itemNumber);
            itemNumber -= ITEM_ASSOCIATED_DELTA;
            logger.debug("itemNumber after : " + itemNumber);
        }
        itemRequest = getManager().findItembyItemNumber(itemNumber);

        if (itemRequest == null) {
            sendItemVoidedExceptionResponseToSsco(new SscoError());
            getMessageHandler().getResponses().add(addEndResponse());
            return;
        }

        logger.info("UPC: " + itemRequest.getUpc());
        logger.info("itemNumber: " + itemRequest.getItemNumber());
        logger.info("qta: " + qta);
        logger.info("scanned: " + scanned);

        getManager().voidItemRequest(itemRequest);

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        if (sscoError.getCode() == SscoError.OK) {
            syncPromotions(itemRequest);
            sendItemVoidedResponseToSsco(itemRequest, 0);
            sendTotalsResponse(sscoError);
        } else {
            sendItemVoidedExceptionResponseToSsco(sscoError);
        }
        getMessageHandler().getResponses().add(addEndResponse());

        logger.debug("Exit");
    }

    private void sendItemVoidedExceptionResponseToSsco(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemVoidException");

        responseToSsco.setIntField("ExceptionType", 0);
        responseToSsco.setIntField("ExceptionId", 0);
        if (sscoError.getMessage().length() > 0) {
            responseToSsco.setStringField("Message.1", sscoError.getMessage());
        }

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    private void sendItemVoidedResponseToSsco(SscoItem itemRequest, int delta) {
        logger.debug("Enter - delta: " + delta);

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemVoided");

        responseToSsco.setStringField("UPC", itemRequest.getUpc());
        responseToSsco.setIntField("ItemNumber", itemRequest.getItemNumber() + delta);

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    public void setAdditionalProcessType(AdditionalProcessType additionalProcessType) {
        this.additionalProcessType = additionalProcessType;
    }
}