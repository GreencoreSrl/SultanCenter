package com.ncr.ssco.communication.manager;

import com.ncr.ssco.communication.entities.Login;

import java.util.List;

/**
 * Created by Umberto on 09/05/2017.
 */
public interface LoginManagerInteface {

    public void writeToJsonFile(List<Login> state);
    public List<Login> readFromJsonFile();
    public int getAuthenticationLevel(String userId, String password);
}
