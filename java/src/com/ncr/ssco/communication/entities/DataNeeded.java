package com.ncr.ssco.communication.entities;

import com.ncr.ssco.communication.entities.pos.SscoTransaction;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public class DataNeeded {
    private static final Logger logger = Logger.getLogger(DataNeeded.class);

    private String name;
    private DataNeededType type = DataNeededType.Clear;
    private String tableName;
    private List<String> topCaptionLines;
    private List<String> topCaptionSubstitutionsLines;
    private List<String> topCaptionTableNameLines;
    private List<String> topCaptionDestinationLines;
    private List<String> summaryInstructionLines;
    private List<String> summaryInstructionSubstitutionsLines;
    private List<String> summaryInstructionTableNameLines;
    private List<String> summaryInstructionDestinationLines;
    private List<String> bottomCaptionLines;
    private List<String> bottomCaptionSubstitutionsLines;
    private List<String> bottomCaptionTableNameLines;
    private List<String> bottomCaptionDestinationLines;
    private List<String> detailedInstructionLines;
    private List<String> detailedInstructionSubstitutionsLines;
    private List<String> detailedInstructionTableNameLines;
    private List<String> detailedInstructionDestinationLines;
    private List<String> buttonDataLines; // AMZ-FLANE#ADD
    private List<String> buttonTextLines; // AMZ-FLANE#ADD
    private Integer enableMsr = null;
    private Integer enableScanner = null;
    private Integer scannerDataFormat = null;
    private Integer keypad = null;
    private String inputMask;
    private Integer minInputLength = null;
    private String soundFile;
    private Integer hideTotal = null;
    private Integer hideHelp = null;
    private Integer hideGoBack = null;
    private Integer hideInput = null;
    private int enableSecurity = 0;
    private int id = 0;
    private int mode = 0;
    private int timeout;

    public class ClearDataNeeded extends DataNeeded {
        public ClearDataNeeded() {
            super("Clear", DataNeededType.Clear, 0);
        }
    }

    public DataNeeded(String name, Properties properties) {
        this.name = name;
        loadProperties(name, properties);
    }

    public DataNeeded(String name, Properties properties, String message) {
        this.name = name;
        loadProperties(name, properties);
        summaryInstructionLines.clear();
        summaryInstructionLines.add(message);
    }

    public DataNeeded(String name, Properties properties, ArrayList<String> messages) {
        this.name = name;
        loadProperties(name, properties);
        summaryInstructionLines.clear();
        summaryInstructionLines.addAll(messages);
    }

    public DataNeeded(String name, DataNeededType type, int id) {
        this.name = name;
        this.type = type;
        this.id = id;
    }

    private String handleMacro(String param) {
        return param;
    }

    private List<String> loadList(String name, String listName, Properties properties) {
        List<String> list = new ArrayList<String>();

        for (int index = 1; ; index++) {
            String param = properties.getProperty(name + "." + listName + "." + index);
            if (param != null) {
                param = handleMacro(param);
                list.add(param);
                logger.debug(name + "." + listName + "." + index + " = " + param);
            } else {
                break;
            }
        }
        return list;
    }

    public int getEnableSecurity() {
        return enableSecurity;
    }

    private void loadProperties(String name, Properties properties) {
        try {

            type = DataNeededType.valueOf(properties.getProperty(name + ".Type"));
            logger.debug("Type = " + type);
            id = Integer.parseInt(properties.getProperty(name + ".Id"));
            logger.debug("Id = " + id);
            mode = Integer.parseInt(properties.getProperty(name + ".Mode"));
            logger.debug("Mode = " + mode);
            tableName = properties.getProperty(name + ".TableName");
            timeout = Integer.parseInt(properties.getProperty(name + ".Timeout", "60000"));
            logger.debug("Timeout = " + timeout);

            topCaptionLines = loadList(name, "TopCaption", properties);
            topCaptionSubstitutionsLines = loadList(name, "TopCaptionSubstitutions", properties);
            topCaptionTableNameLines = loadList(name, "TopCaptionTableName", properties);
            topCaptionDestinationLines = loadList(name, "TopCaptionDestination", properties);
            summaryInstructionLines = loadList(name, "SummaryInstruction", properties);
            summaryInstructionSubstitutionsLines = loadList(name, "SummaryInstructionSubstitutions", properties);
            summaryInstructionTableNameLines = loadList(name, "SummaryInstructionTableName", properties);
            summaryInstructionDestinationLines = loadList(name, "SummaryInstructionDestination", properties);
            bottomCaptionLines = loadList(name, "BottomCaption", properties);
            bottomCaptionSubstitutionsLines = loadList(name, "BottomCaptionSubstitutions", properties);
            bottomCaptionTableNameLines = loadList(name, "BottomCaptionTableName", properties);
            bottomCaptionDestinationLines = loadList(name, "BottomCaptionDestination", properties);
            detailedInstructionLines = loadList(name, "DetailedInstruction", properties);
            detailedInstructionSubstitutionsLines = loadList(name, "DetailedInstructionSubstitutions", properties);
            detailedInstructionTableNameLines = loadList(name, "DetailedInstructionTableName", properties);
            detailedInstructionDestinationLines = loadList(name, "DetailedInstructionDestination", properties);
            // AMZ-FLANE#BEG
            buttonDataLines = loadList(name, "ButtonData", properties);
            buttonTextLines = loadList(name, "ButtonText", properties);

            try {
                enableSecurity = Integer.parseInt(properties.getProperty(name + ".EnableSecurity"));
                logger.debug("enableSecurity = " + enableSecurity);
            } catch (Exception e) {
                logger.warn("Could not get optional param enableSecurity: " + e.getMessage());
            }
            // AMZ-FLANE#END

            try {
                enableScanner = Integer.parseInt(properties.getProperty(name + ".EnableScanner"));
                logger.debug("EnableScanner = " + enableScanner);
            } catch (Exception e) {
                logger.warn("Could not get optional param EnableScanner: " + e.getMessage());
            }
            /*
            try {
                enableMsr = Integer.parseInt(properties.getProperty(name + ".EnableMsr"));
                logger.debug("EnableMsr = " + enableMsr);
            } catch (Exception e) {
                logger.warn("Could not get optional param EnableMsr: ", e);
            }
            try {
                scannerDataFormat = Integer.parseInt(properties.getProperty(name + ".ScannerDataFormat"));
                logger.debug("ScannerDataFormat = " + scannerDataFormat);
            } catch (Exception e) {
                logger.warn("Could not get optional param ScannerDataFormat: ", e);
            }
            */
            try {
                keypad = Integer.parseInt(properties.getProperty(name + ".Keypad"));
                logger.debug("Keypad = " + keypad);
            } catch (Exception e) {
                logger.warn("Could not get optional param Keypad: " + e);
            }

            inputMask = properties.getProperty(name + ".InputMask");
            logger.debug("InputMask = " + inputMask);

            try {
                minInputLength = Integer.parseInt(properties.getProperty(name + ".MinInputLength"));
                logger.debug("MinInputLength = " + minInputLength);
            } catch (Exception e) {
                logger.warn("Could not get optional param MinInputLength: " + e);
            }

            try {
                hideTotal = Integer.parseInt(properties.getProperty(name + ".HideTotal"));
                logger.debug("HideTotal = " + hideTotal);
            } catch (Exception e) {
                logger.warn("Could not get optional param HideTotal: ", e);
            }

            soundFile = properties.getProperty(name + ".SoundFile");
            logger.debug("SoundFile = " + soundFile);
            try {
                hideTotal = Integer.parseInt(properties.getProperty(name + ".HideTotal"));
                logger.debug("HideTotal = " + hideTotal);
            } catch (Exception e) {
                logger.warn("Could not get optional param HideTotal: ", e);
            }
            try {
                hideHelp = Integer.parseInt(properties.getProperty(name + ".HideHelp"));
                logger.debug("HideHelp = " + hideHelp);
            } catch (Exception e) {
                logger.warn("Could not get optional param HideHelp: ", e);
            }
            try {
                hideGoBack = Integer.parseInt(properties.getProperty(name + ".HideGoBack"));
                logger.debug("HideGoBack = " + hideGoBack);
            } catch (Exception e) {
                logger.warn("Could not get optional param HideGoBack: ", e);
            }
            try {
                hideInput = Integer.parseInt(properties.getProperty(name + ".HideInput"));
                logger.debug("HideInput = " + hideInput);
            } catch (Exception e) {
                logger.warn("Could not get optional param HideInput: ", e);
            }
        } catch (Exception e) {
            logger.error("Error: " + e);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataNeededType getType() {
        return type;
    }

    public void setType(DataNeededType type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getTopCaptionLines() {
        return topCaptionLines;
    }

    public void setTopCaptionLines(List<String> topCaptionLines) {
        this.topCaptionLines = topCaptionLines;
    }

    public List<String> getTopCaptionSubstitutionsLines() {
        return topCaptionSubstitutionsLines;
    }

    public void setTopCaptionSubstitutionsLines(List<String> topCaptionSubstitutionsLines) {
        this.topCaptionSubstitutionsLines = topCaptionSubstitutionsLines;
    }

    public List<String> getTopCaptionTableNameLines() {
        return topCaptionTableNameLines;
    }

    public void setTopCaptionTableNameLines(List<String> topCaptionTableNameLines) {
        this.topCaptionTableNameLines = topCaptionTableNameLines;
    }

    public List<String> getTopCaptionDestinationLines() {
        return topCaptionDestinationLines;
    }

    public void setTopCaptionDestinationLines(List<String> topCaptionDestinationLines) {
        this.topCaptionDestinationLines = topCaptionDestinationLines;
    }

    public List<String> getSummaryInstructionLines() {
        return summaryInstructionLines;
    }

    public void setSummaryInstructionLines(List<String> summaryInstructionLines) {
        this.summaryInstructionLines = summaryInstructionLines;
    }

    public List<String> getSummaryInstructionSubstitutionsLines() {
        return summaryInstructionSubstitutionsLines;
    }

    public void setSummaryInstructionSubstitutionsLines(List<String> summaryInstructionSubstitutionsLines) {
        this.summaryInstructionSubstitutionsLines = summaryInstructionSubstitutionsLines;
    }

    public List<String> getSummaryInstructionTableNameLines() {
        return summaryInstructionTableNameLines;
    }

    public void setSummaryInstructionTableNameLines(List<String> summaryInstructionTableNameLines) {
        this.summaryInstructionTableNameLines = summaryInstructionTableNameLines;
    }

    public List<String> getSummaryInstructionDestinationLines() {
        return summaryInstructionDestinationLines;
    }

    public void setSummaryInstructionDestinationLines(List<String> summaryInstructionDestinationLines) {
        this.summaryInstructionDestinationLines = summaryInstructionDestinationLines;
    }

    public List<String> getBottomCaptionLines() {
        return bottomCaptionLines;
    }

    public void setBottomCaptionLines(List<String> bottomCaptionLines) {
        this.bottomCaptionLines = bottomCaptionLines;
    }

    public List<String> getBottomCaptionSubstitutionsLines() {
        return bottomCaptionSubstitutionsLines;
    }

    public void setBottomCaptionSubstitutionsLines(List<String> bottomCaptionSubstitutionsLines) {
        this.bottomCaptionSubstitutionsLines = bottomCaptionSubstitutionsLines;
    }

    public List<String> getBottomCaptionTableNameLines() {
        return bottomCaptionTableNameLines;
    }

    public void setBottomCaptionTableNameLines(List<String> bottomCaptionTableNameLines) {
        this.bottomCaptionTableNameLines = bottomCaptionTableNameLines;
    }

    public List<String> getBottomCaptionDestinationLines() {
        return bottomCaptionDestinationLines;
    }

    public void setBottomCaptionDestinationLines(List<String> bottomCaptionDestinationLines) {
        this.bottomCaptionDestinationLines = bottomCaptionDestinationLines;
    }

    public List<String> getDetailedInstructionLines() {
        return detailedInstructionLines;
    }

    public void setDetailedInstructionLines(List<String> detailedInstructionLines) {
        this.detailedInstructionLines = detailedInstructionLines;
    }

    public List<String> getDetailedInstructionSubstitutionsLines() {
        return detailedInstructionSubstitutionsLines;
    }

    public void setDetailedInstructionSubstitutionsLines(List<String> detailedInstructionSubstitutionsLines) {
        this.detailedInstructionSubstitutionsLines = detailedInstructionSubstitutionsLines;
    }

    public List<String> getDetailedInstructionTableNameLines() {
        return detailedInstructionTableNameLines;
    }

    public void setDetailedInstructionTableNameLines(List<String> detailedInstructionTableNameLines) {
        this.detailedInstructionTableNameLines = detailedInstructionTableNameLines;
    }

    public List<String> getDetailedInstructionDestinationLines() {
        return detailedInstructionDestinationLines;
    }

    public void setDetailedInstructionDestinationLines(List<String> detailedInstructionDestinationLines) {
        this.detailedInstructionDestinationLines = detailedInstructionDestinationLines;
    }

    // AMZ-FLANE#BEG
    public List<String> getButtonDataLines() {
        return buttonDataLines;
    }

    public List<String> getButtonTextLines() {
        return buttonTextLines;
    }

    // AMZ-FLANE#END

    public Integer getEnableMsr() {
        return enableMsr;
    }

    public void setEnableMsr(Integer enableMsr) {
        this.enableMsr = enableMsr;
    }

    public Integer getEnableScanner() {
        return enableScanner;
    }

    public void setEnableScanner(Integer enableScanner) {
        this.enableScanner = enableScanner;
    }

    public Integer getScannerDataFormat() {
        return scannerDataFormat;
    }

    public void setScannerDataFormat(Integer scannerDataFormat) {
        this.scannerDataFormat = scannerDataFormat;
    }

    public Integer getKeypad() {
        return keypad;
    }

    public void setKeypad(Integer keypad) {
        this.keypad = keypad;
    }

    public String getInputMask() {
        return inputMask;
    }

    public void setInputMask(String inputMask) {
        this.inputMask = inputMask;
    }

    public Integer getMinInputLength() {
        return minInputLength;
    }

    public void setMinInputLength(Integer minInputLength) {
        this.minInputLength = minInputLength;
    }

    public String getSoundFile() {
        return soundFile;
    }

    public void setSoundFile(String soundFile) {
        this.soundFile = soundFile;
    }

    public Integer getHideTotal() {
        return hideTotal;
    }

    public void setHideTotal(Integer hideTotal) {
        this.hideTotal = hideTotal;
    }

    public Integer getHideHelp() {
        return hideHelp;
    }

    public void setHideHelp(Integer hideHelp) {
        this.hideHelp = hideHelp;
    }

    public Integer getHideGoBack() {
        return hideGoBack;
    }

    public void setHideGoBack(Integer hideGoBack) {
        this.hideGoBack = hideGoBack;
    }

    public Integer getHideInput() {
        return hideInput;
    }

    public void setHideInput(Integer hideInput) {
        this.hideInput = hideInput;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
