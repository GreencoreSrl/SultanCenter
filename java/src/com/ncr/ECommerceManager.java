package com.ncr;

import com.google.gson.*;
import javax.ws.rs.core.MultivaluedMap;

import com.ncr.ecommerce.data.Basket;
import com.ncr.ecommerce.data.Item;
import com.ncr.restclient.HttpsClient;
import com.ncr.restclient.IHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.log4j.Logger;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

public class ECommerceManager extends Action {
    private static ECommerceManager instance = null;
    private static final Logger logger = Logger.getLogger(ECommerceManager.class);
    public static final String ECOMMERCE_PROPERTIES = "conf/ecommerce.properties";
    private Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    boolean enabled = false;
    private Properties props = new Properties();
    private Basket basket;

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
        try {
            props.load(new FileInputStream(ECOMMERCE_PROPERTIES));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        // Default value set true only for testing...
        enabled = Boolean.valueOf(props.getProperty("Enabled", "false"));
    }

    public boolean checkForNewBasket(String terminal) {
        logger.debug("ECommerceManager:checkForNewBasket - Start");
        boolean result = true;

        if (enabled) {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("authorize", "false");
            IHttpClient client = new HttpsClient(props.getProperty("Url") + "/1", Integer.parseInt(props.getProperty("Timeout", "10")), new HashMap<String, String>());
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("terminal", terminal);

            try {
                String response = client.get(params).getEntity(String.class);
                if(response != null) {
                    JsonObject jsonObject = (new JsonParser()).parse(response).getAsJsonObject();
                    basket = gson.fromJson(jsonObject, Basket.class);
                    result = true;
                }
            } catch (Exception e) {
                logger.error("Error: ", e);
            }
        }
        logger.debug("ECommerceManager:checkForNewBasket - End");
        return result;
    }

    public boolean updateBasket(Basket basket) {
        logger.debug("ECommerceManager:updateBasket - Start");

        boolean result;
        result = false;

        try {
            if(basket != null){
                IHttpClient client = new HttpsClient(props.getProperty("Url"), Integer.parseInt(props.getProperty("Timeout", "10")), new HashMap<String, String>());
                MultivaluedMap<String, String> params = new MultivaluedMapImpl();
                String response = client.post(params, gson.toJson(basket).toString()).getEntity(String.class);
                result = true;
            }
        } catch (JsonSyntaxException e) {
            logger.error("Error: ", e);
        }

        logger.debug("ECommerceManager:updateBasket - End");
        return result;
    }

    public Basket getBasket() {
        return basket;
    }

    public void setBasket(Basket basket) {
        this.basket = basket;
    }

    public int action0(int spec) {
        if (basket == null) return 7;
        //TODO: what kind of transaction is it? RETURN or SALE
        if (basket.RETURN.equals(basket.getType())) {
            tra.spf1 &= M_TRRTRN;
        }
        //TODO: Cycle on items in basket and perform sale;
        for (Item item: basket.getItems()) {

        }
        return 0;
    }
}


