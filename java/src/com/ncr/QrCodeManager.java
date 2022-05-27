package com.ncr;

import com.ncr.qrcode.QrCodeItem;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QrCodeManager extends Action {
    private static QrCodeManager instance = null;
    private static final Logger logger = Logger.getLogger(QrCodeManager.class);

    public static QrCodeManager getInstance() {
        if (instance == null) {
            instance = new QrCodeManager();
        }
        return instance;
    }

    private QrCodeManager() {
    }

    public void resetQrCode() {
        input.qrcode = "";
    }

    public void handleQrCode() {
        logger.info("ENTER handleQrCode");
        List<String> unsoldItems = new ArrayList<String>();
        int sts = 0;
        String[] itmList = input.qrcode.split("\\r?\\n");
        logger.info("input.qrcode: " + input.qrcode);


        List<QrCodeItem> qrCodeItemList = buildQrCodeItemList(itmList);
        input.qrcode = "";

        int stItm = itm.spf1;

        logger.info("itmlist size: " + itmList.length);
        logger.info("itm spf1: " + itm.spf1);
        for (QrCodeItem item : qrCodeItemList) {
            sellQrCodeItem(unsoldItems, stItm, item);
        }
        if (SscoPosManager.getInstance().isUsed() && unsoldItems.size() > 0) {
            String separator = "";
            String message = "";
            for (String item : unsoldItems) {
                message += separator + item;
                separator = " - ";
            }
            SscoPosManager.getInstance().sendDataNeeded("QrCodeUnsoldItems", message);
        }

        logger.info("EXIT handleQrCode");
    }

    private List<QrCodeItem> buildQrCodeItemList(String[] itmList) {
        List<QrCodeItem> qrCodeItemList = new ArrayList<QrCodeItem>();

        for (String itmCurrent : itmList) {
            if(!itmCurrent.contains(";")) {
                qrCodeItemList.add(new QrCodeItem(itmCurrent, BigDecimal.ZERO));
            }
            else {
                String code = itmCurrent.split(";")[0];
                BigDecimal price = new BigDecimal(itmCurrent.split(";")[1]);
                qrCodeItemList.add(new QrCodeItem(code, price));
            }
        }
        return qrCodeItemList;
    }

    private void sellQrCodeItem(List<String> unsoldItems, int stItm, QrCodeItem item) {
        int sts;
        itm.spf1 = stItm;

        input.pb = item.getCode();
        itm.prpov = item.getPrice();
        itm.qrcode = true;

        logger.info("input pb: " + input.pb);
        logger.info("itm prprov: " + itm.prpov);

        input.num = input.pb.trim().length();

        if ((sts = group[5].action2(0)) != 0) {
            logger.info("sts: " + sts);
            if (SscoPosManager.getInstance().isUsed()) {
                unsoldItems.add(input.pb.trim());
            } else {
                panel.clearLink(Mnemo.getInfo(sts), 1);
            }
        }
    }
}
