package com.ncr.ssco.communication.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ncr.ssco.communication.entities.Action;
import com.ncr.ssco.communication.entities.State;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.requestprocessors.ProcessorConstants;
import com.ncr.ssco.communication.requestprocessors.RequestProcessorInterface;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class SscoStateManager implements SscoStateManagerInterface {
    private static final Logger logger = Logger.getLogger(SscoStateManager.class);
    private Gson gson = new GsonBuilder().create();
    private List<State> statesReference;
    private State currentState = new State();
    private State futureState = new State();
    private static String requestMessageName = "";
    private Set<String> generalMessages = new HashSet<String>(Arrays.asList(
            ProcessorConstants.VALIDATE_USER_ID
    ));

    private static SscoStateManager instance = null;

    public SscoStateManager() {
        statesReference = readFromJsonFile();
        setCurrentState("Idle");
    }

    public static SscoStateManager getInstance() {
        if (instance == null)
            instance = new SscoStateManager();

        return instance;
    }

    @Override
    public void writeToJsonFile(List<State> states) {
        Gson gson = new Gson();
        String json = gson.toJson(states);

        try {
            FileWriter writer = new FileWriter("conf/PosStates.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            logger.error("Error: ", e);
        }
    }

    @Override
    public List<State> readFromJsonFile() {
        logger.info("Enter - conf/PosStates.json");

        File posStateJson = new File("conf/PosStates.json");
        List<State> states = new ArrayList<State>();
        String jsonString = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(posStateJson));
            StringBuilder stringBuffer = new StringBuilder("");
            String line = null;

            while((line =br.readLine())!=null)
                stringBuffer.append(line);

            jsonString = stringBuffer.toString();
            states = gson.fromJson(jsonString, new TypeToken<List<State>>(){}.getType());

            logger.info("-- List of State: ");
            for (State state: states) {
                logger.info("---- " + state.getName());
            }
            logger.info("-- End list ");

        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        logger.info("Exit");
        return states;
    }

    @Override
    public boolean isValid(String messageName) {
        logger.info("Enter - request name: " + messageName);

        requestMessageName = messageName;

        if (currentState != null && !currentState.getName().equals("")) {
            logger.info("currentState: " + currentState.getName());

            if (generalMessages.contains(messageName)) {
                logger.info("Message in general messages");
                setFutureState(getCurrentState().getName());
                logger.info("Exit - matched return true");
                return true;
            } else {
                for (Action action : currentState.getActions()) {
                    logger.info("Matching with.. " + action.getMessage());
                    if (action.getMessage().equals(messageName)) {
                        setFutureState(action.getGoToState());
                        logger.info("The command " + messageName + " can be executed for state: " + currentState.getName());
                        logger.info("Exit - matched return true");
                        return true;
                    }
                }
            }
        }

        logger.warn("I haven't found any valid action for the message!!!");
        logger.info("Exit - return false");
        return false;
    }

    public boolean setCurrentState(String nameState) {
        logger.debug("Enter - nameState: " + nameState);

        for (State state : statesReference) {
            logger.info("compare with state: " + state.getName());

            if (state.getName().equals(nameState)) {
                currentState = state;

                logger.info("Exit - matched, return true");
                return true;
            }
        }

        logger.debug("Exit - return false");
        return false;
    }

    public State getCurrentState() {
        logger.debug("Enter Exit - currentState: " + currentState.getName());
        return currentState;
    }

    public State getFutureState() {
        logger.debug("Enter Exit - futureState: " + futureState.getName());
        return futureState;
    }

    public void setFutureState(String nameState) {
        logger.debug("Enter - nameState: " + nameState);

        for (State state : statesReference) {
            if (state.getName().equals(nameState)) {
                futureState = state;
                break;
            }
        }

        logger.debug("Exit");
    }

    public boolean setState(RequestProcessorInterface posProcessor) {
        logger.debug("Enter - futureState: " + futureState.getName());

        if (setCurrentState(futureState.getName())) {
            posProcessor.sendResponses(new SscoError());

            logger.debug("Exit - return true");
            return true;
        }

        logger.debug("Exit - return false");
        return false;
    }

    public String getRequestMessageName() {
        logger.debug("Enter Exit - requestMessageName: " + requestMessageName);
        return requestMessageName;
    }

    public void setRequestMessageName(String requestMessageName) {
        this.requestMessageName = requestMessageName;
    }
}