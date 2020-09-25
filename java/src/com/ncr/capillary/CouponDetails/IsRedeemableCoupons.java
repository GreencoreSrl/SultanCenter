package com.ncr.capillary.CouponDetails;

public class IsRedeemableCoupons {
	private Redeemable redeemable = new Redeemable();
	private ItemStatus itemStatus = new ItemStatus();
	//private ArrayList<Coupon> coupon = new ArrayList<Coupon>();

	/*
	public ArrayList<Coupon> getCoupon() {
		return coupon;
	}

	public void setCoupon(ArrayList<Coupon> coupon) {
		this.coupon = coupon;
	}
    */

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
