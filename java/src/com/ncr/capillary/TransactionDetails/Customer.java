package com.ncr.capillary.TransactionDetails;

public class Customer {
	private String mobile = "";
	private String externalId = "";
	private String firstname = "";
	private String lastname = "";
	private String email = "";
	private String loyaltyPoints = "";
	private String lifetimePoints = "";
	private String lifetimePurchases = "";
	private String currentSlab = "";

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLoyaltyPoints() {
		return loyaltyPoints;
	}

	public void setLoyaltyPoints(String loyaltyPoints) {
		this.loyaltyPoints = loyaltyPoints;
	}

	public String getLifetimePoints() {
		return lifetimePoints;
	}

	public void setLifetimePoints(String lifetimePoints) {
		this.lifetimePoints = lifetimePoints;
	}

	public String getLifetimePurchases() {
		return lifetimePurchases;
	}

	public void setLifetimePurchases(String lifetimePurchases) {
		this.lifetimePurchases = lifetimePurchases;
	}

	public String getCurrentSlab() {
		return currentSlab;
	}

	public void setCurrentSlab(String currentSlab) {
		this.currentSlab = currentSlab;
	}
}
