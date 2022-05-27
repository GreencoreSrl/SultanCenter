package com.ncr.umniah.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by User on 19/01/2018.
 */
public class AuthenticateRequest {
    @SerializedName("UserId")
    private String userId;
    @SerializedName("Password")
    private String password;

    public AuthenticateRequest(String userId, String password) {
        this.userId = userId;
        this.password = password;
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
}
