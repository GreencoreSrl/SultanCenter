package com.ncr.ecommerce.data;

public class Item {
    private String code;
    private Double price;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getPrice() {
        return price;
    }

    public Item() {
    }

    public Item(String code, Double price) {
        this.code = code;
        this.price = price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
