package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.entities.pos.SscoItemPromotion;
import com.ncr.ssco.communication.entities.pos.SscoTotalAmount;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.manager.SscoStateManager;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TransactionProcessor extends DefaultRequestProcessor {
    protected static final Logger logger = Logger.getLogger(TransactionProcessor.class);
    protected final static int ITEM_ASSOCIATED_DELTA = 1000000;

    protected TransactionProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    protected void sendTotalsResponse(SscoError sscoError) {
        logger.debug("Enter");

        SscoTotalAmount totalAmount = getManager().getTotalsAmount();

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("Totals");
        responseToSsco.setIntField("BalanceDue", totalAmount.getBalanceDue());
        responseToSsco.setIntField("ItemCount", totalAmount.getItemCount());
        responseToSsco.setIntField("TotalAmount", totalAmount.getTotalAmount());

        if (totalAmount.getChangeDue() > 0) {
            responseToSsco.setIntField("ChangeDue", totalAmount.getChangeDue());
        }

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    protected void sendEndTransactionResponseToSsco(String idTransaction) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("EndTransaction");
        responseToSsco.setStringField("Id", idTransaction);

        getManager().endTransaction();
        SscoStateManager.getInstance().setFutureState("Open");
        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    protected void sendTransactionExceptionResponseToSsco(int type, int id, String message) {
        logger.debug("Enter");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("TransactionException");
        responseToSsco.setStringField("Message.1", message);
        responseToSsco.setIntField("ExceptionType", type);
        responseToSsco.setIntField("ExceptionId", id);

        getMessageHandler().sendResponseToSsco(responseToSsco, false);

        logger.debug("Exit");
    }

    public void syncPromotions(SscoItem itemResponse) {
        logger.debug("Enter");

        List<SscoItemPromotion> promotions = getManager().getTransaction().getPromotions();
        List<SscoItemPromotion> cursor = new ArrayList<SscoItemPromotion>(promotions);
        for (SscoItemPromotion promo : cursor) {
            if (!promo.isPromoInviata()) {
                promo.setAssociatedItemNumber(itemResponse.getItemNumber());
                if (promo.getDiscountAmount() > 0) {
                    ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemSold");

                    responseToSsco.setIntField("ItemNumber", promo.getItemNumber());
                    responseToSsco.setIntField("DiscountAmount", promo.getDiscountAmount());
                    responseToSsco.setIntField("AssociatedItemNumber", promo.getAssociatedItemNumber());
                    responseToSsco.setStringField("DiscountDescription.1", promo.getDiscountDescription1());
                    responseToSsco.setIntField("ShowRewardPoints", promo.getShowRewardPoints());
                    responseToSsco.setIntField("RewardLocation", promo.getRewardLocation());

                    getMessageHandler().sendResponseToSsco(responseToSsco);
                    promo.setPromoInviata(true);
                    logger.info("PROMOTION ADDED");
                    logger.info("ItemNumber: " + promo.getItemNumber());
                    logger.info("DiscountAmount: " + promo.getDiscountAmount());
                    logger.info("AssociatedItemNumber: " + promo.getAssociatedItemNumber());
                    logger.info("DiscountDescription.1: " + promo.getDiscountDescription1());
                    logger.info("ShowRewardPoints: " + promo.getShowRewardPoints());
                    logger.info("RewardLocation: " + promo.getRewardLocation());
                } else {
                    logger.info("PROMOTION REVERSAL ");
                    logger.info("ItemNumber: " + promo.getItemNumber());
                    logger.info("DiscountAmount: " + promo.getDiscountAmount());
                    logger.info("AssociatedItemNumber: " + promo.getAssociatedItemNumber());
                    logger.info("DiscountDescription.1: " + promo.getDiscountDescription1());

                    boolean found = false;
                    for (SscoItemPromotion promoReversal : promotions) {

                        if (promoReversal.getDiscountAmount() == -promo.getDiscountAmount()) {
                            if (promoReversal.getDiscountDescription1().equals(promo.getDiscountDescription1())) {
                                //if (promoReversal.getItem().index == promo.getItem().index) {
                                if (promoReversal.getAssociatedItemNumber() == promo.getAssociatedItemNumber()) {
                                    logger.info("PROMOTION REVERSAL FOUND -- same entryId = " + promo.getItem().index);
                                    ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemVoided");
                                    responseToSsco.setStringField("UPC", "0");
                                    responseToSsco.setIntField("ItemNumber", promoReversal.getItemNumber());
                                    getMessageHandler().sendResponseToSsco(responseToSsco);

                                    promotions.remove(promo);
                                    promotions.remove(promoReversal);
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!found) {
                        logger.warn(" -- WARNING -- ");
                        logger.warn("PROMOTION REVERSAL without matching entry id ... searching a match");
                        for (SscoItemPromotion promoReversal : promotions) {
                            if (promoReversal.getDiscountAmount() == -promo.getDiscountAmount()) {
                                if (promoReversal.getDiscountDescription1().equals(promo.getDiscountDescription1())) {
                                    logger.info("PROMOTION REVERSAL FOUND but with different entry id");
                                    ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemVoided");
                                    responseToSsco.setStringField("UPC", "0");
                                    responseToSsco.setIntField("ItemNumber", promoReversal.getItemNumber());
                                    getMessageHandler().sendResponseToSsco(responseToSsco);

                                    promotions.remove(promo);
                                    promotions.remove(promoReversal);
                                    break;
                                }
                            }
                        }
                        logger.info(" -- WARNING -- ");
                    }
                }
            }
        }
        logger.debug("Exit");
    }
}