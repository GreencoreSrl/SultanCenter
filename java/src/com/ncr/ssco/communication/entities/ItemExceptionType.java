package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum ItemExceptionType {
    Event(0);

    private final int code;

    ItemExceptionType(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }
}