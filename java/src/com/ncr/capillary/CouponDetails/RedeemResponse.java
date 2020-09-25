package com.ncr.capillary.CouponDetails;

public class RedeemResponse {
	private Status status = new Status();
	private RedeemCoupons coupons = new RedeemCoupons();

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public RedeemCoupons getCoupons() {
		return coupons;
	}

	public void setCoupons(RedeemCoupons coupons) {
		this.coupons = coupons;
	}
}
