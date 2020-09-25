package com.ncr.capillary.CouponDetails;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 24/06/15 Time: 16.15 To change this template use File |
 * Settings | File Templates.
 */
public class RedeemCoupon {
	private String code = "";
	private double discount = 0L;
	private String discountType = "";
    private String discountCode = "";
    private boolean applied = false;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public String getDiscountType() {
		return discountType;
	}

	public void setDiscountType(String discountType) {
		this.discountType = discountType;
	}

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public boolean isApplied() {
		return applied;
	}

	public void setApplied(boolean applied) {
		this.applied = applied;
	}
}
