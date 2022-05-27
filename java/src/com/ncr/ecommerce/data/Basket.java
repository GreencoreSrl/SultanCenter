package com.ncr.ecommerce.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Basket {
    public static final String SALE = "Sale";
    public static final String RETURN = "Return";
    private String basketID;
    private String status;
    private String type;
    private String customerID;
    private String terminalID;
    private int earnedLoyaltyPoints;
    private String transactionID;
    private String barcodeID;
    private String receipt;
    private BigDecimal totalAmount;
    private List<Item> items;
    private List<Item> soldItems;
    private List<Item> notSoldItems;
    private String tenderType;
    private String tenderId;
    private int errorCode;

    public Basket()
    {
    }

    public Basket(String basketID, String status, String customerID, String terminalID, String type, int earnedLoyaltyPoints, String transactionID, String barcodeID, String receipt, BigDecimal totalAmount, ArrayList<Item> items, ArrayList<Item> soldItems, ArrayList<Item> notSoldItems, String tenderType, String tenderId, int errorCode) {
        this.basketID = basketID;
        this.status = status;
        this.customerID = customerID;
        this.terminalID = terminalID;
        this.type = type;
        this.earnedLoyaltyPoints = earnedLoyaltyPoints;
        this.transactionID = transactionID;
        this.barcodeID = barcodeID;
        this.receipt = receipt;
        this.totalAmount = totalAmount;
        this.items = items;
        this.soldItems = soldItems;
        this.notSoldItems = notSoldItems;
        this.tenderType = tenderType;
        this.tenderId=tenderId;
        this.errorCode = errorCode;
    }

    public String getBasketID() {
        return basketID;
    }

    public void setBasketID(String basketID) {
        this.basketID = basketID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if(status==null)
            status="";
        this.status = status;
    }

    public String getTerminalID() {
        return terminalID;
    }

    public void setTerminalID(String terminalID) {
        if(terminalID==null)
            terminalID="";
        this.terminalID = terminalID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if(type==null)
            type="";
        this.type = type;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        if(receipt==null)
            receipt="";
        this.receipt = receipt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        if(transactionID==null)
            transactionID="";
        this.transactionID = transactionID;
    }

    public String getBarcodeID() {
        return barcodeID;
    }

    public void setBarcodeID(String barcodeID) {
        if(barcodeID==null)
            barcodeID="";
        this.barcodeID = barcodeID;
    }

    public String getTenderType() {
        return tenderType;
    }

    public void setTenderType(String tenderType) {
        this.tenderType = tenderType;
    }

    public String getTenderId() {
        return tenderId;
    }

    public void setTenderId(String tenderId) {
        if(tenderId==null)
            tenderId="";
        this.tenderId = tenderId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Item> getSoldItems() {
        return soldItems;
    }

    public void setSoldItems(List<Item> soldItems) {
        this.soldItems = soldItems;
    }

    public List<Item> getNotSoldItems() {
        return notSoldItems;
    }

    public void setNotSoldItems(List<Item> notSoldItems) {
        this.notSoldItems = notSoldItems;
    }

    public void update(Item item, boolean notSold) {
        if (notSold) {
            notSoldItems.add(item);
        } else {
            soldItems.add(item);
        }
    }

    public void reset() {
        notSoldItems = new ArrayList<Item>();
        soldItems = new ArrayList<Item>();
        transactionID = "";
        totalAmount = BigDecimal.valueOf(0);
        receipt = "";
    }

    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }

    public int getEarnedLoyaltyPoints() {
        return earnedLoyaltyPoints;
    }

    public void setEarnedLoyaltyPoints(int earnedLoyaltyPoints) {
        this.earnedLoyaltyPoints = earnedLoyaltyPoints;
    }
}
