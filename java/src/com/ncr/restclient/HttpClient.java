package com.ncr.restclient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;

public class HttpClient implements IHttpClient{

    public Client client;
    public WebResource webResource;

    public HttpClient(String url, int timeout, HashMap<String, String> headers)
    {
        client=Client.create();
        webResource = client.resource(url);
    }

    //Get Method
    public ClientResponse get(MultivaluedMap<String,String> params){

        ClientResponse response = webResource.queryParams( params)
                //.accept("application/json")
                .get(ClientResponse.class);
        if(response.getStatus()!=200){
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
