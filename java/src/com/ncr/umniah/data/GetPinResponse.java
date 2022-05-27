package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

public class GetPinResponse {
    @SerializedName("PIN")
    private String pin;
    @SerializedName("TRANSACTION_ID")
    private String transactionId;
    @SerializedName("FACE_VALUE")
    private String faceValue;
    @SerializedName("SALES_TAX")
    private String salesTax;
    @SerializedName("SPECIAL_TAX")
    private String specialTax;
    @SerializedName("MACHINE_CHARGE")
    private String machineCharge;
    @SerializedName("MACHINE_TAX")
    private String machineTax;
    @SerializedName("TOTAL")
    private String total;
    @SerializedName("ERROR_CODE")
    private String errorCode;
    @SerializedName("ERROR_DESC")
    private String errorDesc;

    public GetPinResponse() {
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(String faceValue) {
        this.faceValue = faceValue;
    }

    public String getSalesTax() {
        return salesTax;
    }

    public void setSalesTax(String salesTax) {
        this.salesTax = salesTax;
    }

    public String getSpecialTax() {
        return specialTax;
    }

    public void setSpecialTax(String specialTax) {
        this.specialTax = specialTax;
    }

    public String getMachineCharge() {
        return machineCharge;
    }

    public void setMachineCharge(String machineCharge) {
        this.machineCharge = machineCharge;
    }

    public String getMachineTax() {
        return machineTax;
    }

    public void setMachineTax(String machineTax) {
        this.machineTax = machineTax;
    }

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

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "GetPinResponse{" +
                "pin='" + pin + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", faceValue='" + faceValue + '\'' +
                ", salesTax='" + salesTax + '\'' +
                ", specialTax='" + specialTax + '\'' +
                ", machineCharge='" + machineCharge + '\'' +
                ", machineTax='" + machineTax + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorDesc='" + errorDesc + '\'' +
                ", total='" + total + '\'' +
                '}';
    }
}
