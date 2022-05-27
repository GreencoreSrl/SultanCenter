package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoCustomer;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import org.apache.log4j.Logger;

public class AirMilesRequestProcessor extends LoyaltyRequestProcessor {
    private static final Logger logger = Logger.getLogger(AirMilesRequestProcessor.class);

    public AirMilesRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");
        setSscoCustomer(new SscoCustomer(
                requestFromSsco.getStringField("AccountNumber"),
                "",
                requestFromSsco.getStringField("EntryMethod"),
                0  //INTEGRATION-PHILOSHOPIC-CGA#A
        ));
        getManager().airMilesRequest(getSscoCustomer());
        setCardType("AirMiles");
        setResponseName("AirMiles");

        logger.debug("Exit");
    }
}