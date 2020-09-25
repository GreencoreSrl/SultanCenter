package com.ncr;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA. User: caterinagalati Date: 16/11/16 Time: 12:03
 */


public class EftTerminalEyePay implements EftTerminal {
	private static final Logger logger = Logger.getLogger(EftTerminalEyePay.class);
    private boolean enableSecondCopy = false;
	private long retryEvery = 500;
	private int maxRetry = 10;
	private String serialPort = "COM1";

	// Return codes
	static final int ERR_OK = 0;
	static final int ERR_NOTCONNECTED = 70;
	static final int ERR_TIMEOUTTRANSACTION = 71;
	static final int ERR_NOTAUTHORIZED = 72;
	static final int ERR_RESPONSE = 73;
	static final int ERR_PARAMETERS = 106;
	static final int ERR_PORT = 107;
	static final int ERR_INVALID_CARD = 92;
	static final int ERR_CARD_EXPIRED = 93;
	static final int ERR_COMMUNICATION_FAILURE = 94;
	static final int ERR_DECLINED = 95;
	static final int ERR_INCORRECT_PIN = 96;

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
	private final String MACRO_case_number = "$CASE_NUMBER$";
	private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
	private final String MACRO_trans_number = "$TRANS_NUMBER$";
	private final String MACRO_rrn_number = "$RRN_NUMBER$";
	private final String MACRO_amount = "$AMOUNT$";



	/* parameter from properties file */
	private HashMap receiptDatas = new HashMap();
	private String authorizationCode = "";

	public static final String DEFAULT_APPLICATION_ID = "11";
	private String cardNumber = "";
    private HashMap errorCodeMap = new HashMap<String, Integer>();

    private String cardType; //AMZ-2017-002#ADD

	private static EftTerminalEyePay instance = null;

	public static EftTerminalEyePay getInstance() {
		if (instance == null)
			instance = new EftTerminalEyePay();

		return instance;
	}
	//1610 end
	//public EftTerminalEyePay() { //1610

	private EftTerminalEyePay() {
		//loadReciptRows();  //EFT-CGA
        loadErrorCodeMap();
	}
    // AMZ-2017-002#BEG
    public String getCardType() {
        return cardType;
    }
    // AMZ-2017-002#END

    public void loadEftTerminalParams(String txt) {
		enableSecondCopy = txt.substring(0, 1).equals("1");
		retryEvery = Long.parseLong(txt.substring(2, 5));
		maxRetry = Integer.parseInt(txt.substring(6, 8));
		serialPort = txt.substring(9, 13);
	}

	public String getAuthorizationCode() {
		return authorizationCode;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public int doTransactionWithStatusCheck(long amt, String traNum, LinIo line) {
		logger.debug("ENTER doTransactionWithStatusCheck");
		logger.info("amt: " + amt);
		logger.info("traNum: " + traNum);

		String port = "COM1";
		int result = ERR_OK;
		String amountEyePay = String.valueOf(amt);
		byte[] returnByteArray = new byte[100];

		StartEyePay eyePay = new StartEyePay();

		int ris = (int)eyePay.sendRequest(port, amountEyePay, returnByteArray);
		logger.info("result of DoEyePay: " + ris);

		/*while (wrapper.rcvRES == 0) {
			// just for delay
			try {
				Thread.sleep(retryEvery);
				System.out.println("Slept " + retryEvery + " msec");
				if (retryNumber++ > maxRetry) {
					break;
				}
			} catch (InterruptedException e) {
				System.out.println("Exception = " + e.getMessage());
				e.printStackTrace();

				logger.debug("EXIT doTransactionWithStatusCheck - result: " + ERR_RESPONSE);
				return ERR_RESPONSE;
			}
		}*/

		switch (ris) {
			case -4:
				logger.info("Timeout Error");
				result = ERR_TIMEOUTTRANSACTION;

				break;
			case -3:
				logger.info("Unable to communicate with EyePay");
				result = ERR_NOTAUTHORIZED;

				break;
			case -2:
				logger.info("Failed to set serial communication default parameters");
				result = ERR_PARAMETERS;

				break;
			case -1:
				logger.info("Could not open serial communication port that was requested");
				result = ERR_PORT;

				break;
			case 0:
				logger.info("OK Received data");

				logger.info("Transaction Id: " + new String(eyePay.getTransId()));
				logger.info("Transaction Type: " + new String(eyePay.getTransType()));
				logger.info("Transaction Amount: " + new String(eyePay.getTransAmount()));
				logger.info("Terminal Id: " + new String(eyePay.getTerminalId()));
				logger.info("Auth Code: " + new String(eyePay.getApprovalCode()));
				logger.info("BadCardResult Code: " + new String(eyePay.getRspCode()));
				logger.info("Reference No: " + new String(eyePay.getRefNumber()));
				logger.info("Beneficiary No: " + new String(eyePay.getBeneficiary()));

				break;
			default:
				// error
				logger.info("Invalid value");
				result = ERR_NOTAUTHORIZED;

				break;
		}

		if (result == ERR_OK) {
			logger.info("Error code sent: " + eyePay.getRspCode());
			if (eyePay.getRspCode().length() > 0) {

				try {
					Set keys = errorCodeMap.keySet();
					String rspCode = new String(eyePay.getRspCode()).trim();
					int errorCode = 0;

					if (keys.contains(rspCode)) {
						logger.info("Error code map contains this code");
						errorCode = Integer.parseInt(errorCodeMap.get(rspCode).toString());
					} else {
						logger.info("Error code map not contains this code");
						errorCode = Integer.parseInt(rspCode);
					}

					logger.info("EftTerminalEyePay - Error code: " + errorCode);
					if (errorCode == 0) {
						//addReceiptValues(NEW_RECEIPT_TYPE, eyePay);  //EFT-CGA
						//addReceiptValues(SAME_RECEIPT_TYPE, eyePay);  //EFT-CGA
						addReceiptValues(eyePay);  //EFT-CGA

						authorizationCode = new String(eyePay.getApprovalCode());
						cardNumber = new String(eyePay.getCardNumber());
                        cardType = new String(eyePay.getCardType()); //AMZ-2017-002#ADD -- MA NON SO SE E' OK
						Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, amt);
					} else {
						if (errorCode < 1000) {
							result = errorCode + 1000;
						} else {
							result = errorCode;
						}
						logger.info("EftTerminalEyePay - result error code: " + result);
						logger.info("set result as ERRORS");
						result = ERR_RESPONSE;
					}
				} catch (Exception e) {
					logger.debug("EXIT Exception [" + e.getMessage() + "]");
					return ERR_NOTAUTHORIZED;
				}
			}
		}

		Itmdc.IDC_write('t', Struc.tra.tnd, 0, eyePay.getBeneficiary(), 0, amt);
		Itmdc.IDC_write('b', Struc.tra.tnd, 0, eyePay.getRefNumber(), 1, amt);

		logger.debug("EXIT doTransactionWithStatusCheck - result: " + result);
		return result;
	}


	private void addReceiptValues(StartEyePay ecr) {  //EFT-CGA
		receiptDatas = new HashMap();
		addReceiptValues(NEW_RECEIPT_TYPE, ecr);
		addReceiptValues(SAME_RECEIPT_TYPE, ecr);
	}

	private void addReceiptValues(String type, StartEyePay ecr) {
		logger.debug("ENTER addReceiptValues");
		logger.info("type: " + type);
		try {
			File file = new File(type);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Vector voucher = new Vector();

			if (reader != null) {
				try {
					String line = null;

					while ((line = reader.readLine()) != null) {
						line = manageMacro(line, ecr);
						logger.info("line reader: " + line);

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
			logger.info("addReceiptValues exception : " + exception.toString());
			exception.printStackTrace();
			logger.debug("EXIT addReceiptValues 1");

			return;
		}
	}



	/*private void addReceiptValues(String type, StartEyePay ecr) {
		logger.debug("ENTER addReceiptValues");
		logger.info("type: " + type);

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
			logger.info("reader != null && file != null");

			try {
				file.seek(0);

				while ((line = reader.readLine()) != null) {
					line = manageMacro(line, ecr);
					logger.info("line reader: " + line);

					voucher.add(line);
				}
			} catch (Exception exception) {
				logger.info("addReceiptValues exception : " + exception.toString());
				exception.printStackTrace();
				logger.debug("EXIT addReceiptValues 1");

				return;
			}

			boolean contains = receiptDatas.containsKey(type);

			logger.info("receipt data contains type: " + contains);
			logger.info("voucher.size: " + voucher.size());
			if (contains && voucher.size() > 0) {
				tmp = (Vector) receiptDatas.remove(type);
				tmp.add(voucher);
				receiptDatas.put(type, tmp);
			} else {
				tmp = new Vector();
				tmp.add(voucher);
				receiptDatas.put(type, tmp);
			}
		} else {
			logger.info("ERROR in addReceiptValues() , textVoucher is null check file S_PLU");
		}

		logger.debug("EXIT addReceiptValues 2");
	}*/


	private String manageMacro(String line, StartEyePay ecr) {
		logger.debug("ENTER manageMacro - line: " + line);

		String logline = "";

		//while (hasMacro(line)) {  //EFT-CGA
		logger.info("hasMacro(line) is true");

		if (line.indexOf(MACRO_case_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_case_number)) + new String(ecr.getBeneficiary()) +
					line.substring(line.indexOf(MACRO_case_number) + MACRO_case_number.length());

			logger.info("replace MACRO_case_number");

			logline = line;
		}

		if (line.indexOf(MACRO_amount) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_amount)) + ecr.getAmtEyePay() +
					line.substring(line.indexOf(MACRO_amount) + MACRO_amount.length());

			logger.info("replace MACRO_amount");

			logline = line;
		}

		if (line.indexOf(MACRO_authorisation_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_authorisation_number)) + new String(ecr.getApprovalCode()) +
					line.substring(line.indexOf(MACRO_authorisation_number) + MACRO_authorisation_number.length());

			logger.info("replace MACRO_authorisation_number");

			logline = line;
		}

		if (line.indexOf(MACRO_trans_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_trans_number)) + new String(ecr.getTransId()) +
					line.substring(line.indexOf(MACRO_trans_number) + MACRO_trans_number.length());

			logger.info("replace MACRO_trans_number");

			logline = line;
		}

		if (line.indexOf(MACRO_rrn_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_rrn_number)) + new String(ecr.getRefNumber()) +
					line.substring(line.indexOf(MACRO_rrn_number) + MACRO_rrn_number.length());

			logger.info("replace MACRO_rrn_number");

			logline = line;
		}
		//}

		logger.info("line: " + logline);
		logger.debug("EXIT manageMacro");
		return line;
	}

	/*private boolean hasMacro(String row) {  //EFT-CGA
		logger.debug("EXIT hasMacro - row: " + row);

		return (row.indexOf(MACRO_case_number) >= 0) || (row.indexOf(MACRO_authorisation_number) >= 0) || (row.indexOf(MACRO_amount) >= 0) ||
				(row.indexOf(MACRO_trans_number) >= 0) || (row.indexOf(MACRO_rrn_number) >= 0) ;
	}*/


	@Override
	public void printVouchers(String type) {
		logger.debug("ENTER printVouchers");
		logger.info("printVouchers EyePay type: " + type);

		Vector container;
		Vector tmp;

		if (type.equals(NEW_RECEIPT_TYPE) && !EftTerminalGeidea.isEnableSecondCopy()) {
			logger.info("exit printVouchers EyePay 1");
			return;
		}

		if ((Struc.tra.mode == Struc.M_CANCEL) || (Struc.tra.mode == Struc.M_SUSPND)) {
			logger.info("exit printVouchers EyePay 2");
			return;
		}

		if (receiptDatas.containsKey(type)) {
			logger.info("receiptDatas contains type");

			container = (Vector) receiptDatas.remove(type);

			if (container != null && container.size() > 0) {
				/*if (type.equals(NEW_RECEIPT_TYPE)) {
					logger.info("type is NEW_RECEIPT_TYPE");

					GdRegis.set_tra_top();
				} */

				logger.info("container isn't empty");

				for (int i = 0; i < container.size(); ++i) {
					tmp = (Vector) container.get(i);
					logger.info("tmp size: " + tmp.size());

					for (int j = 0; j < tmp.size(); ++j) {
						System.out.println("On additional receipt printing: " + tmp.get(j));
						Struc.prtLine.init((String) tmp.get(j)).book(3);
					}

					if (type.equals(NEW_RECEIPT_TYPE)) {
						logger.info("type is NEW_RECEIPT_TYPE");
						//GdRegis.prt_trailer(2);
						GdRegis.hdr_print();
					} else {
						logger.info("type is SAME_RECEIPT_TYPE");
						Struc.prtLine.init(' ').book(3);
					}
				}
			}
		}

		logger.info("exit printVouchers EyePay 2");
	}

	/*public void loadReciptRows() { //EFT-CGA
		logger.debug("ENTER loadReciptRows");

		fileNewReceipt = null;
		textVoucherNewReceipt = null;
		try {
			fileNewReceipt = new RandomAccessFile("S_PLURCO.DAT", "r");
			textVoucherNewReceipt = new BufferedReader(new FileReader(fileNewReceipt.getFD()));
		} catch (Exception E) {
			logger.debug("EXIT loadReciptRows - Error on reading S_PLUEYP.DAT file :" + E.getMessage());
			return;
		}

		fileSameReceipt = null;
		textVoucherSameReceipt = null;
		try {
			fileSameReceipt = new RandomAccessFile("S_PLURCP.DAT", "r");
			textVoucherSameReceipt = new BufferedReader(new FileReader(fileSameReceipt.getFD()));
		} catch (Exception E) {
			logger.debug("EXIT loadReciptRows - Error on reading S_PLURCP.DAT file :" + E.getMessage());
			return;
		}
	}*/

    private void loadErrorCodeMap() {
		logger.debug("ENTER loadErrorCodeMap");

		Properties prop = new Properties();

		try {
			prop.load(new FileInputStream("conf/errorCodes.properties"));

			for (Object key : prop.keySet().toArray()) {
				if (key.toString().startsWith("hashMap")) {
					String value = prop.getProperty(key.toString());
					errorCodeMap.put(key.toString().substring(8), value);
				}
			}
		} catch (Exception e) {
			logger.info("EXCEPTION " + e.getMessage());
		}

		logger.debug("EXIT loadErrorCodeMap");
	}
}