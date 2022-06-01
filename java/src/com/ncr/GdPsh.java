package com.ncr;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.*;

import com.ncr.giftcard.GiftCardPluginInterface;
import com.ncr.psh.data.CustomTender;
import com.ncr.ssco.communication.entities.pos.SscoCustomer;
import com.ncr.ssco.communication.manager.SscoPosManager;
import com.ncr.ssco.communication.requestprocessors.LoyaltyRequestProcessor;
import com.ncr.ssco.communication.requestprocessors.ProcessorConstants;
import com.ncr.struc.Customer;
import com.philoshopic.smash.ARSBAPI.LogController;
import com.philoshopic.smash.ARSBAPI.PrepayXLController;
import com.philoshopic.smash.ARSBAPI.SmashController;
import org.apache.log4j.Logger;

/**
 * Main PhiloShopic integration class
 *
 * @author Andrea Michele Zoia - Greencore for NCR Corp.
 */

//INTEGRATION-PHILOSHOPIC-CGA#A BEG
public class GdPsh extends Action implements GiftCardPluginInterface {
    private static final Logger logger = Logger.getLogger(GdPsh.class);
    private static GdPsh instance = null;
    public final static int MNEMO_ERROR_BASE = 86;
    public final static int MNEMO_MNEMO_BASE = 89;
    public final static int GIFT_EMPTY = 103;
    public final static int MNEMO_DIAGS_BASE = 20;
    private final static int SYNCERROR_BUY = 0;
    private final static int SYNCERROR_TOPUP = 1;
    private final static int SYNCERROR_PAY = 2;
    private final static int SYNCERROR_POINTS = 3;
    //DMA-TLOG_UPLOADING#A BEG
    private final static String TLOG_OK = "0;OK";
    private final static String TLOG_MISSING_PARAMETERS = "MISSING_PARAMETERS";
    private final static String TLOG_INVALID__TXN_ID = "INVALID__TXN_ID";
    private final static String TLOG_INVALID_PAYLOAD = "INVALID_PAYLOAD";
    //DMA-TLOG_UPLOADING#A END
    public static boolean readingGCSerial = false;
    private final static ArrayList<Itemdata> venduto = new ArrayList<Itemdata>();
    private final static ArrayList<Itemdata> ricariche = new ArrayList<Itemdata>();
    private final static ArrayList<Itemdata> pagamenti = new ArrayList<Itemdata>();
    private final static ArrayList<Itemdata> utilities = new ArrayList<Itemdata>();  //PSH-ENH-20151120-CGA#A
    private final static ArrayList<Itemdata> premi = new ArrayList<Itemdata>();
    private final static Properties prop = new Properties();
    private final static String PROP_FILENAME = "conf/philoshopic.properties";
    private final static String ACCOUNT_NUMBER_REQUIRED = "accountNumberRequired";
    private static boolean enabled = false;
    private static String par_httpPrimaryServer = "";
    private static String par_httpSecondaryServer = "";
    private static String par_storeid = "";
    private static String par_secret = "";

    //DMA-TLOG_UPLOADING#A BEG
    private static String par_httpPrimaryServerTlog = "";
    private static String par_httpSecondaryServerTlog = "";
    private static boolean tlogSyncingEnabled = false;
    private static boolean customerUpdateEnabled = false;
    //DMA-TLOG_UPLOADING#A END

    //PSH-ENH-20151120-CGA#A BEG
    private static String par_checkAmtGift = "";
    private static String par_httpPrimaryServerUtility = "";
    private static String par_httpSecondaryServerUtility = "";
    private static String par_storeidUtility = "";
    private static String par_secretUtility = "";
    //PSH-ENH-20151120-CGA#A END
    private static boolean printEnabled = false;
    private static boolean printSerialEnabled = false;
    private static boolean isEnableUtilityWithoutPr = false;   //UTILITY-WITHOUTPR-CGA#A
    private static int par_httpTimeoutSec = 5;
    private static boolean par_kbdenabled = true;
    private static String par_prefix = "";
    private static String par_prefixToEan = "";
    private static final String GIFTCARD = "GIFTCARD";
    private static final String LOYALTY = "LOYALTY";
    private static final String CREDIT = "CREDIT";
    private static final String DEBIT = "DEBIT";
    private static final String REPLY_OK_VALUE = "0";
    private static final String REQUEST_MOBILE = "-1";
    private static final String REQUEST_CARDDIG = "-2";
    private static final String REQUEST_CPIN = "-3";
    private static final String REQUEST_SMSPIN = "-4";
    private static final int SRV_RETRY = 5;
    private static SmashController controller;
    private static LogController logController; //DMA-TLOG_UPLOADING#A
    private static PrepayXLController ppController;  //PSH-ENH-20151120-CGA#A
    private static String lastTransactionID = "";
    private static HashMap<Integer, String> messages = new HashMap<Integer, String>();
    private static String uniqueTransactionId = ""; // AMZ-2017-003#ADD
    private static boolean pxlEnabled = false;  // AMZ-2017-003-004#ADD
    private static boolean smashEnabled = false; // AMZ-2017-003-004#ADD
    private static boolean barcodeEnabled = false; // AMZ-2017-003-004#ADD
    private static boolean priceAskDisabled = false; // AMZ-2017-003-004#ADD
    //PRINTBARCODE-CGA#A BEGAN
    private static boolean newBarcode=false;
    private final static int PTR_BCS_Code128_Parsed = 123;
    private final static int PTR_BCS_Code128 = 110;
    private final static String PAR_NEW_BARCODE ="newBarcode";
    //PRINTBARCODE-CGA#A END
    private final static String PROP_PXL_FILENAME = "conf/prepayXL.properties"; // AMZ-2017-003-004#ADD
    private final static String MASK_START_PV = "PV";//PSH-ENH-20211215-CGA#A
    private static String PAR_SELF_SELL = "printSelfSellItemEnabled";//PSH-ENH-20211215-CGA#A
    private static String SELF_SELL_ALL = "all";//PSH-ENH-20211215-CGA#A
    private static String tenderMaskProps = "accountType.";
    private static ArrayList<CustomTender> accountList = new ArrayList<CustomTender>();


    public static GdPsh getInstance() {
        if (instance == null)
            instance = new GdPsh();
        return instance;
    }

    public GdPsh() {
    }

    // AMZ-2017-003#BEG
    static void setUTID() {
        /*
        A unique POS transaction ID will be generated having the following structure:
        SSSSRRRTTTTYYYYMMDDHHmmss
        Where:
        SSSS – is the store number
        RRR – is the register number (terminal)
        TTTT – is the transaction number
        YYYYMMDDHHmmss – is the date and time of the transaction
        This code will be used for Philoshopic communication and printed at the end of receipt as a
        Code128 barcode.
         */
        uniqueTransactionId = //
                "20" + ctl.date + editNum(ctl.sto_nbr, 3) + editNum(ctl.reg_nbr, 3) + editNum(ctl.tran, 4);

        logger.debug("uniqueTransactionId = " + uniqueTransactionId);
    }

    public static String getUniqueTransactionId() {
        return uniqueTransactionId;
    }

    static void printBarcode() {
        // AMZ-2017-003#NOTA -- magari controllare il tipo transazione per non stampare barcode inutili
        // AMZ-2017-003-FIX1#BEG—
        logger.info("ENTER printBarcode");
        if (!barcodeEnabled) {
            logger.info("EXIT printBarcode() - par_isEnabledBarcode: disabled");
            return;
        }
        if (ctl.uniqueId.length() == 0) {
            logger.info("EXIT printBarcode() - uniqueTransactionId.length(): " + ctl.uniqueId.length());
            return;
        }
        // AMZ-2017-003-FIX1#END
        logger.info("EXIT printBarcode() - call DevIo.tpmLabel(2, uniqueTransactionId);");
        //DevIo.tpmLabel(2, ctl.uniqueId);
        //PRINTBARCODE-CGA#A BEGAN
        logger.info("newBarcode property= "+ isNewBarcode());
        if(isNewBarcode()){
            DevIo.tpmLabel(2,ctl.uniqueId,PTR_BCS_Code128_Parsed);
        }else{
            DevIo.tpmLabel(2,ctl.uniqueId,PTR_BCS_Code128);
        }
        //PRINTBARCODE-CGA#A END
    }
    // AMZ-2017-003#END

    /**
     * @return true if all Philoshopic functions are enabled, false otherwise
     */

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSmashEnabled() {
        return isEnabled() && smashEnabled;
    }

    static boolean isBarcodeEnabled() {
        return barcodeEnabled;
    }

    public boolean isPxlEnabled() {
        return isEnabled() && pxlEnabled;
    }

    public static boolean isNewBarcode() {
        return newBarcode;
    }

    static boolean priceAskDisabled() {
        return priceAskDisabled;
    }

    static PrepayXLController getPpController() {
        return ppController;
    }

    /**
     * @return true if keyboard is enabled for card serial number input
     */
    static boolean isKbdEnabled() {
        return par_kbdenabled;
    }

    public static boolean isEnabledPrintAllGiftItem() {
        logger.debug("printSelfSellItemEnabled = " + prop.getProperty(PAR_SELF_SELL, SELF_SELL_ALL));
        return (prop.getProperty(PAR_SELF_SELL, SELF_SELL_ALL).trim().toLowerCase().equalsIgnoreCase(SELF_SELL_ALL));
    }

    public static ArrayList<CustomTender> getAccountList() {
        return accountList;
    }

    static void readPregpar(String txt, int ind) throws Exception {
        if (ind != 0) {
            throw new Exception("Bad PSHP line number in p_regpar, must be PSHP0");
        }
        enabled = (Integer.parseInt(txt.substring(0, 2)) == 1);
        par_kbdenabled = (Integer.parseInt(txt.substring(2, 4)) == 1);
        par_prefix = txt.substring(4, 12);
        par_prefixToEan = txt.substring(12, 20);
        par_checkAmtGift = txt.substring(20, 22); //PSH-ENH-20151120-CGA#A
        printEnabled = txt.charAt(22) == '1';
        printSerialEnabled = txt.charAt(23) == '1';
        isEnableUtilityWithoutPr = txt.charAt(24) == '1';  //UTILITY-WITHOUTPR-CGA#A
        Integer.parseInt(par_prefix); // Check against errors
        par_prefixToEan = "" + Integer.parseInt(par_prefixToEan); // Check against errors & remove zeroes
        readConfig();
        if (enabled) {
            logger.debug("PSHP:Enabled");
            logger.debug("PSHP:Primary Server endpoint " + par_httpPrimaryServer);
            logger.debug("PSHP:Secondary Server endpoint " + par_httpSecondaryServer);
            logger.debug("PSHP:Server timeout " + par_httpTimeoutSec);
            logger.debug("PSHP:Keyboard " + (par_kbdenabled ? "enabled" : "disabled"));
            logger.debug("PSHP:topup card prefix " + par_prefix);
            logger.debug("PSHP:topup item map on EAN " + par_prefixToEan);
            logger.debug("PSHP:Store number " + par_storeid);
            logger.debug("PSHP:Primary Server endpoint utility " + par_httpPrimaryServerUtility);
            logger.debug("PSHP:Secondary Server endpoint utility " + par_httpSecondaryServerUtility);
            logger.debug("PSHP:Primary Server endpoint tlog " + par_httpPrimaryServerTlog);
            logger.debug("PSHP:Secondary Server endpoint tlog " + par_httpSecondaryServerTlog);
            logger.debug("PSHP:Store number utility " + par_storeidUtility);
            logger.debug("PSHP:check Amount Gift card " + par_checkAmtGift);
            // AMZ-2017-003-004#BEG
            logger.debug("PSHP:SMASH enabled " + smashEnabled);
            logger.debug("PSHP:Barcode printing enabled " + barcodeEnabled);
            logger.debug("PSHP:PXL enabled " + pxlEnabled);
            logger.debug("PSHP:no ask price to cashier " + priceAskDisabled);
            logger.debug("PSHP:utility without properties " + isEnableUtilityWithoutPr);
            // AMZ-2017-003-004#END
        }
        controller = new SmashController(par_httpPrimaryServer, par_httpSecondaryServer, par_storeid, par_secret);
        ppController = new PrepayXLController(par_httpPrimaryServerUtility, par_httpSecondaryServerUtility, par_storeidUtility, par_secretUtility);
        logController = new LogController(par_httpPrimaryServerTlog, par_httpSecondaryServerTlog, par_storeid, par_secret);
        loadMessages();
    }

    /**
     * read from config file philoshopic.conf
     */
    static void readConfig() throws Exception {
        try {
            prop.load(new FileInputStream(PROP_FILENAME));
            String par;
            try {
                par = prop.getProperty("httpTimeoutSeconds");
                par_httpTimeoutSec = Integer.parseInt(par);
            } catch (final Exception e) {
                throw new Exception(
                        "missing or malformed httpTimeoutSeconds in file " + PROP_FILENAME + ", " + e.getMessage());
            }

            par_httpPrimaryServer = prop.getProperty("httpServer.primary.endpoint");
            if (par_httpPrimaryServer == null) {
                throw new Exception("missing or malformed httpServer.primary.endpoint in file " + PROP_FILENAME);
            }

            par_httpSecondaryServer = prop.getProperty("httpServer.secondary.endpoint");
            if (par_httpSecondaryServer == null) {
                throw new Exception("missing or malformed httpServer.secondary.endpoint in file " + PROP_FILENAME);
            }

            par_storeid = prop.getProperty("storeId");
            if (par_storeid == null) {
                throw new Exception("missing or malformed storeId in file " + PROP_FILENAME);
            }

            par_secret = prop.getProperty("secret");
            if (par_secret == null) {
                throw new Exception("missing or malformed httpServer in file " + PROP_FILENAME);
            }

            //PSH-ENH-20151120-CGA#A BEG
            par_httpPrimaryServerUtility = prop.getProperty("httpServer.primary.endpoint.utility");
            if (par_httpPrimaryServerUtility == null) {
                throw new Exception("missing or malformed httpServer.primary.endpoint.utility in file " + PROP_FILENAME);
            }

            par_httpSecondaryServerUtility = prop.getProperty("httpServer.secondary.endpoint.utility");
            if (par_httpSecondaryServerUtility == null) {
                throw new Exception("missing or malformed httpServer.secondary.endpoint.utility in file " + PROP_FILENAME);
            }

            par_storeidUtility = prop.getProperty("storeId.utility");
            if (par_storeidUtility == null) {
                throw new Exception("missing or malformed storeId.utility in file " + PROP_FILENAME);
            }

            par_secretUtility = prop.getProperty("secret.utility");
            if (par_secretUtility == null) {
                throw new Exception("missing or malformed httpServer.utility in file " + PROP_FILENAME);
            }
            //PSH-ENH-20151120-CGA#A END

            //DMA-TLOG_UPLOADING#A BEG
            par_httpPrimaryServerTlog = prop.getProperty("httpServer.primary.endpoint.tlog");
            if (par_httpPrimaryServerTlog == null) {
                throw new Exception("missing or malformed httpServer.primary.endpoint.tlog in file " + PROP_FILENAME);
            }

            par_httpSecondaryServerTlog = prop.getProperty("httpServer.secondary.endpoint.tlog");
            if (par_httpPrimaryServerTlog == null) {
                throw new Exception("missing or malformed httpServer.secondary.endpoint.tlog in file " + PROP_FILENAME);
            }
            try {
                tlogSyncingEnabled = Boolean.parseBoolean(prop.getProperty("tlogSyncingEnabled"));
            } catch (Exception e) {
                throw new Exception("missing or malformed tlogSyncingEnabled parameter in file " + PROP_FILENAME);
            }
            try {
                customerUpdateEnabled = Boolean.parseBoolean(prop.getProperty("customerUpdateEnabled"));
            } catch (Exception e) {
                throw new Exception("missing or malformed customerUpdateEnabled parameter in file " + PROP_FILENAME);
            }
            //DMA-TLOG_UPLOADING#A END
            // AMZ-2017-003-004#BEG
            try {
                smashEnabled = Boolean.parseBoolean(prop.getProperty("isEnabled"));
            } catch (Exception e) {
                throw new Exception("missing or malformed isEnabled parameter in file " + PROP_FILENAME);
            }
            try {
                barcodeEnabled = Boolean.parseBoolean(prop.getProperty("barcodeEnabled"));
            } catch (Exception e) {
                throw new Exception("missing or malformed barcodeEnabled parameter in file " + PROP_FILENAME);
            }
            //PRINTBARCODE-CGA#A BEGAN
            try {
                newBarcode = Boolean.parseBoolean(prop.getProperty(PAR_NEW_BARCODE));
            } catch (Exception e) {
                throw new Exception("missing or malformed newBarcode parameter in file " + PROP_FILENAME);
            }
            //PRINTBARCODE-CGA#A END

            readAccountNumberList();

            try {
                prop.load(new FileInputStream(PROP_PXL_FILENAME));
            } catch (Exception e) {
                //UTILITY-WITHOUTPR-CGA#A BEG
                if (GdPsh.getInstance().isEnableUtilityWithoutPr()) {
                    return;
                }
                //UTILITY-WITHOUTPR-CGA#A END
                throw new Exception("missing or malformed noAskPrice parameter in file " + PROP_PXL_FILENAME);
            }

            try {
                pxlEnabled = Boolean.parseBoolean(prop.getProperty("isEnabled", "false"));
            } catch (Exception e) {
                throw new Exception("missing or malformed isEnabled parameter in file " + PROP_PXL_FILENAME);
            }
            try {
                priceAskDisabled = Boolean.parseBoolean(prop.getProperty("noAskPrice", "false"));
            } catch (Exception e) {
                throw new Exception("missing or malformed noAskPrice parameter in file " + PROP_PXL_FILENAME);
            }
            // AMZ-2017-003-004#END
        } catch (final Exception e) {

            throw new Exception("malformed or missing file " + PROP_FILENAME + ", " + e.getMessage());
        }
    }

    private static void readAccountNumberList() {
        Set<Object> listKeys = prop.keySet();
        for (Object key : listKeys) {
            if (key.toString().startsWith(tenderMaskProps)) {
                String tender = prop.getProperty(key.toString());
                String id = key.toString().substring(tenderMaskProps.length());
                String[] str = tender.split(";");
                accountList.add(new CustomTender(Integer.parseInt(id), str[0], Boolean.parseBoolean(str[1])));
            }
        }

    }

    //UTILITY-WITHOUTPR-CGA#A BEG
    public static boolean isEnableUtilityWithoutPr() {
        return isEnableUtilityWithoutPr;
    }
    //UTILITY-WITHOUTPR-CGA#A END

    /**
     * Info Giftcard topup
     */
    @Override
    public int action0(int spec) {
        if (!enabled) return 7; // unavailable

        Itemdata itm = new Itemdata();
        itm.accountType = GIFTCARD;

        int res = readSerial32(itm);
        if (res == -1) return 0;
        if (res > 0) return res;

        res = srv_totalGiftCardBalance(itm);
        if (res > 0) return res;

        panel.clearLink(Mnemo.getDiag(MNEMO_DIAGS_BASE + 2).trim() + " " + editMoney(0, itm.amt), 0x81);
        return 0;
    }

    /**
     * Info Loyalty points
     */
    @Override
    public int action1(int spec) {
        logger.debug("ENTER action1");

        if (!isSmashEnabled()) {
            logger.debug("SMASH controller disabled. exiting");
            return 7; // unavailable
        }

        if (cus.getNumber() == null) {
            logger.debug("ENTER action1_2");
            return 7;
        }
        if (cus.getNumber().trim().length() == 0) {
            logger.debug("ENTER action1_3");
            return 7;
        }
        logger.info("customer number: " + cus.getNumber());

        int res = srv_customerPointsBalance(cus);
        logger.info("res: " + res);

        if (res > 0) {
            logger.debug("ENTER action1_4");
            return res;
        }

        panel.clearLink(Mnemo.getDiag(MNEMO_DIAGS_BASE + 3).trim() + " " + cus.getPnt(), 0x81);

        logger.debug("ENTER action1_5");
        return 0;
    }

    //PSH-ENH-20151120-CGA#A BEG
    public static int readCodePrepay(String descr) {
        logger.debug("ENTER readCodePrepay");
        logger.info("item prepayXL: " + descr.trim());


        //AMZ-2017-003-005#BEG -- lettura articolo da server

        logger.info("Searching barcode on server XL");
        try {
            String[] prc = ppController.getBarcode(descr.trim()).split(";");
            // NON ho le specifiche di come e' formattata la stringa in risposta
            if ("0".equals(prc[0])) {
                itm.utilityCode = prc[1];
                itm.utilityName = descr.trim();
                try {
                    itm.utilityMaxPrice = Integer.parseInt(prc[2]);
                } catch (Exception e) {
                    itm.utilityMaxPrice = 0;
                    logger.info("set itm.utilityMaxPrice = 0");
                    logger.info("EXCEPTION: " + e.getMessage());
                }

                logger.info("Server getBarcode : itm.utilityCode = " + itm.utilityCode);
                logger.info("Server getBarcode : itm.utilityMaxPrice = " + itm.utilityMaxPrice);
                if (itm.utilityCode.length() > 0) {
                    logger.debug("EXIT readCodePrepay");
                    return 0;
                }
            } else {
                logger.debug("Server getBarcode : ERROR: " + prc[0]);
            }
        } catch (Exception e) {
            logger.info("EXCEPTION: " + e.getMessage());
        }

        logger.info("Server failure : Searching barcode in local properties file");


        //AMZ-2017-003-005#END


        Properties prepayXl = new Properties();
        String description = "";

        itm.utilityName = descr.trim();

        try {
            prepayXl.load(new FileInputStream("conf/prepayXL.properties"));

            if (prepayXl.values().contains(descr.trim())) {
                logger.info("value found in the properties file");

                Set<Object> listKeys = prepayXl.keySet();
                for (Object key : listKeys) {
                    description = prepayXl.getProperty((String) key).trim();
                    logger.info("value: " + description);

                    if (description.equals(descr.trim())) {
                        logger.info("key found");
                        logger.debug("EXIT readCodePrepay - return: " + key);

                        String[] fields = ((String) key).split("\\.");
                        itm.utilityCode = fields[1];

                        if (descr.startsWith("dir") && (prepayXl.getProperty(fields[0] + "." + fields[1] + ".max")) != null) {
                            itm.utilityMaxPrice = Integer.parseInt(prepayXl.getProperty(fields[0] + "." + fields[1] + ".max").trim());
                        }

                        break;
                    }
                }
            } else {
                logger.info("value not found in the properties file, read default value");
                itm.utilityCode = prepayXl.getProperty("default.code").trim();

                if (descr.startsWith("dir")) {
                    itm.utilityMaxPrice = Integer.parseInt(prepayXl.getProperty("default.max").trim());
                }
            }
        } catch (Exception e) {
            logger.info("EXCEPTION: " + e.getMessage());
            return 7;
        }

        logger.debug("EXIT readCodePrepay - ean not found");

        return 0;
    }

    static int insertMobile() {
        logger.debug("ENTER insertMobile");

        ConIo newInput = new ConIo(20);
        for (; ; ) {
            ModDlg dlg = new ModDlg(Mnemo.getText(85));
            dlg.block = false;
            dlg.input = newInput;

            ConIo mtio = Motor.input;
            Motor.input = dlg.input;

            dlg.input.prompt = Mnemo.getText(15);
            newInput.init(0x00, 20, 0, 0);

            oplToggle(2, Mnemo.getText(15));
            dlg.show("PSH");

            input.reset("");
            Motor.input = mtio;
            oplToggle(0, null);

            if (dlg.code != 0) {
                return 2;
            }
            if (dlg.input.key == ConIo.CLEAR) {
                logger.debug("EXIT abort operation without user message panel");

                return -1;
            }
            if (dlg.input.num < 1) {
                continue;
            }
            if (dlg.input.key == ConIo.ENTER) {
                if (par_kbdenabled) {
                    break;
                }
                logger.debug("EXIT utility server error");

                return MNEMO_ERROR_BASE + 13;
            }
            if (dlg.input.key == 0x4d4d) {
                break;
            }
            if (dlg.input.key == 0x4f4f) {
                break;
            }
        }

        cus.setMobile(newInput.pb);

        logger.info("mobile: " + cus.getMobile());
        logger.debug("EXIT insertMobile - return ok");
        return 0;
    }
    //PSH-ENH-20151120-CGA#A END

    /**
     * Read the serial 32 chars of gift card
     *
     * @return 0 ok, -1 error without messages, elsewhere nmemo error
     */
    public int readSerial32(Itemdata plu) {
        // do not ask serial again if one is provided
        // eg : by topup direct serial number
        if (plu.gCardSerial.length() > 0) {
            return 0;
        }
        ConIo newInput = new ConIo(20);
        for (; ; ) {
            ModDlg dlg = new ModDlg(Mnemo.getDiag(MNEMO_DIAGS_BASE + 0));
            dlg.block = false;
            dlg.input = newInput;
            ConIo mtio = Motor.input;
            // Il codice che legge le strisce msr e' in ConIo.track
            // mentre in PosIo si trova il wedge (MSR)
            Motor.input = dlg.input; // per far arrivare il codice da scanner e MSR
            // Succede che Motor si salva l'oggetto input creato nella Action
            // ed e' quello che voglio cambiare per aumentare la lunghezza
            dlg.input.prompt = Mnemo.getText(MNEMO_MNEMO_BASE + 0);
            //newInput.init(0x00, 20, 0, 0);
            newInput.init(0x10, 255, 0, 0);
            //minifont(true);
            oplToggle(2, Mnemo.getText(MNEMO_MNEMO_BASE + 0));
            readingGCSerial = true;
            dlg.show("PSH");
            readingGCSerial = false;
            input.reset("");
            //minifont(false);
            Motor.input = mtio;

            oplToggle(0, null);
            if (dlg.code != 0) {
                return 2;
            }
            if (dlg.input.key == ConIo.CLEAR) {
                return -1; // abort operation without user message panel
            }
            if (dlg.input.num < 1) {
                continue;
            }
            if (dlg.input.key == ConIo.ENTER) {
                if (par_kbdenabled) {
                    break;
                }
                return MNEMO_ERROR_BASE + 4;
            }
            if (dlg.input.key == 0x4d4d) {
                break;
            }
            if (dlg.input.key == 0x4f4f) {
                break;
            }
        }
        plu.gCardSerial = newInput.pb;
        return 0;
    }

    static String readExtinfo() {
        return readExtinfo(Mnemo.getDiag(MNEMO_DIAGS_BASE + 4));
    }

    /**
     * Read the aux information due server request
     *
     * @return the input string or null to cancel operation
     */
    static String readExtinfo(String dialogText) {
        for (; ; ) {
            ModDlg dlg = new ModDlg(dialogText);
            dlg.block = false;
            dlg.input.prompt = Mnemo.getText(MNEMO_MNEMO_BASE + 1);
            oplToggle(2, Mnemo.getText(MNEMO_MNEMO_BASE + 1));
            dlg.input.reset("");
            dlg.input.init(0, 30, 1, 0);
            dlg.show("PSH");
            oplToggle(0, null);
            if (dlg.code != 0) {
                return null;
            }
            if (dlg.input.key == ConIo.CLEAR) {
                return null; // abort operation
            }
            if (dlg.input.num < 1) {
                continue;
            }
            if (dlg.input.key == ConIo.ENTER) {
                return dlg.input.pb;
            }
            if (dlg.input.key == 0x4d4d) {
                return dlg.input.pb;
            }
            if (dlg.input.key == 0x4f4f) {
                return dlg.input.pb;
            }
        }
    }

    static int pointsRedemption(Itemdata itm) {
        int ret;
        ret = srv_pointsRedemption(itm);
        if (ret == 0) {
            premi.add(itm.copy());
        }

        return ret;
    }

    public void setCashierId(int posId, int cashierId) {
        if (!enabled) return;

        logger.info("SmashController 1 call function setCashierId [" + posId + "] [" + cashierId + "]");
        controller.setCashierID(String.valueOf(posId), String.valueOf(cashierId));

        if (isPxlEnabled()) {
            logger.info("PrepayXLController call function setCashierId [" + posId + "] [" + cashierId + "]");
            ppController.setCashierID(String.valueOf(posId), String.valueOf(cashierId));
        }
    }

    /**
     * sell (activate) gift card
     *
     * @param itm
     * @return 0 if ok, >0 error code in nmemo
     */
    static int sellGiftCard(Itemdata itm) {
        int ret;
        if (itm.gCardTopup) {
            ret = srv_sellTopupGiftCard(itm);
            if (ret == 0) {
                ricariche.add(itm.copy());
            }
        } else {
            ret = srv_sellGiftCard(itm);
            if (ret == 0) {
                venduto.add(itm.copy());
            }
        }
        return ret;
    }

    /**
     * pay with gift card
     *
     * @param itm
     * @return 0 if ok, >0 error code in nmemo
     */
    @Override
    public int redemptionGiftCard(Itemdata itm, Transact tra) {

        if (!isEnabled()) {
            return 7; // unavailable
        }

        if (itm.amt > tra.bal) {
            return MNEMO_ERROR_BASE + 3;
        }

        // ask for 32 digit card code
        int res = 0;
        /**
         * Check accountTypeRequired paramter to ask for card code or not
         */
        if (itm.accountNumberRequired) {
            res = readSerial32(itm);
        }

        if (res > 0) {
            return res;
        }
        if (res < 0) {
            return 5;
        }

        res = srv_payGiftCard(itm);
        if (res > 0) {
            return res;
        }
        itm.gCardPayment = true;
        tra.gctnd += itm.amt;
        pagamenti.add(itm.copy());

        return 0;
    }

    /**
     * cancel payment with gift card
     *
     * @param itm
     * @return 0 if ok, >0 error code in nmemo
     */
    static int cancelPayGiftCard(Itemdata itm, Transact tra) {

        if (!enabled) {
            return 7; // unavailable
        }

        for (Itemdata itmloop : pagamenti) {
            if (itmloop.gCardSerial.trim().compareTo(itm.gCardSerial.trim()) != 0)
                continue;
            if (itmloop.number.trim().compareTo(itm.number.trim()) != 0)
                continue;
            if (itmloop.amt != itm.amt)
                continue;
            int ret = srv_cancelPayGiftCard(itmloop);
            if (ret == 0) {
                pagamenti.remove(itmloop);
                // during VOID the item is not yet fully decorated cause is regenerated from scratch
                itm.utilityEnglishText = itmloop.utilityEnglishText;
                itm.gCardDsc = itmloop.gCardDsc;
                return 0;
            }
            return ret;
        }
        return 0;
    }

    /**
     * cancel payment with gift card
     *
     * @param itm
     * @return 0 if ok, >0 error code in nmemo
     */
    public int cancelRedemption(Itemdata itm) {

        if (!isSmashEnabled()) {
            logger.debug("SMASH controller disabled. exiting");
            return 7; // unavailable
        }

        for (Itemdata itmloop : premi) {
            if (itmloop.prpnt != itm.prpnt)
                continue;
            if (itmloop.number.trim().compareTo(itm.number.trim()) != 0)
                continue;
            if (itmloop.amt != itm.amt)
                continue;
            int ret = srv_cancelRedemption(itmloop);
            if (ret == 0) {
                premi.remove(itmloop);
                // during VOID the item is not yet fully decorated cause is regenerated from scratch
                itm.redemptionDsc = itmloop.redemptionDsc;
                return 0;
            }
            return ret;
        }
        return 0;
    }

    /**
     * Pay with gift card to server
     *
     * @param itm
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_payGiftCard(Itemdata itm) {
        String ret[] = srvDoTransaction(DEBIT, itm.accountType, itm.gCardSerial, itm.amt);


        if (!REPLY_OK_VALUE.equals(ret[0])) {
            if (ret[0].equals(String.valueOf(GIFT_EMPTY))) {
                return GIFT_EMPTY;
            }
            return MNEMO_ERROR_BASE + 0;
        }
        try {
            itm.gCardTransaction = ret[2];
            if (Integer.parseInt(ret[3]) == 99 && ret[4].length() > 0) {
                itm.utilityEnglishText = ret[4];
            } else {
                itm.gCardDsc = messages.get(Integer.parseInt(ret[3]));
            }
            itm.gCardBal = ret[1];
        } catch (Exception e) {
            System.err.println("PSHP:Error parsing response. Setting defaults.");
            itm.gCardTransaction = lastTransactionID;
            itm.utilityEnglishText = "";
            itm.gCardDsc = "";
            itm.gCardBal = "";
        }
        return 0; // ok
    }

    public static void printText(String engTxt) {
        logger.debug("ENTER printUtility");
        int pos = 0;

        String rowEng[] = engTxt.split("<br />");

        for (int i = 0; i < rowEng.length; i++) {
            if (rowEng[i].contains("<h") || rowEng[i].contains("</h")) {
                String rowBold[] = rowEng[i].split("<h");

                int x = 0;
                if (rowBold[0].length() > 0 && !rowBold[0].contains("</h")) {
                    prtLine.init(rowBold[0]).book(3);
                    x = 1;
                } else if (rowBold[0].length() == 0) {
                    x = 1;
                }

                for (; x < rowBold.length; x++) {
                    if ((pos = rowBold[x].indexOf("</h")) >= 0) {
                        if (rowBold[x].charAt(1) == '>') {
                            engTxt = rowBold[x].substring(2, pos);
                        } else {
                            engTxt = rowBold[x].substring(0, pos);
                        }

                        prtDwide(3, engTxt);

                        if (pos + 5 < rowBold[x].length()) {
                            prtLine.init(rowBold[x].substring(pos + 5)).book(3);
                        }
                    } else {
                        engTxt = rowBold[x].substring(2);
                        prtDwide(3, engTxt);
                    }
                }
            } else if (rowEng[i].contains("<b>")) {
                String rowBold[] = rowEng[i].split("<b>");

                int x = 0;
                if (rowBold[0].length() > 0 && !rowBold[0].contains("</b>")) {
                    prtLine.init(rowBold[0]).book(3);
                    x = 1;
                } else if (rowBold[0].length() == 0) {
                    x = 1;
                }

                for (; x < rowBold.length; x++) {
                    if ((pos = rowBold[x].indexOf("</b>")) >= 0) {
                        engTxt = rowBold[x].substring(0, pos);
                        prtDwide(3, engTxt);

                        if (pos + 5 < rowBold[x].length()) {
                            prtLine.init(rowBold[x].substring(pos + 5)).book(3);
                        }
                    } else {
                        engTxt = rowBold[x].substring(0);
                        prtDwide(3, engTxt);
                    }
                }
            } else {
                prtLine.init(rowEng[i]).book(3);
            }
        }
    }

    /**
     * Ask the money to server for the gift card
     *
     * @param itm
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_totalGiftCardBalance(Itemdata itm) {
        String[] reply = srvGetAccountBalance(itm.accountType, itm.gCardSerial);
        if (!REPLY_OK_VALUE.equals(reply[0])) {
            return MNEMO_ERROR_BASE + 0;
        } else {
            if (reply[1].equals("null")) {
                reply[1] = "0";
            }

            if (reply[2].equals("null")) {
                reply[2] = "0";
            }

            itm.amt = Long.parseLong(reply[1].replace(",", "").replace(".", "").trim());

            return 0; // ok
        }
    }

    /**
     * Ask the points to server for the customer
     *
     * @param cus
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_customerPointsBalance(Customer cus) {
        return srv_customerCheck(cus);
    }

    private static int srv_cancelRedemption(Itemdata itm) {
        String ret[] = srvCancelTransaction(DEBIT, itm.accountType, cus.getNumber(), itm.prpnt, itm.redemptionTransaction);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 5;
        }
        return 0; // ok
    }

    /**
     * Cancel the payment to the server
     *
     * @param itm
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_cancelPayGiftCard(Itemdata itm) {
        String ret[] = srvCancelTransaction(DEBIT, itm.accountType, itm.gCardSerial, itm.amt, itm.gCardTransaction);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        return 0; // ok
    }

    /**
     * Sells a gift card to server (activation)
     *
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_sellGiftCard(Itemdata itm) {
        if (itm.qty != 1) {
            // no multiplier
            return MNEMO_ERROR_BASE + 1;
        }
        String ret[] = srvDoActivation(GIFTCARD, itm.gCardSerial, itm.price);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        try {
            itm.gCardTransaction = ret[2];
            if (Integer.parseInt(ret[3]) == 99 && ret[4].length() > 0) {
                itm.utilityEnglishText = ret[4];
            } else {
                itm.gCardDsc = messages.get(Integer.parseInt(ret[3]));
            }
            itm.gCardBal = ret[1];
        } catch (Exception e) {
            System.err.println("PSHP:Error parsing response. Setting defaults.");
            itm.gCardTransaction = lastTransactionID;
            itm.utilityEnglishText = "";
            itm.gCardDsc = "";
            itm.gCardBal = "";
        }
        return 0; // ok
    }

    /**
     * Sells a topup gift card to server
     *
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_sellTopupGiftCard(Itemdata itm) {
        if (itm.qty != 1) {
            // no multiplier
            return MNEMO_ERROR_BASE + 1;
        }
        String ret[] = srvDoTransaction(CREDIT, GIFTCARD, itm.gCardSerial, itm.price);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        try {
            itm.gCardTransaction = ret[2];
            if (Integer.parseInt(ret[3]) == 99 && ret[4].length() > 0) {
                itm.utilityEnglishText = ret[4];
            } else {
                itm.gCardDsc = messages.get(Integer.parseInt(ret[3]));
            }
            itm.gCardBal = ret[1];
        } catch (Exception e) {
            System.err.println("PSHP:Error parsing response. Setting defaults.");
            itm.gCardTransaction = lastTransactionID;
            itm.utilityEnglishText = "";
            itm.gCardDsc = "";
            itm.gCardBal = "";
        }
        return 0; // ok
    }

    /**
     * Cancel all gift cards to server <br>
     * Called by Abort Transaction <br>
     * ! In any case cannot stop the POS to abort the transaction !
     *
     * @return 0 if ok, Errors are traced
     */
    public int cancelAll() {
        ArrayList<Itemdata> vendutoLoop = new ArrayList<Itemdata>(venduto);
        for (Itemdata itm : vendutoLoop) {
            int res = srv_cancelSellGiftCard(itm);
            if (res != 0) {
                Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, itm.gCardSerial, SYNCERROR_BUY, itm.price);
            } else {
                venduto.remove(itm);
            }
        }
        ArrayList<Itemdata> ricaricheLoop = new ArrayList<Itemdata>(ricariche);
        for (Itemdata itm : ricaricheLoop) {
            int res = srv_cancelSellTopupGiftCard(itm);
            if (res != 0) {
                Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, itm.gCardSerial, SYNCERROR_TOPUP, itm.price);
            } else {
                ricariche.remove(itm);
            }
        }
        ArrayList<Itemdata> pagamentiLoop = new ArrayList<Itemdata>(pagamenti);
        for (Itemdata itm : pagamentiLoop) {
            int res = srv_cancelPayGiftCard(itm);
            if (res != 0) {
                Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, itm.gCardSerial, SYNCERROR_PAY, itm.amt);
            } else {
                pagamenti.remove(itm);
            }
        }
        ArrayList<Itemdata> premiLoop = new ArrayList<Itemdata>(premi);
        for (Itemdata itm : premiLoop) {
            int res = srv_cancelRedemption(itm);
            if (res != 0) {
                Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, itm.gCardSerial, SYNCERROR_PAY, itm.amt);
            } else {
                pagamenti.remove(itm);
            }
        }
        return 0;
    }

    /**
     * begin of a transaction, do a reset
     */
    @Override
    public void resetAll() {
        venduto.clear();
        ricariche.clear();
        pagamenti.clear();
        premi.clear();
        utilities.clear();  //PSH-ENH-20151120-CGA#A
    }

    /**
     * cancel single gift card
     *
     * @param itm the gift card
     * @return 0 if ok, elsewhere the error in nmemo
     */
    public int cancelGiftCard(Itemdata itm) {
        if (itm.gCardTopup) {
            for (Itemdata itmloop : ricariche) {
                if (itmloop.gCardSerial.trim().compareTo(itm.gCardSerial.trim()) != 0)
                    continue;
                if (itmloop.number.trim().compareTo(itm.number.trim()) != 0)
                    continue;
                if (itmloop.price != itm.price)
                    continue;
                int ret = srv_cancelSellTopupGiftCard(itmloop);
                if (ret == 0) {
                    ricariche.remove(itmloop);
                    // during VOID the item is not yet fully decorated cause is regenerated from scratch
                    itm.gCardDsc = itmloop.gCardDsc;
                    itm.utilityEnglishText = itmloop.utilityEnglishText;
                    return 0;
                }
                return ret;
            }
        } else {
            for (Itemdata itmloop : venduto) {
                if (itmloop.gCardSerial.trim().compareTo(itm.gCardSerial.trim()) != 0)
                    continue;
                if (itmloop.number.trim().compareTo(itm.number.trim()) != 0)
                    continue;
                if (itmloop.price != itm.price)
                    continue;
                int ret = srv_cancelSellGiftCard(itmloop);
                if (ret == 0) {
                    venduto.remove(itmloop);
                    // during VOID the item is not yet fully decorated cause is regenerated from scratch
                    itm.gCardDsc = itmloop.gCardDsc;
                    itm.utilityEnglishText = itmloop.utilityEnglishText;
                    return 0;
                }
                return ret;
            }
        }
        return MNEMO_ERROR_BASE + 2;
    }

    /**
     * Cancel the sell to the server
     *
     * @param itm
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_cancelSellGiftCard(Itemdata itm) {
        String ret[] = srvCancelActivation(GIFTCARD, itm.gCardSerial, itm.price, itm.gCardTransaction);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        return 0; // ok
    }

    /**
     * Cancel the topup sell to the server
     *
     * @param itm
     * @return 0 ok, elsewhere the error in nmemo
     */
    private static int srv_cancelSellTopupGiftCard(Itemdata itm) {
        String ret[] = srvCancelTransaction(CREDIT, GIFTCARD, itm.gCardSerial, itm.price, itm.gCardTransaction);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        return 0; // ok
    }

    /**
     * sell (activate) gift card
     *
     * @param itm
     * @return 0 if ok, >0 error code in nmemo
     */

    @Override
    public int activationGiftCard(Itemdata itm, Terminal ctl) {
        int ret;

        ret = srv_sellGiftCard(itm);
        if (ret == 0) {
            venduto.add(itm.copy());
        }
        return ret;
    }


    @Override
    public int reloadGiftCard(Itemdata itm) {
        int ret;
        ret = srv_sellTopupGiftCard(itm);
        if (ret == 0) {
            ricariche.add(itm.copy());
        }
        return ret;
    }


    @Override
    public int reconciliationGiftCard() {
        return 0;
    }

    @Override
    public int confirmTransaction(Itemdata itm) {
        return 0;
    }

    @Override
    public int cancelTransaction(Itemdata itm, String transactionType) {
        return 0;
    }

    @Override
    public int cancelTransaction(Itemdata itm) {
        return 0;
    }

    @Override
    public boolean isGiftCard(Itemdata itm) {
        return ((LEGACY_TENDER_ID + PSH_TENDER_ID).indexOf(itm.gCard) >= 0) && isEnabled();
    }

    @Override
    public String getTenderId() {
        return PSH_TENDER_ID;
    }

    //PSH-ENH-20151120-CGA#A BEG
    public boolean isUtility(Itemdata itm) {
        logger.debug("ENTER isUtility");
        logger.info("flag: " + itm.gCard);
        logger.debug("EXIT return: " + ((itm.gCard == '2') && isPxlEnabled()));

        return (itm.gCard == '2') && isPxlEnabled();
    }

    @Override
    public CustomTender getCustomTender(int tenderId) {
        CustomTender accountFound = null;
        for (CustomTender account : accountList) {
            if (account.getTenderId() == tenderId) {
                accountFound = account;
                break;
            }
        }
        return accountFound;
    }

    @Override
    public int confirmAllGiftCard() {
        return 0;
    }

    static int readDescriptionUtility(String code) {
        logger.debug("ENTER readDescriptionUtility");
        logger.info("item code: " + code.trim());

        Properties prepayXl = new Properties();

        try {
            prepayXl.load(new FileInputStream("conf/prepayXL.properties"));

            String key = "product." + code.trim() + ".name";
            String max = "product." + code.trim() + ".max";

            if (prepayXl.keySet().contains(key)) {
                plu.utilityName = prepayXl.getProperty(key).trim();

                if (prepayXl.keySet().contains(max)) {
                    plu.utilityMaxPrice = Integer.parseInt(prepayXl.getProperty(max).trim());
                }
            } else {
                logger.debug("EXIT readDescriptionUtility - code not found, return error");
                return 7;
            }
        } catch (Exception e) {
            logger.info("Exception: " + e.getMessage());
            return 7;
        }

        logger.debug("EXIT requestParam - return: " + plu.utilityName);

        return 0;
    }

    static ArrayList<Itemdata> getLstUtilities() {
        return utilities;
    }


    public static void printGiftCardPayments() {
        if (printEnabled) {
            for (Itemdata pagamento : pagamenti) {
                printGiftCardPayment(pagamento);
            }
        }
    }

    public static void printGiftCardPayment(Itemdata item) {
        logger.debug("ENTER printGiftCardPayment");

        prtLine.init(' ').type(2);
        prtLine.init(tnd[item.tnd].tx20);
        prtLine.onto(20, tnd[itm.tnd].symbol).upto(40, editMoney(item.tnd, item.amt));
        prtLine.push(item.mark).type(2);
        if (printSerialEnabled) {
            prtLine.init(mask(item.gCardSerial)).type(2);
        }
        prtLine.init(' ').type(2);
        GdRegis.set_trailer();
        prtLine.type(2);
        GdRegis.hdr_print();

        logger.debug("EXIT printGiftCardPayment");
    }

    public static void printUtilities() {
        for (Itemdata utility : utilities) {
            printUtility(utility);
        }
    }

    private static void printUtility(Itemdata item) {
        logger.debug("ENTER printUtility");
        int pos = 0;

        prtLine.init(' ').type(2);
        prtLine.init("####################").type(2);
        prtLine.init(' ').type(2);

        ///////// english section

        String engTxt = item.utilityEnglishText;

        String rowEng[] = engTxt.split("<br />");

        for (int i = 0; i < rowEng.length; i++) {
            if (rowEng[i].contains("<h") || rowEng[i].contains("</h")) {
                String rowBold[] = rowEng[i].split("<h");

                int x = 0;
                if (rowBold[0].length() > 0 && !rowBold[0].contains("</h")) {
                    prtLine.init(rowBold[0]).type(2);
                    x = 1;
                } else if (rowBold[0].length() == 0) {
                    x = 1;
                }

                for (; x < rowBold.length; x++) {
                    if ((pos = rowBold[x].indexOf("</h")) >= 0) {
                        if (rowBold[x].charAt(1) == '>') {
                            engTxt = rowBold[x].substring(2, pos);
                        } else {
                            engTxt = rowBold[x].substring(0, pos);
                        }

                        prtDwide(3, engTxt);

                        if (pos + 5 < rowBold[x].length()) {
                            prtLine.init(rowBold[x].substring(pos + 5)).type(2);
                        }
                    } else {
                        engTxt = rowBold[x].substring(2);
                        prtDwide(3, engTxt);
                    }
                }
            } else if (rowEng[i].contains("<b>")) {
                String rowBold[] = rowEng[i].split("<b>");

                int x = 0;
                if (rowBold[0].length() > 0 && !rowBold[0].contains("</b>")) {
                    prtLine.init(rowBold[0]).type(2);
                    x = 1;
                } else if (rowBold[0].length() == 0) {
                    x = 1;
                }

                for (; x < rowBold.length; x++) {
                    if ((pos = rowBold[x].indexOf("</b>")) >= 0) {
                        engTxt = rowBold[x].substring(0, pos);
                        prtDwide(3, engTxt);

                        if (pos + 5 < rowBold[x].length()) {
                            prtLine.init(rowBold[x].substring(pos + 5)).type(2);
                        }
                    } else {
                        engTxt = rowBold[x].substring(0);
                        prtDwide(3, engTxt);
                    }
                }
            } else {
                prtLine.init(rowEng[i]).type(2);
            }
        }

        ///////// arabic section

        prtLine.init(' ').type(2);
        prtLine.init(' ').type(2);
        String convertText = "";
        String arabTxt = item.utilityArabText;
        String rowArab[] = arabTxt.split("<br />");

        for (int i = 0; i < rowArab.length; i++) {
            if (rowArab[i].contains("<h") || rowArab[i].contains("</h")) {
                String rowBold[] = rowArab[i].split("<h");

                int x = 0;
                if (rowBold[0].length() > 0 && !rowBold[0].contains("</h")) {
                    //convertText = convert(rowBold[0]);
                    prtLine.init(rowBold[0]).type(2);
                    x = 1;
                } else if (rowBold[0].length() == 0) {
                    x = 1;
                }

                for (; x < rowBold.length; x++) {
                    if ((pos = rowBold[x].indexOf("</h")) >= 0) {
                        if (rowBold[x].charAt(1) == '>') {
                            arabTxt = rowBold[x].substring(2, pos);
                        } else {
                            arabTxt = rowBold[x].substring(0, pos);
                        }

                        //convertText = convert(arabTxt);
                        prtDwide(3, arabTxt);

                        if (pos + 5 < rowBold[x].length()) {
                            //convertText = convert(rowBold[x].substring(pos+5));
                            prtLine.init(rowBold[x].substring(pos + 5)).type(2);
                        }
                    } else {
                        arabTxt = rowBold[x].substring(2);
                        //convertText = convert(arabTxt);
                        prtDwide(3, arabTxt);
                    }
                }
            } else if (rowArab[i].contains("<b>")) {
                String rowBold[] = rowArab[i].split("<b>");

                int x = 0;
                if (rowBold[0].length() > 0 && !rowBold[0].contains("</b>")) {
                    //convertText = convert(rowBold[0]);
                    prtLine.init(rowBold[0]).type(2);
                    x = 1;
                } else if (rowBold[0].length() == 0) {
                    x = 1;
                }

                for (; x < rowBold.length; x++) {
                    if ((pos = rowBold[x].indexOf("</b>")) >= 0) {
                        arabTxt = rowBold[x].substring(0, pos);
                        //convertText = convert(arabTxt);

                        prtDwide(3, arabTxt);

                        if (pos + 5 < rowBold[x].length()) {
                            //convertText = convert(rowBold[x].substring(pos+5));

                            prtLine.init(rowBold[x].substring(pos + 5)).type(2);
                        }
                    } else {
                        arabTxt = rowBold[x].substring(0);
                        //convertText = convert(arabTxt);

                        prtDwide(3, arabTxt);
                    }
                }
            } else {
                //convertText = convert(rowArab[i]);

                prtLine.init(rowArab[i]).type(2);
            }
        }

        prtLine.init(' ').type(2);
        prtLine.init("####################").type(2);
        prtLine.init(' ').type(2);

        GdRegis.hdr_print();

        logger.debug("EXIT printUtility");
    }

    static int srvBuyUtility(boolean online) {
        logger.debug("ENTER srvBuyUtility");

        if (itm.qty != 1) {
            // no multiplier
            return MNEMO_ERROR_BASE + 1;
        }

        String ret[] = srvDoPrepayXLTransactionActivation(online);
        logger.info("error message from server: " + ret[0]);

        if (!REPLY_OK_VALUE.equals(ret[0])) {
            logger.debug("EXIT srvBuyUtility - error message to display");

            return MNEMO_ERROR_BASE + 12;
        }

        try {
            itm.utilityTransaction = ret[ret.length - 1].replaceAll("(\\r|\\n)", "").trim();
            itm.utilityEnglishText = ret[1];
            itm.utilityArabText = ret[2];
            itm.utilitySerial = ret[3];
            itm.utilityPin = ret[4];
        } catch (Exception e) {
            logger.info("EXCEPTION: " + e.getMessage());
            logger.debug("EXIT return error message");

            itm.utilityTransaction = lastTransactionID;
            return 97;  //UTILITY FAILURE
        }

        logger.info("description item: " + itm.utilityName);
        logger.info("english text: " + itm.utilityEnglishText);
        logger.info("arabic text: " + itm.utilityArabText);
        logger.info("utility transaction: " + itm.utilityTransaction);

        logger.debug("EXIT srvBuyUtility - return ok");
        return 0; // ok
    }

    private static String[] srvDoPrepayXLTransactionActivation(boolean online) {
        logger.debug("ENTER srvDoPrepayXLTransactionActivation");

        String transactionId = "";
        String message = "";
        String[] reply = {};

        logger.info("item description: " + itm.utilityName);

        try {
            logger.info("call function beginTransaction");
            if (online) {
                // message = ppController.beginTransaction(itm.utilityName, String.valueOf(itm.price), cus.mobile); // AMZ-2017-003#DEL
                message = ppController.beginTransaction(itm.utilityName, String.valueOf(itm.price), cus.getMobile(), ctl.uniqueId); // AMZ-2017-003#ADD
            } else {
                // message = ppController.beginTransaction(itm.utilityName);// AMZ-2017-003#DEL
                message = ppController.beginTransaction(itm.utilityName, ctl.uniqueId);// AMZ-2017-003#ADD
            }

            logger.info("response from server: " + message);

            reply = message.replaceAll("\r\n", "").split(";");
            logger.info("error code: " + reply[0]);
            logger.info("transactionId: " + reply[1]);

            if (Integer.parseInt(reply[0]) <= 0) {
                transactionId = reply[1];
                String additionalInfo = "";
                lastTransactionID = transactionId;
                itm.utilityTransaction = transactionId;
                if (Integer.parseInt(reply[0]) < 0) {
                    // Need more info
                    // Prompt message box to enter additional code
                    additionalInfo = readExtinfo(reply[2]);
                    if (additionalInfo == null) {
                        reply[0] = "999";

                        logger.debug("EXIT srvDoPrepayXLTransactionActivation - user abort");
                        return reply;
                    }
                }

                for (int time = 1; time <= SRV_RETRY; time++) {
                    logger.info("call function processTransaction");
                    if (online) {
                        message = ppController.processTransaction(itm.utilityName, String.valueOf(itm.price), cus.getMobile(), transactionId);
                    } else {
                        message = ppController.processTransaction(itm.utilityName, transactionId);
                    }

                    logger.info("response from server: " + message);
                    message = message + ";" + transactionId;
                    logger.info("response with appended transactionId: " + message);

                    reply = message.split(";");

                    if (REPLY_OK_VALUE.equals(reply[0])) {
                        logger.info("response from server: ok");

                        break;
                    }

                    if (Integer.valueOf(reply[0]) < 100) {
                        logger.info("non-recoverable error such as account out of credit.");

                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.info("EXCEPTION: " + e.getMessage());
        }

        logger.debug("EXIT srvDoPrepayXLTransactionActivation");
        return reply;
    }

    public int cancelBuyUtility(Itemdata itm) {
        logger.debug("ENTER cancelBuyUtility");
        logger.info("item code: " + itm.number.trim());

        if (!isPxlEnabled()) {
            logger.debug("EXIT cancelBuyUtility - philoshopic PXL disabled, return error message");
            return 7;
        }

        try {
            for (Itemdata itmloop : utilities) {
                logger.info("list item" + itmloop.number.trim());

                if (itmloop.number.trim().equals(itm.number.trim())) {
                    logger.info("item found");

                    int ret = srvCancelBuyUtility(itmloop);
                    logger.info("response from server: " + ret);

                    if (ret == 0) {
                        logger.info("remove item from the list");
                        utilities.remove(itmloop);
                    }

                    logger.debug("EXIT cancelBuyUtility - return: " + ret);
                    return ret;
                }
            }
        } catch (Exception e) {
            logger.info("cancelBuyUtility - EXCEPTION: " + e.getMessage());
            return 97;
        }

        logger.debug("EXIT cancelBuyUtility - return: 0");
        return 0;
    }

    private static int srvCancelBuyUtility(Itemdata itm) {
        logger.debug("ENTER srvCancelBuyUtility");
        logger.info("transactionId: " + itm.utilityTransaction);

        String ret[] = srvCancelPrepayXLTransactionActivation(itm.utilityTransaction);
        logger.info("error code from server response: " + ret[0]);

        if (!REPLY_OK_VALUE.equals(ret[0])) {
            logger.debug("EXIT srvCancelBuyUtility - error message to display");
            return MNEMO_ERROR_BASE + 12;
        }

        logger.debug("EXIT srvCancelBuyUtility - return ok");
        return 0; // ok
    }

    private static String[] srvCancelPrepayXLTransactionActivation(String transactionId) {
        logger.debug("ENTER srvCancelPrepayXLTransactionActivation");
        logger.info("transactionId: " + transactionId);
        String message = "";
        String reply[] = {"999"};

        try {
            for (int time = 1; time <= SRV_RETRY; time++) {
                message = ppController.cancelTransaction(transactionId);
                logger.info("response from server: " + message);

                reply = message.split(";");

                if (REPLY_OK_VALUE.equals(reply[0])) {
                    break;
                }
                // ERROR HANDLING
                logger.info("Error for transaction : " + transactionId);
                if (Integer.valueOf(reply[0]) < 100) {
                    break; // non-recoverable error such as account out of credit.
                }
            }
        } catch (Exception e) {
            logger.info("srvCancelPrepayXLTransactionActivation - EXCEPTION: " + e.getMessage());
        }

        logger.debug("EXIT srvCancelPrepayXLTransactionActivation");
        return reply;
    }

    static int cancelAllUtilities() {
        logger.debug("ENTER cancelAllUtilities");

        try {
            ArrayList<Itemdata> utilitiesLoop = new ArrayList<Itemdata>(utilities);
            for (Itemdata itm : utilitiesLoop) {
                logger.info("list item: " + itm.number);

                int res = srvCancelBuyUtility(itm);
                logger.info("response from server: " + res);

                if (res != 0) {
                    Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, itm.utilityTransaction, SYNCERROR_BUY, itm.price);
                } else {
                    logger.info("remove item from list");
                    utilities.remove(itm);
                }
            }
        } catch (Exception e) {
            logger.info("cancelAllUtilities - EXCEPTION: " + e.getMessage());
            return 97;
        }

        logger.debug("EXIT cancelAllUtilities - return ok");
        return 0;
    }
    //PSH-ENH-20151120-CGA#A END

    /**
     * Ask the customer info to server
     *
     * @param cus
     * @return 0 ok, elsewhere the error in nmemo
     */

    private static int srv_customerCheck(Customer cus) {
        logger.debug("ENTER srv_customerCheck");

        String[] reply = srvGetAccountBalance(LOYALTY, cus.getNumber());
        logger.info("reply[0]: " + reply[0]);
        if (!REPLY_OK_VALUE.equals(reply[0])) {
            logger.debug("EXIT srv_customerCheck_1");
            return MNEMO_ERROR_BASE + 0;
        }

        logger.info("reply[1]: " + reply[1]);
        //cus.pnt = Integer.parseInt(reply[1]);
        cus.setPnt((int) Double.parseDouble(reply[1]));
        cus.setName(reply[3]);
        // AMZ-2017-003-006#BEG
        // l'elenco di articoli da vendere automaticamente e' in fondo al messaggio
        if (reply.length > 4) {
            cus.setSelfSellEANList(reply[4]);
        } else {
            cus.setSelfSellEANList("");
        }
        logger.debug("EXIT srv_customerCheck_2");
        return 0; // ok
    }
    // AMZ-2017-003-006#END

    private static int dmy_customerCheck(Customer cus) {
        cus.setPnt((int) Double.parseDouble("100.00"));
        cus.setName("Customer Name");
        cus.setSelfSellEANList("");
        return 0;
    }

    /**
     * update the customer. does not return an error, in this case write off a datacollect
     *
     * @param cus
     * @param tra
     */
    static void customerUpdate(Customer cus, Transact tra) {
        if (!customerUpdateEnabled) {
            logger.info("EXIT customerUpdate, Update disabled");
            return;
        }

        int ret = srv_customerUpdate(cus, tra);
        if (ret != 0) {
            if (cus.getNumber() != null) { // AMZ-2017-003-002#ADD
                Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, cus.getNumber(), SYNCERROR_POINTS, tra.pnt - cus.getPnt());
                // AMZ-2017-003-002#BEG
            } else {
                Itmdc.IDC_write('Z', sc_value(tra.spf1), 0, "0000000000000", SYNCERROR_POINTS, tra.pnt - cus.getPnt());
            }
            // AMZ-2017-003-002#END
        }
    }

    /**
     * sends the loyalty point to server
     *
     * @param cus
     * @param tra
     * @return 0 if ok, index in mnemo if errors
     */
    private static int srv_customerUpdate(Customer cus, Transact tra) {
        String ret[] = srvDoTransaction(CREDIT, LOYALTY, cus.getNumber(), tra.pnt + tra.prpnt - cus.getPnt(), tra.amt, tra.gctnd);

        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        try {
            if (Integer.parseInt(ret[3]) == 99 && ret[4].length() > 0) {
                printText(ret[4]);
            } else {
                if (messages.get(Integer.parseInt(ret[3])).length() > 0) {
                    prtLine.init(messages.get(Integer.parseInt(ret[3]))).book(3);
                }
            }
        } catch (Exception e) {
            System.err.println("PSHP:Error parsing response. Setting defaults.");
        }
        return 0; // ok
    }

    /**
     * sends the loyalty point to server
     *
     * @param itm
     * @return 0 if ok, index in mnemo if errors
     */
    private static int srv_pointsRedemption(Itemdata itm) {
        String ret[] = srvDoTransaction(DEBIT, LOYALTY, cus.getNumber(), -itm.prpnt);
        if (!REPLY_OK_VALUE.equals(ret[0])) {
            return MNEMO_ERROR_BASE + 0;
        }
        try {
            itm.redemptionTransaction = ret[2];
            if (Integer.parseInt(ret[3]) == 99 && ret[4].length() > 0) {
                itm.utilityEnglishText = ret[4];
            } else {
                itm.redemptionDsc = messages.get(Integer.parseInt(ret[3]));
            }
        } catch (Exception e) {
            System.err.println("PSHP:Error parsing response. Setting defaults.");
            itm.redemptionTransaction = lastTransactionID;
            itm.redemptionDsc = "";
        }
        return 0; // ok
    }

    /**
     * peek the customer information from server
     *
     * @param cus the customer information
     * @return 0 if ok, index in mnemo if errors
     */
    static int customerCheck(Customer cus) {
        int ret = 0;
        if (SscoPosManager.getInstance().isUsed()) {
            SscoPosManager.getInstance().forceProcessor(ProcessorConstants.LOYALTY_CARD);
            ((LoyaltyRequestProcessor) (SscoPosManager.getInstance().getProcessor())).setLoyaltyInfo(new SscoCustomer(cus.getNumber(), "", "", cus.getPnt()));

            ret = SscoPosManager.getInstance().isTestEnvironment() ? dmy_customerCheck(cus) : srv_customerCheck(cus);
        } else {
            ret = srv_customerCheck(cus);
        }
        if (ret != 0) {
            return ret;
        }
        // AMZ-2017-003-006#BEG -- vendita automatica
        if (cus.getSelfSellEANList().length() > 0) {
            tra.number = cus.getNumber(); //CGA-2017-003-006#A
            String eans[] = cus.getSelfSellEANList().split(",");
            int sts;

            if (!tra.isActive())
                GdRegis.set_tra_top();

            int nxt = event.nxt;
            event.spc = input.msk = 0;

            for (String ean : eans) {
                if (ean.startsWith(MASK_START_PV)) {
                    Long promoCode = Long.parseLong(ean.substring(2, ean.indexOf("=")));
                    Long promoValue = Long.parseLong(ean.substring(ean.indexOf("=") + 1));
                    logger.debug("There is a Promotion to apply PV code = " + promoCode + " , Value= " + promoValue);
                    Promo.setPromovar(promoCode, promoValue);
                } else {
                    input.prompt = "";
                    input.reset(ean.trim());

                    sts = group[5].action2(0); // GdPrice.action2 = plu number
                    if (sts != 0) {
                        continue;
                        //break;
                    }
                }
            }
            event.nxt = nxt;
        }
        // AMZ-2017-003-006#END -- vendita automatica

        cus.setMobile("");
        //cus.name = "";
        cus.setAdrs("");
        cus.setCity("");
        cus.setNam2("");
        cus.setDtbl("");
        cus.setFiscalId(null);
        return 0;
    }

    /**
     * checks if a keyed/scanned code is a serial number of a gift card in that case sets a topup item preselector flag
     * and change the EAN number. Shows the card amount.
     *
     * @param inp
     * @return 0 if ok or no gift card, -1 if silently abort operation, else the mnemo error code
     */
    static int checkDirectCodeTopup(ConIo inp, Itemdata plu) {
        if (input.pb.length() >= 8) {
            String code = inp.pb;
            if (code.substring(0, 8).compareTo(par_prefix) == 0) {
                // is a topup item

                // discharge the keyboard input
                if (!par_kbdenabled) {
                    if (inp.key == 0x0d) {
                        return MNEMO_ERROR_BASE + 4;
                    }
                }

                // just change the EAN of the just scanned plu to our mapped EAN
                // that EAN must be a Gift Card PLU correctly configured
                input.pb = par_prefixToEan;
                input.num = par_prefixToEan.length();
                plu.gCardTopup = true; // preselector for topup
                plu.gCardSerial = code;

                int res = srv_totalGiftCardBalance(itm);
                if (res > 0) {
                    return res;
                }
                res = panel.clearLink(Mnemo.getDiag(MNEMO_DIAGS_BASE + 2).trim() + editMoney(0, itm.amt), 0x83);
                if (res == 1) { // 1 = ESC
                    return -1;
                }
            }
        }
        return 0;
    }

    //
    //
    // SERVER CALLS
    //
    //

    private static String moneyForServer(String type, long money) {
        BigDecimal bd = new BigDecimal(money);
        String ret = bd.toString();

        if (!type.equals(LOYALTY)) {
            ret = bd.movePointLeft(tnd[0].dec).toString();
            ret.replace(",", ".");
        }
        return ret;
    }

    private static String[] srvGetAccountBalance(String accountType, String accountNo) {
        logger.debug("PSHP:srvGetAccountBalance : " + accountType + ", " + accountNo);
        String reply[] = {"999"};
        for (int time = 1; time <= SRV_RETRY; time++) {
            String message = controller.getAccountBalance(accountType, accountNo, null);
            logger.debug("PSHP:Server reply : " + message);
            reply = message.split(";");
            if (REPLY_OK_VALUE.equals(reply[0])) {
                return reply;
            }
        }

        return reply;
    }

    private static String[] srvDoActivation(String accountType, String accountNo, long value) {
        return srvDoTransactionActivation("", accountType, accountNo, value);
    }

    private static String[] srvDoTransaction(String transactionType, String accountType, String accountNo, long value, long amt, long amt2) {
        return srvDoTransactionActivation(transactionType, accountType, accountNo, value, amt, amt2, true);
    }

    private static String[] srvDoTransaction(String transactionType, String accountType, String accountNo, long value) {
        return srvDoTransactionActivation(transactionType, accountType, accountNo, value);
    }

    private static String[] srvDoTransactionActivation(String transactionType, String accountType, String accountNo,
                                                       long value) {
        return srvDoTransactionActivation(transactionType, accountType, accountNo, value, -1, -1, false);
    }

    private static String[] srvDoTransactionActivation(String transactionType, String accountType, String accountNo,
                                                       long value, long amt, long amt2, boolean silent) {

        if (accountNo != null) {
            logger.debug("PSHP:srvDoTransactionActivation : " + transactionType + ", " + accountType + ", "
                    + mask(accountNo) + ", " + value + ", " + amt + ", " + amt2 + ", uniqueTransactionId: " + ctl.uniqueId);
        } else {
            logger.debug("PSHP:srvDoTransactionActivation : " + transactionType + ", " + accountType + ", "
                    + "no customer" + ", " + value + ", " + amt + ", " + amt2 + ", uniqueTransactionId: " + ctl.uniqueId);
        }

        String message;

        //PSH-ENH-20151120-CGA#A BEG
        boolean tenderExist = false;
        for (CustomTender pshTender : accountList) {
            if (pshTender.getAccountType().trim().toLowerCase().equals(accountType.trim().toLowerCase())) {
                tenderExist = true;
                break;
            }
        }

        if (amt < 0 && amt2 < 0 && transactionType.equals("DEBIT") && tenderExist) {

            String[] replyAmt = srvGetAccountBalance(accountType, itm.gCardSerial);
            long amtGift = 0;
            if (!REPLY_OK_VALUE.equals(replyAmt[0])) {
                return replyAmt;
            } else {
                if (replyAmt[1].equals("null")) {
                    replyAmt[0] = MNEMO_ERROR_BASE + "";
                    return replyAmt;
                }
                amtGift = Long.parseLong(replyAmt[1].replace(",", "").replace(".", "").trim());
            }

            if (amtGift < itm.amt) {
                int sts = 0;
                if (par_checkAmtGift.equals("01")) {
                    if (amtGift == 0) {
                        replyAmt[0] = GIFT_EMPTY + "";
                        return replyAmt;
                    } else {
                        sts = panel.clearLink(Mnemo.getInfo(102), 3);
                    }

                    if (sts == 1) {  //CLEAR
                        replyAmt[0] = MNEMO_ERROR_BASE + "";
                        return replyAmt;
                    }
                }

                if (par_checkAmtGift.equals("02")
                        || sts == 2) { //ENTER
                    value = amtGift;
                    itm.pos = itm.amt = value;
                }
            }
        }
        //PSH-ENH-20151120-CGA#A END

        String valueField = moneyForServer(accountType, value);
        if (amt >= 0) {
            valueField = new StringBuilder(valueField).append(";").append(moneyForServer(accountType, amt)).append(";").append(moneyForServer(accountType, amt2)).toString();
        }
        if (transactionType.trim().length() > 0) {
            // message = controller.beginTransaction(transactionType, accountType, accountNo, valueField); // AMZ-2017-003#DEL
            message = controller.beginTransaction(transactionType, accountType, accountNo, valueField, ctl.uniqueId);    // AMZ-2017-003#ADD
        } else {
            // message = controller.beginActivation(accountType, accountNo, valueField); // AMZ-2017-003#DEL
            logger.info("Call beginActivation. accountType: " + accountType + " accountNo: " + accountNo + " valueField: " + valueField + " uniqueTransactionId: " + ctl.uniqueId);
            message = controller.beginActivation(accountType, accountNo, valueField, ctl.uniqueId); // AMZ-2017-003#ADD
        }
        logger.debug("PSHP:Server reply : " + message);
        String reply[] = message.split(";");

        try {
            if (Integer.parseInt(reply[0]) <= 0) {
                String transactionId = reply[1];
                String additionalInfo = "";
                lastTransactionID = transactionId;
                if (Integer.parseInt(reply[0]) < 0) {
                    // Need more info
                    // Prompt message box to enter additional code
                    additionalInfo = readExtinfo(reply[2]);
                    if (additionalInfo == null) {
                        reply[0] = "999"; // user abort
                        return reply;
                    }
                }
                logger.debug("PSHP:Process " + transactionType + ", " + accountType + ", " + mask(accountNo) + ", "
                        + valueField + ", " + transactionId + ", " + additionalInfo);
                for (int time = 1; time <= SRV_RETRY; time++) {
                    if (transactionType.trim().length() > 0) {
                        message = controller.processTransaction(transactionType, accountType, accountNo,
                                valueField, transactionId, additionalInfo);
                    } else {
                        message = controller.processActivation(accountType, accountNo, valueField, transactionId,
                                additionalInfo);
                    }
                    logger.debug("PSHP:Server reply : " + message);
                    reply = message.split(";");

                    if (REPLY_OK_VALUE.equals(reply[0])) {
                        break;
                    }
                    // ERROR HANDLING
                    logger.debug("PSHP:Error for transaction : " + transactionId);
                    if (Integer.valueOf(reply[0]) < 100) {
                        break; // non-recoverable error such as account out of credit.
                    }
                }
            } else {
                logger.debug("PSHP:Could not get transactionID. Exit.");
            }
        } catch (NumberFormatException e) {
            System.err.println("PSHP:Error parsing response. Exit.");
        }

        if (reply[0].length() > 0 && Integer.parseInt(reply[0]) > 0) {
            if (!silent) {
                dspLine.init(reply[1]).show(1);
            }
        }



        return reply;
    }

    private static String[] srvCancelActivation(String accountType, String accountNo, long value,
                                                String transactionId) {
        return srvCancelTransactionActivation("", accountType, accountNo, value, transactionId);
    }

    private static String[] srvCancelTransaction(String transactionType, String accountType, String accountNo,
                                                 long value, String transactionId) {
        return srvCancelTransactionActivation(transactionType, accountType, accountNo, value, transactionId);
    }

    private static String mask(String serial) {
        if (serial == null || serial.length() == 0) {
            return "";
        }
        return serial.substring(0, serial.length() - 2) + "**";
    }

    private static String[] srvCancelTransactionActivation(String transactionType, String accountType, String accountNo,
                                                           long value, String transactionId) {
        logger.debug("PSHP:srvCancelTransactionActivation : " + transactionType + ", " + accountType + ", "
                + accountNo + ", " + value + ", " + transactionId);
        String message;
        String reply[] = {"999"};
        logger.debug("PSHP:Process " + transactionType + ", " + accountType + ", " + mask(accountNo) + ", "
                + moneyForServer(accountType, value) + ", " + transactionId);
        for (int time = 1; time <= SRV_RETRY; time++) {
            if (transactionType.trim().length() > 0) {
                message = controller.cancelTransaction(transactionType, accountType, accountNo, moneyForServer(accountType, value),
                        transactionId);
            } else {
                message = controller.cancelActivation(accountType, accountNo, moneyForServer(accountType, value), transactionId);
            }
            logger.debug("PSHP:Server reply : " + message);
            reply = message.split(";");

            if (REPLY_OK_VALUE.equals(reply[0])) {
                break;
            }
            // ERROR HANDLING
            logger.debug("PSHP:Error for transaction : " + transactionId);
            if (Integer.valueOf(reply[0]) < 100) {
                break; // non-recoverable error such as account out of credit.
            }
        }

        return reply;
    }

    public static void loadMessages() {
        int rec = 0;

        DatIo lPHM = new DatIo("PHM", 4, 46);
        lPHM.open(null, "P_REG" + lPHM.id + ".DAT", 1);
        while (lPHM.read(++rec) > 0) {
            int ind = lPHM.scanNum(3);
            String txt = lPHM.pb.substring(lPHM.scan(':').index);
            messages.put(ind, txt);
        }
        lPHM.close();
    }

    private static String buildDataCollect() {
        int nbr = 0, rec;
        StringBuffer dataCollect = new StringBuffer();

        for (rec = 0; lTRA.read(++rec) > 0; ) {
            char type = lTRA.pb.charAt(32);
            if (type == 'C') /* skip empl/cust % template */
                if (lTRA.pb.charAt(35) == '9')
                    continue;
            if (type == 'u') {
                String ean = lTRA.pb.substring(43, 59);
                int i = WinUpb.getInstance().findUpbTra(ean, false);
                if (i >= 0 && tra.itemsVsUPB.get(i).isConfirmed()) {
                    lTRA.pb = lTRA.pb.substring(0, lTRA.pb.length() - 1) + "0";
                }
            }
            dataCollect.append(lTRA.scan(28));
            dataCollect.append(editNum(++nbr, 3));
            dataCollect.append(lTRA.skip(3).scan(3));
            dataCollect.append(editNum(tra.mode, 1));
            dataCollect.append(lTRA.pb.substring(++lTRA.index));
            dataCollect.append("\n");
        }
        return dataCollect.toString();
    }

    //DMA-TLOG_UPLOADING#A BEG
    public static void sendDataCollect() {
        logger.info("ENTER sendDataCollect()");

        if (!tlogSyncingEnabled) {
            logger.info("EXIT sendDataCollect(), Syncing disabled");
            return;
        }

        String dataCollect = buildDataCollect();

        String tlogResult = " ";
        int retry = 3;
        if (tra.mode == M_GROSS) {
            //while (!tlogResult.equals(TLOG_OK) && retry > 0) {
            while (tlogResult.indexOf(TLOG_OK) < 0 && retry > 0) {
                logger.info("send datacollect : logSTORE(" + ctl.uniqueId + ", dc_length: " + dataCollect.length() + " ) ");
                logger.debug("send datacollect : logSTORE(" + ctl.uniqueId + ",\n" + dataCollect + " ) ");
                tlogResult = logController.logSTORE(ctl.uniqueId, dataCollect);
                retry--;
                logger.info("tlogResult : >" + tlogResult + "<");
                try {
                    logger.info("wait 50 msec");
                    Thread.sleep(50);
                } catch (Exception e) {
                    logger.info("exception e: " + e.getMessage());
                }
            }
        }
        logger.info("EXIT sendDataCollect()");
    }
    //DMA-TLOG_UPLOADING#A END
}
//INTEGRATION-PHILOSHOPIC-CGA#A END
