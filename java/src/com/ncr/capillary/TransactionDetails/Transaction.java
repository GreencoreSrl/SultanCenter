package com.ncr.capillary.TransactionDetails;

public class Transaction {
	private long id = 0L;
	private String type = "";
    private String returnType = "";
	private String number = "";
	private String amount = "";
	private String billingTime = "";
    private String purchaseTime = "";
	private String grossAmount = "";
	private String discount = "";
	private Customer customer = new Customer();
	private ItemStatus itemStatus = new ItemStatus();
	private PaymentDetails paymentDetails = new PaymentDetails();
	private LineItems lineItems = new LineItems();
	private SideEffects sideEffects = new SideEffects();


    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getBillingTime() {
		return billingTime;
	}

    public String getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(String purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public String getGrossAmount() {
		return grossAmount;
	}

	public void setGrossAmount(String grossAmount) {
		this.grossAmount = grossAmount;
	}

	public String getDiscount() {
		return discount;
	}

	public void setDiscount(String discount) {
		this.discount = discount;
	}

	public void setBillingTime(String billingTime) {
		this.billingTime = billingTime;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public PaymentDetails getPaymentDetails() {
		return paymentDetails;
	}

	public void setPaymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
	}

	public LineItems getLineItems() {
		return lineItems;
	}

	public void setLineItems(LineItems lineItems) {
		this.lineItems = lineItems;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}

	public SideEffects getSideEffects() {
		return sideEffects;
	}

	public void setSideEffects(SideEffects sideEffects) {
		this.sideEffects = sideEffects;
	}
}
