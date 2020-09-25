package com.ncr.capillary.TransactionDetails;

import java.util.ArrayList;

public class LineItems {
	private ArrayList<LineItem> lineItem = new ArrayList<LineItem>();

	public ArrayList<LineItem> getLineItem() {
		return lineItem;
	}

	public void setLineItem(ArrayList<LineItem> lineItem) {
		this.lineItem = lineItem;
	}
}
