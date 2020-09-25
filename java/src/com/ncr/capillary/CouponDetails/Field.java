package com.ncr.capillary.CouponDetails;

import java.util.ArrayList;

public class Field {
	private String name = "";
	private ArrayList<String> value = new ArrayList<String>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<String> getValue() {
		return value;
	}

	public void setValue(ArrayList<String> value) {
		this.value = value;
	}

}
