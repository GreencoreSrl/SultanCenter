package com.ncr.ssco.communication.requestprocessors;

public final class ProcessorConstants {
    private ProcessorConstants() {}

    // Actions Constants
    /*public static final String ENTER = "000D";
    public static final String AUTHO = "000A";
    public static final String CLEAR = "000B";
    public static final String SLIP = "000F";
    public static final String SCANNER = "4F4F";
    public static final String MSR = "4D4D";
    public static final String SIGNOFF = "00A2";
    public static final String VOID = "00B9";
    public static final String SUSPEND = "001D";
    public static final String ABORT = "001B";
    public static final String TOTAL = "0019";
    public static final String MENU_REGIS = "00F2";
    public static final String PICKUP = "00AB";
    public static final String LOAN = "00AA";
    public static final String LOYALTY_CARD = "00A4";
    public static final String EMPLOYEE = "00A3";
    public static final String QUANTITY = "0010";
    public static final String EOD = "00F1";*/

    // Messages Constants
    public static final String INITIALIZE = "Initialize";
    public static final String REQUEST_POS_STATE = "RequestPosState";
    public static final String VALIDATE_USER_ID = "ValidateUserId";
    public static final String SIGN_ON = "SignOn";
    public static final String SIGN_OFF = "SignOff";
    public static final String SHUTTING_DOWN = "ShuttingDown";
    public static final String SUSPEND_TRANSACTION = "SuspendTransaction";
    public static final String VOID_TRANSACTION = "VoidTransaction";
    public static final String PRINT_REPORT = "PrintReport";
    public static final String VOID_ITEM = "VoidItem";
    public static final String ITEM = "Item";
    public static final String COUPON = "Coupon";
    public static final String TENDER = "Tender";
    public static final String LOAN_CONST = "Loan";
    public static final String BALANCE_CONST = "Balance";
    public static final String PICKUP_CONST = "Pickup";
    public static final String LANGUAGE = "Language";
    public static final String LOYALTY_CARD = "LoyaltyCard";
    public static final String AIR_MILES = "AirMiles";
    public static final String ENTER_TENDER_MODE = "EnterTenderMode";
    public static final String EXIT_TENDER_MODE = "ExitTenderMode";
    public static final String DATA_NEEDED_REPLY = "DataNeededReply";
    public static final String RELOAD_OPTIONS = "ReloadOptions";
    public static final String CUSTOM_ASSIST_MODE = "CustomAssistMode"; // AMZ-FLANE#ADD
    public static final String CUSTOM_ASSIST_TENDER_MODE = "CustomAssistModeTender";
    public static final String REQUEST_TOTAL = "RequestTotal";
    public static final String COMMAND = "Command";
    public static final String ENTER_ASSIST_MODE = "EnterAssistMode";
    public static final String EXIT_ASSIST_MODE = "ExitAssistMode";
    public static final String EFT_SETTLE = "EcrSettle";   //EFT-SETTLE-CGA#A
    public static final String ENTER_TRAINING_MODE = "EnterTrainingMode";
    public static final String EXIT_TRAINING_MODE = "ExitTrainingMode";

    // Choices Constants
    public static final String ENTER_EXIT_TRAINING = "16";

    // Error Codes Constants
    public static final int OK = 0;
    public static final int ERROR_1_NOT_FOUND = 27;
    public static final int ERROR_2_NOT_FOUND = 16;
    public static final int ERROR_PRINT = 17;

    public static final int INTERFACE_TENDER_ERROR = 9001;

    //DMA-UPB_NOT_BAGGED#A BEG
    /*valori per:
        RequiresSecurityBagging
        RequiresSubsCheck
     */
    public static final int  SECURITY_TO_DECIDE = -1;
    public static final int  NO = 2;
    public static final int  YES = 3;
    public static final int  NOWEIGHT= 4;
    public static final int  BYPASS_BAGGING_PROMPT = 5;
    //DMA-UPB_NOT_BAGGED#A END
}
