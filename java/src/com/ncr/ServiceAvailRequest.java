package com.ncr;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author daniele This class implements the Service Availability Request (code 0xC0)
 */
public class ServiceAvailRequest {
	private String message = "";

	private int msgLength = 0;
	private static final char MSG_CODE = 0xC0;
	private static final char MSG_VERSION = 0x01;
	private String transNumber = "";
	private int opType = 0;
	private static final char CURRENCY = 0x45; // 'E' Euro
	private String amount = "";
	private static final String GATEWAY_ID = "00";
	private static final String PROVIDER_ID = "00";
	private String productId = "";
	private int userDataType = 0;
	private String userData = "";

	private String extraData = "";
	private String cashier = "";
	private String receiptNumber = "";
	private int CRC = 0;

	public ServiceAvailRequest(Transact tran, Itemdata itm) {
		message = String.format("%c", MSG_CODE);
		message += String.format("%c", MSG_VERSION);
		message += String.format("%05s", tran.number);
		message += String.format("%d", opType);
		message += String.format("%c", CURRENCY);
		message += String.format("%09lf", itm.amt);
		message += String.format("%2s", GATEWAY_ID);
		message += String.format("%2s", PROVIDER_ID);
		message += String.format("%32s", itm.number);
		message += String.format("%d", userDataType);
		message += String.format("%20", userData);
		message += String.format("%02d", extraData.length());

		if (extraData.length() > 0)
			message += extraData;

		message += String.format("%08s", cashier);
		message += String.format("%08s", tran.number);
	}

	public void setExtraData(String data) {
		extraData = data;
	}

}
