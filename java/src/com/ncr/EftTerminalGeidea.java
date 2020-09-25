package com.ncr;

import SPAN.SPAN;
import org.apache.log4j.Logger;
import org.omg.IOP.ExceptionDetailMessage;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA. User: stefanobertarello Date: 26/05/14 Time: 12:03
 */
public class EftTerminalGeidea implements EftTerminal {
	private static final Logger logger = Logger.getLogger(EftTerminalGeidea.class);
	private static boolean enableSecondCopy = false;
	private long retryEvery = 500;
	private int maxRetry = 10;
	private String serialPort = "COM1";

	// Return codes
	static final int ERR_OK = 0;
	static final int ERR_NOTCONNECTED = 70;
	static final int ERR_TIMEOUTTRANSACTION = 71;
	static final int ERR_NOTAUTHORIZED = 72;
	static final int ERR_RESPONSE = 73;

	static final String IDLE_SCREEN = "39";

	// Objects to manage voucher row file*/
	private BufferedReader textVoucherNewReceipt;
	private RandomAccessFile fileNewReceipt;
	private BufferedReader textVoucherSameReceipt;
	private RandomAccessFile fileSameReceipt;

	/* Constant to identify voucher type */
	//public static final String NEW_RECEIPT_TYPE = "NEW_RECEIPT";  //EFT-CGA
	//public static final String SAME_RECEIPT_TYPE = "SAME_RECEIPT";  //EFT-CGA
	public static final String NEW_RECEIPT_TYPE = "S_PLURCO.DAT";  //EFT-CGA
	public static final String SAME_RECEIPT_TYPE = "S_PLURCP.DAT";  //EFT-CGA

	/* Macro for payment voucher */
	private final String MACRO_cc_number = "$CC_NUMBER$";
	private final String MACRO_amount = "$AMOUNT$";
	private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
	private final String MACRO_card_scheme_name = "$SCHEME_NAME$";

	/* parameter from properties file */
	private HashMap receiptDatas = new HashMap();
	private String authorizationCode = "";

	public static final String DEFAULT_APPLICATION_ID = "11";
	private String cardNumber;
    private String cardType; //AMZ-2017-002#ADD

	public EftTerminalGeidea() {

		//loadReciptRows(); //EFT-CGA
	}

	public void loadEftTerminalParams(String txt) {
		enableSecondCopy = txt.substring(0, 1).equals("1");
		retryEvery = Long.parseLong(txt.substring(2, 5));
		maxRetry = Integer.parseInt(txt.substring(6, 8));
		serialPort = txt.substring(9, 13);
	}

	public static boolean isEnableSecondCopy() {
		return enableSecondCopy;
	}

	public String getAuthorizationCode() {
		return authorizationCode;
	}

	public String getCardNumber() {
		return cardNumber;
	}

    // AMZ-2017-002#BEG
    public String getCardType() {
        return cardType;
    }
    // AMZ-2017-002#END

	public int doTransactionWithStatusCheck(long amt, String traNum, LinIo line) {
		SPAN span = new SPAN();
		int result = doTransaction(amt, traNum, line, span);
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
			addReceiptValues(NEW_RECEIPT_TYPE);
			addReceiptValues(SAME_RECEIPT_TYPE);

			authorizationCode = SPAN.AuthResponse;
			cardNumber = SPAN.CardNumber;
            cardType = SPAN.MerchantID; //AMZ-2017-002#ADD
			Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, amt);

			return ERR_OK;
		}
	}

	private void addReceiptValues(String type) {  //EFT-CGA
		try {
			File file = new File(type);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Vector voucher = new Vector();

			if (reader != null) {
				try {
					String line = null;

					while ((line = reader.readLine()) != null) {
						line = manageMacro(line);
						voucher.add(line);
					}

				} catch (Exception e) {
					logger.error("addReceiptValues exception : ", e);
					return;

				}
				receiptDatas.put(type, voucher);
			} else {
				logger.info("Error in addReceiptValues() , textVoucher is null check file S_PLU");
			}
		} catch (Exception exception) {
			System.out.println("addReceiptValues exception : " + exception.toString());
			exception.printStackTrace();
			return;
		}
	}

	/*private void addReceiptValues(String type) {  //EFT-CGA
		Vector voucher = new Vector();
		Vector tmp;
		String line;
		BufferedReader reader = null;
		RandomAccessFile file = null;

		if (type.equals(NEW_RECEIPT_TYPE)) {
			reader = textVoucherNewReceipt;
			file = fileNewReceipt;
		} else if (type.equals(SAME_RECEIPT_TYPE)) {
			reader = textVoucherSameReceipt;
			file = fileSameReceipt;
		}

		if (reader != null && file != null) {

			try {
				file.seek(0);

				while ((line = reader.readLine()) != null) {
					line = manageMacro(line);
					voucher.add(line);
				}
			} catch (Exception exception) {
				System.out.println("addReceiptValues exception : " + exception.toString());
				exception.printStackTrace();
				return;
			}
			if (receiptDatas.containsKey(type) && voucher.size() > 0) {
				tmp = (Vector) receiptDatas.remove(type);
				tmp.add(voucher);
				receiptDatas.put(type, tmp);
			} else {
				tmp = new Vector();
				tmp.add(voucher);
				receiptDatas.put(type, tmp);
			}
		} else {
			System.out.println("Error in addReceiptValues() , textVoucher is null check file S_PLU");
		}
	}*/

	private String manageMacro(String line) {

		//while (hasMacro(line)) {  //EFT-CGA
		if (line.indexOf(MACRO_authorisation_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_authorisation_number)) + SPAN.AuthResponse + line
					.substring(line.indexOf(MACRO_authorisation_number) + MACRO_authorisation_number.length());
		}
		if (line.indexOf(MACRO_amount) >= 0 && SPAN.Amount.length() > 0) {
			line = line.substring(0, line.indexOf(MACRO_amount)) + GdRegis.editMoney(0, Long.parseLong(SPAN.Amount))
					+ line.substring(line.indexOf(MACRO_amount) + MACRO_amount.length());
		}
		if (line.indexOf(MACRO_cc_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_cc_number)) + SPAN.CardNumber
					+ line.substring(line.indexOf(MACRO_cc_number) + MACRO_cc_number.length());
		}
		if (line.indexOf(MACRO_card_scheme_name) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_card_scheme_name)) + SPAN.MerchantID
				+ line.substring(line.indexOf(MACRO_card_scheme_name) + MACRO_card_scheme_name.length());
		}
		//}
		return line;
	}

	/*private boolean hasMacro(String row) {  //EFT-CGA
		return (row.indexOf(MACRO_authorisation_number) >= 0) || (row.indexOf(MACRO_amount) >= 0)
				|| (row.indexOf(MACRO_cc_number) >= 0) || (row.indexOf(MACRO_card_scheme_name) >= 0);
	}*/

	public void printVouchers(String type) {
		logger.debug("ENTER");
		logger.info("printVouchers type: " + type);

		Vector container;
		Vector tmp;

		if ((Struc.tra.mode == Struc.M_CANCEL) || (Struc.tra.mode == Struc.M_SUSPND)) {
			logger.info("exit printVouchers 1");
			return;
		}

		if (receiptDatas.containsKey(type)) {
			logger.info("receiptDatas contains type");

			container = (Vector) receiptDatas.remove(type);

			if (container != null && container.size() > 0 && EftTerminalGeidea.isEnableSecondCopy()) {
				if (type.equals(NEW_RECEIPT_TYPE)) {
					logger.info("type is NEW_RECEIPT_TYPE");

					GdRegis.set_tra_top();
				}

				for (int i = 0; i < container.size(); ++i) {
					tmp = (Vector) container.get(i);
					logger.info("tmp size: " + tmp.size());

					for (int j = 0; j < tmp.size(); ++j) {
						System.out.println("On additional receipt printing: " + tmp.get(j));
						Struc.prtLine.init((String) tmp.get(j)).book(3);
					}

					if (type.equals(NEW_RECEIPT_TYPE)) {
						logger.info("type is NEW_RECEIPT_TYPE");
						GdRegis.prt_trailer(2);
					} else {
						logger.info("type is SAME_RECEIPT_TYPE");
						Struc.prtLine.init(' ').book(3);
					}
				}
			}
		}

		logger.debug("EXIT");
	}

	/*public void loadReciptRows() {  //EFT-CGA
		fileNewReceipt = null;
		textVoucherNewReceipt = null;
		try {
			fileNewReceipt = new RandomAccessFile("S_PLURCO.DAT", "r");
			textVoucherNewReceipt = new BufferedReader(new FileReader(fileNewReceipt.getFD()));
		} catch (Exception E) {
			System.out.println("Error on reading S_PLURCO.DAT file :" + E.getMessage());
			return;
		}

		fileSameReceipt = null;
		textVoucherSameReceipt = null;
		try {
			fileSameReceipt = new RandomAccessFile("S_PLURCP.DAT", "r");
			textVoucherSameReceipt = new BufferedReader(new FileReader(fileSameReceipt.getFD()));
		} catch (Exception E) {
			System.out.println("Error on reading S_PLURCP.DAT file :" + E.getMessage());
			return;
		}
	}*/
}
