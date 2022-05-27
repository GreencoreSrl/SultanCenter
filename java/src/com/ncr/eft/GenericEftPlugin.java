package com.ncr.eft;

import SPAN.SPAN;
import com.ncr.*;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

public class GenericEftPlugin extends Action implements EftPlugin {
    private static final Logger logger = Logger.getLogger(GenericEftPlugin.class);
    private boolean customerCopyEnabled = false;
    private boolean merchantCopyEnabled = false;
    private boolean[] idcEnabled = new boolean[EftPlugin.FLAGS_SIZE];
    private boolean[] receiptEnabled = new boolean[EftPlugin.FLAGS_SIZE];

    protected String authorizationCode = "";
    protected String cardNumber = "";
    protected String cardType = "";
    protected String terminalId = "";
    protected String receiptNumber = "";
    protected String rrn = "";
    protected long authorizedAmount;

    protected HashMap receiptData = new HashMap();
    protected HashMap errorCodeMap = new HashMap<String, Integer>();

    @Override
    public int doTransactionWithStatusCheck(Itemdata itm, String traNum, LinIo line) {
        return 0;
    }

    @Override
    public void printVouchers(String type) {
        if ((Struc.tra.mode == Struc.M_CANCEL) || (Struc.tra.mode == Struc.M_SUSPND)) {
            return;
        }

        logger.debug("Printing vouchers");
        if (type.equals(SAME_RECEIPT_TYPE) && receiptData.containsKey(type)) {
            Vector receipt = (Vector) this.receiptData.get(SAME_RECEIPT_TYPE);
            if (receipt != null && receipt.size() > 0 && customerCopyEnabled) {
                for (int index = 0; index < receipt.size(); ++index) {
                    logger.debug("Same Receipt Line [" + index + "] : "+ (String) receipt.get(index));
                    Struc.prtLine.init((String) receipt.get(index)).book(3);
                }
                Struc.prtLine.init(' ').book(3);
            }
        } else if (type.equals(NEW_RECEIPT_TYPE) && receiptData.containsKey(type)) {
            Vector receipt = (Vector) receiptData.get(NEW_RECEIPT_TYPE);
            if (receipt != null && receipt.size() > 0 && merchantCopyEnabled) {
                for (int index = 0; index < receipt.size(); ++index) {
                    logger.debug("New Receipt Line [" + index + "] : "+ (String) receipt.get(index));
                    Struc.prtLine.init((String) receipt.get(index)).type(2);
                }

                GdRegis.set_trailer();
                Struc.prtLine.type(2);
                GdRegis.hdr_print();
            }
        }
    }

    @Override
    public void resetVouchers() {
        receiptData = new HashMap();
    }

    public void loadErrorCodeMap() {
        logger.debug("ENTER loadErrorCodeMap");

        Properties prop = new Properties();

        try {
            prop.load(new FileInputStream("conf/errorCodes.properties"));

            for (Object key : prop.keySet().toArray()) {
                if (key.toString().startsWith("hashMap")) {
                    String value = prop.getProperty(key.toString());
                    errorCodeMap.put(key.toString().substring(8), value);
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("EXIT loadErrorCodeMap");
    }

    @Override
    public void loadEftTerminalParams(int line, String txt) {
        switch (line) {
            case 0:
                merchantCopyEnabled = txt.substring(0, 1).equals("1");
                customerCopyEnabled = txt.substring(1, 2).equals("1");
                break;
            case 1:
                for (int index = 0; index < FLAGS_SIZE; index++) {
                    idcEnabled[index] = txt.charAt(index) > '0';
                    receiptEnabled[index] = txt.charAt(20 + index) > '0';
                }
                break;
            default:
                break;
        }
    }

    @Override
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    @Override
    public String getCardNumber() {
        return cardNumber;
    }

    @Override
    public String getCardType() {
        return cardType;
    }

    @Override
    public String getTerminalId() {
        return terminalId;
    }

    @Override
    public String getTenderId() {
        return NO_TENDER_ID;
    }

    @Override
    public long getAuthorizedAmount() {
        return authorizedAmount;
    }

    @Override
    public String getReceiptNumber() {
        return receiptNumber;
    }

    @Override
    public String getRrn() {
        return rrn;
    }

    @Override
    public boolean isIdcEnabled(int index) {
        return idcEnabled[index];
    }

    @Override
    public boolean isReceiptEnabled(int index) {
        return receiptEnabled[index];
    }

    public boolean isCustomerCopyEnabled() {
        return customerCopyEnabled;
    }

    public boolean isMerchantCopyEnabled() {
        return merchantCopyEnabled;
    }

    public void addReceiptValues() {
        addReceiptValues(NEW_RECEIPT_TYPE);
        addReceiptValues(SAME_RECEIPT_TYPE);
    }

    public void addReceiptValues(String type) {
        try {
            File file = new File(type);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Vector voucher = (Vector) receiptData.get(type);
            if (voucher == null) voucher = new Vector();

            if (reader != null) {
                String line = null;

                while ((line = reader.readLine()) != null) {
                    line = manageMacro(line);
                    voucher.add(line);
                }
                receiptData.put(type, voucher);
            } else {
                logger.info("Error in addReceiptValues() , textVoucher is null check file S_PLU");
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
            return;
        }
    }

    private String manageMacro(String line) {

        if (line.indexOf(EFT_AUTH_NUMBER) >= 0) {
            line = line.substring(0, line.indexOf(EFT_AUTH_NUMBER)) + authorizationCode + line
                    .substring(line.indexOf(EFT_AUTH_NUMBER) + EFT_AUTH_NUMBER.length());
        }
        if (line.indexOf(EFT_AMOUNT) >= 0 && authorizedAmount > 0) {
            line = line.substring(0, line.indexOf(EFT_AMOUNT)) + GdRegis.editMoney(0, authorizedAmount)
                    + line.substring(line.indexOf(EFT_AMOUNT) + EFT_AMOUNT.length());
        }
        if (line.indexOf(EFT_CC_NUMBER) >= 0) {
            line = line.substring(0, line.indexOf(EFT_CC_NUMBER)) + cardNumber
                    + line.substring(line.indexOf(EFT_CC_NUMBER) + EFT_CC_NUMBER.length());
        }
        if (line.indexOf(EFT_CARD_SCHEME) >= 0) {
            line = line.substring(0, line.indexOf(EFT_CARD_SCHEME)) + cardType
                    + line.substring(line.indexOf(EFT_CARD_SCHEME) + EFT_CARD_SCHEME.length());
        }
        if (line.indexOf(EFT_TERMINAL_ID) >= 0) {
            line = line.substring(0, line.indexOf(EFT_TERMINAL_ID)) + terminalId
                    + line.substring(line.indexOf(EFT_TERMINAL_ID) + EFT_TERMINAL_ID.length());
        }
        if (line.indexOf(EFT_RECEIPT_NUMBER) >= 0) {
            line = line.substring(0, line.indexOf(EFT_RECEIPT_NUMBER)) + receiptNumber
                    + line.substring(line.indexOf(EFT_RECEIPT_NUMBER) + EFT_RECEIPT_NUMBER.length());
        }
        if (line.indexOf(EFT_RRN_NUMBER) >= 0) {
            line = line.substring(0, line.indexOf(EFT_RRN_NUMBER)) + rrn
                    + line.substring(line.indexOf(EFT_RRN_NUMBER) + EFT_RRN_NUMBER.length());
        }
        return line;
    }

    private String mask(String serial) {
        return serial.substring(0, serial.length() - 2) + "**";
    }

    public int action0(int spec) {
        return 0;
    }

    public int action1(int spec) {
        return 0;
    }

    public int action2(int spec) {
        return 0;
    }

    public int action3(int spec) {
        return 0;
    }

    public int action4(int spec) {
        return 0;
    }

    public int action5(int spec) {
        return 0;
    }

    public int action6(int spec) {
        return 0;
    }

    public int action7(int spec) {
        return 0;
    }

    public int action8(int spec) {
        return 0;
    }

    public int action9(int spec) {
        return 0;
    }
}
