package com.ncr.ssco.communication.entities.pos;

import com.ncr.ssco.communication.entities.DataNeeded;

import java.util.ArrayList;
import java.util.List;
//import java.util.stream.IntStream;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public class SscoTransaction {

    private int transactionId;
    private String type;
    private SscoTotalAmount totalAmount = new SscoTotalAmount();
    private int balance;
    private int changeDue;
    private String savingsAmount = "";
    private List<SscoItem> items = null;
    private List<SscoTender> tenders = null;
    private SscoCustomer customer = null;
    private List<SscoItemPromotion> promotions = new ArrayList<SscoItemPromotion>();
    private static int contItemNumber = 0;

    public SscoTransaction(int transactionId) {
        this.transactionId = transactionId;
        items = new ArrayList<SscoItem>();
        promotions = new ArrayList<SscoItemPromotion>();
        tenders = new ArrayList<SscoTender>();
        totalAmount = new SscoTotalAmount();
    }

    public SscoTransaction(){
    }

    public int addItemNumber(){
        return ++contItemNumber;
    }

    public int getLastItemNumber(){
        return contItemNumber;
    }

    public void resetItemNumber(){
        contItemNumber = 0;
    }

    public List<SscoItemPromotion> getPromotions() {
        return promotions;
    }

    public void calTotalAmount(){
        int amount = 0;

        for( SscoItem item : getItems() )
            amount += item.getPrice();

        this.totalAmount.setTotalAmount(amount);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public SscoTotalAmount getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(SscoTotalAmount totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getChangeDue() {
        return changeDue;
    }

    public void setChangeDue(int changeDue) {
        this.changeDue = changeDue;
    }

    public List<SscoItem> getItems() {
        return items;
    }

    public void setItems(List<SscoItem> items) {
        this.items = items;
    }

    public List<SscoTender> getTenders() {
        return tenders;
    }

    public void setTenders(List<SscoTender> tenders) {
        this.tenders = tenders;
    }

    public String getSavingsAmount() {
        return savingsAmount;
    }

    public void setSavingsAmount(String savingsAmount) {
        this.savingsAmount = savingsAmount;
    }

    public SscoCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(SscoCustomer customer) {
        this.customer = customer;
    }
}
