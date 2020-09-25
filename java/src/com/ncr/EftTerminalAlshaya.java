package com.ncr;

import ecr.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
/**
 * Created by IntelliJ IDEA. User: stefanobertarello Date: 26/05/14 Time: 12:03
 */
public class EftTerminalAlshaya implements EftTerminal {
	private static final Logger logger = Logger.getLogger(EftTerminalAlshaya.class);
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
	private final String MACRO_cc_number = "$CC_NUMBER$";
	private final String MACRO_amount = "$AMOUNT$";
	private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
	private final String MACRO_card_scheme_name = "$SCHEME_NAME$";

	/* parameter from properties file */
	private HashMap receiptDatas = new HashMap();
	private String authorizationCode = "";

	public static final String DEFAULT_APPLICATION_ID = "11";
	private String cardNumber;
    private HashMap errorCodeMap = new HashMap<String, Integer>();

    private String cardType; //AMZ-2017-002#ADD

	private static EftTerminalAlshaya instance = null;

	public static EftTerminalAlshaya getInstance() {
		if (instance == null)
			instance = new EftTerminalAlshaya();

		return instance;
	}

	//public EftTerminalAlshaya() {
	private EftTerminalAlshaya() {  //1610 end
		//loadReciptRows();  //EFT-CGA
        loadErrorCodeMap(); // ENH-20160107-CGA#A
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
		byte[] ecrNo = new byte[] { 0x30, 031 };
		byte[] addField1 = new byte[] { 0x41 };
		byte[] addField2 = new byte[] { 0x42 };
		byte[] addField3 = new byte[] { 0x43 };
		byte[] addField4 = new byte[] { 0x44 };
		byte[] addField5 = new byte[] { 0x45 };
		String port = "COM1";
		StartAUECR ecr = new StartAUECR(port);
		int retryNumber = 0;
		int result = ERR_OK;

		int stat = ecr.openComm();
		ecr.sendPurRequest(ecrNo, traNum.getBytes(), String.valueOf(amt).getBytes(), addField1, addField2, addField3, addField4, addField5);

		while (ecr.rcvRES == 0) { //rcvRES 0 = no result yet, 1 = response received, 2 = timeout, 3 = invalid value
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
				return ERR_RESPONSE;
			}
		}
		switch (ecr.rcvRES) {
			case 0:
				System.out.println("Timeout POS");
				result = ERR_TIMEOUTTRANSACTION;
				break;
			case 1:
				System.out.println("OK Received data");
				System.out.println("Amount: " + new String(ecr.getRspAmount()));
				System.out.println("Auth Code: " + new String(ecr.getRspAuthCode()));
				System.out.println("Expiry Date: " + new String(ecr.getRspExpiryDate()));
				System.out.println("Card Type: " + new String(ecr.getRspCardType()));
				System.out.println("ECR No: " + new String(ecr.getRspEcrNo()));
				System.out.println("ECR Receipt No: " + new String(ecr.getRspEcrRcptNo()));
				System.out.println("PAN: " + new String(ecr.getRspPan()));
				System.out.println("Transaction Date: " + new String(ecr.getRspTranDate()));
				System.out.println("Transaction Time: " + new String(ecr.getRspTranTime()));
				System.out.println("Reference No: " + new String(ecr.getRspRrn()));
				System.out.println("Add Line Message 1 : " + new String(ecr.getRspAddLineMsg1()));
				System.out.println("Add Line Message 2 : " + new String(ecr.getRspAddLineMsg2()));
				System.out.println("Add Line Message 3 : " + new String(ecr.getRspAddLineMsg3()));
				System.out.println("Add Line Message 4 : " + new String(ecr.getRspAddLineMsg4()));
				break;
			case 2:
				// timeout ECR
				System.out.println("Timeout ECR");
				result = ERR_TIMEOUTTRANSACTION;
				break;
			default:
				// error
				System.out.println("Invalid value");
				result = ERR_NOTAUTHORIZED;
				break;
		}
		ecr.closeComm();

		System.out.println("Result of transaction [" + result + "]");

		if (result == ERR_OK) {
			if (ecr.getRspCode().length > 0) {
				try {
                    // ENH-20160107-CGA#A BEG
                    Set keys = errorCodeMap.keySet();
                    String rspCode = new String(ecr.getRspCode()).trim();
                    int errorCode = 0;

                    logger.info("Error code sent: " + rspCode);
                    if (keys.contains(rspCode)) {
                        logger.info("Error code map contains this code");

                        errorCode = Integer.parseInt(errorCodeMap.get(rspCode).toString());
                    } else { // ENH-20160107-CGA#A END
                        logger.info("Error code map not contains this code");

    					errorCode = Integer.parseInt(rspCode);
                    }
                    logger.info("EftTerminalAlshaya - Error code: " + errorCode);
                    switch (errorCode) {
                        // ENH-20160107-CGA#A BEG
                        case 0:
                        case 1:
                        case 3:
                        case 7:
                        case 87:
                        case 89:
                        case 300:
                        case 400:
                        case 500:
                        case 800:
                            //addReceiptValues(NEW_RECEIPT_TYPE, ecr); //EFT-CGA
                            //addReceiptValues(SAME_RECEIPT_TYPE, ecr);  //EFT-CGA
							addReceiptValues(ecr);  //EFT-CGA

                            authorizationCode = new String(ecr.getRspAuthCode());
                            cardNumber = new String(ecr.getRspPan());
                            cardType = new String(ecr.getRspCardType()); //AMZ-2017-002#ADD
                            Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, amt);
                            break;

                        default:
                            if (errorCode < 1000) {
                                result = errorCode + 1000;
                            } else {
                                result = errorCode;
                            }
                            logger.info("EftTerminalAlshaya - result error code: " + result);

                            break;

                        /*case 0:
                            addReceiptValues(NEW_RECEIPT_TYPE, ecr);
                            addReceiptValues(SAME_RECEIPT_TYPE, ecr);

                            authorizationCode = new String(ecr.getRspAuthCode());
                            cardNumber = new String(ecr.getRspPan());
                            Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, amt);
                            break;

                        case 111:
                                  // Invalid card
                                  result = ERR_INVALID_CARD;
                                  break;
                        case 101:
                              // Expired
                              result = ERR_CARD_EXPIRED;
                              break;
                        case 190:
                              // Communication failure
                              result = ERR_COMMUNICATION_FAILURE;
                              break;
                        case 116:
                              // Declined
                              result = ERR_DECLINED;
                              break;
                        case 117:
                              // Incorrect PIN
                              result = ERR_INCORRECT_PIN;
                              break;
                        default:
                              // Nessuno
                              System.out.println("Error Code [" + errorCode + "]");
                              result = ERR_NOTAUTHORIZED;
                              break;      */
                        // ENH-20160107-CGA#A END
                    }
				} catch (Exception e) {
					System.out.println("Exception [" + e.getMessage() + "]");
					return ERR_NOTAUTHORIZED;
				}
			}
		}
		return result;
	}

	private void addReceiptValues(StartAUECR ecr) {  //EFT-CGA
		receiptDatas = new HashMap();
		addReceiptValues(NEW_RECEIPT_TYPE, ecr);
		addReceiptValues(SAME_RECEIPT_TYPE, ecr);
	}

	private void addReceiptValues(String type, StartAUECR ecr) {  //EFT-CGA
		try {
			File file = new File(type);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Vector voucher = new Vector();

			if (reader != null) {
				try {
					String line = null;

					while ((line = reader.readLine()) != null) {
						line = manageMacro(line, ecr);
						voucher.add(line);
					}
				} catch (Exception exception) {
					logger.error("addReceiptValues exception : ", exception);
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


	/*
	private void addReceiptValues(String type, StartAUECR ecr) {  //EFT-CGA
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
					line = manageMacro(line, ecr);
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
	}
	*/

	private String manageMacro(String line, StartAUECR ecr) {

		//while (hasMacro(line)) {  //EFT-CGA
		if (line.indexOf(MACRO_authorisation_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_authorisation_number)) + new String(ecr.getRspAuthCode()) + line
					.substring(line.indexOf(MACRO_authorisation_number) + MACRO_authorisation_number.length());
		}
		if (line.indexOf(MACRO_amount) >= 0 && ecr.getRspAmount().length > 0) {
			line = line.substring(0, line.indexOf(MACRO_amount)) + GdRegis.editMoney(0, Long.parseLong(new String(ecr.getRspAmount())))
					+ line.substring(line.indexOf(MACRO_amount) + MACRO_amount.length());
		}
		if (line.indexOf(MACRO_cc_number) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_cc_number)) + new String(ecr.getRspPan())
					+ line.substring(line.indexOf(MACRO_cc_number) + MACRO_cc_number.length());
		}
		if (line.indexOf(MACRO_card_scheme_name) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_card_scheme_name)) + new String(ecr.getRspCardType())
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
		Vector container;
		Vector tmp;
		if ((Struc.tra.mode == Struc.M_CANCEL) || (Struc.tra.mode == Struc.M_SUSPND)) {
			return;
		}
		if (type.equals(SAME_RECEIPT_TYPE) && receiptDatas.containsKey(type)) {
			container = (Vector) receiptDatas.remove(SAME_RECEIPT_TYPE);
			if (container != null && container.size() > 0 && enableSecondCopy) {
				for (int i = 0; i < container.size(); ++i) {
					tmp = (Vector) container.get(i);
					for (int j = 0; j < tmp.size(); ++j) {
						System.out.println("Printing: " + tmp.get(j));
						Struc.prtLine.init((String) tmp.get(j)).book(3);
					}
					Struc.prtLine.init(' ').book(3);
				}
			}
		} else if (type.equals(NEW_RECEIPT_TYPE) && receiptDatas.containsKey(type)) {
			container = (Vector) receiptDatas.remove(NEW_RECEIPT_TYPE);
			if (container != null && container.size() > 0 && enableSecondCopy) {
				GdRegis.set_tra_top();
				for (int i = 0; i < container.size(); ++i) {
					tmp = (Vector) container.get(i);
					for (int j = 0; j < tmp.size(); ++j) {
						System.out.println("On additional receipt printing: " + tmp.get(j));
						Struc.prtLine.init((String) tmp.get(j)).book(3);
					}
					GdRegis.prt_trailer(2);
				}
			}
		}
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
    // ENH-20160107-CGA#A BEG
    private void loadErrorCodeMap() {
        logger.debug("ENTER");

        Properties prop = new Properties();
        String errorMessage = "";

        try {
            prop.load(new FileInputStream("conf/errorCodes.properties"));

            for (Object key : prop.keySet().toArray()) {
                if (key.toString().startsWith("hashMap")) {
                    String value = prop.getProperty(key.toString());
                    errorCodeMap.put(key.toString().substring(8), value);
                }
            }
        } catch(Exception e) {
            logger.info("EXCEPTION " + e.getMessage());
        }

        logger.debug("EXIT");
    }
    // ENH-20160107-CGA#A END
}