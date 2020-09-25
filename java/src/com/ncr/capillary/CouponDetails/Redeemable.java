package com.ncr.capillary.CouponDetails;

public class Redeemable {
	private String code = "";
	private String validationCode = "";
	private Customer customer = new Customer();
	private CustomFields customFields = new CustomFields();
	private Transaction transaction = new Transaction();
	private String mobile = "";
	private boolean isRedeemable = false;
	private ItemStatus itemStatus = new ItemStatus();
	private SeriesInfo seriesInfo = new SeriesInfo();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	public String getValidationCode() {
		return validationCode;
	}

	public void setValidationCode(String validationCode) {
		this.validationCode = validationCode;
	}

	public CustomFields getCustomFields() {
		return customFields;
	}

	public void setCustomFields(CustomFields customFields) {
		this.customFields = customFields;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public boolean isRedeemable() {
		return isRedeemable;
	}

	public void setRedeemable(boolean redeemable) {
		isRedeemable = redeemable;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}

	public SeriesInfo getSeriesInfo() {
		return seriesInfo;
	}

	public void setSeriesInfo(SeriesInfo seriesInfo) {
		this.seriesInfo = seriesInfo;
	}
}
