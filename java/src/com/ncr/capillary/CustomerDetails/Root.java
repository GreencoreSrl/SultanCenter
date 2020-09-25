package com.ncr.capillary.CustomerDetails;

import java.util.ArrayList;

public class Root {
	private ArrayList<Customer> customer = new ArrayList<Customer>();

	public ArrayList<Customer> getCustomer() {
		return customer;
	}

	public void setCustomer(ArrayList<Customer> customer) {
		this.customer = customer;
	}
}
