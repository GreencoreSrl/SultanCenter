package com.ncr.ecommerce.data;

public class TenderProperties {
    public final static char VALUE = '$';
    public final static char PERCENTAGE = '%';

    private String commissionItem;
    private char commissionType;
    private int commissionValue;

    public TenderProperties(String commissionItem, char commissionType, int commissionValue) {
        this.commissionItem = commissionItem;
        this.commissionType = commissionType;
        this.commissionValue = commissionValue;
    }

    public String getCommissionItem() {
        return commissionItem;
    }

    public void setCommissionItem(String commissionItem) {
        this.commissionItem = commissionItem;
    }

    public char getCommissionType() {
        return commissionType;
    }

    public void setCommissionType(char commissionType) {
        this.commissionType = commissionType;
    }

    public int getCommissionValue() {
        return commissionValue;
    }

    public void setCommissionValue(int commissionValue) {
        this.commissionValue = commissionValue;
    }
}
