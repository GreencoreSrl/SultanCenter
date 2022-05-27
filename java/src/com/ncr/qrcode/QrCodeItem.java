package com.ncr.qrcode;

import com.ncr.Struc;
import java.math.BigDecimal;

/**
 * QrCode class generate
 */
public class QrCodeItem {

    private String code;
    private int price;

    public QrCodeItem(String code, BigDecimal price) {
        this.code = code;
        this.price = price.multiply(new BigDecimal(Math.pow(10, Struc.tnd[0].dec))).intValue();
    }

    public String getCode() {
        return code;
    }

    public int getPrice() {
        return price;
    }
}