package com.ncr.ssco.communication.manager;

import com.ncr.ssco.communication.entities.pos.*;

import java.awt.*;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public interface SscoPosManagerInterface {
    SscoTransaction startTransaction(int id);
    void endTransaction();
    void initialize (Component component, EventQueue queue, String reg);

    // Requests
    void tenderRequest(SscoTender sscoTender);
    boolean signOnRequest(SscoCashier sscoCashier);
    boolean signOffRequest();
    boolean shuttingDownRequest();
    boolean itemRequest(SscoItem item);
    void voidTransactionRequest(String id);
    void suspendTransactionRequest(String id);
    void enterTenderModeRequest();
    void exitTenderModeRequest();
    void voidItemRequest(SscoItem item);
    void loyaltyRequest(SscoCustomer sscoCustomer);
    void airMilesRequest(SscoCustomer sscoCustomer);
    void tenderInquiryRequest(SscoTender sscoTender);
    void enterExitTrainingModeRequest();
    void clearRequest();
    boolean eftSettleRequest();   //EFT-SETTLE-CGA#A

    // Responses
    void tenderResponse();
    void signOnResponse();
    void signOffResponse();
    void shuttingDownResponse();
    void itemResponse();
    void voidTransactionResponse();
    void suspendTransactionResponse();
    void enterTenderModeResponse();
    void exitTenderModeResponse();
    void voidItemResponse();
    void loyaltyResponse();
    void eftSettleResponse();   //EFT-SETTLE-CGA#A
    void tenderSelectedResponse();
    void enterExitTrainingModeResponse();
}
