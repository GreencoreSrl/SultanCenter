package com.ncr;

import com.ncr.ecommerce.data.TenderProperties;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class ECommerce extends Action {
    private static final Logger logger = Logger.getLogger(ECommerce.class);
    
    private static boolean isAutomaticAmazonItem = false;
    private static boolean isAutomaticVoidAmazonItem = false;
    private static boolean alreadyAmzCommCalc = false;
    private static int amtItem = 0;
    private static final String AMAZON_FILENAME = "conf/charge.properties";
    private static final Properties prop = new Properties();
    private static final Map<Integer, TenderProperties> tenderProperties = new HashMap<Integer, TenderProperties>();
    private static final HashMap<Integer, Integer> amtMaxMap = new HashMap<Integer, Integer>();
    private static final HashMap<Integer, String> instashopChoiceMap = new HashMap<Integer, String>();
    private static final HashMap<Integer, String> ddqCodeMap = new HashMap<Integer, String>();
    private static final TreeMap<Integer, String> cardTypeMap = new TreeMap<Integer, String>();
    private static HashMap<Integer, Integer> automaticTndOnline = new HashMap<Integer, Integer>();
    private static HashMap<Integer, String> secondCopyDelivery = new HashMap<Integer, String>();
    private static HashMap<Integer, String> secondCopyDeliveryEnable = new HashMap<Integer, String>();   //qui second copy
    private static HashMap<Integer, Integer> automaticTndCard = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> automaticTndCash = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Vector> choicePaymentMap = new HashMap<Integer, Vector>();
    private static String instashopChoice = "0";
    private static String instashopChoiceType = "";
    private static Vector vetKeyChoices = new Vector();
    private static long eanAutomaticItm = 0;
    private static int tndInstashop = 0;
    private static String account = "";
    private static String transactionResumed = "";
    private static boolean isFinalizeInstashop = false;
    private static boolean isResumeInstashop = false;
    private static int cardTypeTnd = 0;
    private static String cardTypeDesc = "";
    private static boolean isInstanshopResume = false;
    private static int numberTraResume = 0;
    private static String eanItemComm = "";
    private static long amountInstashop = 0;
    private static int tenderInstashopUsed = 0;
    private static String accountInstashop = "";
    private static final int INPUT_TRANS_NUM = 0;
    private static final int PAYMENT = 1;
    
    public static void loadAMZNfile() {
        logger.debug("ENTER loadAMZNfile");

        try {
            try {
                prop.load(new FileInputStream(AMAZON_FILENAME));

                for (Object element : prop.keySet()) {
                    String key = element.toString();

                    if (key.startsWith("commission")) {
                        int tnd = Integer.parseInt(key.split("\\.")[1]);
                        String[] tokens = prop.getProperty(key, "100,$,0").split(",");
                        tenderProperties.put(tnd, new TenderProperties(tokens[0], tokens[1].charAt(0), Integer.parseInt(tokens[2])));
                    } else if (key.startsWith("choice")) {  //INSTASHOP-SELL-CGA#A BEG
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        vetKeyChoices.add(key);
                        logger.info("key " + key + " : tender = " + tnd);
                    } else if (key.startsWith("tnd.S.")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        int value = Integer.parseInt(prop.getProperty(key));
                        automaticTndCash.put(tnd, value);
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + value);
                    } else if (key.startsWith("tnd.SC.")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        int value = Integer.parseInt(prop.getProperty(key));
                        automaticTndCard.put(tnd, value);
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + value);
                    } else if (key.startsWith("tnd.F.")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        int value = Integer.parseInt(prop.getProperty(key));
                        automaticTndOnline.put(tnd, value);
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + value);
                    } else if (key.startsWith("secondcopy.delivery")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        secondCopyDelivery.put(tnd, prop.getProperty(key));
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + prop.getProperty(key));
                    } else if (key.startsWith("secondcopy.enable")) {    //qui second copy
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        secondCopyDeliveryEnable.put(tnd, prop.getProperty(key));
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + prop.getProperty(key));
                    } else if (key.startsWith("amt.max")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        int value = Integer.parseInt(prop.getProperty(key));
                        amtMaxMap.put(tnd, value);
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + value);
                    } else if (key.startsWith("ddq.code")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        String value = prop.getProperty(key);
                        ddqCodeMap.put(tnd, value);
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + value);
                    } else if (key.startsWith("card.type")) {
                        int tnd = Integer.parseInt(key.split("\\.")[2]);
                        String value = prop.getProperty(key);
                        cardTypeMap.put(tnd, value);
                        logger.info("key " + key + " : tender = " + tnd + ", value = " + value);
                    }
                    //INSTASHOP-SELL-CGA#A END
                }
            } catch(Exception e){
                throw new Exception("missing or malformed parameter in file " + AMAZON_FILENAME);
            }
        } catch (Exception e){
            logger.error("EXCEPTION: " + e.getMessage());
        }

        orderListChoices();  //INSTASHOP-SELL-CGA#A
        logger.debug("EXIT loadAMZNfile");
    }

    public int action0(int spec) {
        logger.info("ENTER amazon 0 - spec: " + spec);

        switch (spec) {
            case INPUT_TRANS_NUM:
                dspLine.init(Mnemo.getMenu(100));

                isFinalizeInstashop = true;
                isResumeInstashop = true;
                break;
            case PAYMENT:
                dspLine.init(' ');

                isFinalizeInstashop = false;
                isResumeInstashop = false;
                break;
        }

        logger.info("EXIT amazon0, return 0");
        return 0;
    }

    public static boolean isFinalizeInstashop() {
        return isFinalizeInstashop;
    }

    public static void setIsFinalizeInstashop(boolean isFinalizeInstashop) {
        ECommerce.isFinalizeInstashop = isFinalizeInstashop;
    }
    public static boolean isResumeInstashop() {
        return isResumeInstashop;
    }

    public static void setIsResumeInstashop(boolean isResumeInstashop) {
        ECommerce.isResumeInstashop = isResumeInstashop;
    }

    public int action1(int spec) {
        logger.info("ENTER amazon 1");

        int sts = 0;

        if ((sts = sc_checks(2, 6)) > 0)
            return sts;

        prtLine.init(' ').type(2);
        prtLine.init("####################").type(2);
        prtLine.init(' ').type(2);

        prtLine.init(Mnemo.getMenu(107)).type(2);
        prtLine.init(' ').type(2);

        try {
            File instanshopFile = new File("data//transactionsInstashop.txt");

            if (!instanshopFile.exists()) {
                prtLine.init("NO INSTASHOP TRANS. TO FINALIZE").type(2);
            } else {
                BufferedReader br = new BufferedReader(new FileReader(instanshopFile));
                String line = "";

                while((line = br.readLine()) != null) {
                    String numberTrans = line.split("_")[0].trim();
                    logger.info("numberTrans: " + numberTrans);

                    String tenderUsed = line.split("_")[3].trim();
                    logger.info("tenderUsed: " + tenderUsed);

                    String amountInstashop = line.split("_")[4].trim();
                    logger.info("amountInstashop: " + amountInstashop);

                    String operatorInstashop = line.split("_")[6].trim();
                    logger.info("operatorInstashop: " + operatorInstashop);

                    prtLine.init("OPERATOR NUMBER: " + operatorInstashop).type(2);
                    prtLine.init("TRANSACTION NUMBER: " + numberTrans).type(2);
                    prtLine.init("TENDER NUMBER: " + tnd[Integer.parseInt(tenderUsed)].tx20.trim() + " (" + tenderUsed + ")").type(2);
                    prtLine.init("TRANSACTION AMOUNT: " + editMoney(0, Long.parseLong(amountInstashop))).type(2);

                    prtLine.init(' ').type(2);
                    prtLine.init(' ').type(2);
                }

                br.close();
            }
        } catch (Exception e) {

        }

        prtLine.init(' ').type(2);
        prtLine.init("####################").type(2);
        prtLine.init(' ').type(2);

        GdRegis.hdr_print();

        logger.info("EXIT amazon 1");
        return 0;
    }

    private static void orderListChoices() {
        logger.info("ENTER orderListChoices");

        Collections.sort(vetKeyChoices);

        for (Object element : vetKeyChoices) {
            String key = element.toString();
            int tnd = Integer.parseInt(key.split("\\.")[2]);

            String choices = instashopChoiceMap.get(tnd) != null ? instashopChoiceMap.get(tnd) + prop.getProperty(key) + "," : prop.getProperty(key) + ",";
            instashopChoiceMap.put(tnd, choices);
        }

        logger.info("EXIT orderListChoices");
    }

    public static HashMap<Integer, String> getInstashopChoiceMap() {
        return instashopChoiceMap;
    }

    public static void setInstashopChoiceType(String instashopChoiceType) {
        ECommerce.instashopChoiceType = instashopChoiceType;
    }

    public static String getInstashopChoiceType() {
        return instashopChoiceType;
    }
    public static void setInstashopChoice(String instashopChoice) {
        ECommerce.instashopChoice = instashopChoice;
    }

    public static String getInstashopChoice() {
        return instashopChoice;
    }

    public static long getEanAutomaticItm() {
        return eanAutomaticItm;
    }

    public static String getAccount() {
        return account;
    }

    public static HashMap<Integer, String> getSecondCopyDelivery() {
        return secondCopyDelivery;
    }

    public static HashMap<Integer, String> getSecondCopyDeliveryEnable() {    //qui second copy
        return secondCopyDeliveryEnable;
    }
    public static TreeMap<Integer, String> getCardTypeMap() {
        return cardTypeMap;
    }

    public static HashMap<Integer, String> getDdqCodeMap() {
        return ddqCodeMap;
    }

    public static void setAccount(String acnt) {
        account = acnt;
    }

    public static HashMap<Integer, Integer> getAutomaticTndCash() {
        return automaticTndCash;
    }

    public static HashMap<Integer, Integer> getAutomaticTndOnline() {
        return automaticTndOnline;
    }

    public static HashMap<Integer, Integer> getAutomaticTndCard() {
        return automaticTndCard;
    }

    public static int getTndInstashop() {
        return tndInstashop;
    }

    public static void setTndInstashop(int tndInstashop) {
        ECommerce.tndInstashop = tndInstashop;
    }

    public static HashMap<Integer, Integer> getAmtMaxMap() {
        return amtMaxMap;
    }

    public static int getAmtAutomaticItem() {
        return amtItem;
    }

    public static int getCardTypeTnd() {
        return cardTypeTnd;
    }

    public static void setCardTypeTnd(int cardTypeTnd) {
        ECommerce.cardTypeTnd = cardTypeTnd;
    }

    public static String getCardTypeDesc() {
        return cardTypeDesc;
    }

    public static void setCardTypeDesc(String cardTypeDesc) {
        ECommerce.cardTypeDesc = cardTypeDesc;
    }

    public static String getTransactionResumed() {
        return transactionResumed;
    }

    public static void setTransactionResumed(String transactionResumed) {
        ECommerce.transactionResumed = transactionResumed;
    }

    private static int calculateAmtAutomaticItem() {
        logger.debug("ENTER calculateAmtAutomaticItem");

        amtItem = tenderProperties.get(itm.tnd).getCommissionValue();  //default value

        if (tenderProperties.get(itm.tnd).getCommissionType() == TenderProperties.PERCENTAGE) {  //perc
            amtItem = (int)((amtItem / 100) * tra.amt) / 100;
        }

        alreadyAmzCommCalc = true;
        logger.info("tra.amt: " + tra.amt);
        logger.debug("EXIT calculateAmtAutomaticItem - return: " + amtItem);
        return amtItem;
    }

    public static boolean isAutomaticAmazonItem() {
        return isAutomaticAmazonItem;
    }

    public static void resetAutomaticAmazonItem() {
        isAutomaticAmazonItem = false;
    }

    public static boolean isAutomaticVoidAmazonItem() {
        return isAutomaticVoidAmazonItem;
    }

    public static void resetAutomaticVoidAmazonItem() {
        isAutomaticVoidAmazonItem = false;
    }

    public static int getAmtItem() {
        return amtItem;
    }

    public static int automaticSaleItem() {
        logger.debug("ENTER automaticSaleItem");

        eanAutomaticItm = 0;
        int sts = 0;

        try {
            if (ECommerce.getAmtMaxMap().get(itm.tnd) != null) {
                if (tra.amt > ECommerce.getAmtMaxMap().get(itm.tnd)) {
                    return 0;
                }
            }

            calculateAmtAutomaticItem();

            int nxt = event.nxt;
            event.spc = input.msk = 0;

            isAutomaticAmazonItem = true;
            input.prompt = "";
            input.qrcode = "";

            input.reset(ECommerce.tenderProperties.get(itm.tnd).getCommissionItem());

            String tmpDspLine = dspLine.toString();
            sts = group[5].action2(0);
            dspLine.init(tmpDspLine);

            event.nxt = nxt;
            isAutomaticAmazonItem = false;
        } catch (Exception e) {

        }

        logger.debug("EXIT automaticSaleItem - return: " + sts);
        return sts;
    }

    public static int automaticVoidItem() {
        logger.debug("ENTER automaticVoidItem");

        int nxt = event.nxt;
        event.spc = input.msk = 0;

        isAutomaticAmazonItem = true;
        isAutomaticVoidAmazonItem = true;
        input.prompt = "";

        input.reset(String.valueOf(eanAutomaticItm));

        String tmpDspLine = dspLine.toString();

        int sts = group[5].action2(0);
        dspLine.init(tmpDspLine);

        event.nxt = nxt;
        isAutomaticAmazonItem = false;
        isAutomaticVoidAmazonItem = false;

        logger.debug("EXIT automaticVoidItem - return: " + sts);
        return sts;
    }

    public static boolean chooseCardType() {
        logger.info("ENTER chooseCardType");

        input.init(0x00, 1, 1, 0);

        Set keys = ECommerce.getCardTypeMap().keySet();

        SelDlg dlg = new SelDlg(Mnemo.getText(22));

        int i = 1;
        List<String> tndList = new ArrayList<String>();
        List<String> descList = new ArrayList<String>();
        for (Object key : keys) {
            tndList.add(key.toString());
            String typeCard = ECommerce.getCardTypeMap().get(key);
            descList.add(typeCard);
            dlg.add(9, editNum(i, 1), " " + typeCard);
            i++;
        }
        dlg.show("MNU");


        ECommerce.setCardTypeTnd(Integer.parseInt(tndList.get(Integer.parseInt(dlg.input.pb)-1)));
        ECommerce.setCardTypeDesc(descList.get(Integer.parseInt(dlg.input.pb)-1));

        logger.info("EXIT chooseCardType - false");
        return false;
    }

    public static boolean handleInstashopPayment() {
        logger.info("ENTER handleInstashopPayment");

        ArrayList<String> choiceList = new ArrayList<String>();
        ArrayList<String> suspendList = new ArrayList<String>();

        String InstashopValue = ECommerce.getInstashopChoiceMap().get(itm.tnd);

        for (String s : InstashopValue.split(",")) {
            choiceList.add(s.split(";")[0]);
            suspendList.add(s.split(";")[1]);
        }

        input.prompt = Mnemo.getText(13);
        input.init(0x00, 1, 1, 0);
        panel.display(1, Mnemo.getMenu(103));
        SelDlg dlg = new SelDlg(Mnemo.getText(22));

        for (int i = 0; i < choiceList.size(); i++) {
            dlg.add(9, editNum(i+1, 1), " " + choiceList.get(i));
        }
        dlg.show("MNU");

        try {
            int choiceIndex = Integer.parseInt(dlg.input.pb) - 1;

            ECommerce.setInstashopChoice(dlg.input.pb);
            ECommerce.setInstashopChoiceType(suspendList.get(choiceIndex));

            if (suspendList.get(choiceIndex).trim().startsWith("S")) {
                logger.info("EXIT handleInstashopPayment - true");
                return true;
            }
        } catch (Exception e) {
            logger.error("EXIT handleInstashopPayment: " + e.getMessage());

            if (dlg.input.pb.equals("")) {
                panel.clearLink(Mnemo.getInfo(8), 1);
                return handleInstashopPayment();

            }
        }

        logger.info("EXIT handleInstashopPayment - false");
        return false;
    }

    public static boolean isAlreadyAmzCommCalc() {
        return alreadyAmzCommCalc;
    }

    public static void resetAlreadyAmzCommCalc() {
        logger.debug("resetAlreadyAmzCommCalc");
        alreadyAmzCommCalc = false;
    }

    public static void writeInstashopSuspend() {
        logger.info("ENTER writeInstashopSuspend");

        try {
            FileWriter instanshopSuspend = new FileWriter("data//transactionsInstashop.txt", true);
            BufferedWriter bw = new BufferedWriter(instanshopSuspend);

            logger.info("ctl.tran: " + ctl.tran);

            String line = leftFill(ctl.tran + "", 4, '0') + " _ " + ECommerce.getEanAutomaticItm() + " _ " + ECommerce.getInstashopChoice()
                    + " _ " + itm.tnd + " _ " + tra.amt + " _ " + itm.number + " _ " + ctl.ckr_nbr + "\r\n";
            bw.write(line);

            bw.close();
            instanshopSuspend.close();
        } catch(Exception e) {
        }

        logger.info("EXIT writeInstashopSuspend");
    }

    public static int searchInstashopSuspend(String transaction) {
        logger.info("ENTER searchInstashopSuspend - transaction: " + transaction);

        String line = "";

        try {
            File fileSuspend = new File("data//transactionsInstashop.txt");
            BufferedReader br = new BufferedReader(new FileReader(fileSuspend));

            while((line = br.readLine()) != null) {
                if (line.split("_")[0].trim().startsWith(transaction)) {
                    numberTraResume = Integer.parseInt(transaction);
                    logger.info("numberTraResume: " + numberTraResume);
                    ECommerce.setTransactionResumed(String.valueOf(numberTraResume));

                    eanItemComm = line.split("_")[1].trim();
                    logger.info("eanItemComm: " + eanItemComm);

                    tenderInstashopUsed = Integer.parseInt(line.split("_")[3].trim());
                    logger.info("tenderInstashopUsed: " + tenderInstashopUsed);

                    amountInstashop = Long.parseLong(line.split("_")[4].trim());
                    logger.info("amountInstashop: " + amountInstashop);

                    accountInstashop = line.split("_")[5].trim();
                    logger.info("accountInstashop: " + accountInstashop);
                    ECommerce.setAccount(accountInstashop);

                    br.close();

                    logger.info("EXIT searchInstashopSuspend, return 0");
                    return 0;
                }
            }
        } catch(Exception e) {
            logger.error("EXCEPTION: " + e.getMessage());
        }

        logger.info("EXIT searchInstashopSuspend, return -1");
        return -1;
    }
    
    public static boolean checkFileInstashop() {
        logger.info("ENTER checkFileInstashop");

        try {
            File instanshopFile = new File("data//transactionsInstashop.txt");

            if (!instanshopFile.exists()) {
                logger.info("EXIT checkFileInstashop - return false");
                return ctl.block = false;
            }

            BufferedReader br = new BufferedReader(new FileReader(instanshopFile));

            if (br.readLine() != null) {
                ctl.block = true;
            }

            br.close();
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.info("EXIT checkFileInstashop - return " + ctl.block);
        return ctl.block;
    }

    public static void handleInstashopResume(int transaction) {
        logger.info("ENTER handleInstashopResume - transaction " + transaction);

        try {
            BufferedReader br = new BufferedReader(new FileReader("data//transactionsInstashop.txt"));
            StringBuffer sb = new StringBuffer("");
            String line = "";

            while ((line = br.readLine()) != null) {
                if (!line.split("_")[0].trim().startsWith(String.valueOf(leftFill(transaction + "", 4, '0')))) {
                    logger.info("rewrite the line");
                    sb.append(line + "\n");
                }
            }

            br.close();

            FileWriter fw = new FileWriter(new File("data//transactionsInstashop.txt"));
            logger.info("file writer");

            fw.write(sb.toString());
            logger.info("write all");

            fw.close();
            logger.info("file closed");
        } catch (Exception e) {
            logger.error("EXCEPTION: " + e.getMessage());
        }

        logger.info("EXIT handleInstashopResume");
    }

    public static void resetInstashop() {
        logger.info("ENTER resetInstashop");

        eanItemComm = "";
        tenderInstashopUsed = 0;
        amountInstashop = 0;
        numberTraResume = 0;
        accountInstashop = "";
        ECommerce.setIsFinalizeInstashop(false);
        ECommerce.setIsResumeInstashop(false);

        logger.info("EXIT resetInstashop");
    }

    public static void printInstashopReport() {
        try {
            File instanshopFile = new File("data//transactionsInstashop.txt");

            if (instanshopFile.exists()) {
                prtLine.init(' ').type(2);
                prtLine.init("****Instashop Transactions**************").book(2);

                BufferedReader br = new BufferedReader(new FileReader(instanshopFile));
                String line = "";
                long totalInstashop = 0;

                while ((line = br.readLine()) != null) {
                    String numberTrans = line.split("_")[0].trim();
                    long amountInstashop = Long.parseLong(line.split("_")[4].trim());
                    String operatorInstashop = line.split("_")[6].trim();

                    if (ctl.ckr_nbr == Integer.parseInt(operatorInstashop)) {
                        totalInstashop += amountInstashop;
                        String lineTransaction = "Transaction " + leftFill(numberTrans, 8, ' ') + leftFill(editMoney(0, amountInstashop), 20, ' ');
                        prtLine.init(lineTransaction).book(2);
                    }
                }

                String lineTotal = "Instashop Total " + leftFill(editMoney(0, totalInstashop), 24, ' ');
                prtLine.init(lineTotal).book(2);

                prtLine.init(' ').type(2);
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    public static String getEanItemComm() {
        return eanItemComm;
    }

    public static String getAccountInstashop() {
        return accountInstashop;
    }

    public static long getAmountInstashop() {
        return amountInstashop;
    }

    public static int getTenderInstashopUsed() {
        return tenderInstashopUsed;
    }

    public static int getNumberTraResume() {
        return numberTraResume;
    }

    public static Map<Integer, TenderProperties> getTenderProperties() {
        return tenderProperties;
    }
}