package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

public class GetPinRequest {
    @SerializedName("RECHARGE_TYPE")
    private String rechargeType;
    @SerializedName("API_KEY")
    private String apiKey;
    @SerializedName("VOLUME")
    private String volume;

    public GetPinRequest(String rechargeType, String apiKey, String volume) {
        this.rechargeType = rechargeType;
        this.apiKey = apiKey;
        this.volume = volume;
    }

    public String getRechargeType() {
        return rechargeType;
    }

    public void setRechargeType(String rechargeType) {
        this.rechargeType = rechargeType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "GetPinRequest{" +
                "rechargeType='" + rechargeType + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", volume='" + volume + '\'' +
                '}';
    }
}
