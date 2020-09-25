package com.ncr;
/*import com.ncr.capillary.CustomerDetails.*;
import com.ncr.capillary.CustomerDetails.DataCustomer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;    */

import com.ncr.capillary.CustomerDetails.*;
import com.ncr.capillary.CustomerDetails.Customer;
import com.ncr.capillary.CustomerDetails.Status;
import com.ncr.capillary.CustomerDetails.StructResponseCustomer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 08/06/15 Time: 17.39 To change this template use File |
 * Settings | File Templates.
 */

public class CommunicationCapillaryForCustomer {
	private static final Logger logger = Logger.getLogger(CommunicationCapillaryForCustomer.class);
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	private static CommunicationCapillaryForCustomer instance = null;
	private String urlRequest = "";
	private Status status;
	private Customer customer;
	private boolean alreadyRegistered = false;

	public static CommunicationCapillaryForCustomer getInstance() {
		if (instance == null)
			instance = new CommunicationCapillaryForCustomer();

		return instance;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public boolean isAlreadyRegistered() {
		return alreadyRegistered;
	}

	public void setAlreadyRegistered(boolean alreadyRegistered) {
		this.alreadyRegistered = alreadyRegistered;
	}

    public int customerGet(String number, boolean mobile) {
		logger.debug("ENTER");
		logger.info("number: " + number);
        logger.info("mobile: " + mobile);

        String responseJson = "";
		CapillaryService.getInstance().requestParam();

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "customer/get?format=json";
		logger.info("urlRequest: " + urlRequest);

		try {
			logger.info("KeyStore");
			KeyStore trustStore = KeyStore.getInstance("BKS");
			logger.info("TrustManagerFactory");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			logger.info("Loading file conf/CapKeyStore");
			trustStore.load(new FileInputStream("conf/CapKeyStore"), "changeit".toCharArray());
			logger.info("Initializing");
			tmf.init(trustStore);
			logger.info("SSL Context");
			SSLContext ctx = SSLContext.getInstance("SSL");
			logger.info("Initializing Context");
			ctx.init(null, tmf.getTrustManagers(), null);

			//Jersey client
			ClientConfig clientConfig = new DefaultClientConfig();
			logger.info("Putting properties");
			clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
			Client client = Client.create(clientConfig);
			client.addFilter(new HTTPBasicAuthFilter(CapillaryService.getInstance().getUsername(),
					CapillaryService.getInstance().getMD5Hex(CapillaryService.getInstance().getPassword())));
			WebResource resource = client.resource(urlRequest);
			if (mobile) {
				responseJson = resource.queryParam("mobile", number).accept(MediaType.APPLICATION_XML).get(String.class);
			} else {
				responseJson = resource.queryParam("external_id", number).accept(MediaType.APPLICATION_XML).get(String.class);
			}
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("EXIT");

			return 82;
		}
		/* try {
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(CapillaryService.getInstance().getUsername(),
							CapillaryService.getInstance().getMD5Hex(CapillaryService.getInstance().getPassword())));

			CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider)
					.build();
			HttpGet request = new HttpGet(urlRequest);

			HttpResponse response = client.execute(request);

			responseJson = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			logger.exiting("customerGet");

            return 82;
		} */

		logger.debug("EXIT");
		return checkCustomerCard(responseJson, number);
	}

    private int checkCustomerCard(String jsonString, String newCode) {
        logger.debug("ENTER");
		logger.info("jsonString: " + jsonString);

		StructResponseCustomer responseCustomer = new StructResponseCustomer();
		alreadyRegistered = false;

		try {
			responseCustomer = gson.fromJson(jsonString, StructResponseCustomer.class);

            status = responseCustomer.getResponse().getStatus();

            if (status.getSuccess().equals("true")) {
                customer = responseCustomer.getResponse().getCustomers().getCustomer().get(0);
				if (customer.getExternalId() != null) {
					alreadyRegistered = true;
				}
                /*
                 * if (!customer.getExternalId().equals(newCode)) { alreadyRegistered = true; logger.exiting(CLASSNAME,
                 * "checkCustomerCard - customer already registered - return false"); return false; }
                 */

                logger.debug("EXIT - return 0");
                return 0;
            }
		} catch (Exception e) {
			logger.info("Exception: ", e);
			logger.debug("EXIT - return ERROR");

            return 82;
		}

		com.ncr.capillary.CustomerDetails.ItemStatus itemStatus = responseCustomer.getResponse().getCustomers().getCustomer().get(0).getItemStatus();
		logger.debug("EXIT - return; " + (itemStatus.getCode() + 2000) + " " + itemStatus.getMessage());
		return itemStatus.getCode() + 2000;
		//logger.entering("checkCustomerCard - return: " + (status.getCode() + 2000));
        //return status.getCode() + 2000;
	}

    public int customerAdd(String numberCard, String mobile) {
        logger.debug("ENTER");
		logger.info("numberCard: " + numberCard);
		logger.info("mobile: " + mobile);

		String responseJson = "";
		CapillaryService.getInstance().requestParam();

		StructRequestCustomerAdd requestAdd = fullStructRequestCustomerAdd(numberCard, mobile);

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "customer/add?format=json";
		logger.info("urlRequest: " + urlRequest);

		try {
			KeyStore trustStore = KeyStore.getInstance("BKS");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			trustStore.load(new FileInputStream("conf/CapKeyStore"), "changeit".toCharArray());
			tmf.init(trustStore);
			SSLContext ctx = SSLContext.getInstance("SSL");
			ctx.init(null, tmf.getTrustManagers(), null);

			//Jersey client
			ClientConfig clientConfig = new DefaultClientConfig();
			clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
			Client client = Client.create(clientConfig);
			client.addFilter(new HTTPBasicAuthFilter(CapillaryService.getInstance().getUsername(),
					CapillaryService.getInstance().getMD5Hex(CapillaryService.getInstance().getPassword())));
			WebResource resource = client.resource(urlRequest);
			String requestJson = writeJson(requestAdd);
			logger.info("requestJson: " + requestJson);
			responseJson = resource.accept(MediaType.APPLICATION_XML).type("application/json").post(String.class, requestJson);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("EXIT");

			return 82;
		}
		/* try {
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(CapillaryService.getInstance().getUsername(),
							CapillaryService.getInstance().getMD5Hex(CapillaryService.getInstance().getPassword())));

			CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpPost postRequest = new HttpPost(urlRequest);

			String requestJson = writeJson(requestAdd);
			logger.info("requestJson: " + requestJson);

			StringEntity input = new StringEntity(requestJson);

			input.setContentType("application/json");
			postRequest.setEntity(input);

			HttpResponse response = client.execute(postRequest);

			responseJson = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			logger.exiting("customerAdd");

            return 82;
		} */

		logger.debug("EXIT");
		return checkCustomerCard(responseJson, numberCard);
	}

	private StructRequestCustomerAdd fullStructRequestCustomerAdd(String code, String mobile) {
		logger.debug("ENTER");
		logger.info("code: " + code);
		logger.info("mobile: " + mobile);

		com.ncr.capillary.CustomerDetails.Customer customer = new com.ncr.capillary.CustomerDetails.Customer();
		customer.setMobile(mobile);
		customer.setExternalId(code);

		ArrayList<Customer> listCustomers = new ArrayList<com.ncr.capillary.CustomerDetails.Customer>();
		listCustomers.add(customer);

		Root root = new Root();
		root.setCustomer(listCustomers);

		StructRequestCustomerAdd requestAdd = new StructRequestCustomerAdd();
		requestAdd.setRoot(root);

		logger.debug("EXIT - return: " + requestAdd.toString());
		return requestAdd;
	}

	private String writeJson(StructRequestCustomerAdd requestAdd) {
		logger.debug("ENTER");

		String request = "";

		try {
			request = gson.toJson(requestAdd).toString();
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
		}

		logger.debug("EXIT - return request: " + request);
		return request;
	}
}
