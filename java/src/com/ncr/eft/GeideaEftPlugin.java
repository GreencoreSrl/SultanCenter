package com.ncr.eft;

import SPAN.SPAN;
import com.ncr.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class GeideaEftPlugin extends GenericEftPlugin {
	private static final Logger logger = Logger.getLogger(GeideaEftPlugin.class);
	private long retryEvery = 500;
	private int maxRetry = 10;
	private String serialPort = "COM1";

	public static final String DEFAULT_APPLICATION_ID = "11";

	public GeideaEftPlugin() {
	}

	public void loadEftTerminalParams(int line, String txt) {
		super.loadEftTerminalParams(line, txt);

		if (line == 0) {
			retryEvery = Long.parseLong(txt.substring(2, 5));
			maxRetry = Integer.parseInt(txt.substring(6, 8));
			serialPort = txt.substring(9, 14).trim();
		}
	}

	public int doTransactionWithStatusCheck(Itemdata itm, String traNum, LinIo line) {
		SPAN span = new SPAN();
		int result = doTransaction(itm.pos, traNum, line, span);
		String answer = "";
		int retryNumber = 0;

		System.out.println("Result of transaction [" + result + "]");

		if (result != ERR_NOTCONNECTED) {
			try {
				do {
					System.out.println("Checking status for the " + retryNumber + " time");
					answer = span.CheckStatus();
					System.out.println("checkStatus result [" + answer + "]");
					Thread.sleep(retryEvery);
					System.out.println("Slept " + retryEvery + " msec");
					if (retryNumber > 10 && answer.length() > 5 && answer.charAt(4) == '|') {
						line.init(answer.substring(5)).show(2);
					}
				} while (answer.startsWith("3004|") && ++retryNumber <= maxRetry);
			} catch (Exception e) {
				System.out.println("Exception = " + e.getMessage());
				e.printStackTrace();
				return ERR_RESPONSE;
			}
		}
        // result = 0; // AMZ-2017-002#TEST-ADD
		return result;
	}

	public int doTransaction(long amt, String traNum, LinIo line, SPAN span) {
		boolean result;
		String answer = "";
		int retryNumber = 0;
		String[] responseCodes = { "48", "49", "4D", "46", "4A", "52", "51", "50", "41", "47", "43", "42", "44", "45",
				"55", "58", "4F" };

		System.out.println("Connecting to port [" + serialPort + "]");
		result = span.Connect(serialPort);
		System.out.println("Connect result [" + result + "]");

		if (!result) {
			return ERR_NOTCONNECTED;
		}

		DecimalFormat df = new DecimalFormat("#.00");
		System.out.println("Sending data [" + df.format((double) amt / 100) + "] [" + traNum + "] ["
				+ DEFAULT_APPLICATION_ID + "]");
		answer = span.SendData(df.format((double) amt / 100), traNum, DEFAULT_APPLICATION_ID);
		System.out.println("SendData result [" + answer + "]");

		try {
			Thread.sleep(retryEvery * 2);
			do {
				System.out.println("Getting data for the " + retryNumber + " time");
				answer = span.getData();
				System.out.println("getData result [" + answer + "]");
				System.out.println("SPAN.TerminalID [" + SPAN.TerminalID + "]");
				System.out.println("SPAN.CardNumber [" + SPAN.CardNumber + "]");
				System.out.println("SPAN.Amount [" + SPAN.Amount + "]");
				System.out.println("SPAN.CardType [" + SPAN.CardType + "]");
				System.out.println("SPAN.MerchantID [" + SPAN.MerchantID + "]");
				System.out.println("SPAN.RRN [" + SPAN.RRN + "]");
				System.out.println("SPAN.AuthResponse [" + SPAN.AuthResponse + "]");
				System.out.println("SPAN.ResponseCode [" + SPAN.ResponseCode + "]");
				System.out.println("SPAN.ApprovedPurchase [" + SPAN.ApprovedPurchase + "]");
				if (answer.length() > 3 && answer.charAt(2) == '|'
						&& new ArrayList<String>(Arrays.asList(responseCodes)).contains(answer.substring(0, 2))) {
					line.init(answer.substring(3)).show(2);
				}
				Thread.sleep(retryEvery);
                // AMZ-2017-002#TEST BEG
                /*
                SPAN.CardNumber = "123456789000000";
                SPAN.TerminalID = "22224444";
                SPAN.Amount = "100                 ";
                SPAN.ApprovedPurchase = true;
                SPAN.MerchantID = "VC-VISA";
                */
                // break;
                // AMZ-2017-002#TEST END
				System.out.println("Slept " + retryEvery + " msec");
			} while ((SPAN.CardNumber.length() < 6 || SPAN.TerminalID.length() < 8 || SPAN.Amount.length() < 12)
					&& !answer.startsWith("39|") && ++retryNumber <= maxRetry);
			if (!answer.startsWith("39|")) {
				line.init("").show(2);
			}
		} catch (Exception e) {
			System.out.println("Exception = " + e.getMessage());
			e.printStackTrace();

			System.out.println("return ERR_RESPONSE - 1");
			return ERR_RESPONSE;
		}
		if (SPAN.CardNumber == null || SPAN.CardNumber.length() == 0) {
			System.out.println("return ERR_RESPONSE - 2");
			return ERR_RESPONSE;
		} else if (SPAN.Amount == null || SPAN.Amount.length() == 0) {
			System.out.println("return ERR_RESPONSE - 3");
			return ERR_RESPONSE;
		}

		if (retryNumber > maxRetry) {
			return ERR_TIMEOUTTRANSACTION;
		} else if (!SPAN.ApprovedPurchase) {
			return ERR_NOTAUTHORIZED;
		} else {
			authorizationCode = SPAN.AuthResponse;
			cardNumber = SPAN.CardNumber;
            cardType = SPAN.CardType;
            terminalId = SPAN.TerminalID;
            authorizedAmount = Long.parseLong(SPAN.Amount);
            receiptNumber = SPAN.ECRNumber;
            rrn = SPAN.RRN;
			addReceiptValues();

			Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, amt);
			return ERR_OK;
		}
	}

	@Override
	public String getTenderId() {
		return GEIDEA_TENDER_ID;
	}
}
