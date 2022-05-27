package com.ncr.ssco.communication.entities;

import java.util.ArrayList;
import java.util.List;

public class State {
    private String name = "";
    private List<Action> actions = new ArrayList<Action>();
    private String posState = "";

    public State() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public String getPosState() {
        return posState;
    }

    public void setPosState(String posState) {
        this.posState = posState;
    }
}
