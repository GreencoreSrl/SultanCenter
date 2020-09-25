package com.ncr.capillary.PointDetails;

public class Redeemable {
	private String mobile = "";
	private long points = 0;
	private String externalId = "";
	private boolean isRedeemable = false;
	private String pointsRedeemValue = "";
	private ItemStatus itemStatus = new ItemStatus();

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public long getPoints() {
		return points;
	}

	public void setPoints(long points) {
		this.points = points;
	}

	public boolean isRedeemable() {
		return isRedeemable;
	}

	public void setRedeemable(boolean redeemable) {
		isRedeemable = redeemable;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getPointsRedeemValue() {
		return pointsRedeemValue;
	}

	public void setPointsRedeemValue(String pointsRedeemValue) {
		this.pointsRedeemValue = pointsRedeemValue;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}
}
