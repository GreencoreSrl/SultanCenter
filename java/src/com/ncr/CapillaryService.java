package com.ncr;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Properties;

public class CapillaryService {
	private static final Logger logger = Logger.getLogger(CapillaryService.class);
	private static CapillaryService instance = null;
	private boolean enabled;
	private String baseAddress = "";
	private String username = "";
	private String password = "";

	public static CapillaryService getInstance() {
		if (instance == null) {
			instance = new CapillaryService();
		}
		return instance;
	}

	public void init() {
		createKeyStore();
	}

	public boolean isEnabled() {
		return GdSarawat.getInstance().isCapillaryEnabled();
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseAddress() {
		return baseAddress;
	}

	public void setBaseAddress(String baseAddress) {
		this.baseAddress = baseAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void requestParam() {
		logger.debug("ENTER requestParam");

		Properties capillaryParam = new Properties();

		try {
			capillaryParam.load(new FileInputStream("conf/Capillary.properties"));

			baseAddress = capillaryParam.getProperty("baseAddress");
			username = capillaryParam.getProperty("username");
			password = capillaryParam.getProperty("password");

			logger.info("baseAddress: " + baseAddress);
			logger.info("username: " + username);
			logger.info("password: " + password);
		} catch (Exception e) {
			logger.error("Exception: ", e);
		}

		logger.debug("EXIT requestParam");
	}

	public void resetAllParam() {
		logger.debug("ENTER resetAllParam");

		GdSarawat.getInstance().resetAllSarawat();
		CommunicationCapillaryForCoupon.getInstance().resetAllForCoupon();
		CommunicationCapillaryForPoints.getInstance().resetAllForPoints();
		GdTrans.resetParamCapillaryVoucher();

		logger.debug("EXIT resetAllParam");
	}

	public String getMD5Hex(final String inputString) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(inputString.getBytes());

		byte[] digest = md.digest();

		return convertByteToHex(digest);
	}

	private String convertByteToHex(byte[] byteData) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}

	private void createKeyStore() {
		BufferedInputStream inputStream = null;
		FileOutputStream outputStream = null;

		try {
			KeyStore ks = KeyStore.getInstance("BKS");
			ks.load( null, null);

			inputStream = new BufferedInputStream(new FileInputStream("conf/capcer.cer"));
			CertificateFactory cf = CertificateFactory.getInstance( "X.509" );
			Certificate cert = null;

			//Get certs from the root certificate file
			while (inputStream.available() > 0) {
				cert = cf.generateCertificate(inputStream);
				ks.setCertificateEntry( "capcert", cert );
			}
			//Add to the keystore and given an alias 'capcert'
			ks.setCertificateEntry("capcert", cert);

			//Save the keystore to a file 'CapKeyStore'. This will be created in same folder. Can give exact location.
			outputStream = new FileOutputStream("conf/CapKeyStore");
			ks.store(outputStream, "changeit".toCharArray());

		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		} finally {
			try {
				inputStream.close();
				outputStream.close();
			} catch (Exception e) {
				logger.error("Exception: ", e);
			}
		}
	}
}
