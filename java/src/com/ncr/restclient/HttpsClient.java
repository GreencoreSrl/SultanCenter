package com.ncr.restclient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import javax.net.ssl.*;
import javax.ws.rs.core.MultivaluedMap;
import java.security.cert.X509Certificate;
import java.util.HashMap;

public class HttpsClient implements IHttpClient {

    private Client client;
    private WebResource webResource;
    private ClientConfig clientConfig = new DefaultClientConfig();

    public HttpsClient(String url, int timeout, HashMap<String, String> headers) {
        client = Client.create(setAuthorize(Boolean.valueOf(headers.get("authorize"))));
        webResource = client.resource(url);
    }

    private ClientConfig setAuthorize(boolean authorize) {
        if (!authorize) {

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
                //Jersey client Config

                clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));

            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
        return clientConfig;
    }

    //Get Method REST
    public ClientResponse get(MultivaluedMap<String, String> params) {

        ClientResponse response = webResource.queryParams(params)
                .accept("application/json")
                .get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed HTTP error code: " + response.getStatus());
        }
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

        ClientResponse response = webResource.queryParams(params)
                .accept("application/json")
                .type("application/json")
                .post(ClientResponse.class, content);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed HTTP error code: " + response.getStatus());
        }
        return response;
    }

    @Override
    public ClientResponse delete(MultivaluedMap<String, String> params) {
        return null;
    }
}
