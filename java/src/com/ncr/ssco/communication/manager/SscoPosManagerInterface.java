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
    boolean tenderRequest(SscoTender sscoTender);
    boolean signOnRequest(SscoCashier sscoCashier);
    boolean signOffRequest();
    boolean shuttingDownRequest();
    boolean itemRequest(SscoItem item);
    boolean voidTransactionRequest(String id);
    boolean suspendTransactionRequest(String id);
    boolean enterTenderModeRequest();
    boolean exitTenderModeRequest();
    boolean voidItemRequest(SscoItem item);
    boolean loyaltyRequest(SscoCustomer sscoCustomer);
    boolean airMilesRequest(SscoCustomer sscoCustomer);
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
}
