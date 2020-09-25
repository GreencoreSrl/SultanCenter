package com.ncr.capillary.PointDetails;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 23/06/15 Time: 14.58 To change this template use File |
 * Settings | File Templates.
 */
public class Customer {
	private String mobile = "";
	private String email = "";
	private String externalId = "";

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
