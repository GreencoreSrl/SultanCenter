package com.ncr.wsLoyalty.data;

/**
 * Created by User on 15/12/2017.
 */
public class ResponseMessage {
    private int code;
    private String description;

    public ResponseMessage() {}
    public ResponseMessage(int code, String description) {
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
