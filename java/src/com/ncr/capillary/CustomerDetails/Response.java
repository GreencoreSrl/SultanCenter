package com.ncr.capillary.CustomerDetails;

public class Response {
	private Status status = new Status();
	private Customers customers = new Customers();

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Customers getCustomers() {
		return customers;
	}

	public void setCustomers(Customers customers) {
		this.customers = customers;
	}
}
