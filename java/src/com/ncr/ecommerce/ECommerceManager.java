package com.ncr.ecommerce;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.Properties;

public class ECommerceManager {
    private static ECommerceManager instance = null;
    private static final Logger logger = Logger.getLogger(ECommerceManager.class);
    public static final String ECOMMERCE_PROPERTIES = "conf/ecommerce.properties";
    private int timer;
    boolean enabled = false;
    private Properties props = new Properties();

    public static ECommerceManager getInstance() {
        if (instance == null) {
            instance = new ECommerceManager();
        }
        return instance;
    }

    private ECommerceManager() {
        loadProperties();
    }

    private void loadProperties() {
        logger.debug("Enter");
        try {
            props.load(new FileInputStream(ECOMMERCE_PROPERTIES));
        } catch (Exception e)  {
            logger.error("Error: ", e);
        }

        enabled = Boolean.valueOf(props.getProperty("Enabled", "false"));
        logger.debug("Exit");
    }

    public boolean checkForNewBasket(String terminal) {
        boolean result = false;

        if (enabled) {
            //TODO: Check for new basket invoking web services
            //HttpClient.getInstance().get();
        }
        return result;
    }
}
