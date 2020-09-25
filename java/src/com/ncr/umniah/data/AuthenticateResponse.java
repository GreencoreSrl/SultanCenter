package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by User on 19/01/2018.
 */


public class AuthenticateResponse {
    @SerializedName("Success")
    private boolean success;
    @SerializedName("Error")
    private ErrorMessage error;
    @SerializedName("Data")
    private DataMessage data;
    @SerializedName("Security")
    private String security;

    public AuthenticateResponse() {}

    public AuthenticateResponse(boolean success, ErrorMessage error, DataMessage data, String security) {
        this.success = success;
        this.error = error;
        this.data = data;
        this.security = security;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorMessage getError() {
        return error;
    }

    public void setError(ErrorMessage error) {
        this.error = error;
    }

    public DataMessage getData() {
        return data;
    }

    public void setData(DataMessage data) {
        this.data = data;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }
}
