package com.ncr;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 14/07/15 Time: 12.29 To change this template use File |
 * Settings | File Templates.
 */
public class PosVersion {
	private static final Logger logger = Logger.getLogger(PosVersion.class);

	private final static String BASE_NAME = "POS JAVA TSC";
	private final static String BASE_VERSION = "3.01";
	private final static String CUSTOMER_ID = "1";
	private final static String CUSTOMER_BUILD = "16";
	private final static String CUSTOMER_REVISION = "3";
	private final static String BASE_DATE = "2022-05-30";

	private static String version = "";

	public static String windowTitle() {
		String tmp = "";

		tmp = BASE_NAME + " " + BASE_VERSION + "." + getBaseRevision();
		return tmp;
	}

	public static String getBaseDate() {
		logger.info("BASE_DATE: " + BASE_DATE);

		return BASE_DATE;
	}

	public static String getBaseVersion() {
		logger.info("BASE_VERSION: " + BASE_VERSION);

		return BASE_VERSION;
	}

	public static String getBaseName() {
		logger.info("BASE_NAME: " + BASE_NAME);

		return BASE_NAME;
	}

	public static String getBaseRevision() {
		logger.info("CUSTOMER_ID: " + CUSTOMER_ID);
		logger.info("CUSTOMER_BUILD: " + CUSTOMER_BUILD);
		logger.info("CUSTOMER_REVISION: " + CUSTOMER_REVISION);

		return CUSTOMER_ID + "." + CUSTOMER_BUILD + "." + CUSTOMER_REVISION;
	}

	public static void setVersion(String v) {
		version = v;
	}

	public static String getVersion() {
		return version;
	}
}
