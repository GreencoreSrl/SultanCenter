package com.ncr.restclient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MultivaluedMap;
import java.security.cert.X509Certificate;

public class HttpClient implements IHttpClient {

    private Client client;
    private WebResource webResource;

    public HttpClient(String url, int timeout, boolean isHttps) {
        if (isHttps) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};

                SSLContext ctx = SSLContext.getInstance("SSL");
                ctx.init(null, trustAllCerts, null);

                //Jersey client
                ClientConfig clientConfig = new DefaultClientConfig();
                clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
                client = Client.create(clientConfig);
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        } else {
            client = Client.create();
        }
        webResource = client.resource(url);
    }

    //Get Method
    public ClientResponse get(MultivaluedMap<String, String> params) {

        ClientResponse response = webResource.queryParams(params)
                //.accept("application/json")
                .get(ClientResponse.class);
        return response;
    }

    @Override
    public ClientResponse put(MultivaluedMap<String, String> params) {
        return null;
    }

    @Override
    public ClientResponse post(MultivaluedMap<String, String> params) {
        return null;
    }

    @Override
    public ClientResponse post(MultivaluedMap<String, String> params, String content) {
        return null;
    }

    @Override
    public ClientResponse delete(MultivaluedMap<String, String> params) {
        return null;
    }
}
