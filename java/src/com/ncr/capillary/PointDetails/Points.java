package com.ncr.capillary.PointDetails;

public class Points {
	private Redeemable redeemable = new Redeemable();
	private ItemStatus itemStatus = new ItemStatus();

	public Redeemable getRedeemable() {
		return redeemable;
	}

	public void setRedeemable(Redeemable redeemable) {
		this.redeemable = redeemable;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}
}
