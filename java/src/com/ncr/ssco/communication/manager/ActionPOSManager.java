package com.ncr.ssco.communication.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ncr.ssco.communication.entities.ActionPOS;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ActionPOSManager implements ActionPOSManagerInterface {

    private static final Logger logger = Logger.getLogger(ActionPOS.class);
    private Gson gson = new GsonBuilder().create();
    private List<ActionPOS> actionPOSReferences;

    private static ActionPOSManager instance = null;

    @Override
    public void writeToJsonFile(List<ActionPOS> action) {
        Gson gson = new Gson();
        String json = gson.toJson(action);

        try {
            FileWriter writer = new FileWriter("conf/actionConstants.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            logger.error("Error: ", e);
        }
    }
    public ActionPOSManager() {
        actionPOSReferences = readFromJsonFile();
    }

    public static ActionPOSManager getInstance() {
        if (instance == null)
            instance = new ActionPOSManager();

        return instance;
    }
    @Override
    public List<ActionPOS> readFromJsonFile() {
        logger.info("Enter readFromJsonFile LOGIN - conf/actionConstants.json");
        File posActionsJson = new File("conf/actionConstants.json");
        List<ActionPOS> actions = new ArrayList<ActionPOS>();
        String jsonString = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(posActionsJson));

            StringBuilder stringBuffer = new StringBuilder("");
            String line = null;

            while((line =br.readLine())!=null)
                stringBuffer.append(line);

            jsonString = stringBuffer.toString();
            actions = gson.fromJson(jsonString, new TypeToken<List<ActionPOS>>(){}.getType());

            logger.info("-- List of Action for ActionPOS: ");
            for (ActionPOS action: actions) {
                logger.info("---- " + action.getAction());
            }
            logger.info("-- End list ");

        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        logger.info("Exit readFromJsonFile LOGIN ");
        return actions;
    }

    @Override
    public boolean isValid(String messageName) {
        return false;
    }

    @Override
    public ActionPOS getActionPOSByName(String command) {
        for (ActionPOS action : actionPOSReferences) {
            if( action.getAction().equals(command) ){
                return action;
            }
        }
        return null;
    }
}
