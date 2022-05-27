package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum DataNeededType {
    Clear (0),
    Alert (1),
    Confirmation (2),
    DataEntry (3),
    Selection(4),
    Unknown(9999);

    private int code;

    DataNeededType(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }
}