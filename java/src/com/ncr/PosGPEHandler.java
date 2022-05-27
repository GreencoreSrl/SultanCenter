package com.ncr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.ncr.gpe.*;
import org.apache.log4j.Logger;

public class PosGPEHandler implements MessageToPosHandlerInterface {
	private static final Logger logger = Logger.getLogger(PosGPEHandler.class);
	public static final int COPIES = 2;
	private final String MACRO_ERROR_DESCRIPTION = "$ERROR_DESCRIPTION$";
	private final String MACRO_ERROR_CODE = "$ERROR_CODE$";
	private CommandFromPos currentCommand = null;

	private Map processors;

	PosGPEHandler() {
		processors = new HashMap();
		processors.put(GpeResult_ReceiptDataInterface.class, new PosGPEPaymentResult());
		processors.put(GpeResult_ListOfOptionsInterface.class, new PosGPEOptionListHandler());
		processors.put(GpeResult_PanCheckingDataInterface.class, new PosGPEPanChecking());
		processors.put(GpeResult_PaymentAckFromPosInterface.class, new PosGPEAckReceived());
	}

	PosGPEHandler(CommandFromPos cmd) {
		this();
		currentCommand = cmd;
	}

	public void toPosHandleSuccess(Map messageMap) {
		logger.info("ENTER toPosHandleSuccess");

		GpeResultProcessorInterface processor;

		PosGPE.doLog("PosHandleSuccess");
		PosGPE.sts = 2;
		Class typeOfResponse = DefaultGpe.getResponseType(messageMap);

		if ((Integer) messageMap.get("UsedRetries") != null) {
			if (!String.valueOf((Integer) messageMap.get("UsedRetries")).equals("-1")) {
				logger.info("call setRetry");

				PosGPE.setRetry(((Integer) messageMap.get("UsedRetries")).intValue());
			}
		}
		PosGPE.crdSts = messageMap.get("CardStatus") != "NOT INSERTED";
		EptsCardInfo card = PosGPE.getLastEptsCardInfo();

		card.setTrack1((String) messageMap.get("Track1AsString"));
		card.setTrack2((String) messageMap.get("Track2AsString"));
		card.setTrack3((String) messageMap.get("Track3AsString"));
		if (typeOfResponse != null) {
			PosGPE.doLog("Non ho typeOfResponse");
			processor = (GpeResultProcessorInterface) processors.get(typeOfResponse);
			processor.processResult(messageMap);
		}

		logger.info("EXIT toPosHandleSuccess");
	}

	public void toPosHandleFailure(int errorCode, String errorDescription, Map messageMap) {
		logger.info("ENTER toPosHandleFailure");

		PosGPE.sts = 3;
		PosGPE.doLog("PosHandleFailure. Erorcode:" + errorCode + " Msg: " + errorDescription);
		PosGPE.deleteLastEptsReceiptData();
		Class typeOfResponse = DefaultGpe.getResponseType(messageMap);
		if (typeOfResponse != null) {
			PosGPE.doLog("Type of response: " + typeOfResponse);
			GpeResultProcessorInterface processor = (GpeResultProcessorInterface) processors.get(typeOfResponse);
			messageMap.put("ErrorCode", String.valueOf(errorCode));
			messageMap.put("ErrorDescription", errorDescription);
			messageMap.put("Failure", "true");
			processor.processResult(messageMap);
		} else {
			logger.info("Handling error: [" + errorCode + "] " + errorDescription);
			logger.info("Current command: " + currentCommand);
			if (currentCommand != null && currentCommand instanceof CommandFromPos_Payment) {
				printDefaultReceipt(errorCode, errorDescription);
			}
		}
		PosGPE.fail(errorCode, errorDescription);

		logger.info("EXIT toPosHandleFailure");
	}

	private void printDefaultReceipt(int errorCode, String errorDescription) {
		DevIo.addVoucherCopyNumber(COPIES);
		try {
			String filename = "S_WE" + Action.editNum(errorCode, 4) + ".DAT";
			File file = new File(filename);
			BufferedReader reader = new BufferedReader(new FileReader(file));

			if (reader != null) {
				CreditCardVoucher lineToAdd = new CreditCardVoucher();

				lineToAdd.setTypeOfLine('B');
				lineToAdd.setPrintedLineDescription("");
				logger.info("lineToAdd [" + lineToAdd + "]");
				DevIo.pushVirtualVoucherElements(lineToAdd);
				try {
					String line = null;

					while ((line = reader.readLine()) != null) {
						CreditCardVoucher lineToAddDescription = new CreditCardVoucher();

						lineToAddDescription.setTypeOfLine('D');
						lineToAddDescription.setPrintedLineDescription(manageMacro(line, errorDescription, errorCode));
						logger.info("lineToAddDescription  [" + lineToAddDescription.getPrintedLineDescription() + "]");
						DevIo.pushVirtualVoucherElements(lineToAddDescription);
					}
				} catch (Exception e) {
					logger.error("addReceiptValues exception : ", e);
					return;
				} finally {
					CreditCardVoucher lineToAddEnd = new CreditCardVoucher();

					lineToAddEnd.setTypeOfLine('E');
					lineToAddEnd.setPrintedLineDescription("");
					DevIo.pushVirtualVoucherElements(lineToAddEnd);
					logger.info("lineToAddEnd [" + lineToAddEnd.getPrintedLineDescription() + "]");
				}
			} else {
				logger.info("Error in addReceiptValues() , textVoucher is null check file S_EFT" + Action.editNum(errorCode, 3) + ".DAT");
			}
		} catch (Exception exception) {
			System.out.println("addReceiptValues exception : " + exception.toString());
			exception.printStackTrace();
			return;
		}
	}

	private String manageMacro(String line, String errorDescription, int errorCode) {
		if (line.indexOf(MACRO_ERROR_DESCRIPTION) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_ERROR_DESCRIPTION)) + errorDescription + line
					.substring(line.indexOf(MACRO_ERROR_DESCRIPTION) + MACRO_ERROR_DESCRIPTION.length());
		}
		if (line.indexOf(MACRO_ERROR_CODE) >= 0) {
			line = line.substring(0, line.indexOf(MACRO_ERROR_CODE)) + Action.editNum(errorCode, 4) + line
					.substring(line.indexOf(MACRO_ERROR_CODE) + MACRO_ERROR_CODE.length());
		}
		return line;
	}

	public void toPosHandleFailure(Map map) {
	}
}
