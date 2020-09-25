package com.ncr.wsLoyalty.data;

/**
 * Created by Administrator on 02/12/17.
 */
public class PhoneCode {
    private String country = "";
    private String phoneCode = "";
    //SPINNEYS-2017-033-CGA#A BEG
    private int rangeMin = 0;
    private int rangeMax = 0;
    //SPINNEYS-2017-033-CGA#A END
    private boolean trimLeadingZero = false;

    public PhoneCode(String country, String phoneCode, int rangeMin, int rangeMax, boolean trimLeadingZero) {
        this.country = country;
        this.phoneCode = phoneCode;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.trimLeadingZero = trimLeadingZero;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
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
