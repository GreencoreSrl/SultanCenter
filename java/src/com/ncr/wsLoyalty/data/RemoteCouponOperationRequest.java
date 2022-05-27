package com.ncr.wsLoyalty.data;

import java.util.Date;

public class RemoteCouponOperationRequest {
    private String action;
    private String billId;
    private String customerId;
    private String store;
    private String tillId;
    private Date timestamp;
    private String voucherCode;
    private String offline;

    public RemoteCouponOperationRequest() {
    }

    public RemoteCouponOperationRequest(String action, String billId, String customerId, String store, String tillId, Date timestamp, String voucherCode) {
        this.action = action;
        this.billId = billId;
        this.customerId = customerId;
        this.store = store;
        this.tillId = tillId;
        this.timestamp = timestamp;
        this.voucherCode = voucherCode;
        this.offline = " ";
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getTillId() {
        return tillId;
    }

    public void setTillId(String tillId) {
        this.tillId = tillId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getOffline() {
        return offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }

    @Override
    public String toString() {
        return "RemoteCouponOperationRequest{" +
                "action='" + action + '\'' +
                ", billId='" + billId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", store='" + store + '\'' +
                ", tillId='" + tillId + '\'' +
                ", timestamp=" + timestamp +
                ", voucherCode='" + voucherCode + '\'' +
                ", offline='" + offline + '\'' +
                '}';
    }
}
