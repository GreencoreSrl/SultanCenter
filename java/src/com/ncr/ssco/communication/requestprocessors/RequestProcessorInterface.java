package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.DataNeeded;
import com.ncr.ssco.communication.entities.AdditionalProcessType;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;

public interface RequestProcessorInterface {
	void process(RequestFromSsco requestFromSsco);
    void additionalProcess();
    void sendResponses();
    void sendResponses(SscoError sscoError);
    void setProcessRequest(RequestFromSsco requestFromSsco);
    void setAdditionalProcessType(AdditionalProcessType info);
    String addEndResponse();
    void sendDataNeeded(DataNeeded dataNeeded);
    void sendShutdownRequested();

    void setItemResponse(SscoItem responseBySSCO);
}
