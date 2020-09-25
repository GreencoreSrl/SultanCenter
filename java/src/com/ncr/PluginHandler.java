package com.ncr;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.Properties;

public class PluginHandler {
	private static final Logger logger = Logger.getLogger(PluginHandler.class);
	private static PluginHandler instance = null;
	private boolean toneTagPluginEnabled = false;
	private boolean eyePayPluginEnabled = false;
	private boolean alshayaPluginEnabled = false;
	private boolean verifonePluginEnabled = false;  //ECR-CGA#A
	private final static String PROP_FILENAME = "conf/eftTerminalEnable.properties";
	private final static Properties prop = new Properties();


	public static PluginHandler getInstance() {
		if (instance == null)
			instance = new PluginHandler();

		return instance;
	}

	private PluginHandler() {
		loadEftTerminalProperties();
	}

	private void loadEftTerminalProperties() {

		try {
			prop.load(new FileInputStream(PROP_FILENAME));

			try {
				toneTagPluginEnabled = prop.getProperty("toneTag", "false").equals("true");
			} catch (Exception e) {
				logger.error("missing or malformed toneTag in file " + PROP_FILENAME + ", " + e.getMessage());
			}

			try {
				eyePayPluginEnabled = prop.getProperty("eyePay", "false").equals("true");
			} catch (Exception e) {
				logger.error("missing or malformed eyePay in file " + PROP_FILENAME + ", " + e.getMessage());
			}

			try {
				alshayaPluginEnabled = prop.getProperty("alshaya", "false").equals("true");
			} catch (Exception e) {
				logger.error("missing or malformed alshaya in file " + PROP_FILENAME + ", " + e.getMessage());
			}
			//ECR-CGA#A BEG
			try {
				verifonePluginEnabled = prop.getProperty("verifone", "false").equals("true");
			} catch (Exception e) {
				logger.error("missing or malformed alshaya in file " + PROP_FILENAME + ", " + e.getMessage());
			}
			//ECR-CGA#A END
		} catch (Exception e) {
			logger.error("error during the load file " + PROP_FILENAME + ", " + e.getMessage());
		}

		logger.info("toneTagPluginEnabled " + toneTagPluginEnabled);
		logger.info("eyePayPluginEnabled " + eyePayPluginEnabled);
		logger.info("alshayaPluginEnabled " + alshayaPluginEnabled);
		logger.info("verifonePluginEnabled " + verifonePluginEnabled);
	}

	public boolean isToneTagPluginEnabled() {

		return toneTagPluginEnabled;
	}

	public void setToneTagPluginEnabled(boolean toneTagPluginEnabled) {
		this.toneTagPluginEnabled = toneTagPluginEnabled;
	}

	public boolean isEyePayPluginEnabled() {
		return eyePayPluginEnabled;
	}

	public void setEyePayPluginEnabled(boolean eyePayPluginEnabled) {
		this.eyePayPluginEnabled = eyePayPluginEnabled;
	}

	public boolean isAlshayaPluginEnabled() {
		return alshayaPluginEnabled;
	}

	public void setAlshayaPluginEnabled(boolean alshayaPluginEnabled) {
		this.alshayaPluginEnabled = alshayaPluginEnabled;
	}

	//ECR-CGA#A BEG
	public boolean isVerifonePluginEnabled() {
		return verifonePluginEnabled;
	}
	//ECR-CGA#A END
}