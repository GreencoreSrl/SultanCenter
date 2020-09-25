package com.ncr.capillary.CouponDetails;

public class SeriesInfo {
	private String description = "";
	private String discountCode = "";
	private String validTill = "";
	private String discountType = "";
	private long discountValue = 0L;
	private String discountOn = "";
	private String detailedInfo = "";

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDiscountCode() {
		return discountCode;
	}

	public void setDiscountCode(String discountCode) {
		this.discountCode = discountCode;
	}

	public String getValidTill() {
		return validTill;
	}

	public void setValidTill(String validTill) {
		this.validTill = validTill;
	}

	public String getDiscountType() {
		return discountType;
	}

	public void setDiscountType(String discountType) {
		this.discountType = discountType;
	}

	public long getDiscountValue() {
		return discountValue;
	}

	public void setDiscountValue(long discountValue) {
		this.discountValue = discountValue;
	}

	public String getDiscountOn() {
		return discountOn;
	}

	public void setDiscountOn(String discountOn) {
		this.discountOn = discountOn;
	}

	public String getDetailedInfo() {
		return detailedInfo;
	}

	public void setDetailedInfo(String detailedInfo) {
		this.detailedInfo = detailedInfo;
	}
}
