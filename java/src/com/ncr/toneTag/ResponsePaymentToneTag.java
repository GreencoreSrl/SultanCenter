package com.ncr.toneTag;

/**
 * Created by User on 20/05/2019.
 */
public class ResponsePaymentToneTag {
    private int statusCode = 0;
    private Message message = new Message();

    public ResponsePaymentToneTag(int statusCode, Message message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
