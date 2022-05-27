package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DenominationsResponse {
    @SerializedName("ERROR_CODE")
    private String errorCode;
    @SerializedName("ERROR_DESC")
    private String errorDesc;
    @SerializedName("DETAILS")
    private List<OperandDetails> details = new ArrayList<OperandDetails>();

    public DenominationsResponse() {}

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public List<OperandDetails> getDetails() {
        return details;
    }

    public void setDetails(List<OperandDetails> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "DenominationsResponse{" +
                "errorCode='" + errorCode + '\'' +
                ", errorDesc='" + errorDesc + '\'' +
                ", details=" + details +
                '}';
    }
}
