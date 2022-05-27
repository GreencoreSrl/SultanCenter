package com.ncr.ssco.communication.entities.pos;

import com.ncr.Itemdata;

public class SscoItemPromotion {
    private int itemNumber;
    private int discountAmount;
    private int associatedItemNumber;
    private String discountDescription1;
    private int showRewardPoints;
    private int rewardLocation;
    private Itemdata item;
    private boolean promoInviata = false;

    public SscoItemPromotion(Itemdata item, int itemNumber, int discountAmount, int associatedItemNumber, String discountDescription_1, int showRewardPoints, int rewardLocation) {// AMZ-FLANE#DISCOUNT#ADD
        this.item = item;
        this.itemNumber = itemNumber;
        this.discountAmount = discountAmount;
        this.associatedItemNumber = associatedItemNumber;
        discountDescription1 = discountDescription_1;
        this.showRewardPoints = showRewardPoints;
        this.rewardLocation = rewardLocation;
        promoInviata = false;
    }

    public boolean isPromoInviata() {
        return promoInviata;
    }

    public void setPromoInviata(boolean promoInviata) {
        this.promoInviata = promoInviata;
    }

    public int getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(int itemNumber) {
        this.itemNumber = itemNumber;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public int getAssociatedItemNumber() {
        return associatedItemNumber;
    }

    public void setAssociatedItemNumber(int associatedItemNumber) {
        this.associatedItemNumber = associatedItemNumber;
    }

    public String getDiscountDescription1() {
        return discountDescription1;
    }

    public void setDiscountDescription1(String discountDescription1) {
        this.discountDescription1 = discountDescription1;
    }

    public int getShowRewardPoints() {
        return showRewardPoints;
    }

    public void setShowRewardPoints(int showRewardPoints) {
        this.showRewardPoints = showRewardPoints;
    }

    public int getRewardLocation() {
        return rewardLocation;
    }

    public void setRewardLocation(int rewardLocation) {
        this.rewardLocation = rewardLocation;
    }

    public Itemdata getItem() {
        return item;
    }

    public void setItem(Itemdata item) {
        this.item = item;
    }
}