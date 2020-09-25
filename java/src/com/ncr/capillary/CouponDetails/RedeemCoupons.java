package com.ncr.capillary.CouponDetails;

public class RedeemCoupons {
	private ItemStatus itemStatus = new ItemStatus();
	private Coupon coupon = new Coupon();

	public Coupon getCoupon() {
		return coupon;
	}

	public void setCoupon(Coupon coupon) {
		this.coupon = coupon;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}
}
