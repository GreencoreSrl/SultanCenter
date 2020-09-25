package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum ItemVoidExceptionType {
    Event(0),
    NumericEntry(3);

    private final int code;

    ItemVoidExceptionType(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }
}