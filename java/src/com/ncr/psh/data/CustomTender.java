package com.ncr.psh.data;

//INTEGRATION-PHILOSHOPIC-CGA#A BEG
public class CustomTender {
    private int tenderId;
    private String accountType;
    private boolean accountNumberRequired;

    public CustomTender(int tenderId, String accountType, boolean accountNumberRequired) {
        this.tenderId = tenderId;
        this.accountType = accountType;
        this.accountNumberRequired = accountNumberRequired;
    }

    public int getTenderId() {
        return tenderId;
    }

    public void setTenderId(int tenderId) {
        this.tenderId = tenderId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public boolean isAccountNumberRequired() {
        return accountNumberRequired;
    }

    public void setAccountNumberRequired(boolean accountNumberRequired) {
        this.accountNumberRequired = accountNumberRequired;
    }
}
//INTEGRATION-PHILOSHOPIC-CGA#A END
