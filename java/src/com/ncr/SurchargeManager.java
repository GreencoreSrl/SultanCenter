package com.ncr;

import com.ncr.struc.Customer;
import org.apache.log4j.Logger;

import static com.ncr.Constant.M_ERRCOR;
import static com.ncr.Constant.M_VOID;

public class SurchargeManager {
    public static final int EXCLUDE_DISCOUNT = 1;
    public static final int EXCLUDE_SURCHARGE = 2;
    private static SurchargeManager instance;
    private static final Logger logger = Logger.getLogger(SurchargeManager.class);

    private boolean enabledNetSurcharge = false;
    private boolean enabledNetRateDiscount = false;

    public static SurchargeManager getInstance() {
        if (instance == null) {
            instance = new SurchargeManager();
        }
        return instance;
    }

    private SurchargeManager() {}

    public void loadSCPParams(String txt) {
        logger.debug("ENTER loadSCPParams - txt: " + txt);

        enabledNetRateDiscount = txt.charAt(0) == '1';
        enabledNetSurcharge = txt.charAt(1) == '1';
        logger.debug("enableNetRateDiscount: " + enabledNetRateDiscount);
        logger.debug("enableNetSurcharge: " + enabledNetSurcharge);
        logger.debug("EXIT loadSCPParams");
    }

    public void applySurcharge(Itemdata itm, Customer cus) {
        int rate = 0;

        logger.debug("Initial price: " + itm.price + " originalPrice: " + itm.originalPrice + " prlbl: " + itm.prlbl);
        if (itm.spf1 != M_VOID && itm.spf1 != M_ERRCOR) {
            if (enabledNetSurcharge && cus.getExtra() > 0 && (itm.discountFlag & EXCLUDE_SURCHARGE) == 0) {
                rate = cus.getExtra();
                logger.debug("Extra Rate: " + rate);
            }
//            if (enabledNetRateDiscount && cus.getRate() > 0 && (itm.discountFlag & EXCLUDE_DISCOUNT) == 0) {
//                rate = -cus.getRate();
//                logger.debug("Discount Rate: " + rate);
//            }
            if (rate > 0) {
                itm.price = itm.originalPrice + (int)Action.roundBy(itm.originalPrice * rate, 1000);
                itm.originalPrice += Action.roundBy(itm.originalPrice * rate, 1000);
                itm.prlbl += Action.roundBy(itm.prlbl * rate, 1000);
                logger.debug("Changed price: " + itm.price + " originalPrice: " + itm.originalPrice + " prlbl: " + itm.prlbl);
            }
        }
    }

    public boolean isEnabledNetSurcharge() {
        return enabledNetSurcharge;
    }

    public boolean isEnabledNetRateDiscount() {
//        return enabledNetRateDiscount;
        return false;
    }
}
