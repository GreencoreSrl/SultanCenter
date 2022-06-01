package com.ncr.ecommerce.data;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ncr.ECommerceManager;
import com.ncr.restclient.HttpClientFactory;
import com.ncr.restclient.IHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MultivaluedMap;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimerTask;

public class HeartBeatTimerTask //extends TimerTask
{
    private static final Logger logger = Logger.getLogger(HeartBeatTimerTask.class);
    private static final String HEARTBEAT_PROPERTIES = "conf/heartbeat.properties";
    private String terminalId = "";
    private Integer errorCode = 0;
    private Properties props = new Properties();
    private Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();


    public HeartBeatTimerTask(String terminalID, Integer errorCode)
    {
        this.terminalId = terminalID;
        this.errorCode = errorCode;
        loadProperties();
    }

    public HeartBeatTimerTask(String terminalId) {
        this.terminalId = terminalId;
        loadProperties();
    }

    private void loadProperties() {
        try {
            props.load(new FileInputStream(HEARTBEAT_PROPERTIES));
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }


   /* @Override
    public void run() {
        TerminalItem item = new TerminalItem(terminalId, errorCode);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("authorize", "false");
        IHttpClient client = new HttpClientFactory().getClient(props);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        String content = gson.toJson(item);
        client.post(params, content);
        logger.debug("sendHeartBeatMessage to WS: TerminalID: " + terminalId + "- ErrorCode: " + errorCode);
    }*/
    public void sendRequest(int errorCode) {
        TerminalItem item = new TerminalItem(terminalId, errorCode);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("authorize", "false");
        IHttpClient client = new HttpClientFactory().getClient(props);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        String content = gson.toJson(item);
        client.post(params, content);
        logger.debug("sendHeartBeatMessage to WS: TerminalID: " + terminalId + "- ErrorCode: " + errorCode);
    }
}
