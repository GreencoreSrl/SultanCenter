package com.ncr.ssco.communication.entities.pos;

import com.ncr.ssco.communication.entities.TenderTypeEnum;

/**
 * Created by stefanobertarello on 02/03/17.
 */
public class SscoTender {
    int amount;
    TenderTypeEnum tenderType;
    String description;
    String upc;

    public SscoTender(TenderTypeEnum tenderType, int amount) {
        this.tenderType = tenderType;
        this.description = tenderType.toString();
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public TenderTypeEnum getTenderType() {
        return tenderType;
    }

    public void setTenderType(TenderTypeEnum tenderType) {
        this.tenderType = tenderType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpc() {
        return upc;
    }

    public void setUpc(String upc) {
        this.upc = upc;
    }
}
