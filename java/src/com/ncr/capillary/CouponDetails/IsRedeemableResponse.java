package com.ncr.capillary.CouponDetails;

public class IsRedeemableResponse {
	private Status status = new Status();
	private IsRedeemableCoupons coupons = new IsRedeemableCoupons();

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public IsRedeemableCoupons getCoupons() {
		return coupons;
	}

	public void setCoupons(IsRedeemableCoupons coupons) {
		this.coupons = coupons;
	}
}
