package com.ncr.eft;

import com.ncr.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class EyePayEftPlugin extends GenericEftPlugin {
	private static final Logger logger = Logger.getLogger(EyePayEftPlugin.class);
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

	/* Macro for payment voucher */
	private final String MACRO_case_number = "$CASE_NUMBER$";
	private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
	private final String MACRO_trans_number = "$TRANS_NUMBER$";
	private final String MACRO_rrn_number = "$RRN_NUMBER$";
	private final String MACRO_amount = "$AMOUNT$";

	/* parameter from properties file */
	private HashMap receiptDatas = new HashMap();
    private HashMap errorCodeMap = new HashMap<String, Integer>();

	public EyePayEftPlugin() {
        loadErrorCodeMap();
	}

    public void loadEftTerminalParams(int line, String txt) {
		super.loadEftTerminalParams(line, txt);

		if (line == 0) {
			retryEvery = Long.parseLong(txt.substring(2, 5));
			maxRetry = Integer.parseInt(txt.substring(6, 8));
			serialPort = txt.substring(9, 13);
		}
	}

	public int doTransactionWithStatusCheck(Itemdata itm, String traNum, LinIo line) {
		logger.debug("ENTER doTransactionWithStatusCheck");
		logger.info("amt: " + itm.pos);
		logger.info("traNum: " + traNum);

		String port = "COM1";
		int result = ERR_OK;
		String amountEyePay = String.valueOf(itm.pos);
		byte[] returnByteArray = new byte[100];

		StartEyePay eyePay = new StartEyePay();

		int ris = (int)eyePay.sendRequest(port, amountEyePay, returnByteArray);
		logger.info("result of DoEyePay: " + ris);

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

					logger.info("EyePayEftPlugin - Error code: " + errorCode);
					if (errorCode == 0) {
						addReceiptValues(eyePay);  //EFT-CGA

						authorizationCode = new String(eyePay.getApprovalCode());
						cardNumber = new String(eyePay.getCardNumber());
                        cardType = new String(eyePay.getCardType()); //AMZ-2017-002#ADD -- MA NON SO SE E' OK
						Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, itm.pos);
					} else {
						if (errorCode < 1000) {
							result = errorCode + 1000;
						} else {
							result = errorCode;
						}
						logger.info("EyePayEftPlugin - result error code: " + result);
						logger.info("set result as ERRORS");
						result = ERR_RESPONSE;
					}
				} catch (Exception e) {
					logger.debug("EXIT Exception [" + e.getMessage() + "]");
					return ERR_NOTAUTHORIZED;
				}
			}
		}

		Itmdc.IDC_write('t', Struc.tra.tnd, 0, eyePay.getBeneficiary(), 0, itm.pos);
		Itmdc.IDC_write('b', Struc.tra.tnd, 0, eyePay.getRefNumber(), 1, itm.pos);

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

	private String manageMacro(String line, StartEyePay ecr) {
		logger.debug("ENTER manageMacro - line: " + line);

		String logline = "";

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

		logger.info("line: " + logline);
		logger.debug("EXIT manageMacro");
		return line;
	}

	@Override
	public String getTenderId() {
		return EYEPAY_TENDER_ID;
	}
}