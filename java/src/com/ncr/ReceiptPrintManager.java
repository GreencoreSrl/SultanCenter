package com.ncr;

import com.ncr.ssco.communication.entities.DataNeeded;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

public class ReceiptPrintManager extends Basis {
    private static final Logger logger = Logger.getLogger(ReceiptPrintManager.class);
    private static ReceiptPrintManager instance;
    private boolean enabled = false;
    private long promovar = 0;

    public static ReceiptPrintManager getInstance() {
        if (instance == null) {
            instance = new ReceiptPrintManager();
        }
        return instance;
    }

    private ReceiptPrintManager() {}

    public boolean ask(String message) {
        int response;

        if (!enabled) return true;
        if (promovar != 0 && Promo.getPromovar(promovar) > 0) return false;
        if (SscoPosManager.getInstance().isUsed()) {
            DataNeeded dataNeeded = SscoPosManager.getInstance().sendDataNeeded("OkCancelReceiptPrint");
            response = SscoPosManager.getInstance().waitForDataneededReply(dataNeeded.getTimeout());
        } else {
            response = gui.clearLink(message, 3);
        }
        return response == 2;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void loadParams(String line) {
        enabled = "01".equals(line.substring(0, 2));
        try {
            promovar = Long.parseLong(line.substring(2, 10));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }
}
