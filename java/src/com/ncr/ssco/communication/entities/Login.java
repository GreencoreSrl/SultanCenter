package com.ncr.ssco.communication.entities;

/**
 * Created by Umberto on 09/05/2017.
 */
public class Login {

    private String userId;
    private String password;
    private int authenticationLevel;

    public Login(String userId, String password, int authenticationLevel) {
        this.userId = userId;
        this.password = password;
        this.authenticationLevel = authenticationLevel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    public void setAuthenticationLevel(int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }
}
