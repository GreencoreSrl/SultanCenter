package com.ncr.capillary.TransactionDetails;

public class Effect {
	private String type = "";
	private String awardedPoints = "";
	private String totalPoints = "";
	private String code = "";
	private String description = "";
	private String id = "";
	private String validTill = "";
	private String discountCode = "";
	private String discountValue = "";

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDiscountValue() {
		return discountValue;
	}

	public void setDiscountValue(String discountValue) {
		this.discountValue = discountValue;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAwardedPoints() {
		return awardedPoints;
	}

	public void setAwardedPoints(String awardedPoints) {
		this.awardedPoints = awardedPoints;
	}

	public String getTotalPoints() {
		return totalPoints;
	}

	public void setTotalPoints(String totalPoints) {
		this.totalPoints = totalPoints;
	}

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
}
