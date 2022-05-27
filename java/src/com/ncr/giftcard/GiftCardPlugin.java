package com.ncr.giftcard;


import com.ncr.Itemdata;
import com.ncr.Terminal;
import com.ncr.Transact;
import com.ncr.psh.data.CustomTender;

//INTEGRATION-PHILOSHOPIC-CGA#A BEG
public class GiftCardPlugin implements GiftCardPluginInterface {
    public static boolean readingGCSerial = false;



    @Override
    public int activationGiftCard(Itemdata itm, Terminal ctl) {
        return 0;
    }

    @Override
    public int reloadGiftCard(Itemdata itm) {
        return 0;
    }

    @Override
    public int redemptionGiftCard(Itemdata itm, Transact tra) {
        return 0;
    }

    @Override
    public int reconciliationGiftCard() {
        return 0;
    }

    @Override
    public int confirmTransaction(Itemdata itm) {
        return 0;
    }

    @Override
    public int cancelTransaction(Itemdata itm, String transactionType) {
        return 0;
    }

    @Override
    public int cancelTransaction(Itemdata itm) {
        return 0;
    }

    @Override
    public int cancelGiftCard(Itemdata itm) {
        return 0;
    }

    @Override
    public int cancelAll() {
        return 0;
    }

    @Override
    public String getTenderId() {
        return null;
    }

    @Override
    public boolean isGiftCard(Itemdata itm) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

   /* @Override
    public int payGiftCard(Itemdata itm, Transact tra) {
        return 0;
    }*/


    @Override
    public int cancelRedemption(Itemdata itm) {
        return 0;
    }

    @Override
    public int readSerial32(Itemdata plu) {
        return 0;
    }

    @Override
    public CustomTender getCustomTender(int tenderId) {
        return null;
    }

    @Override
    public int confirmAllGiftCard() {
        return 0;
    }

    @Override
    public void resetAll() {

    }
}
//INTEGRATION-PHILOSHOPIC-CGA#A END
