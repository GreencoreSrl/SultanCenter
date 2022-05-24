package com.ncr.eft;

import com.ncr.*;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import static com.ncr.Struc.prtLine;

public class EftPluginManager {
    private static final Logger logger = Logger.getLogger(EftPluginManager.class);

    private static EftPluginManager instance = null;
    private final static String PROP_FILENAME = "conf/eftPlugins.properties";
    private final static Properties props = new Properties();
    private final static String extTenderPropertiesFilePath = "conf/eftTenders.properties";
    private final static HashMap<String, Integer> extTenderMap = new HashMap<String, Integer>();
    private Map<String, EftPlugin> plugins = new HashMap<String, EftPlugin>();
    public final static String EFT_TENDER_IDS = "JKNOMQ";

    public static EftPluginManager getInstance() {
        if (instance == null)
            instance = new EftPluginManager();

        return instance;
    }

    private EftPluginManager() {
        loadEftTerminalProperties();
    }

    private void loadEftTerminalProperties() {
        String packageName = EftPluginManager.class.getPackage().getName();

        try {
            props.load(new FileInputStream(PROP_FILENAME));

            for (Object key : props.keySet()) {
                String className = (String) key;
                if (props.getProperty(className, "false").equals("true")) {
                    Class<EftPlugin> eftClass = (Class<EftPlugin>) Class.forName(packageName + "." + className);
                    EftPlugin eftPlugin = eftClass.newInstance();
                    plugins.put(eftPlugin.getTenderId(), eftPlugin);
                    logger.info("Plugin " + className + " enabled");
                }
            }
        } catch (Exception e) {
            logger.error("Error creating plugins", e);
        }
    }

    public boolean isPluginEnabled(String pluginName) {
        return plugins.containsKey(pluginName);
    }

    public EftPlugin getPlugin(String pluginName) {
        return plugins.get(pluginName);
    }

    public void printVouchers(String type) {
        for (EftPlugin eftPlugin : plugins.values()) {
            eftPlugin.printVouchers(type);
            if (type == EftPlugin.NEW_RECEIPT_TYPE) {
                eftPlugin.resetVouchers();
            }
        }
    }

    public void writeInfo(String tenderId, Transact tra, Itemdata itm) {
        if (ECommerce.isResumeInstashop()) return;
        EftPlugin eftPlugin = getPlugin(tenderId);
        if (eftPlugin != null) {
            logger.info("Hanndling idc and receipt for plugin " + eftPlugin.getClass().getSimpleName());
            for (int index = 0; index < eftPlugin.FLAGS_SIZE; index++) {
                if (eftPlugin.isIdcEnabled(index)) {
                    writeIdc(eftPlugin, index, tra, itm);
                }
                if (eftPlugin.isReceiptEnabled(index)) {
                    writeReceipt(eftPlugin, index);
                }
            }
        }
    }

    private void writeReceipt(EftPlugin eftPlugin, int index) {
        logger.info("Writing receipt for plugin" + eftPlugin.getClass().getSimpleName() + " index: " + index);
        switch (index) {
            case EftPlugin.CARD_NUM_INDEX:
                prtLine.init(Mnemo.getText(31)).onto(20, eftPlugin.getCardNumber()).book(3);
                break;
            case EftPlugin.TERMIINAL_ID_INDEX:
                prtLine.init(Mnemo.getText(108)).onto(20, eftPlugin.getTerminalId()).book(3);
                break;
            case EftPlugin.AUTH_CODE_INDEX:
                prtLine.init(Mnemo.getText(80)).onto(20, eftPlugin.getAuthorizationCode()).book(3);
                break;
            case EftPlugin.TRANSACTION_ID_INDEX:
                break;
            default:
                break;
        }
    }

    private void writeIdc(EftPlugin eftPlugin, int index, Transact tra, Itemdata itm) {
        logger.info("Writing idc for plugin" + eftPlugin.getClass().getSimpleName() + " index: " + index);
        switch (index) {
            case EftPlugin.CARD_NUM_INDEX:
                Itmdc.IDC_write('N', tra.tnd, 1, eftPlugin.getCardNumber(), itm.cnt, itm.pos);
                break;
            case EftPlugin.TERMIINAL_ID_INDEX:
                Itmdc.IDC_write('N', tra.tnd, 4, eftPlugin.getTerminalId(), itm.cnt, itm.pos);
                break;
            case EftPlugin.AUTH_CODE_INDEX:
                break;
            case EftPlugin.TRANSACTION_ID_INDEX:
                Itmdc.IDC_write('N', tra.tnd, 5, eftPlugin.getReceiptNumber(), itm.cnt, itm.pos);
                break;
            default:
                break;
        }
    }

    public void loadPluginParameters(String key, int line, String txt) {
        logger.info("Loading params: " + key + " line: " + line + " record: " + txt);
        EftPlugin eftPlugin = getPlugin("" + key.charAt(3));
        if (eftPlugin != null) {
            eftPlugin.loadEftTerminalParams(line, txt);
        }
    }

    public int doTransaction(String tenderId, Itemdata itm, Terminal ctl, LinIo oplLine) {
        int sts = 0;

        if (EFT_TENDER_IDS.contains(tenderId)) {
            EftPlugin eftPlugin = getPlugin(tenderId);
            if (eftPlugin != null) {
                logger.info("Handling transaction for plugin " + eftPlugin.getClass().getSimpleName() + " amount: " + itm.pos);

                oplLine.init(Mnemo.getInfo(74)).show(2);
                sts = eftPlugin.doTransactionWithStatusCheck(itm, String.valueOf(ctl.tran), oplLine);

                if (sts == EftPlugin.ERR_OK) {
                    itm.tnd = getEftTender(eftPlugin.getCardType(), itm.tnd);
                    //itm.number = eftPlugin.getCardNumber();
                }
            } else {
                logger.warn("Plugin not enabled for tender " + tenderId);
            }
        }

        logger.info("Returning " + sts + " tender: " + itm.tnd + " number: " + itm.number);
        return sts;
    }

    public void readExtendedTender() {
        logger.debug("ENTER");
        Properties extTenderParam = new Properties();
        try {
            extTenderParam.load(new FileInputStream(extTenderPropertiesFilePath));
            for (String s : extTenderParam.stringPropertyNames()) {
                String name = extTenderParam.getProperty(s);
                int tnd = Integer.parseInt(s.split("\\.")[1]);
                if (name.contains(";")) {
                    String[] tokens = name.split(";");
                    for (String token : tokens) {
                        extTenderMap.put(token, tnd);
                        logger.info("param " + s + " : tender = " + tnd + ", value = " + token);
                    }
                } else {
                    extTenderMap.put(name, tnd);
                    logger.info("param " + s + " : tender = " + tnd + ", value = " + name);
                }
            }
            logger.info("test getExtTenderNumber(\"AMZ-TEST-23\") = " + getEftTender("AMZ-TEST-23", 100));
            logger.info("test getExtTenderNumber(\"xxx\") = " + getEftTender("xxx", 100));
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
        logger.debug("EXIT");
    }

    public int getEftTender(String tenderName, int defaultValue) {
        logger.debug("ENTER");
        Integer tnd;
        if (tenderName != null) {
            tnd = extTenderMap.get(tenderName);
            if (tnd == null) {
                logger.warn("Cannot find tender name = " + tenderName);
                logger.warn("Returning default tender = " + defaultValue);
                tnd = defaultValue;
            }
        } else {
            logger.warn("Cannot find tender name = null");
            logger.warn("Returning default tender = " + defaultValue);
            tnd = defaultValue;
        }
        logger.debug("EXIT tender returned = " + tnd);
        return tnd;
    }
}