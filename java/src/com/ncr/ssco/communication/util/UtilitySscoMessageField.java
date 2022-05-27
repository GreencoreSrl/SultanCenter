package com.ncr.ssco.communication.util;

import com.ncr.util.Base64;
import com.ncr.ssco.communication.SscoMessageFieldType;
import com.ncr.ssco.communication.hook.AutomationMessage;
import org.apache.log4j.Logger;

public class UtilitySscoMessageField {
	private static final Logger logger = Logger.getLogger(UtilitySscoMessageField.class);

	public static String detectType(Object field) {
		if ((field instanceof String)) {
			return "string";
		}
		if ((field instanceof Integer)) {
			return "int";
		}
		if ((field instanceof byte[])) {
			return "bin.base64";
		}
		if ((field instanceof Boolean)) {
			return "boolean";
		}
		return "unknown";
	}

	
	public static boolean isValidType(Object field, SscoMessageFieldType fieldType) {

		if ((field instanceof String) && (fieldType.equals(SscoMessageFieldType.TYPE_STRING))) {
			return true;
		}
		if ((field instanceof Integer) && (fieldType.equals(SscoMessageFieldType.TYPE_INT))) {
			return true;
		}
		if ((field instanceof byte[]) && (fieldType.equals(SscoMessageFieldType.TYPE_BIN_BASE64))) {
			return true;
		}
        return (field instanceof Boolean) && (fieldType.equals(SscoMessageFieldType.TYPE_BOOLEAN));
    }
	
	public static SscoMessageFieldType convertToMessageFieldType(Object field) {
		if ((field instanceof String)) {
			return SscoMessageFieldType.TYPE_STRING;
		}
		if ((field instanceof Integer)) {
			return SscoMessageFieldType.TYPE_INT;
		}
		if ((field instanceof byte[])) {
			return SscoMessageFieldType.TYPE_BIN_BASE64;
		}
		if ((field instanceof Boolean)) {
			return SscoMessageFieldType.TYPE_BOOLEAN;
		}
		return SscoMessageFieldType.TYPE_UNKNOWN;
	}


	public static void convertAndAddToAutomationMessage(AutomationMessage automationMessage, String fieldName, Object fieldValue) {

		if (fieldValue instanceof String) {
			logger.debug("Valid map field type :\"String\"");
			automationMessage.addField(fieldName, (String) fieldValue);
			return;
		}
		if (fieldValue instanceof Integer) {
			logger.debug("Valid map field type :\"Integer\"");
			automationMessage.addField(fieldName, (Integer) fieldValue);
			return;
		}
		if (fieldValue instanceof byte[]) {
			logger.debug("Valid map field type :\"byte[]\"");
			String base64String = Base64.encodeBytes((byte[])fieldValue);
			automationMessage.addField(fieldName, base64String.getBytes());
			return;
		}
		if (fieldValue instanceof Boolean) {
			logger.debug("Valid map field type :\"Boolean\"");
			automationMessage.addField(fieldName, (Boolean) fieldValue);
			return;
		}

		logger.error("INVALID map field type :\"" + fieldValue + "\" !!! Not adding to response map");
	
	}
}
