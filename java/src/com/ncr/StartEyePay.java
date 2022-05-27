package com.ncr;

import org.apache.log4j.Logger;

/**
 * Created by Administrator on 22/11/16.
 */


public class StartEyePay {
    private static final Logger logger = Logger.getLogger(StartEyePay.class);
    private String transType = "";
    private String transId = "";
    private String transAmount = "";
    private String beneficiary = "";
    private String rspCode = "";
    private String approvalCode = "";
    private String refNumber = "";
    private String invoiceNumber = "";
    private String terminalId = "";
    private String batchNumber = "";
    private Iris iris = new Iris();
    private String cardNumber = ""; //field not mentioned in the document
    private String cardType = ""; //field not mentioned in the document
    private String amtEyePay = "";

    public StartEyePay() { }

    public long sendRequest(String port, String amountEyePay, byte[] returnByteArray) {

        EyePayWrapper wrapper = new EyePayWrapper();

        String amountEyePayDec = GdRegis.editMoney(0, Long.parseLong(amountEyePay)).replace(",", ".");

        amtEyePay = GdRegis.editMoney(0, Long.parseLong(amountEyePay));

        logger.info("amountEyePayDec: " + amountEyePayDec);

        long ris = wrapper.EyePay(port, amountEyePayDec.trim(), (long) amountEyePayDec.trim().length(), returnByteArray);
        logger.info("ris wrapper eye pay: " + ris);

        String risp = "";



        for(int i = 0; i < returnByteArray.length; i++) {
            logger.info("returnByteArray " + i + " = " + returnByteArray[i]);

            /*if (i != 0 && returnByteArray[i] != 28) {
                risp += GdRegis.editHex(returnByteArray[i], 1);

            }*/

            if (i != 0 && returnByteArray[i] != 28) {
                if (i > 0 && i < 3) {
                    transType += (char) returnByteArray[i];
                }

                if (i >= 4 && i < 19) {
                    transId += GdRegis.editHex(returnByteArray[i], 1);
                }

                if (i >= 20 && i < 32) {
                    transAmount += GdRegis.editHex(returnByteArray[i], 1);
                }

                if (i >= 40 && i < 52) {
                    beneficiary += (char) returnByteArray[i];
                }

                if (i >= 53 && i < 55) {
                    rspCode += GdRegis.editHex(returnByteArray[i], 1);
                }

                if (i >= 56 && i < 62) {
                    approvalCode += (char) returnByteArray[i];
                }

                if (i >= 63 && i < 75) {
                    refNumber += GdRegis.editHex(returnByteArray[i], 1);
                }

                if (i >= 76 && i < 82) {
                    invoiceNumber += GdRegis.editHex(returnByteArray[i], 1);
                }

                if (i >= 83 && i < 91) {
                    terminalId += (char) returnByteArray[i];
                }
            }
        }


        transAmount = transAmount.replace("E", ".");
        transAmount = transAmount.replace("C", ".");


        /*transType = "01";
        transId = "123463264973249";
        transAmount = "000000020.00";
        rspCode = "00";
        approvalCode = "123456";
        refNumber = "123463264973";
        invoiceNumber = "123456";
        terminalId = "123445678";
        batchNumber = "123456";
        cardNumber = "1111111111111111111";
        cardType = "01";

        transType = risp.substring(0, 2); //num
        transId = risp.substring(2, 17);   //num
        transAmount = risp.substring(17, 29).replace("E", "."); //num
        beneficiary = risp.substring(35, 47);  //alpha
        rspCode = risp.substring(47, 49);   //num
        approvalCode = risp.substring(49, 55);  //alpha
        refNumber = risp.substring(55, 67);  //num
        invoiceNumber = risp.substring(67, 73); //num
        terminalId = risp.substring(73, 81); //alpha   */

        return ris;
    }

    public String getTransType() {
        logger.info("transType: " + transType);
        return transType;
    }

    public void setTransType(String transType) {
        logger.info("settransType: " + transType);
        this.transType = transType;
    }

    public String getTransId() {
        logger.info("transId: " + transId);
        return transId;
    }

    public void setTransId(String transId) {
        logger.info("settransId: " + transId);
        this.transId = transId;
    }

    public String getTransAmount() {
        logger.info("transAmount: " + transAmount);
        return transAmount;
    }

    public void setTransAmount(String transAmount) {
        logger.info("settransAmount: " + transAmount);
        this.transAmount = transAmount;
    }

    public String getRspCode() {
        logger.info("rspCode: " + rspCode);
        return rspCode;
    }

    public void setRspCode(String rspCode) {
        logger.info("setrspCode: " + rspCode);
        this.rspCode = rspCode;
    }

    public String getApprovalCode() {
        logger.info("approvalCode: " + approvalCode);
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        logger.info("setapprovalCode: " + approvalCode);
        this.approvalCode = approvalCode;
    }

    public String getRefNumber() {
        logger.info("refNumber: " + refNumber);
        return refNumber;
    }

    public void setRefNumber(String refNumber) {
        logger.info("setrefNumber: " + refNumber);
        this.refNumber = refNumber;
    }

    public String getInvoiceNumber() {
        logger.info("invoiceNumber: " + invoiceNumber);
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        logger.info("setinvoiceNumber: " + invoiceNumber);
        this.invoiceNumber = invoiceNumber;
    }

    public String getTerminalId() {
        logger.info("terminalId: " + terminalId);
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        logger.info("setterminalId: " + terminalId);
        this.terminalId = terminalId;
    }

    public String getBatchNumber() {
        logger.info("batchNumber: " + batchNumber);
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        logger.info("setbatchNumber: " + batchNumber);
        this.batchNumber = batchNumber;
    }

    public Iris getIris() {
        logger.info("iris: " + iris);
        return iris;
    }

    public void setIris(Iris iris) {
        logger.info("setiris: " + iris);
        this.iris = iris;
    }

    public String getCardNumber() {
        logger.info("cardNumber: " + cardNumber);
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        logger.info("setcardNumber: " + cardNumber);
        this.cardNumber = cardNumber;
    }

    public String getCardType() {
        logger.info("cardType: " + cardType);
        return cardType;
    }

    public void setCardType(String cardType) {
        logger.info("setcardType: " + cardType);
        this.cardType = cardType;
    }

    public String getBeneficiary() {
        logger.info("beneficiary: " + beneficiary);
        return beneficiary;
    }

    public void setBeneficiary(String beneficiary) {
        logger.info("beneficiary: " + beneficiary);
        this.beneficiary = beneficiary;
    }

    public String getAmtEyePay() {
        logger.info("amtEyePay: " + amtEyePay);
        return amtEyePay;
    }
}
