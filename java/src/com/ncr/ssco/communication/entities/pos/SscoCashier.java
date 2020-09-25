package com.ncr.ssco.communication.entities.pos;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public class SscoCashier {

    private String userId;
    private String password;
    private boolean supervisor;

    public SscoCashier(String userId, String password ) {
        this.userId = userId;
        this.password = password;
        this.supervisor = supervisor;
    }

    public SscoCashier(){

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

    public boolean isSupervisor() {
        return supervisor;
    }

    public void setSupervisor(boolean supervisor) {
        this.supervisor = supervisor;
    }
}
