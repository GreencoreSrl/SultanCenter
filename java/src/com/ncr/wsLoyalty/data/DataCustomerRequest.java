package com.ncr.wsLoyalty.data;

/**
 * Created by User on 15/12/2017.
 */
public class DataCustomerRequest {
    private String cardCode;
    private String phoneNumber;

    public DataCustomerRequest(String cardCode, String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.cardCode = cardCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }
}
