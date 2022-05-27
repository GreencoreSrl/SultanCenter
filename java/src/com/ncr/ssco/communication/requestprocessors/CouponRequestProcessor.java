package com.ncr.ssco.communication.requestprocessors;

import com.ncr.Itemdata;
import com.ncr.WsLoyaltyService;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import com.ncr.wsLoyalty.data.CouponCode;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CouponRequestProcessor extends ItemRequestProcessor {
    private static final Logger logger = Logger.getLogger(CouponRequestProcessor.class);
    private Properties processorProperties;

    public CouponRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
        processorProperties = messageHandler.getProcessorsProperties();
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        String upc = requestFromSsco.getStringField("UPC");
        int scanned = 0;
        try {
            scanned = requestFromSsco.getIntField("Scanned");
        } catch (NullPointerException e) {
            logger.warn("Field Scanned missing");
        }
        setItemRequest(new SscoItem(upc, scanned, "", 0, "", 0, 1));
        if (!isCoupon(upc)) {
            logger.warn("Not a coupon");
            sendResponses(new SscoError(SscoError.ERROR));
        } else {
            if (!getManager().itemRequest(getItemRequest())) logger.warn("-- Warning ");
        }

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        if (sscoError.getCode() != SscoError.OK) {
            logger.info("sendCouponException (1): " + sscoError.getMessage() + " code: " + sscoError.getCode());
            sendCouponException(sscoError);
            getMessageHandler().getResponses().add(addEndResponse());
        } else {
            if (!getManager().transactionHasStarted()) {
                sendStartTransaction();
            } else {
                if (sscoError.getCode() != SscoError.OK) {
                    logger.info("sendCouponException (2): " + sscoError.getMessage() + " code: " + sscoError.getCode());
                    sendCouponException(sscoError);
                } else {
                    sendCouponResponse();
                    syncPromotions(getItemResponse());
                    sendTotalsResponse(sscoError);
                }
                getMessageHandler().getResponses().add(addEndResponse());
            }
        }
        logger.debug("Exit");
    }

    private void sendCouponResponse() {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Coupon");
        responseToSsco.setStringField("UPC", getItemResponse().getUpc().trim());
        responseToSsco.setStringField("Description", getItemResponse().getDescription().trim());
        responseToSsco.setIntField("Amount", getItemResponse().getPrice());
        responseToSsco.setIntField("ItemNumber", getItemResponse().getItemNumber());
        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    private void sendCouponException(SscoError sscoError) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("CouponException");
        responseToSsco.setStringField("UPC", getItemRequest().getUpc());
        responseToSsco.setIntField("ExceptionType", 0);
        responseToSsco.setIntField("ExceptionId", 1);
        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    private boolean isCoupon(String upc) {
        for (CouponCode couponCode : getCouponCodes()) {
            if (upc.startsWith(couponCode.getCode()) && upc.length() >= couponCode.getRangeMin() && upc.length() <= couponCode.getRangeMax()) {
                return true;
            }
        }
        return false;
    }

    private List<CouponCode> getCouponCodes () {
        logger.debug("Enter");
        List<CouponCode> property = new ArrayList<CouponCode>();
        try {
            for (int index = 1; index < 99; index++) {
                String value = processorProperties.getProperty("CouponEan." + index, ";");
                if (value.length() > 0) {
                    String[] tokens = value.split(";");
                    if (tokens[1].length() > 0) {
                        property.add(new CouponCode(tokens[0],
                                Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), tokens[3].toLowerCase().equals("true")));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }
}