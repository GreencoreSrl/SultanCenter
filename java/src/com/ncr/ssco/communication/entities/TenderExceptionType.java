package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum TenderExceptionType {
    Event(0),
    Confirm (1),
    Selection (2),
    NumericEntry (3),
    UPCEntry (4);

    private final int code;

    TenderExceptionType(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }
}