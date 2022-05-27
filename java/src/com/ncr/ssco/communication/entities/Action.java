package com.ncr.ssco.communication.entities;

public class Action {
    private String message = "";
    private String goToState = "";
    private String alternativeState = "";

    public Action() {
    }

    public String getAlternativeState() {
        return alternativeState;
    }

    public void setAlternativeState(String alternativeState) {
        this.alternativeState = alternativeState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGoToState() {
        return goToState;
    }

    public void setGoToState(String goToState) {
        this.goToState = goToState;
    }
}
