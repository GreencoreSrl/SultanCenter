package com.ncr;

import com.ncr.capillary.CustomerDetails.StructResponseCustomer;
import com.ncr.capillary.PointDetails.*;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 08/06/15 Time: 17.39 To change this template use File |
 * Settings | File Templates.
 */
public class CommunicationCapillaryForPoints {
	private static final Logger logger = Logger.getLogger(CommunicationCapillaryForPoints.class);
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	private static CommunicationCapillaryForPoints instance = null;
	private String urlRequest = "";
	private String redemptionPvPoints = "";
	private String redemptionPvDiscount = "";
	private Status status;
	private Points points;
	private Redeemable redeemable;
	private int nPointsCustomer = 0;
	private long nPointsRedeem = 0;
	private long discountValue = 0;

	public static CommunicationCapillaryForPoints getInstance() {
		if (instance == null)
			instance = new CommunicationCapillaryForPoints();

		return instance;
	}

	public void resetAllForPoints() {
		nPointsCustomer = 0;
		nPointsRedeem = 0;
		discountValue = 0;
	}

	public Points getPoints() {
		return points;
	}

	public void setPoints(Points points) {
		this.points = points;
	}

	public int getNPointsCustomer() {
		return nPointsCustomer;
	}

	public void setNPointsCustomer(int nPoints) {
		this.nPointsCustomer = nPoints;
	}

	public String getRedemptionPvPoints() {
		return redemptionPvPoints;
	}

	public void setRedemptionPvPoints(String redemptionPvPoints) {
		this.redemptionPvPoints = redemptionPvPoints;
	}

	public String getRedemptionPvDiscount() {
		return redemptionPvDiscount;
	}

	public void setRedemptionPvDiscount(String redemptionPvDiscount) {
		this.redemptionPvDiscount = redemptionPvDiscount;
	}

	public Redeemable getRedeemable() {
		return redeemable;
	}

	public void setRedeemable(Redeemable redeemable) {
		this.redeemable = redeemable;
	}

	public long getNPointsRedeem() {
		return nPointsRedeem;
	}

	public void setNPointsRedeem(long nPointsRedeem) {
		this.nPointsRedeem = nPointsRedeem;
	}

	public long getDiscountValue() {
		return discountValue;
	}

	public void setDiscountValue(long discountValue) {
		this.discountValue = discountValue;
	}

	public void pvParam() {
		logger.debug("ENTER pvParam");

		Properties pvPoints = new Properties();

		try {
			pvPoints.load(new FileInputStream("conf/Capillary-promotions.properties"));

			redemptionPvPoints = pvPoints.getProperty("redemptionPvPoints");
			redemptionPvDiscount = pvPoints.getProperty("redemptionPvDiscount");

			logger.info("redemptionPvPoints: " + redemptionPvPoints);
			logger.info("redemptionPvDiscount: " + redemptionPvDiscount);
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
		}

		logger.debug("EXIT pvParam");
	}

    public int pointsIsRedeemable(String nPoints, String mobile) {
		logger.debug("ENTER pointsIsRedeemable");
		logger.info("nPoints: " + nPoints);
		logger.info("mobile: " + mobile);

		String responseJson = "";
		CapillaryService.getInstance().requestParam();
		pvParam();

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "points/isredeemable?format=json";
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
			responseJson = resource.queryParam("points", nPoints).queryParam("mobile", mobile).accept(MediaType.APPLICATION_XML).get(String.class);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("EXIT pointsIsRedeemable");

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
			logger.debug("EXIT pointsIsRedeemable");

            return 82;
		} */

		logger.debug("EXIT pointsIsRedeemable");
		return checkPointsCard(responseJson);
	}

    private int checkPointsCard(String jsonString) {
		logger.debug("ENTER checkPointsCard");
		logger.info("jsonString: " + jsonString);

		StructResponsePoints responsePoints = new StructResponsePoints();

		try {
			responsePoints = gson.fromJson(jsonString, StructResponsePoints.class);

            status = responsePoints.getResponse().getStatus();
            if (status.getSuccess().equals("true")) {
                points = responsePoints.getResponse().getPoints();
                nPointsRedeem = points.getRedeemable().getPoints();
                discountValue = (long) (Double.parseDouble(points.getRedeemable().getPointsRedeemValue()) * 100);

                logger.info("points: " + points);
                logger.info("nPointsRedeem: " + nPointsRedeem);
                logger.info("discountValue: " + discountValue);

                logger.debug("EXIT return 0");
                return 0;
            }
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			logger.debug("EXIT return ERROR");

            return 82;
		}

		ItemStatus itemStatus = responsePoints.getResponse().getPoints().getRedeemable().getItemStatus();
		logger.debug("EXIT return; " + (itemStatus.getCode() + 2000) + " " + itemStatus.getMessage());
		return itemStatus.getCode() + 2000;
		//logger.debug("EXIT return; " + (status.getCode() + 2000));
        //return status.getCode() + 2000;
	}

    public int pointsRedeem(Transact tra, Customer cus, long points) {
		logger.debug("ENTER pointsRedeem");
		logger.info("points: " + points);

		String responseJson = "";

		StructRequestPointsRedeem requestRedeem = fullStructRequestPointsRedeem(tra, cus, points);

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "points/redeem?format=json";
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
			String requestJson = writeJson(requestRedeem);
			logger.info("requestJson: " + requestJson);
			responseJson = resource.accept(MediaType.APPLICATION_XML).type("application/json").post(String.class, requestJson);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("EXIT pointsRedeem");

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

			String requestJson = writeJson(requestRedeem);
			logger.info("requestJson: " + requestJson);

			StringEntity input = new StringEntity(requestJson);

			input.setContentType("application/json");
			postRequest.setEntity(input);

			HttpResponse response = client.execute(postRequest);

			responseJson = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			logger.debug("EXIT pointsRedeem");

            return 82;
		} */

		logger.debug("EXIT pointsRedeem");
		return checkPoints(responseJson);
	}

    private int checkPoints(String jsonString) {
		logger.debug("ENTER checkPoints");
		logger.info("jsonString: " + jsonString);

		StructResponsePoints responsePoints = new StructResponsePoints();

		try {
			responsePoints = gson.fromJson(jsonString, StructResponsePoints.class);

            status = responsePoints.getResponse().getStatus();
            if (status.getSuccess().equals("true")) {
                points = responsePoints.getResponse().getPoints();

                logger.debug("ENTER checkPoints - return 0");
                return 0;
            }
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			logger.debug("ENTER checkPoints - return ERROR");

            return 82;
		}

		ItemStatus itemStatus = responsePoints.getResponse().getPoints().getItemStatus();
		logger.debug("EXIT checkPoints - return; " + (itemStatus.getCode() + 2000) + " " + itemStatus.getMessage());
		return itemStatus.getCode() + 2000;
		//logger.debug("ENTER checkPoints - return: " + (status.getCode() + 2000));
        //return status.getCode() + 2000;
	}

	private StructRequestPointsRedeem fullStructRequestPointsRedeem(Transact tra, Customer cus, long points) {
		logger.debug("ENTER fullStructRequestPointsRedeem");
		logger.info("points: " + points);

		StructRequestPointsRedeem requestPointsRedeem = new StructRequestPointsRedeem();
		Root root = new Root();
		ArrayList<Redeem> redeem = new ArrayList<Redeem>();
		Redeem redeemElement = new Redeem();

		com.ncr.capillary.PointDetails.Customer customer = new com.ncr.capillary.PointDetails.Customer();

		customer.setMobile(cus.mobile);
		customer.setEmail("");
		customer.setExternalId(cus.number);

		redeemElement.setPointsRedeemed(points);
		redeemElement.setTransactionNumber(tra.number);
		redeemElement.setCustomer(customer);
		redeemElement.setNotes("");
		redeemElement.setValidationCode("");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dataStr = sdf.format(new Date());

        redeemElement.setRedemptionTime(dataStr);

		redeem.add(redeemElement);

		root.setRedeem(redeem);

		requestPointsRedeem.setRoot(root);

		logger.debug("EXIT fullStructRequestPointsRedeem");
		return requestPointsRedeem;
	}

    public int customerGet(String numberCard) {
		logger.debug("ENTER customerGet");
		logger.info("numberCard: " + numberCard);

		if (!CapillaryService.getInstance().isEnabled()) {
			logger.info("Capillary disabled");
			logger.debug("EXIT customerGet - return false");

            return 82;
		}

		String responseJson = "";
		CapillaryService.getInstance().requestParam();
		urlRequest = CapillaryService.getInstance().getBaseAddress() + "customer/get?format=json";
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
			responseJson = resource.queryParam("external_id", numberCard).accept(MediaType.APPLICATION_XML).get(String.class);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("EXIT customerGet");

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
			logger.debug("EXIT customerGet");

            return 82;
		} */

		logger.debug("EXIT customerGet");
		return checkCustomerCardPoints(responseJson);
	}

    private int checkCustomerCardPoints(String jsonString) {
		logger.debug("ENTER checkCustomerCardPoints");
		logger.info("jsonString: " + jsonString);

		StructResponseCustomer responseCustomer = new StructResponseCustomer();

		try {
			responseCustomer = gson.fromJson(jsonString, StructResponseCustomer.class);

            String resp = responseCustomer.getResponse().getStatus().getSuccess();
            if (resp.equals("true")) {
                nPointsCustomer = responseCustomer.getResponse().getCustomers().getCustomer().get(0).getLoyaltyPoints();
                logger.info("nPointsCustomer: " + nPointsCustomer);
                logger.debug("EXIT checkCustomerCardPoints - return true");

                return 0;
            }
        } catch (Exception e) {
			logger.info("Exception " + e.getMessage());
			logger.debug("EXIT checkCustomerCardPoints - return false");

            return 82;
		}

		com.ncr.capillary.CustomerDetails.ItemStatus itemStatus = responseCustomer.getResponse().getCustomers().getCustomer().get(0).getItemStatus();
		logger.debug("EXIT checkCustomerCardPoints - return; " + (itemStatus.getCode() + 2000) + " " + itemStatus.getMessage());
		return itemStatus.getCode() + 2000;
		//logger.debug("EXIT checkCustomerCardPoints - return false");
        //return status.getCode() + 2000;
	}

	private String writeJson(StructRequestPointsRedeem requestRedeem) {
		logger.debug("ENTER writeJson");

		String request = "";

		try {
			request = gson.toJson(requestRedeem).toString();
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
		}

		logger.debug("EXIT writeJson - return request: " + request);
		return request;
	}
}
