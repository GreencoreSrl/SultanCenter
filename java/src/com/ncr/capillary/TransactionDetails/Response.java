package com.ncr.capillary.TransactionDetails;

public class Response {
	private Status status = new Status();
	private Transactions transactions = new Transactions();

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Transactions getTransactions() {
		return transactions;
	}

	public void setTransactions(Transactions transactions) {
		this.transactions = transactions;
	}
}
