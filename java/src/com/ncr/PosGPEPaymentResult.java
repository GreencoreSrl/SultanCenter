package com.ncr;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ncr.gpe.DefaultGpe;
import com.ncr.gpe.GpeResultProcessorInterface;
import com.ncr.gpe.GpeResult_ReceiptDataInterface;
import org.apache.log4j.Logger;

public class PosGPEPaymentResult implements GpeResultProcessorInterface {
	private static final Logger logger = Logger.getLogger(PosGPEPaymentResult.class);

	public void processResult(Map messageMap) {
		logger.info("ENTER processResult");

		PosGPE.sts = 2;
		GpeResult_ReceiptDataInterface data = DefaultGpe.createPaymentResultData(messageMap);
		Integer authorizedAmount = data.getAuthorizedAmount();

		logger.info("authorizedAmount [" + authorizedAmount + "]");

		List receipt = data.getReceipt();
		CreditCardVoucher lineToAdd = new CreditCardVoucher();

		lineToAdd.setTypeOfLine('B');
		lineToAdd.setPrintedLineDescription("");
		logger.info("lineToAdd [" + lineToAdd + "]");
		DevIo.pushVirtualVoucherElements(lineToAdd);
		for (Iterator iter = receipt.iterator(); iter.hasNext();) {
			CreditCardVoucher lineToAddDescription = new CreditCardVoucher();

			lineToAddDescription.setTypeOfLine('D');
			lineToAddDescription.setPrintedLineDescription((String) iter.next());
			logger.info("linedescr 1 [" + lineToAddDescription.getPrintedLineDescription() + "]");

			DevIo.pushVirtualVoucherElements(lineToAddDescription);
		}
		CreditCardVoucher lineToAddBlank = new CreditCardVoucher();

		lineToAddBlank.setTypeOfLine('D');
		lineToAddBlank.setPrintedLineDescription("");

		DevIo.pushVirtualVoucherElements(lineToAddBlank);
		logger.info("linedescr 2 [" + lineToAddBlank.getPrintedLineDescription() + "]");
		if (PosGPE.getFlagEptsVoid()) {
			logger.info("flagEptsVoid 1");

			CreditCardVoucher lineToAddVoid = new CreditCardVoucher();

			lineToAddVoid.setTypeOfLine('D');
			lineToAddVoid.setPrintedLineDescription(Mnemo.getMenu(86));
			DevIo.pushVirtualVoucherElements(lineToAddVoid);
			logger.info("linedescr 3 [" + lineToAddVoid.getPrintedLineDescription() + "]");
		}
		CreditCardVoucher lineToAddEnd = new CreditCardVoucher();

		lineToAddEnd.setTypeOfLine('E');
		lineToAddEnd.setPrintedLineDescription("");
		DevIo.pushVirtualVoucherElements(lineToAddEnd);
		logger.info("linedescr 4 [" + lineToAddEnd.getPrintedLineDescription() + "]");

		DevIo.addVoucherCopyNumber(data.getCopiesToPrint().intValue());

		PosGPE.setLastEptsReceiptData(data);

		if (PosGPE.getFlagEptsVoid()) {
			logger.info("flagEptsVoid 2");
			PosGPE.setAmountEptsVoid(data.getAuthorizedAmount().longValue());
		}

		logger.info("Epts Payment Info:");
		logger.info("AuthAmount: " + data.getAuthorizedAmount().longValue());
		logger.info("PosTenderId: " + data.getPosTenderId().intValue());
		logger.info("Authoriz Code: " + data.getAuthorizationCode());
		logger.info("Transaction Date: " + data.getTransactionDate());
		logger.info("Transaction Time: " + data.getTransactionTime());
		logger.info("Terminal Code: " + data.getTerminalCode());
		logger.info("Card Number: " + data.getCardNumber());

		logger.info("EXIT processResult");
	}
}
