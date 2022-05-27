package com.ncr.ssco.communication.entities;

/**
 * Created by Umberto on 10/05/2017.
 */
public class TenderType {

    private TenderTypeEnum tenderTypeSSSCO;
    private String actionPOS;
    private boolean additionalClear;
    private boolean rounded;
    private int genericException;
    private boolean higherThanAmount;

    public TenderType(TenderTypeEnum tenderTypeSSSCO, String actionPOS, boolean additionalClear) {
        this.tenderTypeSSSCO = tenderTypeSSSCO;
        this.actionPOS = actionPOS;
        this.additionalClear = additionalClear;
    }

    public TenderType(TenderTypeEnum tenderTypeSSSCO, String actionPOS) {
        this.tenderTypeSSSCO = tenderTypeSSSCO;
        this.actionPOS = actionPOS;
    }

    public TenderTypeEnum getTenderTypeSSSCO() {
        return tenderTypeSSSCO;
    }

    public void setTenderTypeSSSCO(TenderTypeEnum tenderTypeSSSCO) {
        this.tenderTypeSSSCO = tenderTypeSSSCO;
    }

    public String getActionPOS() {
        return actionPOS;
    }

    public void setActionPOS(String actionPOS) {
        this.actionPOS = actionPOS;
    }

    public boolean isAdditionalClear() {
        return additionalClear;
    }

    public void setAdditionalClear(boolean additionalClear) {
        this.additionalClear = additionalClear;
    }

    public boolean isRounded() {
        return rounded;
    }

    public void setRounded(boolean rounded) {
        this.rounded = rounded;
    }

    public boolean isHigherThanAmount() {
        return higherThanAmount;
    }

    public void setHigherThanAmount(boolean higherThanAmount) {
        this.higherThanAmount = higherThanAmount;
    }

    public int getGenericException() {
        return genericException;
    }

    public void setGenericException(int genericException) {
        this.genericException = genericException;
    }
}
