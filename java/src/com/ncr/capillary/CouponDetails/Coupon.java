package com.ncr.capillary.CouponDetails;

import com.ncr.capillary.CouponDetails.Customer;

public class Coupon {
	private String code = "";
	private Customer customer = new Customer();
	private Transaction transaction = new Transaction();
	private String discountCode = "";
	private String seriesCode = "";
	private boolean isAbsolute = false;
	private long couponValue = 0L;
	private ItemStatus itemStatus = new ItemStatus();

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

	public String getDiscountCode() {
		return discountCode;
	}

	public void setDiscountCode(String discountCode) {
		this.discountCode = discountCode;
	}

	public String getSeriesCode() {
		return seriesCode;
	}

	public void setSeriesCode(String seriesCode) {
		this.seriesCode = seriesCode;
	}

	public boolean isAbsolute() {
		return isAbsolute;
	}

	public void setIsAbsolute(boolean isAbsolute) {
		this.isAbsolute = isAbsolute;
	}

	public long getCouponValue() {
		return couponValue;
	}

	public void setCouponValue(long couponValue) {
		this.couponValue = couponValue;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}
}
