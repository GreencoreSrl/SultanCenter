package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum ItemExceptionId {
    ItemNotFound(1),
    ItemRecalled(2),
    ItemFoundButCantBeSold(3),
    CantPurchaseAtThisTime(4),
    PriceRequired(5),
    ItemCantBeSold(6),
    QuantityNeeded(7),
    CratableItem(8),
    WeightRequired(9),
    QuantityRestricted(10),
    VisualVerifyRequired(11),
    AgeRequired(12),
    PriceLimit(13),
    StoreClosing(14),
    DuplicateItem(15),
    InvalidDate(16),
    WeightLimit(17),
    CancelMustMatchPreviousEntry(24);

    private final int code;

    ItemExceptionId(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }
}