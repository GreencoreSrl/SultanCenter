package com.ncr.wsLoyalty.data;

import com.ncr.capillary.TransactionDetails.Customer;

/**
 * Created by User on 07/02/2018.
 */


public class Promovar {
    private int id;
    private String type;
    private String code;
    private int value;
    private int customer;

    public Promovar() {}


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getCustomer() {
        return customer;
    }

    public void setCustomer(int customer) {
        this.customer = customer;
    }

    @Override
    public String toString() {
        return "Promovar{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", code='" + code + '\'' +
                ", value=" + value +
                '}';
    }
}
