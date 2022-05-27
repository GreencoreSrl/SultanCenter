package com.ncr.giftcard;

import com.ncr.Itemdata;
import com.ncr.Terminal;
import com.ncr.Transact;
import com.ncr.psh.data.CustomTender;

//INTEGRATION-PHILOSHOPIC-CGA#A BEG
public interface GiftCardPluginInterface {
    String LEGACY_TENDER_ID = "1";
    String PSH_TENDER_ID = "L";
    String OLGB_TENDER_ID = "S";
    String GIFTCARD = "GIFTCARD";
    String LOYALTY = "LOYALTY";
    String CREDIT = "CREDIT";
    String DEBIT = "DEBIT";
    String REPLY_OK_VALUE = "0";
    int STATUS_SUCCESS = 200;
    int MNEMO_ERROR_BASE = 86;
    int MNEMO_MNEMO_BASE = 89;
    int GIFT_EMPTY = 103;
    int MNEMO_DIAGS_BASE = 20;
    int SYNCERROR_BUY = 0;
    int SYNCERROR_TOPUP = 1;
    int SYNCERROR_PAY = 2;
    int SYNCERROR_POINTS = 3;


    int activationGiftCard(Itemdata itm, Terminal ctl);
    int reloadGiftCard(Itemdata itm);
    int redemptionGiftCard(Itemdata itm, Transact tra);
    int reconciliationGiftCard();
    int confirmTransaction(Itemdata itm);
    int cancelTransaction(Itemdata itm,String transactionType);
    int cancelTransaction(Itemdata itm);
    int cancelGiftCard(Itemdata itm);
    int cancelAll();
    String getTenderId();
    boolean isGiftCard(Itemdata itm);
    boolean isEnabled();
    int cancelRedemption(Itemdata itm);
    int readSerial32(Itemdata itm);
    CustomTender getCustomTender(int tenderId);
    int confirmAllGiftCard();
    void resetAll();
}
//INTEGRATION-PHILOSHOPIC-CGA#A END
