package com.ncr.ssco.communication.entities;

/**
 * Created by NCRDeveloper on 13/06/2017.
 */
public enum AdditionalProcessType {
    NONE(0),
    DEPT(1),
    QTY(2),
    PRICE(3),
    LOYALTY(90),
    LOYALTY_NOTVALID(91),
    LOYALTY_DUPLICATE(92);

    private final int code;

    AdditionalProcessType(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }

}
