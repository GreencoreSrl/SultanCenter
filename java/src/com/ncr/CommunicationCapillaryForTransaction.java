package com.ncr;
/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 08/06/15
 * Time: 17.44
 * To change this template use File | Settings | File Templates.
 */

import com.ncr.capillary.TransactionDetails.*;
import com.ncr.capillary.TransactionDetails.Transaction;
import com.ncr.capillary.TransactionDetails.Status;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CommunicationCapillaryForTransaction extends Action {
	private static final Logger logger = Logger.getLogger(CommunicationCapillaryForTransaction.class);
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	private static CommunicationCapillaryForTransaction instance = null;
	private String urlRequest = "";
	private Status status;
	private Transactions transactions;

	public static CommunicationCapillaryForTransaction getInstance() {
		if (instance == null)
			instance = new CommunicationCapillaryForTransaction();

		return instance;
	}

	public Transactions getTransactions() {
		return transactions;
	}

	public void setTransactions(Transactions transactions) {
		this.transactions = transactions;
	}

    public int transactionAdd(Transact tra, Customer cus, Itemdata pit, Terminal ctl) {
		logger.debug("ENTER transactionAdd");

		String responseJson = "";
		CapillaryService.getInstance().requestParam();

		StructRequestTransactionAdd requestAdd = fillStructRequestTransactionAdd(tra, cus, ctl);

		urlRequest = CapillaryService.getInstance().getBaseAddress() + "transaction/add?format=json";
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
			logger.debug("EXIT transactionAdd");

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
			logger.debug("EXIT transactionAdd");

            return 82;
		} */

		logger.debug("EXIT transactionAdd");
		return checkTransaction(responseJson);
	}

    private int checkTransaction(String jsonString) {
		logger.debug("ENTER checkTransaction");
		logger.info("jsonString: " + jsonString);

		StructResponseTransaction responseTransaction = new StructResponseTransaction();

		/*
		 * try { responseTransaction = gson.fromJson(jsonString, StructResponseTransaction.class); } catch (Exception e)
		 * { logger.info("Exception " + e.getMessage()); logger.debug("EXIT checkTransaction - return false");
		 * return false; }
		 * 
		 * status = responseTransaction.getResponse().getStatus();
		 */

        try {
            responseTransaction = gson.fromJson(jsonString, StructResponseTransaction.class);

            status = responseTransaction.getResponse().getStatus();
			if (status.getSuccess().equals("true")) {
				// transactions = responseTransaction.getResponse().getTransactions();
				logger.debug("EXIT checkTransaction - return 0");

                return 0;
			}
		} catch (Exception e) {
			logger.info("Exception " + e.getMessage());
			logger.debug("EXIT checkTransaction - return ERROR");

            return 82;
		}

		logger.debug("EXIT checkTransaction - return: " + (status.getCode() + 2000));
        return status.getCode() + 2000;
	}

	private StructRequestTransactionAdd fillStructRequestTransactionAdd(Transact tra, Customer cus, Terminal ctl) {
		logger.debug("ENTER fillStructRequestTransactionAdd");

		Itemdata itm;
		com.ncr.capillary.TransactionDetails.Customer customer = new com.ncr.capillary.TransactionDetails.Customer();
		PaymentDetails paymentDetails = new PaymentDetails();
		LineItems lineItems = new LineItems();
		ArrayList<Transaction> listTransactions = new ArrayList<Transaction>();
		ArrayList<Payment> listPaymentDetails = new ArrayList<Payment>();
		ArrayList<LineItem> listLineItems = new ArrayList<LineItem>();

		// set structure customer
		customer.setExternalId(cus.number);
		customer.setFirstname(cus.name);
		customer.setLastname("");
		customer.setMobile(cus.mobile);

		// set structure items and payments of transaction
		logger.info("tra.vItems.size(): " + tra.vItems.size());
        long totAmtTrans = 0;
		for (int ind = 0; ind < tra.vItems.size(); ind++) {
			itm = tra.vItems.getElement(ind).copy();

			logger.info("itm.id: " + itm.id);
			if (itm.id == 'S') {
				logger.info("add item into the list");
                totAmtTrans += itm.amt;
				listLineItems.add(fillStructLineItem(itm));
			} else if (itm.id == 'T') {
				logger.info("add payment into the list");
				listPaymentDetails.add(fillStructPayment(itm));
			} else if (itm.id == 'C') {
                logger.info("add discount to item");
                addDiscountItem(itm, listLineItems);
            }
		}

		paymentDetails.setPayment(listPaymentDetails);
		lineItems.setLineItem(listLineItems);

		// set structure transaction
		Transaction transaction = new Transaction();
		if (cus.number == null || cus.number.equals("")) {
            if (tra.spf1 == M_TRRTRN || tra.spf1 == M_TRVOID) {
                transaction.setType("not_interested_return");
                transaction.setReturnType("FULL");
            } else {
                transaction.setType("not_interested");
            }
		} else {
            if (tra.spf1 == M_TRRTRN || tra.spf1 == M_TRVOID) {
                transaction.setType("return");
                transaction.setReturnType("LINE_ITEM");
            } else {
                transaction.setType("regular");
            }

            transaction.setCustomer(customer);
		}

		transaction.setNumber(String.valueOf(ctl.tran));

		transaction.setAmount(editMoney(0, Math.abs(tra.amt)).replace(",", "."));
        transaction.setGrossAmount(editMoney(0, Math.abs(totAmtTrans)).replace(",", "."));
        transaction.setDiscount(editMoney(0, totAmtTrans - tra.amt).replace(",", "."));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dataStr = sdf.format(new Date());

		transaction.setBillingTime(dataStr);
        transaction.setPurchaseTime(dataStr);

        if (tra.spf1 != M_TRRTRN && tra.spf1 != M_TRVOID) {
		    transaction.setPaymentDetails(paymentDetails);
        }
		transaction.setLineItems(lineItems);

		listTransactions.add(transaction);

		com.ncr.capillary.TransactionDetails.Root root = new com.ncr.capillary.TransactionDetails.Root();
		root.setTransaction(listTransactions);

		StructRequestTransactionAdd requestAdd = new StructRequestTransactionAdd();
		requestAdd.setRoot(root);

		logger.debug("EXIT fillStructRequestTransactionAdd");
		return requestAdd;
	}

    private void addDiscountItem(Itemdata itm, ArrayList<LineItem> listLineItems) {
        long amtDscItm = (itm.price * itm.cnt) + itm.amt;

        for (int i = listLineItems.size()-1; i >= 0; i--) {
            if (listLineItems.get(i).getItemCode().trim().equals(itm.number.trim())) {
                listLineItems.get(i).setAmount(editMoney(0, Math.abs(amtDscItm)).replace(",", "."));
                listLineItems.get(i).setDiscount(editMoney(0, Math.abs(itm.amt)).replace(",", "."));

                break;
            }
        }
    }

	private Payment fillStructPayment(Itemdata itm) {
		logger.debug("ENTER fillStructPayment");

		com.ncr.capillary.TransactionDetails.Payment payment = new com.ncr.capillary.TransactionDetails.Payment();
		payment.setMode(itm.text.trim());

		payment.setValue(editMoney(0, itm.amt).replace(",", "."));

		logger.debug("EXIT fillStructPayment");
		return payment;
	}

	private LineItem fillStructLineItem(Itemdata itm) {
		logger.debug("ENTER fillStructLineItem");

		LineItem lineItem = new LineItem();
		lineItem.setSerial(1);
		lineItem.setItemCode(itm.number.trim());
		lineItem.setDescription(itm.text.trim());
		lineItem.setAmount(editMoney(0, Math.abs(itm.amt)).replace(",", "."));
		lineItem.setQty(itm.qty);
		lineItem.setValue(editMoney(0, Math.abs(itm.price)).replace(",", "."));
        lineItem.setDiscount("0");

        if (tra.spf1 != M_TRRTRN && tra.spf1 != M_TRVOID) {
            if (itm.amt < 0) {
                lineItem.setType("return");
            } else {
                lineItem.setType("regular");
            }
        }

		logger.debug("EXIT fillStructLineItem");
		return lineItem;
	}

	private String writeJson(StructRequestTransactionAdd requestAdd) {
		logger.debug("ENTER writeJson");

		String request = "";

		try {
			request = gson.toJson(requestAdd).toString();
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
		}

		logger.debug("EXIT writeJson - return request: " + request);
		return request;
	}
}
