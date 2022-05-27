package com.ncr.ssco.communication;

public enum SscoMessageFieldType {
	TYPE_INT, TYPE_STRING, TYPE_BIN_BASE64,TYPE_BOOLEAN, TYPE_UNKNOWN;

	public static SscoMessageFieldType create(String fieldType) {
		if (fieldType.equalsIgnoreCase("string")) {
			return TYPE_STRING;
		}
		if (fieldType.equalsIgnoreCase("int")) {
			return TYPE_INT;
		}
		if (fieldType.equalsIgnoreCase("integer")) {
			return TYPE_INT;
		}
		if (fieldType.equalsIgnoreCase("bin.base64")) {
			return TYPE_BIN_BASE64;
		}
		if (fieldType.equalsIgnoreCase("boolean")) {
			return TYPE_BOOLEAN;
		}

		return TYPE_UNKNOWN;
	}

}
