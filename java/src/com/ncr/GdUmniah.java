package com.ncr;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ncr.umniah.data.*;
import org.apache.log4j.Logger;

import java.util.List;

public class GdUmniah extends Action {
    private static final Logger logger = Logger.getLogger(GdUmniah.class);
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();
    private static GdUmniah instance = null;
    private WsUmniahService wsUmniahService;
    private String rechargeType = "";
    private String[] choicesDenomination;
    private int amountAutoSellItem = 0;
    private boolean isAutoSellItem = false;
    private String rechargePin = "";
    private String transactionId = "";
    private String token = "";

    public static GdUmniah getInstance() {
        if (instance == null)
            instance = new GdUmniah();

        return instance;
    }

    private GdUmniah() {
        wsUmniahService = WsUmniahService.getInstance();
    }

    public String getRechargePin() {
        return rechargePin;
    }

    public String getTransactionId() {
        return transactionId;
    }

    private int selectRechargeDetails(List<OperandDetails> listDetails) {
        int result = 0;
        int i = 0;
        choicesDenomination = new String[listDetails.size()];

        for (OperandDetails detail : listDetails) {
            choicesDenomination[i] = detail.getOperand();
            i++;
        }

        //visualizzo lista tagli e prendo quello selezionato
        result = selectRecharge(choicesDenomination, 97);

        return result;
    }

    private int selectRecharge(String[] choices, int messageInd) {
        logger.debug("Enter codeList.size: " + choices.length);

        String text = " ";
        int code = 0;

        if (messageInd == 97) {
            text = " " + Mnemo.getText(97).trim() + " ";
        }

        SelDlg dlg = new SelDlg(Mnemo.getText(22));

        for (int index = 0; index < choices.length; index++) {
            dlg.add(8,  "" + index, text + choices[index]);
        }

        input.reset("");
        input.prompt = Mnemo.getMenu(messageInd).trim();
        input.init(0x00, 1, 1, 0);
        input.key = input.CLEAR;

        dlg.show("MNU");

        //if (dlg.code > 0)
            //return dlg.code;

        if (!input.pb.equals("")) {
            return Integer.parseInt(input.pb);
        }

        if (input.key == 0)
            input.key = input.CLEAR;
        if (input.num < 1 || input.key != input.ENTER)
            return -1;
        if ((code = input.adjust(input.pnt)) > 0)
            return code;

        code = input.scanNum(input.num);
        if (code < 1)
            return -1;

        logger.debug("Exit returning " + code);
        return code;
    }

    int action0(int spec) {
        logger.debug("Enter spec: " + spec);
        int res = 0;

        //controllo abilitazione funzionalità
        if (!wsUmniahService.isWsUmniahEnabled()) {
            return 7;
        }

        //lista presa dal properties
        String[] choicesType = wsUmniahService.getRechargeTypeArray();

        //visualizza lista e ritorna scelta
        int code = selectRecharge(choicesType, 96);

        //verifico scelta
        if (code < 0 || code > choicesType.length-1) {
            res = 8;
        } else {
            //UMNIA-20180109-CGA#A BEG
            //invio richiesta autenticazione
            AuthenticateResponse authenticateResponse = sendAuthenticate();

            if (authenticateResponse == null) {
                return 94;  //COMMUNIC. FAILURE
            } else {
                if (authenticateResponse.getError().getCode() != 0) {
                    String error = authenticateResponse.getError().getMessage().length() > 20
                            ? authenticateResponse.getError().getMessage().substring(0, 17) + ".."
                            : authenticateResponse.getError().getMessage();
                    panel.clearLink(error, 1);
                    return 0;
                } else {
                    token = authenticateResponse.getData().getToken();
                }
            }

            //invio scelta e ottengo tagli
            DenominationsResponse denominationsResponse = sendRechargeType(code);

            if (denominationsResponse == null) {
                return 94;  //COMMUNIC. FAILURE
            } else {
                if (!denominationsResponse.getErrorCode().equals("0")) {
                    String error = denominationsResponse.getErrorDesc().length() > 20
                            ? denominationsResponse.getErrorDesc().substring(0, 17) + ".."
                            : denominationsResponse.getErrorDesc();
                    panel.clearLink(error, 1);
                    return 0;
                } else {
                    //ottengo il taglio selezionato
                    code = selectRechargeDetails(denominationsResponse.getDetails());
                    if (code < 0 || code > choicesDenomination.length-1) {
                        return 8;
                    }

                    //invio la scelta al ws
                    GetPinResponse pinResponse = sendRechargeDetails(choicesDenomination[code]);

                    if (pinResponse == null) {
                        return 94;  //COMMUNIC. FAILURE
                    }

                    if (!pinResponse.getErrorCode().equals("0")) {
                        String error = pinResponse.getErrorDesc().length() > 20
                                ? pinResponse.getErrorDesc().substring(0, 17) + ".."
                                : pinResponse.getErrorDesc();
                        panel.clearLink(error, 1);

                        return 0;
                    }

                    int ris = automaticSellItem(pinResponse, choicesDenomination[code]);

                    if (ris == 0) {
                        rechargePin = pinResponse.getPin();
                        transactionId = pinResponse.getTransactionId();

                        Itmdc.IDC_write('a', 0, tra.spf3, tra.number, tra.cnt, 0l);
                        prtLine.init(Mnemo.getText(98).trim() + " " + mask(rechargePin)).book(1); //JRN
                        prtLine.init(Mnemo.getText(98).trim() + " " + rechargePin).book(2); //IDC
                    }

                    return ris;
                }
            }
            //UMNIA-20180109-CGA#A END
        }

        logger.debug("Exit returning: " + res);
        return res;
    }

    //UMNIA-20180109-CGA#A BEG
    public String getToken() {
        return token;
    }

    private static String mask(String serial) {
        if (serial == null || serial.length() == 0) {
            return "";
        }
        return serial.substring(0, serial.length() - 3) + "***";
    }

    private int automaticSellItem(GetPinResponse pinResponse, String denomination) {
        String ean = wsUmniahService.getAutoSellItem(rechargeType, denomination);
        amountAutoSellItem = calculateAmount(pinResponse);
        isAutoSellItem = true;

        if (cus.number != null && !cus.number.equals("")) {
            tra.number = cus.number;
        }

        /*if (!tra.isActive()) {
            GdRegis.set_tra_top();
        }*/

        int nxt = event.nxt;
        event.spc = input.msk = 0;

        input.prompt = "";
        input.reset(ean.trim());

        int success = group[5].action2(0);

        isAutoSellItem = false;

        /*if (sts != 0) {
            continue;
        }*/

        event.nxt = nxt;

        return success;
    }

    private int calculateAmount(GetPinResponse pinResponse) {
        int amount = 0;

        try {
            /*Double faceValue = Double.parseDouble(pinResponse.getFaceValue());
            Double salesTax = Double.parseDouble(pinResponse.getSalesTax());
            Double specialTax = Double.parseDouble(pinResponse.getSpecialTax());
            Double machineCharge = Double.parseDouble(pinResponse.getMachineCharge());
            Double machineTax = Double.parseDouble(pinResponse.getMachineTax());

            Double sum = faceValue + salesTax + specialTax + machineCharge + machineTax;
            String amt = String.valueOf(sum).replace(".", "");
            amount = Integer.parseInt(amt);
            */

            Double total = Double.parseDouble(pinResponse.getTotal());

            int pos = String.valueOf(total).indexOf(".");

            String amt = String.valueOf(total).replace(".", "");
            //amt = editNum(Integer.parseInt(amt), 5);

            amt = rightFill(amt, 4+pos, '0');
            amount = Integer.parseInt(amt);
        } catch (Exception e) {

        }

        return amount;
    }

    public int getAmountAutoSellItem() {
        return amountAutoSellItem;
    }

    public boolean isAutoSellItem() {
        return isAutoSellItem;
    }

    private DenominationsResponse sendRechargeType(int code) {
        logger.debug("Enter sendRechargeType: " + code);

        DenominationsResponse response = new DenominationsResponse();

        String billingNo = WsUmniahService.getInstance().getBillingNo();
        String apiKey = WsUmniahService.getInstance().getApiKey();
        rechargeType = WsUmniahService.getInstance().getRechargeTypeArray()[code];
        String language = WsUmniahService.getInstance().getLanguage();
        String channel = WsUmniahService.getInstance().getChannel();

        DenominationsRequest denomination = new DenominationsRequest(billingNo, apiKey, rechargeType, language, channel);

        try {
            response = WsUmniahService.getInstance().denominationsWsRequest(denomination);
        } catch (Exception e) {
            logger.info("Exception: " + e.getMessage());
        }

        logger.debug("Exit sendRechargeType");

        return response;
    }

    private AuthenticateResponse sendAuthenticate() {
        logger.debug("Enter sendAuthenticate");

        AuthenticateResponse response = new AuthenticateResponse();

        String username = WsUmniahService.getInstance().getWebServicesAuthenticationUsername();
        String password = WsUmniahService.getInstance().getWebServicesAuthenticationPassword();

        AuthenticateRequest authenticate = new AuthenticateRequest(username, password);

        try {
            response = WsUmniahService.getInstance().AuthWsRequest(authenticate);
        } catch (Exception e) {
            logger.info("Exception: " + e.getMessage());
        }

        logger.debug("Exit sendAuthenticate");

        return response;
    }

    private GetPinResponse sendRechargeDetails(String denomination) {
        logger.debug("Enter sendRechargeDetails: " + denomination);

        GetPinResponse response = new GetPinResponse();

        String apiKey = WsUmniahService.getInstance().getApiKey();
        String volume = denomination;
        //String volume = "1";  //forzatura

        GetPinRequest detail = new GetPinRequest(rechargeType, apiKey, volume);

        try {
            response = WsUmniahService.getInstance().pinWsRequest(detail);
        } catch (Exception e) {
            logger.info("Exception: " + e.getMessage());
        }

        logger.debug("Exit sendRechargeType");

        return response;
    }
    //UMNIA-20180109-CGA#A END

    /*
        if (response == null) {
            return 94;  //COMMUNIC. FAILURE
        }
     */
}
