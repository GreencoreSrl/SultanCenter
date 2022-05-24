package com.ncr.ssco.communication.manager;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Created by NCRDeveloper on 29/05/2017.
 */
public class SscoLanguageHandler {
    protected static final Logger logger = Logger.getLogger(SscoLanguageHandler.class);
    private static SscoLanguageHandler instance = null;

    private String primaryLanguage = "default";
    private String customerLanguage = "default";
    private List<String> languages = Arrays.asList(new String[] {"default", "0410", "0409", "040c", "0407", "0408", "0452", "0809"});
    private Map<String, Properties> messages;
    private Map<String, Properties> dataNeededProps;

    public static SscoLanguageHandler getInstance() {
        if(instance == null) {
            instance = new SscoLanguageHandler();
        }

        return instance;
    }

    private SscoLanguageHandler() {
        messages = new HashMap<String, Properties>();
        dataNeededProps = new HashMap<String, Properties>();
        for (String language : languages) {
            try {
                logger.info("Loading Language: " + language);
                Properties props = new Properties();
                props.load(new FileInputStream(new File("conf/lang/Language_" + language + ".properties")));
                messages.put(language, props);
            } catch (Exception e) {
                logger.error("Error: " + language, e);
            }

            try {
                logger.info("Loading Dataneeded Language: " + language);
                Properties props = new Properties();
                props.load(new FileInputStream(new File("conf/lang/DataNeeded_" + language + ".properties")));
                dataNeededProps.put(language, props);
            } catch (Exception e) {
                logger.error("Error: " + language, e);
            }
        }
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public String getCustomerLanguage() {
        return customerLanguage;
    }

    public void setCustomerLanguage(String customerLanguage) {
        this.customerLanguage = customerLanguage;
    }

    public String getMessage(String key, String defMessage) {
        String message = null;
        try {
            if (!customerLanguage.equals(""))
                message = messages.get(customerLanguage).getProperty(key);

            if (message == null) {
                message = messages.get(primaryLanguage).getProperty(key);
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        if (message == null) {
            message = defMessage;
        }
        return message;
    }

    public Properties getDataNeededProperties(String language) {
        return dataNeededProps.get(language);
    }
}