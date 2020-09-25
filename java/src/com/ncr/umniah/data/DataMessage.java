package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by User on 19/01/2018.
 */
public class DataMessage {
    @SerializedName("Token")
    private String token;
    @SerializedName("Message")
    private String message;
    @SerializedName("ExpiryDate")
    private String expiryDate;

    public DataMessage(String token, String message, String expiryDate) {
        this.token = token;
        this.message = message;
        this.expiryDate = expiryDate;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}
