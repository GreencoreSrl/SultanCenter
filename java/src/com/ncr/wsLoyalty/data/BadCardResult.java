package com.ncr.wsLoyalty.data;

/**
 * Created by Administrator on 05/12/17.
 */
public class BadCardResult {
    public static final int CARD_AVAILABLE = 0;
    public static final int CARD_BLOCKED = 1;
    private int code;
    private String description;

    public BadCardResult(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
