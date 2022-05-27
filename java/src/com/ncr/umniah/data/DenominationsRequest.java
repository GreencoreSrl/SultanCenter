package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

public class DenominationsRequest {
    @SerializedName("BILLING_NO")
    private String billingNo;
    @SerializedName("API_KEY")
    private String apiKey;
    @SerializedName("RECHARGE_TYPE")
    private String rechargeType;
    @SerializedName("LANGUAGE")
    private String language;
    @SerializedName("CHANNEL")
    private String channel;

    public DenominationsRequest(String billingNo, String apiKey, String rechargeType, String language, String channel) {
        this.billingNo = billingNo;
        this.apiKey = apiKey;
        this.rechargeType = rechargeType;
        this.language = language;
        this.channel = channel;
    }

    public String getBillingNo() {
        return billingNo;
    }

    public void setBillingNo(String billingNo) {
        this.billingNo = billingNo;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRechargeType() {
        return rechargeType;
    }

    public void setRechargeType(String rechargeType) {
        this.rechargeType = rechargeType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "DenominationsRequest{" +
                "billingNo='" + billingNo + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", rechargeType='" + rechargeType + '\'' +
                ", language='" + language + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }
}
