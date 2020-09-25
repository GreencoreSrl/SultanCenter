package com.ncr.ssco.communication.hook;

/**
 * Created by stefanobertarello on 28/02/17.
 */
public class AutomationException extends RuntimeException {
    public AutomationException() {
    }

    public AutomationException(String msg) {
        super(msg);
    }
}