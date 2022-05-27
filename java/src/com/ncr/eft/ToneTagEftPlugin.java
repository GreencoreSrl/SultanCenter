package com.ncr.eft;

import com.ncr.*;
import com.ncr.toneTag.Message;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

import com.tonetag.fab.v2.*;
import com.tonetag.fab.v2.listener.*;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ToneTagEftPlugin extends GenericEftPlugin {
    private static final Logger logger = Logger.getLogger(ToneTagEftPlugin.class);
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    /* Macro for payment voucher */
    private final String MACRO_amount = "$AMOUNT$";
    private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
    //private final String MACRO_card_scheme_name = "$SCHEME_NAME$";

    /* parameter from properties file */
    private HashMap receiptDatas = new HashMap();
    private HashMap errorCodeMap = new HashMap<String, Integer>();

    private boolean completed = false;
    private Message resultResponse = null;
    private int statusCode = 2001;
    private int timeout = 100;
    private String merchantId = "";

    private ToneTagEftPlugin() {
        loadErrorCodeMap();
    }

    public void loadEftTerminalParams(int line, String txt) {
        super.loadEftTerminalParams(line, txt);

        if (line == 0) {
            //TODO: Add specific parameters
        }
    }

    public int now_ss() {
        Calendar c = Calendar.getInstance();
        int time_hh = c.get(c.HOUR_OF_DAY);
        int time_mm = c.get(c.MINUTE);
        int time_ss = c.get(c.SECOND);

        return ((time_hh * 100) + time_mm) * 100 + time_ss;
    }

    public Date now_Time() {
        Calendar calender = Calendar.getInstance();
        return calender.getTime();
    }

    public int addSeconds(Date date, int sec) {
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(date.getTime());
        calender.add(Calendar.SECOND, sec);

        int time_hh = calender.get(calender.HOUR_OF_DAY);
        int time_mm = calender.get(calender.MINUTE);
        int time_ss = calender.get(calender.SECOND);

        return ((time_hh * 100) + time_mm) * 100 + time_ss;
    }

    public int doTransactionWithStatusCheck(Itemdata itm, String traNum, LinIo line) {
        int resultCode = 0;
        ToneTag_FAB ttfab = new ToneTag_FAB();
        resultResponse = new Message();
        statusCode = 0;
        completed = false;

        logger.debug("merchantId: " + merchantId);
        logger.debug("amt: " + itm.pos);
        try {
            ttfab.requestPaymet(merchantId, String.valueOf(itm.pos), new OnToneTagListener() {
                @Override
                public void onPaymentComplete(int status, String message) {
                    logger.debug("statusCode: " + status);
                    logger.debug("message: " + message);

                    statusCode = status;
                    resultResponse = gson.fromJson(message, Message.class);
                    completed = true;
                }
            });

            int now = 0;
            int lastPolling = addSeconds(now_Time(), timeout);
            logger.info("lastPolling : " + lastPolling);
            logger.info("timeout : " + timeout);

            logger.info("completed beg : " + completed);
            while (now < lastPolling) {
                now = now_ss();

                if (completed) {
                    logger.info("completed true");

                    break;
                }
            }

            logger.info("completed end : " + completed);
            logger.info("statusCode : " + statusCode);

            if (!completed) {
                logger.info("not complete");
                ttfab.cancelTxnProcess();

                resultResponse.setMessage(errorCodeMap.get("TimeoutMsg").toString());
                resultCode = 2000;
                panel.clearLink(resultResponse.getMessage(), 1);
                logger.info("message display");
            } else {
                logger.info("complete: " + statusCode);

                if (statusCode == 2001 || statusCode == 0) {  //successful
                    logger.info("amount: " + resultResponse.getAmount_in_fils());
                    logger.info("txnId: " + resultResponse.getTxn_id());

                    itm.amt = Long.parseLong(resultResponse.getAmount_in_fils());
                    authorizationCode = resultResponse.getTxn_id();

                    addReceiptValues(ttfab); //EFT-CGA

                    Itmdc.IDC_write('z', Struc.tra.tnd, 0, authorizationCode, 1, Long.parseLong(resultResponse.getAmount_in_fils()));
                } else {
                    logger.info("Error statusCode: " + statusCode);
                    ttfab.cancelTxnProcess();

                    Set keys = errorCodeMap.keySet();
                    String rspCode = String.valueOf(statusCode);

                    logger.info("Error code response: " + rspCode);
                    if (keys.contains(rspCode)) {
                        resultResponse.setMessage(errorCodeMap.get(rspCode).toString());
                        logger.info("Error code map contains this code: " + resultResponse.getMessage());
                    } else {
                        resultResponse.setMessage(errorCodeMap.get("Default").toString());
                        logger.info("Error code map not contains this code: " + resultResponse.getMessage());
                    }

                    logger.info("AlshayaEftPlugin - resultResponse error code: " + resultResponse);
                    resultCode = statusCode;
                    panel.clearLink(resultResponse.getMessage(), 1);
                }
            }
        } catch(Exception e) {
            resultCode = statusCode;
        }

        return resultCode;
    }

    private void addReceiptValues(ToneTag_FAB ttf) {  //EFT-CGA
        receiptDatas = new HashMap();

        addReceiptValues(NEW_RECEIPT_TYPE, ttf);
        addReceiptValues(SAME_RECEIPT_TYPE, ttf);
    }

    private void addReceiptValues(String type, ToneTag_FAB ttf) {  //EFT-CGA
        try {
            File file = new File(type);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Vector voucher = new Vector();

            if (reader != null) {
                try {
                    String line = null;

                    while ((line = reader.readLine()) != null) {
                        line = manageMacro(line, ttf);
                        voucher.add(line);
                    }
                } catch (Exception e) {
                    logger.error("addReceiptValues exception : ", e);
                    return;

                }
                receiptDatas.put(type, voucher);
            }
        } catch (Exception exception) {
            System.out.println("addReceiptValues exception : " + exception.toString());
            exception.printStackTrace();
            return;
        }
    }

    private String manageMacro(String line, ToneTag_FAB ttf) {
        if (line.indexOf(MACRO_authorisation_number) >= 0) {
            line = line.substring(0, line.indexOf(MACRO_authorisation_number)) + new String(resultResponse.getTxn_id()) + line
                    .substring(line.indexOf(MACRO_authorisation_number) + MACRO_authorisation_number.length());
        }
        if (line.indexOf(MACRO_amount) >= 0 && resultResponse.getAmount_in_fils().length() > 0) {
            line = line.substring(0, line.indexOf(MACRO_amount)) + GdRegis.editMoney(0, Long.parseLong(new String(resultResponse.getAmount_in_fils())))
                    + line.substring(line.indexOf(MACRO_amount) + MACRO_amount.length());
        }

        return line;
    }

    public void loadErrorCodeMap() {
        logger.debug("ENTER");

        Properties prop = new Properties();

        try {
            prop.load(new FileInputStream("conf/toneTag.properties"));

            for (Object key : prop.keySet().toArray()) {
                if (key.toString().startsWith("errorcode")) {
                    String value = prop.getProperty(key.toString());
                    errorCodeMap.put(key.toString().substring(10), value);
                } else {
                    if (key.toString().equalsIgnoreCase("TimeoutSeconds")) {
                        timeout = Integer.parseInt(prop.getProperty(key.toString()));
                    } else {
                        if (key.toString().equalsIgnoreCase("MerchantID")) {
                            merchantId = prop.getProperty(key.toString());
                        }
                    }
                }
            }
        } catch(Exception e) {
            logger.info("EXCEPTION " + e.getMessage());
        }

        logger.debug("EXIT");
    }

    @Override
    public String getTenderId() {
        return TONETAG_TENDER_ID;
    }
}