package com.ncr.ecommerce.data;

import java.util.TimerTask;

public class TerminalItem {
    private String TerminalId="";
    private Integer ErrorCode;

    public TerminalItem(String terminalID, Integer errorCode) {
        this.TerminalId = terminalID;
        this.ErrorCode = errorCode;
    }

    public String getTerminalId() {
        return TerminalId;
    }

    public void setTerminalID(String terminalId) {
        TerminalId = terminalId;
    }

    public Integer getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(Integer errorCode) {
        ErrorCode = errorCode;
    }
}
