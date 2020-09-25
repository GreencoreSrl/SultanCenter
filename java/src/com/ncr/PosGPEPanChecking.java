package com.ncr;

import java.util.Map;

import com.ncr.gpe.CommandFromPos_Return;
import com.ncr.gpe.DefaultGpe;
import com.ncr.gpe.GpeResultProcessorInterface;
import com.ncr.gpe.GpeResult_PanCheckingDataInterface;
import org.apache.log4j.Logger;

public class PosGPEPanChecking implements GpeResultProcessorInterface {
	private static final Logger logger = Logger.getLogger(PosGPEPanChecking.class);

	private String nibbleByteArrayToString(byte[] trackData, int valueInCardDataLengthField) {
		logger.info("ENTER nibbleByteArrayToString");
		logger.info("valueInCardDataLengthField: " + valueInCardDataLengthField);

		String result = "";
		int indexArray;

		for (indexArray = 0; indexArray < trackData.length; indexArray++) {

			int intValue = (int) trackData[indexArray];
			logger.info("intValue: " + intValue);

			if (intValue < 0) {
				intValue = 256 + intValue;
			}
			result = result + Integer.toHexString(intValue / 16) + Integer.toHexString(intValue % 16);
			logger.info("result: " + result);
		}

		logger.info("EXIT nibbleByteArrayToString - return " + result.substring(0, valueInCardDataLengthField).toUpperCase());
		return result.substring(0, valueInCardDataLengthField).toUpperCase();
	}

	public void processResult(Map messageMap) {
		logger.info("ENTER processResult");

		GpeResult_PanCheckingDataInterface data = DefaultGpe.createPanCheckingData(messageMap);
		try {
			PosGPE.panCheckingAcquirerId = data.getAcquirerId().intValue();
		} catch (Exception exception) {
			logger.error("error 1: " + exception.getMessage());
		}

		try {
			PosGPE.panCheckingPosId = data.getPosId();
		} catch (Exception exception) {
			logger.error("error 2: " + exception.getMessage());
		}

		try {
			PosGPE.panCheckingPan = nibbleByteArrayToString(data.getPanData(), data.getPanData().length);
		} catch (Exception exception) {
			logger.error("error 3: " + exception.getMessage());
		}

		PosGPE.sts = 4;

		logger.info("EXIT processResult");
	}
}
