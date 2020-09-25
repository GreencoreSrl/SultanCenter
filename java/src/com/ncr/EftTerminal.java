package com.ncr;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 18/05/15 Time: 16.16 To change this template use File |
 * Settings | File Templates.
 */
public interface EftTerminal {
	public static final String NEW_RECEIPT_TYPE = "NEW_RECEIPT";
	public static final String SAME_RECEIPT_TYPE = "SAME_RECEIPT";


	public int doTransactionWithStatusCheck(long amt, String traNum, LinIo line);

	public void printVouchers(String type);

	public void loadEftTerminalParams(String txt);

	public String getAuthorizationCode();

	public String getCardNumber();

    public String getCardType(); // AMZ-2017-002#ADD
}