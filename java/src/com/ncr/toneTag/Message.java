package com.ncr.toneTag;

/**
 * Created by User on 20/05/2019.
 */
public class Message {
    //{"txn_id":"1410100003727669652","amount_in_fils":"100","success":true,"txn_time":"03\/10\/2019 07:21 PM","message":"Transaction Successful"}
    private String message = "";
    private String amount_in_fils = "";
    private String txn_time = "";
    private String txn_id = "";
    private String success = "";

    public Message() {

    }

    //{"txn_id":"1410100003727669652","amount_in_fils":"100","success":true,"txn_time":"03\/10\/2019 07:21 PM","message":"Transaction Successful"}

    public Message(String message, String amount_in_fils, String txn_time, String txn_id, String success) {
        this.message = message;
        this.amount_in_fils = amount_in_fils;
        this.txn_time = txn_time;
        this.txn_id = txn_id;
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAmount_in_fils(String amount_in_fils) {
        this.amount_in_fils = amount_in_fils;
    }

    public void setTxn_time(String txn_time) {
        this.txn_time = txn_time;
    }

    public void setTxn_id(String txn_id) {
        this.txn_id = txn_id;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public String getAmount_in_fils() {
        return amount_in_fils;
    }

    public String getTxn_time() {
        return txn_time;
    }

    public String getTxn_id() {
        return txn_id;
    }

    public String getSuccess() {
        return success;
    }
}
