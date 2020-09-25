package com.ncr.ssco.communication.requestprocessors;

import com.ncr.FmtIo;
import com.ncr.GdRegis;
import com.ncr.Struc;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class PrintReportRequestProcessor extends DefaultRequestProcessor {
    private static final Logger logger = Logger.getLogger(PrintReportRequestProcessor.class);
    private static String Type = "";
    private static String printReportFilePath = "";
    private static String Id = "";
    private static ArrayList<String> PrintLines = new ArrayList<String>();

    public PrintReportRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        Type = requestFromSsco.getStringField("Type");

        printReportFilePath = requestFromSsco.getStringField("URL").substring(8);
        logger.debug("printReportFilePath=" + printReportFilePath);

        if (printReportFilePath != null) {
            printReport();
        } else {
            Id = requestFromSsco.getStringField("Id");
            int complete = requestFromSsco.getIntField("Complete");
            logger.debug("Id=" + Id + ", complete=" + complete);

            for (int i = 1; i <= 1024; i++) {
                String line = requestFromSsco.getStringField("PrinterData." + i);
                if (line != null) {
                    PrintLines.add(line);
                } else {
                    break;
                }
            }
            if (complete == 1) {
                printExtendedReport();
            }
        }

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter - error code: " + sscoError.getCode() + " message: " + sscoError.getMessage());

        logger.debug("Exit");
    }

    private void printExtendedReport() {
        try {
            for (String line : PrintLines) {
                logger.debug("Printing Base64 line " + line);
                byte bytesToPrint[] = com.ncr.util.Base64.decode(line);
                String stringToPrint = new String(bytesToPrint, FmtIo.oem);
                logger.debug("Printing decoded line " + stringToPrint);
                Struc.prtLine.init(stringToPrint).book(3);
            }
        } catch (Exception e) {

            logger.error("cannot decode Base64 String" + e);
        }
        GdRegis.hdr_print();
        PrintLines.clear();
    }

    private int printReport() {
        logger.info("Enter printReport - filePath: " + printReportFilePath);
        File fileReport = new File(printReportFilePath);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileReport), Charset.forName("UTF-16LE")));
            String line;

            logger.info("BEGIN PRINT REPORT");
            while ((line = br.readLine()) != null) {
                logger.info("line readed: " + line);
                Struc.prtLine.init(line).book(3);
            }
            logger.info("END PRINT REPORT");

            GdRegis.hdr_print();
        } catch (Exception e) {
            logger.error("EXCEPTION", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                logger.info("Deleting " + fileReport.getAbsolutePath());
                fileReport.delete();
            } catch (Exception ex) {
                logger.error("EXCEPTION", ex);
            }
        }
        logger.debug("Exit");
        return 0;
    }
}