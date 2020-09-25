package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

public class OperandDetails {
    @SerializedName("OPERAND")
    private String operand;
    @SerializedName("VOICE_PRICE")
    private String voicePrice;
    @SerializedName("VOICE_SST_RATE")
    private String voiceSstRate;
    @SerializedName("VOICE_GST_RATE")
    private String voiceGstRate;
    @SerializedName("DATA_PRICE")
    private String dataPrice;
    @SerializedName("DATA_SST_RATE")
    private String dataSstRate;
    @SerializedName("DATA_GST_RATE")
    private String dataGstRate;
    @SerializedName("VOICE_SST_AMOUNT")
    private String voiceSstAmount;
    @SerializedName("VOICE_GST_AMOUNT")
    private String voiceGstAmount;
    @SerializedName("DATA_SST_AMOUNT")
    private String dataSstAmount;
    @SerializedName("DATA_GST_AMOUNT")
    private String dataGstAmount;
    @SerializedName("TOTAL")
    private String total;

    public OperandDetails() {
    }

    public String getOperand() {
        return operand;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public String getVoicePrice() {
        return voicePrice;
    }

    public void setVoicePrice(String voicePrice) {
        this.voicePrice = voicePrice;
    }

    public String getVoiceSstRate() {
        return voiceSstRate;
    }

    public void setVoiceSstRate(String voiceSstRate) {
        this.voiceSstRate = voiceSstRate;
    }

    public String getVoiceGstRate() {
        return voiceGstRate;
    }

    public void setVoiceGstRate(String voiceGstRate) {
        this.voiceGstRate = voiceGstRate;
    }

    public String getDataPrice() {
        return dataPrice;
    }

    public void setDataPrice(String dataPrice) {
        this.dataPrice = dataPrice;
    }

    public String getDataSstRate() {
        return dataSstRate;
    }

    public void setDataSstRate(String dataSstRate) {
        this.dataSstRate = dataSstRate;
    }

    public String getDataGstRate() {
        return dataGstRate;
    }

    public void setDataGstRate(String dataGstRate) {
        this.dataGstRate = dataGstRate;
    }

    public String getVoiceSstAmount() {
        return voiceSstAmount;
    }

    public void setVoiceSstAmount(String voiceSstAmount) {
        this.voiceSstAmount = voiceSstAmount;
    }

    public String getVoiceGstAmount() {
        return voiceGstAmount;
    }

    public void setVoiceGstAmount(String voiceGstAmount) {
        this.voiceGstAmount = voiceGstAmount;
    }

    public String getDataSstAmount() {
        return dataSstAmount;
    }

    public void setDataSstAmount(String dataSstAmount) {
        this.dataSstAmount = dataSstAmount;
    }

    public String getDataGstAmount() {
        return dataGstAmount;
    }

    public void setDataGstAmount(String dataGstAmount) {
        this.dataGstAmount = dataGstAmount;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "OperandDetails{" +
                "operand='" + operand + '\'' +
                ", voicePrice='" + voicePrice + '\'' +
                ", voiceSstRate='" + voiceSstRate + '\'' +
                ", voiceGstRate='" + voiceGstRate + '\'' +
                ", dataPrice='" + dataPrice + '\'' +
                ", dataSstRate='" + dataSstRate + '\'' +
                ", dataGstRate='" + dataGstRate + '\'' +
                ", voiceSstAmount='" + voiceSstAmount + '\'' +
                ", voiceGstAmount='" + voiceGstAmount + '\'' +
                ", dataSstAmount='" + dataSstAmount + '\'' +
                ", dataGstAmount='" + dataGstAmount + '\'' +
                ", total='" + total + '\'' +
                '}';
    }
}
