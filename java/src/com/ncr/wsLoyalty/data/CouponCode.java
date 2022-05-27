package com.ncr.wsLoyalty.data;

/**
 * Created by Administrator on 02/12/17.
 */
public class CouponCode {
    private String code = "";
    private int rangeMin = 0;
    private int rangeMax = 0;
    private boolean trimLeadingZero = false;

    public CouponCode(String code, int rangeMin, int rangeMax, boolean trimLeadingZero) {
        this.code = code;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.trimLeadingZero = trimLeadingZero;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getRangeMin() {
        return rangeMin;
    }

    public void setRangeMin(int rangeMin) {
        this.rangeMin = rangeMin;
    }

    public int getRangeMax() {
        return rangeMax;
    }

    public void setRangeMax(int rangeMax) {
        this.rangeMax = rangeMax;
    }

    public boolean isTrimLeadingZero() {
        return trimLeadingZero;
    }

    public void setTrimLeadingZero(boolean trimLeadingZero) {
        this.trimLeadingZero = trimLeadingZero;
    }
}
