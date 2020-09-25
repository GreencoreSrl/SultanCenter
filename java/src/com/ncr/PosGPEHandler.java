package com.ncr;

import java.util.HashMap;
import java.util.Map;

import com.ncr.gpe.DefaultGpe;
import com.ncr.gpe.GpeResultProcessorInterface;
import com.ncr.gpe.GpeResult_ListOfOptionsInterface;
import com.ncr.gpe.GpeResult_PanCheckingDataInterface;
import com.ncr.gpe.GpeResult_PaymentAckFromPosInterface;
import com.ncr.gpe.GpeResult_ReceiptDataInterface;
import com.ncr.gpe.MessageToPosHandlerInterface;
import org.apache.log4j.Logger;

public class PosGPEHandler implements MessageToPosHandlerInterface {
	private static final Logger logger = Logger.getLogger(PosGPEHandler.class);

	private Map processors;

	PosGPEHandler() {

		processors = new HashMap();
		processors.put(GpeResult_ReceiptDataInterface.class, new PosGPEPaymentResult());
		processors.put(GpeResult_ListOfOptionsInterface.class, new PosGPEOptionListHandler());
		processors.put(GpeResult_PanCheckingDataInterface.class, new PosGPEPanChecking());
		processors.put(GpeResult_PaymentAckFromPosInterface.class, new PosGPEAckReceived());

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

	public void toPosHandleFailure(int errorCode, String errorDescription) {
		logger.info("ENTER toPosHandleFailure");

		PosGPE.sts = 3;
		PosGPE.doLog("PosHandleFailure. Erorcode:" + errorCode + " Msg: " + errorDescription);
		PosGPE.deleteLastEptsReceiptData();
		PosGPE.fail(errorCode, errorDescription);

		logger.info("EXIT toPosHandleFailure");
	}

	public void toPosHandleFailure(Map map) {
	}
}
