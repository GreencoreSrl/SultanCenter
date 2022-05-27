package com.ncr.eft;

import com.ncr.*;
import net.geidea.ecr.ECRWrapper;
import net.geidea.ecr.manager.onApiResponseListener;
import net.geidea.ecr.model.TransactionResponse;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class NewGeideaEftPlugin extends GenericEftPlugin {
    private static final Logger logger = Logger.getLogger(GenericEftPlugin.class);
    private static int PURCHASE_TRANSACTION = 0;
    private long retryEvery = 500;
    private int maxRetry = 10;
    private String serialPort = "COM1";
    private int serialBaud = 38400;
    private int serialParity = 0;
    private int serialDataBits = 8;
    private int serialStopBits = 1;
    private int ecrRef;
    private boolean serial;
    private String tcpAddress = "127.0.0.1";
    private int tcpPort = 6100;
    private boolean processing = false;
    private TransactionResponse transactionResponse;
    private boolean receiptPrintOnEft;
    private List<String> approvedCodes = Arrays.asList(new String[] {"000", "001", "003", "007", "087", "089"});
    private boolean testEnvironment = false;

    public NewGeideaEftPlugin() {
    }

    public void loadEftTerminalParams(int line, String txt) {
        super.loadEftTerminalParams(line, txt);
        ecrRef = ctl.reg_nbr;

        switch (line) {
            case 0:
                serial = true;
                retryEvery = Long.parseLong(txt.substring(2, 5));
                maxRetry = Integer.parseInt(txt.substring(6, 8));
                serialPort = txt.substring(9, 14).trim();
                serialBaud = Integer.parseInt(txt.substring(15, 21).trim());
                serialParity = Integer.parseInt(txt.substring(22, 23));
                serialDataBits = Integer.parseInt(txt.substring(24, 25));
                serialStopBits = Integer.parseInt(txt.substring(26, 27));
                receiptPrintOnEft = txt.charAt(28) == '1';
                testEnvironment  = txt.substring(39, 40).equals("1");
                break;
            case 2:
                serial = false;
                tcpAddress = txt.substring(0, 15).trim();
                tcpPort = Integer.parseInt(txt.substring(16, 21).trim());
                break;
            default:
                break;
        }
    }

    private void logResponse(TransactionResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("Pan No : " + response.getPan() + "\n");
        builder.append("Expiry Date : " + response.getExpirydate()+ "\n");
        builder.append("Amount : " + response.getAmount()+ "\n");
        builder.append("Additional Amount : " + response.getAddtlAmount()+ "\n");
        builder.append("Date : " + response.getTrxDT()+ "\n");
        builder.append("RRN : " + response.getRRN()+ "\n");
        builder.append("Auth Code : " + response.getAuthCode()+ "\n");
        builder.append("Response Code : " + response.getResponseCode()+ "\n");
        builder.append("Terminal ID : " + response.getTerminalId()+ "\n");
        builder.append("Scheme ID : " + response.getSchemeId()+ "\n");
        builder.append("Merchant ID : " + response.getMerchantId()+ "\n");

        logger.debug(builder.toString());
    }

    public int doTransactionWithStatusCheck(Itemdata itm, String traNum, LinIo line) {
        int sts = ERR_OK;
        int retryNumber = 0;
        ECRWrapper mWrapper = ECRWrapper.getInstance();
        String request = itm.pos + ";" + (receiptPrintOnEft ? 1 : 0) + ";" + traNum + "!";

        processing = true;
        transactionResponse = null;
        if (serial) {
            serialRequest(mWrapper, request);
        } else {
            tcpRequest(mWrapper, request);
        }

        try {
            do {
                logger.debug("Waiting response for the " + retryNumber + " time");
                Thread.sleep(retryEvery);
                logger.debug("Slept " + retryEvery + " msec");
            } while (processing && ++retryNumber <= maxRetry);
        } catch (Exception e) {
            logger.error("Errore: ", e);
            return ERR_RESPONSE;
        }

        if (retryNumber > maxRetry) {
            logger.info("Timeout");
            return ERR_TIMEOUTTRANSACTION;
        } else {
            if (transactionResponse != null) {
                if (approvedCodes.contains(transactionResponse.getResponseCode())) {
                    logger.info("Transaction approved");

                    authorizationCode = transactionResponse.getAuthCode();
                    cardType = transactionResponse.getSchemeId();
                    cardNumber = transactionResponse.getPan();
                    terminalId = transactionResponse.getTerminalId();
                    if (transactionResponse.getAmount() != null) {
                        authorizedAmount = (long)(Double.parseDouble(transactionResponse.getAmount()) * Math.pow(10, Struc.tnd[0].dec));
                    }
                    receiptNumber = transactionResponse.getEcrRef();
                    rrn = transactionResponse.getRRN();
                    addReceiptValues();

                    Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, authorizedAmount);
                    sts = ERR_OK;
                } else {
                    sts = ERR_NOTAUTHORIZED;
                }
            } else {
                sts = ERR_NOTCONNECTED;

                if (testEnvironment) {
                    authorizationCode = "1234567890";
                    cardType = "VC";
                    cardNumber = "1111222233334444";
                    terminalId = "1234";
                    authorizedAmount = Long.parseLong("500");
                    receiptNumber = "9876543";
                    rrn = "333";
                    addReceiptValues();

                    Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, authorizedAmount);
                    sts = ERR_OK;
                }
            }
        }

         return sts;
    }

    private void tcpRequest(ECRWrapper mWrapper, String request) {
        mWrapper.api_RequestTCPIPTrxn(tcpAddress, tcpPort, PURCHASE_TRANSACTION, ecrRef, request, new onApiResponseListener<TransactionResponse>() {
            @Override
            public void onSuccess(TransactionResponse response) {
                logResponse(response);
                transactionResponse = response;
                processing = false;
            }

            @Override
            public void onError(String errCode, String message) {
                logger.warn("Error: " + errCode + " " + message);
                processing = false;
            }

            @Override
            public void onECRMessage(String errCode, String message) {
                logger.warn("ECR Message: " + errCode + " " + message);
            }
        });
    }

    private void serialRequest(ECRWrapper mWrapper, String request) {
        mWrapper.api_RequestCOMTrxn(serialPort, serialBaud, serialParity, serialDataBits, serialStopBits, PURCHASE_TRANSACTION, ecrRef, request, new onApiResponseListener<TransactionResponse>() {
            @Override
            public void onSuccess(TransactionResponse response) {
                logResponse(response);
                transactionResponse = response;
                processing = false;
            }

            @Override
            public void onError(String errCode, String message) {
                logger.warn("Error: " + errCode + " " + message);
                processing = false;
            }

            @Override
            public void onECRMessage(String errCode, String message) {
                logger.warn("ECR Message: " + errCode + " " + message);
            }
        });
    }

    @Override
    public String getTenderId() {
        return NEW_GEIDEA_TENDER_ID;
    }
}
