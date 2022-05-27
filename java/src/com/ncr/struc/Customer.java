package com.ncr.struc;

/*******************************************************************
 * customer information data
 *******************************************************************/
public class Customer {
    private int spec;
    private int branch;
    private int rate;
    private int dscnt;
    private int extra;
    private int age;
    private int pnt;
    private long limchk;
    private long limcha;
    private String number;
    private String name;
    private String adrs;
    private String city;
    private String nam2;
    private String dtbl;
    private String fiscalId;
    private String original; // TSC-MOD2014-AMZ#ADD
    private String mobile; // SARAWAT-ENH-20150507-CGA#A
    private String cusId;  //SPINNEYS-2017-033-CGA#A
    private String selfSellEANList; // AMZ-2017-003-006#BEG - articoli da vendere automaticamente  -- e' un elenco di EAN separati da virgola ','
    private boolean noPromo = false;

    /**
     * valid selection from menu of charge types (0=all)
     **/
    public int getSpec() {
        return spec;
    }

    public void setSpec(int spec) {
        this.spec = spec;
    }

    /**
     * 01-09 = blocking reason, 10 - 99 = category
     **/
    public int getBranch() {
        return branch;
    }

    public void setBranch(int branch) {
        this.branch = branch;
    }

    /**
     * employee/customer discount rate (1 decimal place assumed)
     **/
    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    /**
     * rate of discount for cash (1 decimal place assumed)
     **/
    public int getDscnt() {
        return dscnt;
    }

    public void setDscnt(int dscnt) {
        this.dscnt = dscnt;
    }

    /**
     * rate of surcharge on delivery (1 decimal place assumed)
     **/
    public int getExtra() {
        return extra;
    }

    public void setExtra(int extra) {
        this.extra = extra;
    }

    /**
     * date-of-birth yymmdd
     **/
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    /**
     * initial bonuspoints
     **/
    public int getPnt() {
        return pnt;
    }

    public void setPnt(int pnt) {
        this.pnt = pnt;
    }

    /**
     * check limit regarding CLS file data
     **/
    public long getLimchk() {
        return limchk;
    }

    public void setLimchk(long limchk) {
        this.limchk = limchk;
    }

    /**
     * charge limit regarding CLS file data
     **/
    public long getLimcha() {
        return limcha;
    }

    public void setLimcha(long limcha) {
        this.limcha = limcha;
    }

    /**
     * customer number
     **/
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * customer name
     **/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * customer address (street and house number)
     **/
    public String getAdrs() {
        return adrs;
    }

    public void setAdrs(String adrs) {
        this.adrs = adrs;
    }

    /**
     * customer address (ZIP code and town)
     **/
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /**
     * customer company name
     **/
    public String getNam2() {
        return nam2;
    }

    public void setNam2(String nam2) {
        this.nam2 = nam2;
    }

    /**
     * 30 characters reserved for customization issues
     **/
    public String getDtbl() {
        return dtbl;
    }

    public void setDtbl(String dtbl) {
        this.dtbl = dtbl;
    }

    /**
     * 20 characters fiscal identification
     **/
    public String getFiscalId() {
        return fiscalId;
    }

    public void setFiscalId(String fiscalId) {
        this.fiscalId = fiscalId;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    /**
     * customer telephone number
     **/
    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCusId() {
        return cusId;
    }

    public void setCusId(String cusId) {
        this.cusId = cusId;
    }

    public String getSelfSellEANList() {
        return selfSellEANList;
    }

    public void setSelfSellEANList(String selfSellEANList) {
        this.selfSellEANList = selfSellEANList;
    }

    public boolean isNoPromo() {
        return noPromo;
    }

    public void setNoPromo(boolean noPromo) {
        this.noPromo = noPromo;
    }
}
