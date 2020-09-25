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
    private List<String> languages = Arrays.asList(new String[] {"default", "0410", "0409", "040c", "0407", "0408", "0452"});
    private Map<String, Properties> messages;

    public static SscoLanguageHandler getInstance() {
        if(instance == null) {
            instance = new SscoLanguageHandler();
        }

        return instance;
    }

    private SscoLanguageHandler() {
        messages = new HashMap<String, Properties>();
        try {
            for (String language : languages) {
                Properties props = new Properties();
                props.load(new FileInputStream(new File("conf/lang/Language_" + language + ".properties")));
                messages.put(language, props);
            }
        } catch (Exception e) {
            // loggare
            e.getMessage();
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

}
