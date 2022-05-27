package com.ncr.ssco.communication.util;

public class Utilities {

	public static String rightFillWithSpaces(String message) {
		return rightFillWithSpaces(message, 25);
	}

	public static String rightFillWithSpaces(String message, int length) {
		int fillerlength = 0;
		if (message == null) {
			message = "";
		}
		fillerlength = length - message.length();
		if (fillerlength <= 0) {
			return message;
		}

		String filler = "";
		for (int i = 0; i < fillerlength; i++) {
			filler = filler + " ";
		}
		return message + filler;

	}

	public static String leftFillWithSpaces(String message) {
		return rightFillWithSpaces(message, 25);
	}

	public static String leftFillWithSpaces(String message, int length) {
		int fillerlength = 0;
		if (message == null) {
			message = "";
		}
		fillerlength = length - message.length();
		if (fillerlength <= 0) {
			return message;
		}

		String filler = "";
		for (int i = 0; i < fillerlength; i++) {
			filler = filler + " ";
		}
		return filler+ message ;

	}

}
