package com.ncr.capillary.CustomerDetails;

public class Coupon {
	private String id = "";
	private String seriesId = "";
	private String code = "";
	private String description = "";
	private String validTill = "";
	private boolean redeemed = false;
	private ItemStatus itemStatus = new ItemStatus();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSeriesId() {
		return seriesId;
	}

	public void setSeriesId(String seriesId) {
		this.seriesId = seriesId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getValidTill() {
		return validTill;
	}

	public void setValidTill(String validTill) {
		this.validTill = validTill;
	}

	public boolean getRedeemed() {
		return redeemed;
	}

	public void setRedeemed(boolean redeemed) {
		this.redeemed = redeemed;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}
}
