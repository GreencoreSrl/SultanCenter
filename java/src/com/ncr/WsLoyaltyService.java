package com.ncr;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ncr.ssco.communication.manager.SscoPosManager;
import com.ncr.struc.Customer;
import com.ncr.wsLoyalty.data.BadCardResult;
import com.ncr.wsLoyalty.data.CouponCode;
import com.ncr.wsLoyalty.data.LoyaltyCustomer;
import com.ncr.wsLoyalty.data.PhoneCode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Created by Administrator on 30/11/17.
 */
public class WsLoyaltyService {
    private static final Logger logger = Logger.getLogger(WsLoyaltyService.class);
    private static WsLoyaltyService instance = null;
    public static final String WS_PROPERTIES = "conf/WsLoyalty.properties";
    private Properties props = new Properties();
    private LoyaltyCustomer loyaltyCustomer = new LoyaltyCustomer();
    private String responseJson = "";

    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    public static WsLoyaltyService getInstance() {
        if (instance == null) {
            instance = new WsLoyaltyService();
        }
        return instance;
    }

    private WsLoyaltyService() {
        loadProperties();
    }

    private void loadProperties() {
        logger.debug("Enter");
        try {
            props.load(new FileInputStream(WS_PROPERTIES));
        } catch (Exception e)  {
            logger.error("Error: ", e);
        }
        logger.debug("Exit");
    }

    public boolean isWsLoyaltyEnabled () {
        logger.debug("Enter");
        boolean property = true;
        try {
            property = props.getProperty("LoyaltyWebServices.Enabled", "false").equals("true");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getBadCardWsBaseAddress () {
        logger.debug("Enter");
        String property = props.getProperty("BadCard.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getCustomerInfoLayout () {
        logger.debug("Enter");
        String property = props.getProperty("Customer.Info.Layout", "FN;");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getCustomerRegistrationWsBaseAddress() {
        logger.debug("Enter");
        String property = props.getProperty("CustomerRegistration.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getReplaceCardWsBaseAddress() {
        logger.debug("Enter");
        String property = props.getProperty("ReplaceCard.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getReplaceMobileWsBaseAddress() {
        logger.debug("Enter");
        String property = props.getProperty("ReplaceMobile.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getCouponWsBaseAddress() {
        logger.debug("Enter");
        String property = props.getProperty("Coupons.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public boolean getCodePromoTrim() {
        logger.debug("Enter");
        String property = "false";
        try {
            property = props.getProperty("CodePromoTrim", "false");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property.equals("true");
    }


    public String getIdentificationByCardWsBaseAddress() {
        logger.debug("Enter");
        String property = props.getProperty("IdentificationByCard.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getIdentificationByPhoneWsBaseAddress() {
        logger.debug("Enter");
        String property = props.getProperty("IdentificationByPhone.Ws.BaseAddress", "http://localhost");
        logger.debug("Exit returning " + property);
        return property;
    }

    public int getReplaceCardRetries () {
        logger.debug("Enter");
        int property = 5;
        try {
            property = Integer.parseInt(props.getProperty("ReplaceCard.Ws.Retries", "5"));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public int getReplaceMobileRetries () {
        logger.debug("Enter");
        int property = 5;
        try {
            property = Integer.parseInt(props.getProperty("ReplaceMobile.Ws.Retries", "5"));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public boolean isInactiveCustomerEnabled() {
        logger.debug("Enter");
        boolean property = true;
        try {
            property = props.getProperty("InactiveCustomers.Enabled", "false").equals("true");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public int getCustomerRegistrationdTimeout () {
        logger.debug("Enter");
        int property = 5000;
        try {
            property = Integer.parseInt(props.getProperty("CustomerRegistration.Ws.Timeout", "5000"));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public int getCustomerRegistrationRetries() {
        logger.debug("Enter");
        int property = 5;
        try {
            property = Integer.parseInt(props.getProperty("CustomerRegistration.Ws.Retries", "5"));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public int getCouponOperationRetries() {
        logger.debug("Enter");
        int property = 2;
        try {
            property = Integer.parseInt(props.getProperty("Coupons.Ws.Retries", "2"));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public boolean isOfflineCouponEnabled () {
        logger.debug("Enter");
        boolean property = true;
        try {
            property = props.getProperty("Coupons.Ws.AllowOffline", "false").equals("true");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getWebServicesAuthenticationUsername () {
        logger.debug("Enter");
        String property = props.getProperty("LoyaltyWebServices.Authentication.Username", "guest");
        logger.debug("Exit returning " + property);
        return property;
    }

    public String getWebServicesAuthenticationPassword () {
        logger.debug("Enter");
        String property = props.getProperty("LoyaltyWebServices.Authentication.Password", "guest");
        logger.debug("Exit returning " + property);
        return property;
    }

    public List<PhoneCode> getCountryCodes () {
        logger.debug("Enter");
        List<PhoneCode> property = new ArrayList<PhoneCode>();
        try {
            for (int index = 1; index < 99; index++) {
                String value = props.getProperty("CustomerRegistration.CountryCode." + index, ";");
                if (value.length() > 0) {
                    String[] tokens = value.split(";");
                    if (tokens[1].length() > 0) {
                        property.add(new PhoneCode(tokens[0], tokens[1],
                                Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), tokens[4].toLowerCase().equals("true")));  //SPINNEYS-2017-033-CGA#A
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    public List<CouponCode> getCouponCodes () {
        logger.debug("Enter");
        List<CouponCode> property = new ArrayList<CouponCode>();
        try {
            for (int index = 1; index < 99; index++) {
                String value = props.getProperty("CouponEan." + index, " ; ");
                if (value.length() > 0) {
                    String[] tokens = value.split(";");
                    if (tokens[1].trim().length() > 0) {
                        property.add(new CouponCode(tokens[0],
                                Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), tokens[3].toLowerCase().equals("true")));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit returning " + property);
        return property;
    }

    private ClientResponse getClientResponse(String url) throws RuntimeException {
        Client client = Client.create();
        //client.addFilter(new HTTPBasicAuthFilter(getWebServicesAuthenticationUsername(), getWebServicesAuthenticationPassword()));

        WebResource webResource = client.resource(url);
        ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);

        if (response.getStatus() < 200 || response.getStatus() > 299) {
            throw new RuntimeException("Failed - HTTP error code: " + response.getStatus());
        }
        return response;
    }

    public int checkBadCard(String card) {  //REQUEST IN GET
        logger.debug("Enter card: " + card);
        int sts = 0;
        if (SscoPosManager.getInstance().isTestEnvironment()) {
            return 0;
        }

        String url = getBadCardWsBaseAddress() + "?cardCode=" + card;
        logger.info("url Request: " + url);

        try {
            ClientResponse response = getClientResponse(url);

            BadCardResult result = gson.fromJson(response.getEntity(String.class), BadCardResult.class);
            logger.info("Result: " + result.getCode() + " " + result.getDescription());
            if (result.getCode() != 0) {
                sts = 109;
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
            sts = 94;
        }

        logger.debug("Exit returning: " + sts);
        return sts;
    }

    private void createTestLoyaltyCustomer() {
        loyaltyCustomer = new LoyaltyCustomer("9000000022", "971-557163326");
        loyaltyCustomer.setFirstName("Paul James");
        loyaltyCustomer.setLastName("Morgan");
        loyaltyCustomer.setCustomerCode("9000000022");
        loyaltyCustomer.setActive(true);
    }

    public int identificationCustomer(String url) {
        logger.debug("Enter request: " + url);
        int sts = 0;
        if (SscoPosManager.getInstance().isTestEnvironment()) {
            createTestLoyaltyCustomer();
            return 0;
        }

        try {
            ClientResponse response = getClientResponse(url);

            LoyaltyCustomer result = gson.fromJson(response.getEntity(String.class), LoyaltyCustomer.class);
            logger.debug("response: " + result);

            if (result == null) {
                sts = 112;
            } else {
                loyaltyCustomer = result;
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
            sts = 94;
        }

        logger.debug("Exit returning: " + sts);
        return sts;
    }

    public LoyaltyCustomer getLoyaltyCustomer() {
        return loyaltyCustomer;
    }

    public int sendPostRequest(String url, String requestJson) {
        logger.debug("Enter. url: " + url + " requestJson: " + requestJson);

        try {
            Client client = Client.create();
            // client.addFilter(new HTTPBasicAuthFilter(getWebServicesAuthenticationUsername(),getWebServicesAuthenticationPassword()));

            WebResource webResource = client.resource(url);
            ClientResponse response = webResource.type("application/json").accept("application/json")
                    .post(ClientResponse.class, requestJson);

            if (response.getStatus() < 200 || response.getStatus() > 299) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            responseJson = response.getEntity(String.class);

		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("Exit");

			responseJson = "";
            return 94;
		}

        logger.debug("EXIT sendPostRequest: " + responseJson);
        return 0;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public String getCustomerInfo(Customer cus) {
        String layout = getCustomerInfoLayout();
        StringBuffer line = new StringBuffer();

        try {
            String[] tokens = layout.split(";");
            for (int index = 0; index < tokens.length; index++) {
                if ("FirstName".equals(tokens[index])) {
                    line.append(cus.getName());
                } else if ("LastName".equals(tokens[index])) {
                    line.append(cus.getNam2());
                }
                line.append(" ");
            }
        } catch (Exception e) {
            logger.warn("Problem in splitting layout: " + layout);
        }
        return line.toString();
    }
}