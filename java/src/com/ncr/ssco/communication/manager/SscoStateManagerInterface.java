package com.ncr.ssco.communication.manager;

import com.ncr.ssco.communication.entities.State;

import java.util.List;

/**
 * Created by Umberto on 04/05/2017.
 */
public interface SscoStateManagerInterface {

    public void writeToJsonFile(List<State> state);
    public List<State> readFromJsonFile();
    public boolean isValid(String messageName);
}
