package com.ncr.eft;

import com.ncr.*;
import ecr.*;
import org.apache.log4j.Logger;
import java.util.*;

public class AlshayaEftPlugin extends GenericEftPlugin {
	private static final Logger logger = Logger.getLogger(AlshayaEftPlugin.class);
	private long retryEvery = 500;
	private int maxRetry = 10;

	private boolean testEnvironment = false;

	public AlshayaEftPlugin() {
        loadErrorCodeMap();
	}

    public void loadEftTerminalParams(int line, String txt) {
		super.loadEftTerminalParams(line, txt);

		if (line == 0) {
			retryEvery = Long.parseLong(txt.substring(2, 5));
			maxRetry = Integer.parseInt(txt.substring(6, 8));
			testEnvironment  = txt.substring(39, 40).equals("1");
		}
	}

	public int doTransactionWithStatusCheck(Itemdata itm, String traNum, LinIo line) {
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

		if (testEnvironment) {
			authorizationCode = "12345";
			cardNumber = "1234567890123456";
			cardType = "SPAN";
			terminalId = "222222222";
			return ERR_OK;
		}
		int stat = ecr.openComm();
		ecr.sendPurRequest(ecrNo, traNum.getBytes(), String.valueOf(itm.pos).getBytes(), addField1, addField2, addField3, addField4, addField5);

		while (ecr.rcvRES == 0) {
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
                    logger.info("AlshayaEftPlugin - Error code: " + errorCode);
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

                            authorizationCode = new String(ecr.getRspAuthCode());
                            cardNumber = new String(ecr.getRspPan());
                            cardType = new String(ecr.getRspCardType()); //AMZ-2017-002#ADD
							terminalId = new String(ecr.getRspAddLineMsg1());
							authorizedAmount = Long.parseLong(new String(ecr.getRspAmount()));
							addReceiptValues();
                            Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, itm.pos);
                            break;

                        default:
                            if (errorCode < 1000) {
                                result = errorCode + 1000;
                            } else {
                                result = errorCode;
                            }
                            logger.info("AlshayaEftPlugin - result error code: " + result);

                            break;
                    }
				} catch (Exception e) {
					System.out.println("Exception [" + e.getMessage() + "]");
					return ERR_NOTAUTHORIZED;
				}
			}
		}
		return result;
	}

	@Override
	public String getTenderId() {
		return ALSHAYA_TENDER_ID;
	}
}