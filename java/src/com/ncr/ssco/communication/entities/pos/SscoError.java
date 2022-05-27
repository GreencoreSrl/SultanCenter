package com.ncr.ssco.communication.entities.pos;

import com.ncr.ssco.communication.manager.SscoLanguageHandler;

import java.util.Properties;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public class SscoError {
    private int code;
    private String message = "";
    public static final int OK = 0;
    public static final int ZERO_PRICED = 9005;
    public static final int HIGHER_THAN_BALANCE = 9111;
    public static final int CANT_SIGNOFF_NOW = 9112;
    public static final int DEFAULT = -99;
    public static final int ERROR = 9999;

    public SscoError() {
        this.code = OK;
    }

    public SscoError(int code) {
        this.code = code;
    }

    public SscoError(int code, Properties properties) {
        this.code = code;
        this.message = properties.getProperty("Error." + code + ".Message");
    }

    public SscoError(int sts, String info) {
        this.code = sts;
        this.message = info;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return SscoLanguageHandler.getInstance().getMessage("Error." + this.code, this.message);
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
