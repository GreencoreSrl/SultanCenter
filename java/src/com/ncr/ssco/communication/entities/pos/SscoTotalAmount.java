package com.ncr.ssco.communication.entities.pos;

/**
 * Created by NCRDeveloper on 24/05/2017.
 */
public class SscoTotalAmount {

    private int totalAmount;
    private int foodStampAmount;
    private int taxAmount_A;
    private int balanceDue;
    private int itemCount;
    private int discountAmount;
    private int points;
    private int changeDue;
    private int ticketAmount;
    private int glutenFreeAmount;

    public SscoTotalAmount(Integer totalAmount, Integer foodStampAmount, Integer taxAmount_A, Integer balanceDue, Integer itemCount, Integer discountAmount, Integer points) {
        this.totalAmount = totalAmount == null ? 0 : totalAmount.intValue();
        this.foodStampAmount = foodStampAmount == null ? 0 : foodStampAmount.intValue();
        this.taxAmount_A = taxAmount_A == null ? 0 : taxAmount_A.intValue();
        this.balanceDue = balanceDue == null ? 0 : balanceDue.intValue();
        this.itemCount = itemCount == null ? 0 : itemCount.intValue();
        this.discountAmount = discountAmount == null ? 0 : discountAmount.intValue();
        this.points = points == null ? 0 : points.intValue();
        this.ticketAmount = 0;
        this.glutenFreeAmount = 0;
    }

    public SscoTotalAmount(){
        this.totalAmount = 0;
        this.foodStampAmount = 0;
        this.taxAmount_A = 0;
        this.balanceDue = 0;
        this.itemCount = 0;
        this.discountAmount = 0;
        this.points = 0;
        this.ticketAmount = 0;
        this.glutenFreeAmount = 0;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getFoodStampAmount() {
        return foodStampAmount;
    }

    public void setFoodStampAmount(int foodStampAmount) {
        this.foodStampAmount = foodStampAmount;
    }

    public int getTaxAmount_A() {
        return taxAmount_A;
    }

    public void setTaxAmount_A(int taxAmount_A) {
        this.taxAmount_A = taxAmount_A;
    }

    public int getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(int balanceDue) {
        this.balanceDue = balanceDue;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getChangeDue() {
        return changeDue;
    }

    public void setChangeDue(int changeDue) {
        this.changeDue = changeDue;
    }

    public int getTicketAmount() {
        return ticketAmount;
    }

    public void setTicketAmount(int ticketAmount) {
        this.ticketAmount = ticketAmount;
    }

    public int getGlutenFreeAmount() {
        return glutenFreeAmount;
    }

    public void setGlutenFreeAmount(int glutenFreeAmount) {
        this.glutenFreeAmount = glutenFreeAmount;
    }
}
