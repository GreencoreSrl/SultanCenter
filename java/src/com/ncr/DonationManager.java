package com.ncr;

import com.ncr.struc.Customer;
import org.apache.log4j.Logger;

import static com.ncr.Constant.M_ERRCOR;
import static com.ncr.Constant.M_VOID;

public class DonationManager {
    private static DonationManager instance;
    private static final Logger logger = Logger.getLogger(DonationManager.class);

    private boolean enabled = false;
    private int maxDonationAmount;

    public static DonationManager getInstance() {
        if (instance == null) {
            instance = new DonationManager();
        }
        return instance;
    }

    private DonationManager() {}

    public void loadDONParams(String txt) {
        logger.debug("ENTER loadDONParams - txt: " + txt);

        enabled = txt.charAt(0) == '1';
        maxDonationAmount = Integer.parseInt(txt.substring(1, 7));
        logger.debug("enabled: " + enabled);
        logger.debug("maxDonationAmount: " + maxDonationAmount);
        logger.debug("EXIT loadDONParams");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxDonationAmount() {
        return maxDonationAmount;
    }
}
