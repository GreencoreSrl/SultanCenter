package com.ncr.eft;

import com.ncr.*;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

import static com.ncr.GdTndrs.tnd_wridc;
import static com.ncr.Itmdc.IDC_write;


/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 01/02/16
 * Time: 17.25
 * To change this template use File | Settings | File Templates.
 */

//VERIFONE-20160201-CGA#A BEG
public class MarshallEftPlugin extends GenericEftPlugin {
    private static final Logger logger = Logger.getLogger(MarshallEftPlugin.class);

    //say if dll was started correctly
    private boolean initialize = false;

    /*Macro for payment voucher*/
    private final String MACRO_cc_number = "$CC_NUMBER$";
    private final String MACRO_amount = "$AMOUNT$";
    private final String MACRO_authorisation_number = "$AUTH_NUMBER$";
    private final String MACRO_card_scheme_name = "$SCHEME_NAME$";
    private final String MACRO_ecr_amount = "$ECR_AMOUNT$";
    private final String MACRO_ecr_receipt = "$ECR_RECEIPT$";
    private final String MACRO_ecr_receipt_void = "$ECR_RECEIPT_VOID$";
    private final String MACRO_ecr_terminal_id = "$ECR_TERMINAL_ID$";
    private final String MACRO_transaction_id_Ecr = "$ECR_TRANSACTION_ID$";

    private String voidReceiptNumberECR = "";

    private static String ecrReceipt = "";
    private static String ecrReceiptVoid = "";
    private static long ecrAmt = 0;
    private static boolean settleEnabled = false;
    private static boolean testEnvironment = false;
    private static boolean risSettle = false;

    private static MarshallEftPlugin instance = null;

    public static MarshallEftPlugin getInstance() {
        if (instance == null)
            instance = new MarshallEftPlugin();

        return instance;
    }

    public MarshallEftPlugin() {
    }

    public void cleanReceiptData() {
        receiptData = new HashMap();
    }

    public void loadEftTerminalParams(int line, String txt) {
        super.loadEftTerminalParams(line, txt);

        if (line == 0) {
            settleEnabled = txt.substring(2, 3).equals("1");
            testEnvironment  = txt.substring(3, 4).equals("1");
        }
    }

    public int doTransaction(long amt, String traNum) {
        EcrMarshallObject veriFoneManager = testEnvironment ? new EcrObjectMock() : new EcrObject();

        initialize = veriFoneManager.getIsInitialized();
        logger.info("Verifone initialize [" + initialize + "]");

        if (!initialize) return ERR_NOTAUTHORIZED;

        logger.info("Verifone checkResponse");
        veriFoneManager.checkResponse();
        veriFoneManager.setTransactionType(EcrObject.TRANSACTION_TYPE_SALE);
        veriFoneManager.setEcrReceiptNumber(traNum);
        veriFoneManager.setAmoutECR(amt + "");

        ecrReceipt = traNum;
        ecrAmt = amt;

        veriFoneManager.getAuthECR();

        logger.info("Verifone response [" + veriFoneManager.getResponseMessageEcr() + "]");
        if (!veriFoneManager.checkResponse()) {
            logger.info("Verifone checkResponse failed");
            return ERR_RESPONSE;
        } else {
            logger.info("Verifone checkResponse success");

            authorizationCode = veriFoneManager.getApprovalCodeECR();
            cardType = veriFoneManager.getCardSchemaNameECR();
            cardNumber = veriFoneManager.getCardNumberECR();
            terminalId = veriFoneManager.getTIDECR();
            receiptNumber = veriFoneManager.getMessNumECR();

            addReceiptValues(veriFoneManager);
            IDC_write('z', Struc.tra.tnd, 0, veriFoneManager.getApprovalCodeECR(), 1, amt);
            return ERR_OK;
        }
    }

    public int action0(int spec) {
        logger.info("Enter Verifone action0, spec: " + spec);

        try {
            EcrMarshallObject veriFoneManager = testEnvironment ? new EcrObjectMock() : new EcrObject();

            int sts = 0;

            if ((sts = sc_checks(2, 6)) > 0)
                return sts;

            if (spec == 0) {
                return 0;
            }

            if (spec == 2) {
                veriFoneManager.setTransactionType(EcrObject.TRANSACTION_TYPE_SALE);
                veriFoneManager.setEcrReceiptNumber(String.valueOf(ctl.tran));
                veriFoneManager.setAmoutECR(input.pb);

                logger.info("veriFoneManagerECR.TRANSACTION_TYPE_SALE: " + EcrObject.TRANSACTION_TYPE_SALE);
                logger.info("ctl.tran: " + ctl.tran);
                logger.info("input.pb: " + input.pb);

                return 0;
            }

            if (spec == 3) {
                voidReceiptNumberECR = input.pb;
                veriFoneManager.setVoidTransactionNumber(input.pb);
            }

            if (!veriFoneManager.voidTransECR()) {
                logger.error("Error void ECR");
                return 121;
            }

            if (!tra.isActive()) {
                logger.info("call set_tra_top");
                GdRegis.set_tra_top();
            }

            prtTitle(102);

            addReceiptValues(veriFoneManager);

            printVouchers(MarshallEftPlugin.NEW_RECEIPT_TYPE);
            printVouchers(MarshallEftPlugin.SAME_RECEIPT_TYPE);

            logger.info("1 itm.tnd: " + itm.tnd);
            itm.tnd = eftPluginManager.getEftTender(getCardType(), itm.tnd);
            logger.info("2 itm.tnd: " + itm.tnd);

            tra.mode = 0;
            tra.code = 31;
            tra.amt = -Long.parseLong(veriFoneManager.getAmountECR());
            logger.info("amount ECR: " + tra.amt);

            IDC_write('H', trx_pres(), tra.spf3, tra.number, tra.cnt, tra.rate);
            IDC_write('z', tra.tnd, 1, "000", 1, -Long.parseLong(veriFoneManager.getAmountECR()));
            tnd_wridc('T', tra.tnd, 0, 1, -Long.parseLong(veriFoneManager.getAmountECR()));

            GdTrans.tra_finish();
        } catch (Exception e) {
            logger.error("EXCEPTION: " + e.getMessage());
        }

        logger.info("Exit Verifone action0");
        return 0;
    }

    public static String getEcrReceipt() {
        return ecrReceipt;
    }

    public static void setEcrReceipt(String ecrRcp) {
        ecrReceipt = ecrRcp;
    }

    public static String getEcrReceiptVoid() {
        return ecrReceiptVoid;
    }

    public static void setEcrReceiptVoid(String ecrRcpVoid) {
        ecrReceiptVoid = ecrRcpVoid;
    }

    public static long getEcrAmt() {
        return ecrAmt;
    }

    public static void setEcrAmt(long ecrAmount) {
        ecrAmt = ecrAmount;
    }

    public static boolean isEnableSettle() {
        return settleEnabled;
    }

    public int action1(int spec) {
        if (EftPluginManager.getInstance().isPluginEnabled(EftPlugin.MARSHALL_TENDER_ID)) {
            logger.info("call settle");
            prtTitle(105);

            dspLine.init(Mnemo.getText(81)).show(1);  //please wait
            risSettle = settle();
            dspLine.init("").show(1);

            if (risSettle) {
                gui.clearLink(Mnemo.getMenu(106), 1); //completed
                Struc.prtLine.init(Mnemo.getMenu(106)).book(3);
            } else {
                gui.clearLink(Mnemo.getInfo(134), 1); //error
                Struc.prtLine.init(Mnemo.getInfo(134)).book(3);
            }

            GdRegis.prt_trailer(2);
        }

        //EFT-SETTLE-CGA#A BEG
        if (SscoPosManager.getInstance().isEnabled()) {
            SscoPosManager.getInstance().eftSettleResponse();
        }
        //EFT-SETTLE-CGA#A END
        return 0;
    }

    public boolean settle() {
        logger.info("Enter settle");

        EcrMarshallObject veriFoneManager = testEnvironment ? new EcrObjectMock() : new EcrObject();

        boolean result = veriFoneManager.settleECR();
        itm.dpt = result ? 1 : 0;
        logger.info("itm.dpt: " + itm.dpt);

        IDC_write('z', tra.tnd, 2, "000", 1, 0);

        logger.info("Exit settle - result: " + result);
        return result;
    }

    public static boolean isRisSettle() {
        return risSettle;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    private void addReceiptValues(EcrMarshallObject veriFoneManager) {
        receiptData = new HashMap();
        addReceiptValues(NEW_RECEIPT_TYPE, veriFoneManager);
        addReceiptValues(SAME_RECEIPT_TYPE, veriFoneManager);
    }

    private void addReceiptValues(String type, EcrMarshallObject veriFoneManager) {
        logger.debug("ecrReceipt: " + ecrReceipt);
        logger.debug("Type: " + type);

        try {
            File file = new File(type);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Vector voucher = new Vector();

            if (reader != null) {
                try {
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        line = manageMacro(line, veriFoneManager);
                        voucher.add(line);
                        logger.debug("Added line: " + line);
                    }
                } catch (Exception exception) {
                    logger.error("addReceiptValues exception : ", exception);
                    return;
                }
                receiptData.put(type, voucher);
            } else {
                logger.info("Error in addReceiptValues() , textVoucher is null check file S_PLU");
            }
        } catch (Exception e) {
            logger.error("Error on reading S_PLURCO.DAT file : ", e);
            return;
        }
    }

    private String manageMacro(String line, EcrMarshallObject veriFoneManager) {
        if (line.indexOf(MACRO_authorisation_number) >= 0) {
            logger.debug("found macro: " + MACRO_authorisation_number);
            line = line.substring(0, line.indexOf(MACRO_authorisation_number)) + veriFoneManager.getApprovalCodeECR() + line.substring(line.indexOf(MACRO_authorisation_number) + MACRO_authorisation_number.length());
        }
        if (line.indexOf(MACRO_amount) >= 0) {
            logger.debug("found macro: " + MACRO_amount);
            line = line.substring(0, line.indexOf(MACRO_amount)) + GdRegis.editMoney(0, veriFoneManager.getLongAmountECR()) + line.substring(line.indexOf(MACRO_amount) + MACRO_amount.length());
        }
        if (line.indexOf(MACRO_cc_number) >= 0) {
            logger.debug("found macro: " + MACRO_cc_number);
            line = line.substring(0, line.indexOf(MACRO_cc_number)) + veriFoneManager.getCardNumberECR() + line.substring(line.indexOf(MACRO_cc_number) + MACRO_cc_number.length());
        }
        if (line.indexOf(MACRO_card_scheme_name) >= 0) {
            logger.debug("found macro: " + MACRO_card_scheme_name);
            line = line.substring(0, line.indexOf(MACRO_card_scheme_name)) + veriFoneManager.getCardSchemaNameECR() + line.substring(line.indexOf(MACRO_card_scheme_name) + MACRO_card_scheme_name.length());
        }
        if (line.indexOf(MACRO_ecr_amount) >= 0) {
            logger.debug("found macro: " + MACRO_ecr_amount);
            line = line.substring(0, line.indexOf(MACRO_ecr_amount)) + editMoney(0, Long.parseLong(veriFoneManager.getAmountECR())) + line.substring(line.indexOf(MACRO_ecr_amount) + MACRO_ecr_amount.length());
        }
        if (line.indexOf(MACRO_ecr_receipt) >= 0) {
            logger.debug("found macro: " + MACRO_ecr_receipt);
            line = line.substring(0, line.indexOf(MACRO_ecr_receipt)) + veriFoneManager.getEcrReceiptNumber() + line.substring(line.indexOf(MACRO_ecr_receipt) + MACRO_ecr_receipt.length());
        }
        if (line.indexOf(MACRO_ecr_receipt_void) >= 0) {
            logger.debug("found macro: " + MACRO_ecr_receipt_void);
            line = line.substring(0, line.indexOf(MACRO_ecr_receipt_void)) + voidReceiptNumberECR + line.substring(line.indexOf(MACRO_ecr_receipt_void) + MACRO_ecr_receipt_void.length());
        }
        if (line.indexOf(MACRO_ecr_terminal_id) >= 0) {
            logger.debug("found macro: " + MACRO_ecr_terminal_id);
            logger.debug("TIDECR: " + veriFoneManager.getTIDECR());
            //    line = line.substring(0, line.indexOf(MACRO_ecr_terminal_id)) + veriFoneManager.getResponseMessageEcr() + line.substring(line.indexOf(MACRO_ecr_terminal_id) + MACRO_ecr_terminal_id.length());
            line = line.substring(0, line.indexOf(MACRO_ecr_terminal_id)) + veriFoneManager.getTIDECR() + line.substring(line.indexOf(MACRO_ecr_terminal_id) + MACRO_ecr_terminal_id.length());
        }

        if (line.indexOf(MACRO_transaction_id_Ecr) >= 0) {
            logger.debug("found macro: " + MACRO_transaction_id_Ecr);
            logger.debug("messNum: " + veriFoneManager.getMessNumECR());
            line = line.substring(0, line.indexOf(MACRO_transaction_id_Ecr)) + veriFoneManager.getMessNumECR() + line.substring(line.indexOf(MACRO_transaction_id_Ecr) + MACRO_transaction_id_Ecr.length());
        }

        return line;
    }

    @Override
    public String getTenderId() {
        return MARSHALL_TENDER_ID;
    }
}