package com.ncr.ssco.communication.manager;

import com.ncr.ssco.communication.entities.ActionPOS;

import java.util.List;

public interface ActionPOSManagerInterface {

    public void writeToJsonFile(List<ActionPOS> action);
    public List<ActionPOS> readFromJsonFile();
    public boolean isValid(String messageName);
    public ActionPOS getActionPOSByName(String command);// dal nome del comando mi restituisce il comando del POS
}
