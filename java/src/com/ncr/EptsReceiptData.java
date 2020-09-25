package com.ncr;

public class EptsReceiptData {
	private String terminalCode = "";
	private String transactionDate = "";
	private String transactionTime = "";
	private long authAmount = 0;
	private int wineptsErrorCode = 0;
	private int posTenderId = 0;
	private String cardNumber = "";
	private String authorizationCode = "";

	public String getTerminalCode() {
		return terminalCode;
	}

	public String getTransactionDate() {
		return transactionDate;
	}

	public String getTransactionTime() {
		return transactionTime;
	}

	public long getAuthAmount() {
		return authAmount;
	}

	public int getWineptsErrorCode() {
		return wineptsErrorCode;
	}

	public String getCardNumber(int length) {
		String tmp = cardNumber;

		if (tmp.length() == 0) {
			return "";
		}
		if (tmp.length() > length) {
			tmp = tmp.substring((tmp.length() - length));
		}
		return tmp;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public String getAuthorizationCode() {
		return authorizationCode;
	}

	public int getPosTenderId() {
		return posTenderId;
	}

	public void setTerminalCode(String terminalCode) {
		this.terminalCode = terminalCode;
	}

	public void setTransactionDate(String transactionDate) {
		this.transactionDate = transactionDate;
	}

	public void setTransactionTime(String transactionTime) {
		this.transactionTime = transactionTime;
	}

	public void setAuthAmount(long authAmount) {
		this.authAmount = authAmount;
	}

	public void setWineptsErrorCode(int wineptsErrorCode) {
		this.wineptsErrorCode = wineptsErrorCode;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public void setAuthorizationCode(String authorizationCode) {
		this.authorizationCode = authorizationCode;
	}

	public void setPosTenderId(int posTenderId) {
		this.posTenderId = posTenderId;
	}

	public String getFormattedTransactionDate() {
		String tmp = transactionDate;

		if (tmp.length() == 0) {
			return "      ";
		}
		String ret = null;

		ret = tmp.substring(8);
		ret += tmp.substring(3, 5);
		ret += tmp.substring(0, 2);
		return ret;
	}

	public String getFormattedTransactionTime() {
		String tmp = transactionTime;

		if (tmp.length() == 0) {
			return "      ";
		}
		String ret = null;

		ret = tmp.substring(0, 2);
		ret += tmp.substring(3, 5);
		if (tmp.length() == 8) {
			ret += tmp.substring(6, 8);
		} else {
			ret += "00";
		}
		return ret;
	}
}
