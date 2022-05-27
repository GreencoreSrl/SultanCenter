package com.ncr.ecommerce.data;

import java.math.BigDecimal;

public class Item {
    private String code;
    private BigDecimal price;
    private int qty;
    private BigDecimal unitPrice;
    private String barcode;

    public Item() {

    }

    public Item(String code, BigDecimal price, int qty, BigDecimal unitPrice, String barcode) {
        this.code = code;
        this.price = price;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.barcode = barcode;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
