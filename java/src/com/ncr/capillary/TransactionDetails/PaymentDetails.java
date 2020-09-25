package com.ncr.capillary.TransactionDetails;

import java.util.ArrayList;

public class PaymentDetails {
	private ArrayList<Payment> payment = new ArrayList<Payment>();

	public ArrayList<Payment> getPayment() {
		return payment;
	}

	public void setPayment(ArrayList<Payment> payment) {
		this.payment = payment;
	}
}
