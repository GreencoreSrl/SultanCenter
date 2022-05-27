package com.ncr.ssco.communication.entities;

import java.util.List;

/**
 * Created by Umberto on 16/05/2017.
 */
public class ActionPOS {

    private String action;
    private List<Command> command;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<Command> getCommand() {
        return command;
    }

    public void setCommand(List<Command> command) {
        this.command = command;
    }
}
