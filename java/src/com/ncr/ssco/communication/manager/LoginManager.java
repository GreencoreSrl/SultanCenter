package com.ncr.ssco.communication.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ncr.ssco.communication.entities.Login;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LoginManager implements LoginManagerInteface{
    private static final Logger logger = Logger.getLogger(LoginManager.class);
    private Gson gson = new GsonBuilder().create();
    private List<Login> loginReference;
    private static String requestMessageName = "";

    private static LoginManager instance = null;

    public LoginManager() {
        loginReference = readFromJsonFile();
    }

    public static LoginManager getInstance() {
        if (instance == null)
            instance = new LoginManager();

        return instance;
    }

    @Override
    public void writeToJsonFile(List<Login> logins) {
        Gson gson = new Gson();
        String json = gson.toJson(logins);

        try {
            FileWriter writer = new FileWriter("conf/login.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            logger.error("Error: ", e);
        }
    }

    @Override
    public List<Login> readFromJsonFile() {
        logger.info("Enter readFromJsonFile LOGIN - conf/login.json");

        File posStateJson = new File("conf/login.json");
        List<Login> logins = new ArrayList<Login>();
        String jsonString = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(posStateJson));

            //jsonString = br.readLine();
            StringBuilder stringBuffer = new StringBuilder("");
            String line = null;

            while((line =br.readLine())!=null)
                stringBuffer.append(line);

            jsonString = stringBuffer.toString();
            logins = gson.fromJson(jsonString, new TypeToken<List<Login>>(){}.getType());

            logger.info("-- List of UserId for Login: ");
            for (Login login: logins) {
                logger.info("---- " + login.getUserId());
            }
            logger.info("-- End list ");

        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        logger.info("Exit readFromJsonFile LOGIN ");
        return logins;
    }

    @Override
    public int getAuthenticationLevel( String userId, String password ) {

        logger.info("Enter IsValid LOGIN");

        if( this.loginReference == null ){
            logger.info("File Login not present in the Configuration..");
            return 0;
        }

        for( Login login : this.loginReference ){
            if( login.getUserId().equals(userId) && login.getPassword().equals(password) ){
                logger.info("-- matched !");
                logger.info("End IsValid LOGIN");
                return login.getAuthenticationLevel();
            }
        }

        logger.info("-- NOT matched !");
        logger.info("End IsValid LOGIN");
        return 0;
    }
}
