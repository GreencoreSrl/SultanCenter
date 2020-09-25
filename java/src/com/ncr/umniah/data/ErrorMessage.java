package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by User on 19/01/2018.
 */
public class ErrorMessage {
    @SerializedName("Code")
    private int code;
    @SerializedName("Message")
    private String message;

    public ErrorMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
