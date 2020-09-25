package com.ncr.capillary.TransactionDetails;

import java.util.ArrayList;

public class Root {
	private ArrayList<Transaction> transaction = new ArrayList<Transaction>();

	public ArrayList<Transaction> getTransaction() {
		return transaction;
	}

	public void setTransaction(ArrayList<Transaction> transaction) {
		this.transaction = transaction;
	}
}
