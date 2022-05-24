package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.LoginManager;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;
import org.apache.log4j.Logger;

public class ValidateUserIdRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(ValidateUserIdRequestProcessor.class);
    private boolean signOn = false;

    public ValidateUserIdRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        signOn = true;
        sendResponses(new SscoError());
        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.info("Response Validate - Enter" );
        String user = this.getRequest().getStringField("UserId");
        String password = this.getRequest().getStringField("Password");

        int validationUser = LoginManager.getInstance().getAuthenticationLevel( user, password);
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ValidateUserId");

        responseToSsco.setStringField("UserId", user);
        responseToSsco.setIntField("AuthenticationLevel", validationUser);
        if( validationUser == 0 ) {
            responseToSsco.setStringField("Message.1", "Error validating userId");
        }

        getMessageHandler().sendResponseToSsco(responseToSsco);
        getMessageHandler().getResponses().add( addEndResponse() ); // fine sequenza

        logger.debug("Response Validate - Exit");
    }
}
