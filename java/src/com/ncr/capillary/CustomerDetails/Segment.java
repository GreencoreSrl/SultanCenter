package com.ncr.capillary.CustomerDetails;

public class Segment {
	private String name = "";
	private int type = 0;
	private Values values = new Values();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Values getValues() {
		return values;
	}

	public void setValues(Values values) {
		this.values = values;
	}
}
