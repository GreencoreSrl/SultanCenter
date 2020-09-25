package com.ncr;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ncr.umniah.data.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;


/**
 * Created by Administrator on 30/11/17.
 */
public class WsUmniahService {
    private static final Logger logger = Logger.getLogger(WsUmniahService.class);
    private static WsUmniahService instance = null;
    public static final String WS_PROPERTIES = "conf/WsUmniah.properties";
    private Properties props = new Properties();
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();
    private String security = "";


    public static WsUmniahService getInstance() {
        if (instance == null) {
            instance = new WsUmniahService();
        }

        return instance;
    }

    private WsUmniahService() {
        loadProperties();
    }

    private void loadProperties() {
        logger.debug("Enter loadProperties");
        try {
            props.load(new FileInputStream(WS_PROPERTIES));
        } catch (Exception e)  {
            logger.error("Error: ", e);
        }
        logger.debug("Exit loadProperties");
    }

    public Boolean isWsUmniahEnabled () {
        logger.debug("Enter isWsUmniahEnabled");
        boolean property = true;
        try {
            property = props.getProperty("UmniahWebServices.Enabled", "false").equals("true");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit isWsUmniahEnabled: " + property);
        return property;
    }

    public String getAuthenticateWsBaseAddress () {
        logger.debug("Enter getAuthenticateWsBaseAddress");
        String property = "http://localhost";
        try {
            property = props.getProperty("Authenticate.Ws.BaseAddress", "http://localhost");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit getAuthenticateWsBaseAddress: " + property);
        return property;
    }

    public String getDenominationsWsBaseAddress () {
        logger.debug("Enter getDenominationsWsBaseAddress");
        String property = "http://localhost";
        try {
            property = props.getProperty("Denominations.Ws.BaseAddress", "http://localhost");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit getDenominationsWsBaseAddress: " + property);
        return property;
    }

    public String getPinWsBaseAddress() {
        logger.debug("Enter getPinWsBaseAddress");
        String property = "http://localhost";
        try {
            property = props.getProperty("Pin.Ws.BaseAddress", "http://localhost");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit getPinWsBaseAddress: " + property);
        return property;
    }

    public int getTimeout () {
        logger.debug("Enter getTimeout");
        int property = 5000;
        try {
            property = Integer.parseInt(props.getProperty("Umniah.Ws.Timeout", "5000"));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit getTimeout: " + property);
        return property;
    }

    public String getWebServicesAuthenticationUsername () {
        logger.debug("Enter getWebServicesAuthenticationUsername");
        String property = "guest";
        try {
            property = props.getProperty("UmniahWebServices.Authentication.Username", "guest");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }
        logger.debug("Exit getWebServicesAuthenticationUsername: " + property);
        return property;
    }

    public String getWebServicesAuthenticationPassword () {
        logger.debug("Enter getWebServicesAuthenticationPassword");

        String property = "guest";
        try {
            property = props.getProperty("UmniahWebServices.Authentication.Password", "guest");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getWebServicesAuthenticationPassword: " + property);
        return property;
    }

    public String getApiKey () {
        logger.debug("Enter getApiKey");
        String property = "213d1442098b3aceae608c9560c06b2442c13aef0a86762b5ce71a089a3e189d";
        try {
            property = props.getProperty("UmniahWebServices.ApiKey", "");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getApiKey: " + property);
        return property;
    }

    public String getLanguage () {
        logger.debug("Enter getLanguage");

        String property = "EN";
        try {
            property = props.getProperty("UmniahWebServices.Language", "EN");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getLanguage: " + property);
        return property;
    }

    public String getChannel() {
        logger.debug("Enter getChannel");

        String property = "SAFEWAY";
        try {
            property = props.getProperty("UmniahWebServices.Channel", "SAFEWAY");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getChannel: " + property);
        return property;
    }

    public String[] getRechargeTypeArray() {
        logger.debug("Enter getRechargeTypeArray");

        String property = "MOBILE;ONE_PIN;INTERNET";
        try {
            property = props.getProperty("UmniahWebServices.RechargeTypes", "MOBILE;ONE_PIN;INTERNET");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getRechargeTypeArray: " + property);
        return property.split(";");
    }

    //UMNIA-20180109-CGA#A BEG
    public String getBillingNo () {
        logger.debug("Enter getBillingNo");

        String property = "null";
        try {
            property = props.getProperty("UmniahWebServices.BillingNo", "null");
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getBillingNo: " + property);
        return property;
    }


    //UmniahWebServices.RechargeTypes=MOBILE;ONE_PIN;INTERNET
    //UmniahWebServices.AutoSellItem.MOBILE=1000000001


    public String getAutoSellItem(String type, String denomination) {
        logger.debug("Enter getAutoSellItem - type: " + type + " denomination: " + denomination);

        String property = "";
        try {
            property = props.getProperty("UmniahWebServices.AutoSellItem." + type + "_" + denomination, "");
            if (property == null || property.length() == 0) {
                property = props.getProperty("UmniahWebServices.AutoSellItem." + type + "_default", "");
                if (property == null || property.length() == 0) {
                    property = props.getProperty("UmniahWebServices.AutoSellItem." + type, "");
                }
            }
        } catch(Exception e) {
            logger.error("Error: ", e);
        }

        logger.debug("Exit getAutoSellItem: " + property);
        return property;
    }

    public String getCertificateFilePath () {
        logger.debug("Enter getCertificateFilePath");

        String property = "";

        try {
            property = props.getProperty("Certificate.File.Path", "");
        } catch(Exception e) {}

        logger.debug("Exit getCertificateFilePath: " + property);
        return property;
    }
    //UMNIA-20180109-CGA#A END



    ///http://www.javased.com/index.php?api=com.sun.jersey.api.client.Client


    public AuthenticateResponse AuthWsRequest(AuthenticateRequest request) {
        logger.debug("Enter AuthWsRequest - request: " + request);
        AuthenticateResponse authResponse = new AuthenticateResponse();
        String url = getAuthenticateWsBaseAddress();
        //security = "security";

        try {
            Client client = Client.create();
            client.setConnectTimeout(getTimeout());
            client.setReadTimeout(getTimeout());

            client.addFilter(new HTTPBasicAuthFilter(getWebServicesAuthenticationUsername(),
                    getWebServicesAuthenticationPassword()));

            WebResource webResource = client.resource(url);

            //webResource.header("Security", security);

            ClientResponse response = webResource.type("application/json").accept("application/json")
                    .post(ClientResponse.class, gson.toJson(request));

            if (response.getStatus() < 200 || response.getStatus() > 299) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            authResponse = gson.fromJson(response.getEntity(String.class), AuthenticateResponse.class);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            logger.debug("Exit AuthWsRequest");

            return null;
        }

        logger.debug("EXIT AuthWsRequest - response: " + authResponse.toString());
        return authResponse;
    }

    public DenominationsResponse denominationsWsRequest(DenominationsRequest request) {
        logger.debug("Enter denominationsWsRequest - request: " + request);
        DenominationsResponse denominationsResponse = new DenominationsResponse();
        String url = getDenominationsWsBaseAddress();

        try {
            Client client = Client.create();
            client.setConnectTimeout(getTimeout());
            client.setReadTimeout(getTimeout());

            client.addFilter(new HTTPBasicAuthFilter(getWebServicesAuthenticationUsername(),
                    getWebServicesAuthenticationPassword()));

            WebResource webResource = client.resource(url);
            //webResource.header("X-Token", GdUmniah.getInstance().getToken());

            logger.debug("token: >" + GdUmniah.getInstance().getToken() + "<");
            webResource.setProperty("X-Token", GdUmniah.getInstance().getToken());

            ClientResponse response = webResource.type("application/json").accept("application/json").header("X-Token", GdUmniah.getInstance().getToken())
                    .post(ClientResponse.class, gson.toJson(request));

            if (response.getStatus() < 200 || response.getStatus() > 299) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            denominationsResponse = gson.fromJson(response.getEntity(String.class), DenominationsResponse.class);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("Exit denominationsWsRequest");

            return null;
		}

        logger.debug("EXIT denominationsWsRequest - response: " + denominationsResponse.toString());
        return denominationsResponse;
    }

    public GetPinResponse pinWsRequest(GetPinRequest request) {
        logger.debug("Enter pinWsRequest - request: " + request);
        GetPinResponse pinResponse = new GetPinResponse();
        String url = getPinWsBaseAddress();

        try {
            Client client = Client.create();
            client.setConnectTimeout(getTimeout());
            client.setReadTimeout(getTimeout());

            client.addFilter(new HTTPBasicAuthFilter(getWebServicesAuthenticationUsername(),
                    getWebServicesAuthenticationPassword()));

            WebResource webResource = client.resource(url);
            //webResource.header("X-Token", GdUmniah.getInstance().getToken());
            logger.debug("token: >" + GdUmniah.getInstance().getToken() + "<");
            webResource.setProperty("X-Token", GdUmniah.getInstance().getToken());

            ClientResponse response = webResource.type("application/json").accept("application/json").header("X-Token", GdUmniah.getInstance().getToken())
                    .post(ClientResponse.class, gson.toJson(request));

            if (response.getStatus() < 200 || response.getStatus() > 299) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            pinResponse = gson.fromJson(response.getEntity(String.class), GetPinResponse.class);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            logger.debug("Exit pinWsRequest");

            return null;
        }

        logger.debug("EXIT pinWsRequest - response: " + pinResponse.toString());
        return pinResponse;
    }


    //recupero la chiave nel file di certificazione


    //QUELLO CHE DOVREBBERO AVER FATTO LORO:
    //installazione del certificato
    //il certificato serve per verificare la firma

    //NOI:
    //bisogna accodare la richiesta con la chiave, che si trova nel certificato
    //ogni richiesta deve essere serializzata in un json, prima della firma
    //la firma digitale deve essere eseguita usando l'algoritmo di hash SHA1


    /*public boolean VerifyMessage(String filename) throws Exception {
        List<byte[]> list = new ArrayList<byte[]>();

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
            list = (List<byte[]>) in.readObject();
            in.close();
        } catch(Exception e) {

        }

        return verifySignature(list.get(0), list.get(1));
    }

    private boolean verifySignature(byte[] data, byte[] signature) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(getPublicKey());
        sig.update(data);

        return sig.verify(signature);
    }

    public PublicKey getPublicKey() {
        logger.debug("Enter getPublicKey");

        PublicKey key = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(getCertificateFilePath());
            Certificate c = cf.generateCertificate(in);
            key = c.getPublicKey();
            in.close();
        } catch(Exception e) {

        }

        logger.debug("Exit getPublicKey");
        return key;
    }*/


    //devo avere la chiave del certificato, contenuta nel file
    //la leggerò da parametro nel file di properties
    //la userò nella funzione per ottenere il valore del campo security
    //invio la richiesta di autenticazione
    //la risposta mi darà un token che dovrò utilizzare, insieme alla security, per le mie richieste
    //controllare bene i campi
}
