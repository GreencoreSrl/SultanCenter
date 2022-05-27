package com.ncr.ssco.communication.requestdecoder;

import com.ncr.ssco.communication.util.Utilities;
import com.ncr.ssco.communication.util.UtilitySscoMessageField;
import org.apache.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.util.Map;

public class RequestFromSsco {
	private static final Logger logger = Logger.getLogger(RequestFromSsco.class);
	private String messageName;
	private Map<String, Object> fieldsMap;

	public RequestFromSsco(String messageName, Map<String, Object> fieldsMap) {
		this.messageName = messageName;
		this.fieldsMap = fieldsMap;
		dump();
	}

	public String getMessageName() {
		return this.messageName;
	}

	public Integer getIntField(String fieldName) {
		Object value = fieldsMap.get(fieldName);
		if (value == null) {
			logger.warn("Field \"" + fieldName + "\" not present!");
			return null;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		logger.warn("Field \"" + fieldName + "\" present but type is not \"Integer\"!");
		return null;
	}

	public String getBin64Field(String fieldName) {
		Object value = fieldsMap.get(fieldName);
		if (value == null) {
			logger.warn("Field \"" + fieldName + "\" not present!");
			return null;
		}
		if (value instanceof byte[]) {
			logger.info("getStringField - value is a byte[]: " + value);
			byte[] valueByte = (byte[])value;

			String encoding = DatatypeConverter.printBase64Binary(valueByte);
			String decoded = new String(valueByte);

			logger.info("getBin64gField - return value converted to String: " + decoded);
			return decoded;
		}
		return null;
	}

	public String getStringField(String fieldName) {
		Object value = fieldsMap.get(fieldName);

		if (value == null) {
			logger.warn("Field \"" + fieldName + "\" not present!");
			return null;
		}
		if (value instanceof String) {
			return (String) value;
		}
		logger.warn("Field \"" + fieldName + "\" present but type is not \"String\"!");
		return null;
	}

	private void dump() {
		logger.debug("----------------------- BEGIN Dump RequestFromSsco -------------------------");

		logger.debug("messageName = \"" + messageName + "\"");
		logger.debug(Utilities.leftFillWithSpaces("------ NAME ------", 20) + Utilities.leftFillWithSpaces("----- TYPE -----", 20)
				+ Utilities.leftFillWithSpaces("----- VALUE -----", 20));

		for (String key : fieldsMap.keySet()) {
			Object value = fieldsMap.get(key);
			String type = UtilitySscoMessageField.detectType(value);
			logger.debug(Utilities.leftFillWithSpaces(key, 20) + Utilities.leftFillWithSpaces(type, 20) + Utilities.leftFillWithSpaces("" + value, 20));
		}
		logger.debug("----------------------- END   Dump RequestFromSsco -------------------------");
	}
}
