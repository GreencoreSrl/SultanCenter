package com.ncr.capillary.PointDetails;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 23/06/15 Time: 14.58 To change this template use File |
 * Settings | File Templates.
 */
public class Redeem {
	private long pointsRedeemed = 0;
	private String transactionNumber = "";
	private Customer customer = new Customer();
	private String notes = "";
	private String validationCode = "";
	private String redemptionTime = "";

	public long getPointsRedeemed() {
		return pointsRedeemed;
	}

	public void setPointsRedeemed(long pointsRedeemed) {
		this.pointsRedeemed = pointsRedeemed;
	}

	public String getTransactionNumber() {
		return transactionNumber;
	}

	public void setTransactionNumber(String transactionNumber) {
		this.transactionNumber = transactionNumber;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getValidationCode() {
		return validationCode;
	}

	public void setValidationCode(String validationCode) {
		this.validationCode = validationCode;
	}

	public String getRedemptionTime() {
		return redemptionTime;
	}

	public void setRedemptionTime(String redemptionTime) {
		this.redemptionTime = redemptionTime;
	}
}
