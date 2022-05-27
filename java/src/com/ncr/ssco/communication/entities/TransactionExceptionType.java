package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum TransactionExceptionType {
    None(0),
    ExitTenderMode (1),
    RequestTransaction (2),
    ResumeTransaction (3),
    SuspendTransaction (4),
    VoidTransaction (5);

    private final int exceptionId;

    TransactionExceptionType(int exceptionId) {
        this.exceptionId = exceptionId;
    }

    public boolean equals(int id) {
        return exceptionId == id;
    }

    public int getExceptionId() {
        return exceptionId;
    }
}