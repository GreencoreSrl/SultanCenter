package com.ncr.ssco.communication.manager;

import com.ncr.ssco.communication.entities.TenderType;
import com.ncr.ssco.communication.entities.TenderTypeEnum;

import java.util.List;

/**
 * Created by Umberto on 10/05/2017.
 */
public interface TenderTypeManagerInterface {
    public void writeToJsonFile(List<TenderType> tenders);
    public List<TenderType> readFromJsonFile();
    public boolean isValid(String tenderToTest);
    public TenderType getActionPOSByName(TenderTypeEnum tenderType);
}
