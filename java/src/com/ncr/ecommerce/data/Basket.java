package com.ncr.ecommerce.data;

import java.util.ArrayList;
import java.util.List;

public class Basket {
    public static final String SALE = "SALE";
    public static final String RETURN = "RETURN";
    private String basketID;
    private String status;
    private String terminalID;
    private String type;
    private String receipt;
    private Double totalAmount;
    private List<Item> items;
    private List<Item> soldItems;
    private List<Item> notSoldItems;

    public Basket()
    {}

    public Basket(String basketID, String status, String terminalID, String type, String receipt, Double totalAmount, ArrayList<Item> items, ArrayList<Item> soldItems, ArrayList<Item> notSoldItems) {
        this.basketID = basketID;
        this.status = status;
        this.terminalID = terminalID;
        this.type = type;
        this.receipt = receipt;
        this.totalAmount = totalAmount;
        this.items = items;
        this.soldItems = soldItems;
        this.notSoldItems = notSoldItems;
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
        this.status = status;
    }

    public String getTerminalID() {
        return terminalID;
    }

    public void setTerminalID(String terminalID) {
        this.terminalID = terminalID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
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
}
