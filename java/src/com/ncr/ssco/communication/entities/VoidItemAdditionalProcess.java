package com.ncr.ssco.communication.entities;
/**
 * Created by NCRDeveloper on 13/06/2017.
 */
public enum VoidItemAdditionalProcess {

    PRICE(1);

    private final int code;

    VoidItemAdditionalProcess(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }

}
