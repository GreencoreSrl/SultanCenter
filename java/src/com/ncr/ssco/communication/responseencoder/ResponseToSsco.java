package com.ncr.ssco.communication.responseencoder;

import java.util.HashMap;
import java.util.Map;

public class ResponseToSsco {
	private String messageName;
	private Map<String, Object> fieldsMap;

	public ResponseToSsco(String messageName) {
		this.messageName = messageName;
		fieldsMap=new HashMap<String, Object>();
	}

	public void setIntField(String fieldName, int fieldValue) {
		fieldsMap.put(fieldName, new Integer(fieldValue));
	}

    public void setLongField(String fieldName, long fieldValue) {
        fieldsMap.put(fieldName, new Long(fieldValue));
    }

    public void setStringField(String fieldName, String fieldValue) {
		fieldsMap.put(fieldName, fieldValue);
	}
	
	public void setByteArrayField (String fieldName, byte[] fieldValue) {
		fieldsMap.put(fieldName, fieldValue);
	}
	
	public void setBooleanField(String fieldName, boolean fieldValue) {
		fieldsMap.put(fieldName, (Boolean)fieldValue);
	}	

	public String getMessageName(){
		return messageName;
	}
	
	public Map<String, Object> getFieldsMap(){
		return fieldsMap;
	}

}
