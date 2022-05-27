package com.ncr.ssco.communication.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ncr.ssco.communication.entities.TenderType;
import com.ncr.ssco.communication.entities.TenderTypeEnum;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Umberto on 10/05/2017.
 */
public class TenderTypeManager implements TenderTypeManagerInterface{
    private static final Logger logger = Logger.getLogger(TenderTypeManager.class);
    private Gson gson = new GsonBuilder().create();
    private List<TenderType> tenderReference;

    private static TenderTypeManager instance = null;

    public TenderTypeManager() {
        tenderReference = readFromJsonFile();
    }

    public static TenderTypeManager getInstance() {
        if (instance == null)
            instance = new TenderTypeManager();

        return instance;
    }

    @Override
    public void writeToJsonFile(List<TenderType> tender) {
        Gson gson = new Gson();
        String json = gson.toJson(tender);

        try {
            FileWriter writer = new FileWriter("conf/tenderType.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            logger.error("Error: ", e);
        }
    }

    @Override
    public List<TenderType> readFromJsonFile() {
        logger.info("Enter readFromJsonFile - conf/tenderType.json");

        File posStateJson = new File("conf/tenderType.json");
        List<TenderType> tenders = new ArrayList<TenderType>();
        String jsonString = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(posStateJson));

            //jsonString = br.readLine();
            StringBuilder stringBuffer = new StringBuilder("");
            String line = null;

            while((line =br.readLine())!=null)
                stringBuffer.append(line);

            jsonString = stringBuffer.toString();
            tenders = gson.fromJson(jsonString, new TypeToken<List<TenderType>>(){}.getType());

            // Log per stampare gli stati validi del file JSON
            logger.info("-- List of Tender: ");
            for (TenderType tender: tenders) {
                logger.info("---- " + tender.getTenderTypeSSSCO());
            }
            logger.info("-- End list ");

        } catch (Exception e) {
            logger.error("Error: ", e);
        }

        logger.info("Exit readFromJsonFile");
        return tenders;
    }

    @Override
    public boolean isValid(String tenderToTest) {
        try {
            TenderTypeEnum.valueOf(tenderToTest);
            return true;
        } catch(IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public TenderType getActionPOSByName(TenderTypeEnum tenderType) {
        for (TenderType tender : tenderReference) {
            if( tender.getTenderTypeSSSCO().equals(tenderType) ){
                return tender;
            }
        }
        return null;
    }
}
