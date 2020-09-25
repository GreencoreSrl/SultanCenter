package com.ncr.wsLoyalty.data;

import java.util.Set;

/**
 * Created by User on 14/12/2017.
 */


public class LoyaltyCustomer {
    private int id;
    private String customerCode;
    private String phoneNumber;
    private String cardCode;
    private String firstName;
    private String lastName;
    private boolean active;
    private Set<Promovar> promovars;

    public LoyaltyCustomer() {}

    public LoyaltyCustomer(String customerCode) {
        this.customerCode = customerCode;
    }

    public LoyaltyCustomer(String cardCode, String phoneNumber) {
        this.cardCode = cardCode;
        this.phoneNumber = phoneNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public Set<Promovar> getPromovars() {
        return promovars;
    }

    public void setPromovars(Set<Promovar> promovars) {
        this.promovars = promovars;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", customerCode='" + customerCode + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", cardCode='" + cardCode + '\'' +
                ", active=" + active +
               // ", promovars=" + promovars +
                '}';
    }
}



