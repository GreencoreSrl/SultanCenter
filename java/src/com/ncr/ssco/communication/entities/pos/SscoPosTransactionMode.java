package com.ncr.ssco.communication.entities.pos;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum SscoPosTransactionMode {
    Normal (1),
    Abort (2),
    Suspend (3),
    Unknown(9999);

    private int code;

    SscoPosTransactionMode(int code) {
        this.code = code;
    }

    public boolean equals(int id) {
        return code == id;
    }

    public int getCode() {
        return code;
    }
}
