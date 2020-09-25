package com.ncr;
/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 08/06/15
 * Time: 17.44
 * To change this template use File | Settings | File Templates.
 */

import com.ncr.capillary.CouponDetails.*;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;

public class CommunicationCapillaryForCoupon {
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CommunicationCapillaryForCoupon.class);
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	private static CommunicationCapillaryForCoupon instance = null;
	private String urlRequest = "";
	private Status status;
	private Redeemable redeemable;
	private IsRedeemableCoupons coupons;
	private ArrayList<RedeemCoupon> listCouponIsRedeemable = new ArrayList<RedeemCoupon>();
	private RedeemCoupon redeemCoupon = new RedeemCoupon();
	//private ArrayList<Long> listRangeCode = new ArrayList<Long>();

	public static CommunicationCapillaryForCoupon getInstance() {
		if (instance == null)
			instance = new CommunicationCapillaryForCoupon();

		return instance;
	}

	public void resetAllForCoupon() {
		logger.info("resetAllForCoupon");

		listCouponIsRedeemable = new ArrayList<RedeemCoupon>();
	}

	public IsRedeemableCoupons getCoupons() {
		return coupons;
	}

	public void setCoupons(IsRedeemableCoupons coupons) {
		this.coupons = coupons;
	}

	public Redeemable getRedeemable() {
		return redeemable;
	}

	public void setRedeemable(Redeemable redeemable) {
		this.redeemable = redeemable;
	}

	public RedeemCoupon getRedeemCoupon() {
		return redeemCoupon;
	}

	public void setRedeemCoupon(RedeemCoupon redeemCoupon) {
		this.redeemCoupon = redeemCoupon;
	}

	public ArrayList<RedeemCoupon> getListCouponIsRedeemable() {
		return listCouponIsRedeemable;
	}

	public void setListCouponIsRedeemable(ArrayList<RedeemCoupon> listCouponIsRedeemable) {
		this.listCouponIsRedeemable = listCouponIsRedeemable;
	}

	/*public ArrayList<Long> getListRangeCode() {
		return listRangeCode;
	}

	public void setListRangeCode(ArrayList<Long> listRangeCode) {
		this.listRangeCode = listRangeCode;
	}

	private void readDpRange() {
		logger.entering(CLASSNAME, "readDpRange");

		Properties prop = new Properties();

		try {
			prop.load(new FileInputStream("conf/Capillary-promotions.properties"));

			for (int i = 1;; i++) {
				String rangeCodeMin = prop.getProperty("couponDP." + i + ".min");
				String rangeCodeMax = prop.getProperty("couponDP." + i + ".max");

				if (rangeCodeMin == null || rangeCodeMax == null) {
					break;
				}

				listRangeCode.add(Long.parseLong(rangeCodeMin));
				listRangeCode.add(Long.parseLong(rangeCodeMax));
			}
		} catch (Exception e) {
			logger.info("Exception " + e.getMessage());
		}
	} */

    public int couponIsRedeemable(String code, String mobile) {
		logger.debug("ENTER - code: " + code + ", mobile: " + mobile);

		String responseJson = "";
		CapillaryService.getInstance().requestParam();
		//readDpRange();

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "coupon/isredeemable?format=json";
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
			responseJson = resource.queryParam("code", code).queryParam("mobile", mobile).queryParam("details", "true").accept(MediaType.APPLICATION_XML).get(String.class);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			logger.debug("EXIT 82");

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
			logger.exiting(CLASSNAME, "couponIsRedeemable");

            return 82;
		} */

		logger.debug("EXIT");
		return checkCoupon(responseJson, true);
	}

    private int checkCoupon(String jsonString, boolean isredeem) {
		logger.debug("ENTER - jsonString: " + jsonString);

		try {
			if (isredeem) {
				StructResponseIsRedeemableCoupon responseCoupon = gson.fromJson(jsonString, StructResponseIsRedeemableCoupon.class);

				status = responseCoupon.getResponse().getStatus();
				if (status.getSuccess().equals("true")) {
					coupons = responseCoupon.getResponse().getCoupons();
					redeemCoupon = new RedeemCoupon();

                    redeemCoupon.setCode(coupons.getRedeemable().getCode());
                    redeemCoupon.setDiscountType(coupons.getRedeemable().getSeriesInfo().getDiscountType());
                    redeemCoupon.setDiscount(coupons.getRedeemable().getSeriesInfo().getDiscountValue());
                    redeemCoupon.setDiscountCode(coupons.getRedeemable().getSeriesInfo().getDiscountCode());

                    listCouponIsRedeemable.add(redeemCoupon);

					logger.debug("EXIT - return 0");
					return 0;
                } else {
					ItemStatus itemStatus = responseCoupon.getResponse().getCoupons().getRedeemable().getItemStatus();

					logger.debug("EXIT - return; " + (itemStatus.getCode() + 2000) + " " + itemStatus.getMessage());
					return itemStatus.getCode() + 2000;
				}
            } else {
				StructResponseRedeemCoupon responseCoupon = gson.fromJson(jsonString, StructResponseRedeemCoupon.class);

				status = responseCoupon.getResponse().getStatus();
				if (status.getSuccess().equals("true")) {
					logger.debug("EXIT - return 0");
					return 0;
				} else {
					ItemStatus itemStatus = responseCoupon.getResponse().getCoupons().getCoupon().getItemStatus();

					logger.debug("EXIT - return; " + (itemStatus.getCode() + 2000) + " " + itemStatus.getMessage());
					return itemStatus.getCode() + 2000;
				}
			}
		} catch (Exception e) {
			logger.info("Exception: ", e);
			logger.debug("EXIT - return ERROR");

            return 82;
		}
		//logger.exiting(CLASSNAME, "checkCoupon - return: " + (status.getCode() + 2000));
        //return status.getCode() + 2000;
	}

    public int couponRedeem(Transact tra, Customer cus, String code) {
		logger.debug("ENTER - code: " + code);

		String responseJson = "";
		StructRequestCouponRedeem requestRedeem = fullStructRequestCouponRedeem(tra, cus, code);

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "coupon/redeem?format=json";
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
			logger.debug("EXIT - return 82");

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
			logger.exiting(CLASSNAME, "couponRedeem");

            return 82;
		} */

		logger.debug("EXIT");
		return checkCoupon(responseJson, false);
	}

	private StructRequestCouponRedeem fullStructRequestCouponRedeem(Transact tra, Customer cus, String code) {
		logger.debug("ENTER");

		ArrayList<Coupon> couponPassed = new ArrayList<Coupon>();
		Transaction transaction = new Transaction();
		com.ncr.capillary.CouponDetails.Customer customer = new com.ncr.capillary.CouponDetails.Customer();

		transaction.setNumber(tra.number);
		transaction.setAmount(tra.amt);

		customer.setMobile(cus.mobile);
		customer.setEmail("");
		customer.setExternalId(cus.number);

		Coupon coupon = new Coupon();

		coupon.setCode(code);
		coupon.setCustomer(customer);
		coupon.setTransaction(transaction);

		couponPassed.add(coupon);

		Root root = new Root();
		root.setCoupon(couponPassed);

		StructRequestCouponRedeem requestAdd = new StructRequestCouponRedeem();
		requestAdd.setRoot(root);

		logger.debug("EXIT");
		return requestAdd;
	}

	private String writeJson(StructRequestCouponRedeem requestRedeem) {
		logger.debug("ENTER");

		String request = "";

		try {
			request = gson.toJson(requestRedeem).toString();
		} catch (Exception e) {
			logger.error("Exception: ", e);
		}

		logger.debug("EXIT - return request: " + request);
		return request;
	}
}
