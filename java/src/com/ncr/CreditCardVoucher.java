
package com.ncr;

/**
 * 
 * Title: Credit Card Voucher Object Description: This class is used to save the
 * voucer of credit card informations
 * 
 */
import org.apache.log4j.Logger;
//import printerobject.enumPrintType;

public class CreditCardVoucher { //implements enumPrintType {
	private static final Logger logger = Logger.getLogger(CreditCardVoucher.class);
	private final int PRINTNORMAL = 1;
	private String printedLineDescription;
	private char typeOfLine;

	CreditCardVoucher() {
		printedLineDescription = "";
		typeOfLine = PRINTNORMAL;

	}

	public void setPrintedLineDescription(String s) {
		printedLineDescription = s;
		logger.info("printedLineDescription = " + printedLineDescription);
	}

	public String getPrintedLineDescription() {
		return printedLineDescription;
	}

	public void setTypeOfLine(char s) {
		typeOfLine = s;
		logger.info("typeOfLine = " + typeOfLine);

	}

	public char getTypeOfLine() {
		return typeOfLine;
	}
}
