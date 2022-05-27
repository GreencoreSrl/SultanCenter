package com.ncr.ssco.communication.entities.pos;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public class SscoItem {
    private String upc = "";
    private int weight;
    private int weightPrice;
    private int tareCode;
    private int itemNumber = 0;
    private int entryId = -1;
    private int qty = 1;
    private String description;
    private int price = 0;
    private int scanned;
    private String department;
    private boolean voidedItem = false;
    private boolean upbItem = false;
    private long operationID = 0;
    private boolean zeroPriced = false;
    private boolean priceChanged = false;
    private String additionalDescription = "";

    public SscoItem() {
    }

    public SscoItem(String upc, Integer scanned, String department, Integer itemNumber, String description, Integer price, Integer quantity) {
        this.upc = upc;
        this.weight = 0;
        this.weightPrice = 0;
        this.tareCode = 0;
        this.itemNumber = itemNumber == null ? 0 : itemNumber.intValue();
        this.qty = quantity == null ? 0 : quantity.intValue();
        this.description = description;
        this.price = price == null ? 0 : price.intValue();
        this.scanned = scanned == null ? 0 : scanned.intValue();
        this.department = department;
    }

    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    public boolean isVoidedItem() {
        return voidedItem;
    }

    public void setVoidedItem(boolean voidedItem) {
        this.voidedItem = voidedItem;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int isScanned() {
        return scanned;
    }

    public int getScanned() {
        return scanned;
    }

    public void setScanned(int scanned) {
        this.scanned = scanned;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getUpc() {
        return upc;
    }

    public void setUpc(String upc) {
        this.upc = upc;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWeightPrice() {
        return weightPrice;
    }

    public void setWeightPrice(int weightPrice) {
        this.weightPrice = weightPrice;
    }

    public int getTareCode() {
        return tareCode;
    }

    public void setTareCode(int tareCode) {
        this.tareCode = tareCode;
    }

    public int getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(int itemNumber) {
        this.itemNumber = itemNumber;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public boolean isUpbItem() {
        return upbItem;
    }

    public void setUpbItem(boolean upbItem) {
        this.upbItem = upbItem;
    }

    public long getOperationID() {
        return operationID;
    }

    public void setOperationID(long operationID) {
        this.operationID = operationID;
    }

    public boolean isZeroPriced() {
        return zeroPriced;
    }

    public void setZeroPriced(boolean zeroPriced) {
        this.zeroPriced = zeroPriced;
    }

    public void setPriceChanged(boolean priceChanged) {
        this.priceChanged = priceChanged;
    }

    public boolean isPriceChanged() {
        return priceChanged;
    }

    public String getAdditionalDescription() {
        return additionalDescription;
    }

    public void setAdditionalDescription(String additionalDescription) {
        this.additionalDescription = additionalDescription;
    }
}