package com.ncr.ssco.communication.entities.pos;

public class SscoCustomer {
    private String accountNumber;
    private String countryCode;
    private String entryMethod;

    public SscoCustomer(String accountNumber, String countryCode, String entryMethod) {
        this.accountNumber = accountNumber;
        this.countryCode = countryCode;
        this.entryMethod = entryMethod;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getEntryMethod() {
        return entryMethod;
    }

    public void setEntryMethod(String entryMethod) {
        this.entryMethod = entryMethod;
    }
}
