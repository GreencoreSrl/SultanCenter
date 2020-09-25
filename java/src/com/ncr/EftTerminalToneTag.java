package com.ncr;

import com.ncr.toneTag.Message;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

import com.tonetag.fab.v2.*;
import com.tonetag.fab.v2.listener.*;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;


/**
 * Created by User on 17/05/2019.
 */

//TONETAG-CGA#A BEG
public class EftTerminalToneTag extends Action implements EftTerminal {
    private static final Logger logger = Logger.getLogger(EftTerminalToneTag.class);
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    private boolean enableSecondCopy = false;
    private long retryEvery = 500;
    private int maxRetry = 10;
    private String serialPort = "COM1";
    private JTextField textField;

    // Objects to manage voucher row file*/
    private BufferedReader textVoucherNewReceipt;
    private RandomAccessFile fileNewReceipt;
    private BufferedReader textVoucherSameReceipt;
    private RandomAccessFile fileSameReceipt;

    /* Constant to identify voucher type */
    //public static final String NEW_RECEIPT_TYPE = "NEW_RECEIPT";  //EFT-CGA
    //public static final String SAME_RECEIPT_TYPE = "SAME_RECEIPT";  //EFT-CGA
    public static final String NEW_RECEIPT_TYPE = "S_PLURCO.DAT";  //EFT-CGA
    public static final String SAME_RECEIPT_TYPE = "S_PLURCP.DAT";  //EFT-CGA

    /* Macro for payment voucher */
    private final String MACRO_amount = "$AMOUNT$";
    private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
    //private final String MACRO_card_scheme_name = "$SCHEME_NAME$";

    /* parameter from properties file */
    private HashMap receiptDatas = new HashMap();
    private String authorizationCode = "";

    private String cardNumber = "";
    private HashMap errorCodeMap = new HashMap<String, Integer>();

    private String cardType;
    private boolean completed = false;
    private Message resultResponse = null;
    private int statusCode = 2001;
    private int timeout = 100;
    private String merchantId = "";
    //1610 beg
    private static EftTerminalToneTag instance = null;

    public static EftTerminalToneTag getInstance() {
        if (instance == null)
            instance = new EftTerminalToneTag();

        return instance;
    }

    //public EftTerminalToneTag() {  //1610 end
    private EftTerminalToneTag() {
        //loadReciptRows();  //EFT-CGA
        loadErrorCodeMap();
    }

    public String getCardType() {
        return cardType;
    }

    public void loadEftTerminalParams(String txt) {
        enableSecondCopy = txt.substring(0, 1).equals("1");
        retryEvery = Long.parseLong(txt.substring(2, 5));
        maxRetry = Integer.parseInt(txt.substring(6, 8));
        serialPort = txt.substring(9, 13);
    }

    /*@Override
    public void onPaymentComplete(int statusCode, String message) {
        logger.debug("statusCode: " + statusCode);
        logger.debug("message: " + message);

        completed = true;
        resultResponse = gson.fromJson(message, Message.class);
        this.statusCode = statusCode;
    }*/

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    //calcolo timeout BEG
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
    }  //12:23:01

    //12:23:01, 120  --> 12:25:01
    public int addSeconds(Date date, int sec) {
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(date.getTime());
        calender.add(Calendar.SECOND, sec);

        int time_hh = calender.get(calender.HOUR_OF_DAY);
        int time_mm = calender.get(calender.MINUTE);
        int time_ss = calender.get(calender.SECOND);

        return ((time_hh * 100) + time_mm) * 100 + time_ss;
    }
    //calcolo timeout END

    public int doTransactionWithStatusCheck(long amt, String traNum, LinIo line) {
        int resultCode = 0;
        ToneTag_FAB ttfab = new ToneTag_FAB();
        resultResponse = new Message();
        statusCode = 0;
        completed = false;

        logger.debug("merchantId: " + merchantId);
        logger.debug("amt: " + amt);
        try {
            //ttfab.requestPaymet(merchantId, String.valueOf(amt), EftTerminalToneTag.this);
            ttfab.requestPaymet(merchantId, String.valueOf(amt), new OnToneTagListener() {
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

            //forzatura beg
            /*statusCode = 2001;
            resultResponse.setAmount_in_fils("100");
            resultResponse.setTxn_id("72346824435");
            completed = false;*/
            //forzatura end

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

                    GdTndrs.setAmountByToneTag(Long.parseLong(resultResponse.getAmount_in_fils()));
                    authorizationCode = resultResponse.getTxn_id();

                    //addReceiptValues(NEW_RECEIPT_TYPE, ttfab);  //EFT-CGA
                    //addReceiptValues(SAME_RECEIPT_TYPE, ttfab); //EFT-CGA
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

                    logger.info("EftTerminalAlshaya - resultResponse error code: " + resultResponse);
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

    /*private void addReceiptValues(String type, ToneTag_FAB ttf) {  //EFT-CGA
        Vector voucher = new Vector();
        Vector tmp;
        String line;
        BufferedReader reader = null;
        RandomAccessFile file = null;

        if (type.equals(NEW_RECEIPT_TYPE)) {
            reader = textVoucherNewReceipt;
            file = fileNewReceipt;
        } else if (type.equals(SAME_RECEIPT_TYPE)) {
            reader = textVoucherSameReceipt;
            file = fileSameReceipt;
        }

        if (reader != null && file != null) {
            try {
                file.seek(0);

                while ((line = reader.readLine()) != null) {
                    line = manageMacro(line, ttf);
                    voucher.add(line);
                }
            } catch (Exception exception) {
                System.out.println("addReceiptValues exception : " + exception.toString());
                exception.printStackTrace();
                return;
            }

            if (receiptDatas.containsKey(type) && voucher.size() > 0) {
                tmp = (Vector) receiptDatas.remove(type);
                tmp.add(voucher);
                receiptDatas.put(type, tmp);
            } else {
                tmp = new Vector();
                tmp.add(voucher);
                receiptDatas.put(type, tmp);
            }
        } else {
            System.out.println("Error in addReceiptValues() , textVoucher is null check file S_PLU");
        }
    }*/

    private String manageMacro(String line, ToneTag_FAB ttf) {
        //while (hasMacro(line)) {  //EFT-CGA
        if (line.indexOf(MACRO_authorisation_number) >= 0) {
            line = line.substring(0, line.indexOf(MACRO_authorisation_number)) + new String(resultResponse.getTxn_id()) + line
                    .substring(line.indexOf(MACRO_authorisation_number) + MACRO_authorisation_number.length());
        }
        if (line.indexOf(MACRO_amount) >= 0 && resultResponse.getAmount_in_fils().length() > 0) {
            line = line.substring(0, line.indexOf(MACRO_amount)) + GdRegis.editMoney(0, Long.parseLong(new String(resultResponse.getAmount_in_fils())))
                    + line.substring(line.indexOf(MACRO_amount) + MACRO_amount.length());
        }
        //}

        return line;
    }

    /*private boolean hasMacro(String row) {  //EFT-CGA
        return (row.indexOf(MACRO_authorisation_number) >= 0) || (row.indexOf(MACRO_amount) >= 0);
    }*/

    public void printVouchers(String type) {
        logger.debug("ENTER");
        logger.info("printVouchers type: " + type);

        Vector container;
        Vector tmp;

        if ((Struc.tra.mode == Struc.M_CANCEL) || (Struc.tra.mode == Struc.M_SUSPND)) {
            logger.info("exit printVouchers 1");
            return;
        }

        if (receiptDatas.containsKey(type)) {
            logger.info("receiptDatas contains type");

            container = (Vector) receiptDatas.remove(type);

            if (container != null && container.size() > 0 && EftTerminalGeidea.isEnableSecondCopy()) {
                if (type.equals(NEW_RECEIPT_TYPE)) {
                    logger.info("type is NEW_RECEIPT_TYPE");

                    GdRegis.set_tra_top();
                }

                for (int i = 0; i < container.size(); ++i) {
                    tmp = (Vector) container.get(i);
                    logger.info("tmp size: " + tmp.size());

                    for (int j = 0; j < tmp.size(); ++j) {
                        System.out.println("On additional receipt printing: " + tmp.get(j));
                        Struc.prtLine.init((String) tmp.get(j)).book(3);
                    }

                    if (type.equals(NEW_RECEIPT_TYPE)) {
                        logger.info("type is NEW_RECEIPT_TYPE");
                        GdRegis.prt_trailer(2);
                    } else {
                        logger.info("type is SAME_RECEIPT_TYPE");
                        Struc.prtLine.init(' ').book(3);
                    }
                }
            }
        }

        logger.debug("EXIT");
    }

    /*public void loadReciptRows() {  //EFT-CGA
        fileNewReceipt = null;
        textVoucherNewReceipt = null;

        try {
            fileNewReceipt = new RandomAccessFile("S_PLURCO.DAT", "r");
            textVoucherNewReceipt = new BufferedReader(new FileReader(fileNewReceipt.getFD()));
        } catch (Exception E) {
            System.out.println("Error on reading S_PLURCO.DAT file :" + E.getMessage());
            return;
        }
    }*/

    private void loadErrorCodeMap() {
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
}
//TONETAG-CGA#A END