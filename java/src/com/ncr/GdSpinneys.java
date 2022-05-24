package com.ncr;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ncr.ssco.communication.manager.SscoPosManager;
import com.ncr.wsLoyalty.data.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GdSpinneys extends Action {
    private static final Logger logger = Logger.getLogger(GdSpinneys.class);
    private static GdSpinneys instance = null;
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
    private StringBuffer phoneNumber = new StringBuffer("");
    private WsLoyaltyService service = WsLoyaltyService.getInstance();
    private int functionLoyalty = -1;
    private List<Coupon> coupons = new ArrayList<Coupon>();
    private String msgError = "";

    private class Coupon {
        private String code;
        private boolean triggered;

        public Coupon(String code, boolean triggered) {
            this.code = code;
            this.triggered = triggered;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public boolean isTriggered() {
            return triggered;
        }

        public void setTriggered(boolean triggered) {
            this.triggered = triggered;
        }
    }

    public static GdSpinneys getInstance() {
        if (instance == null)
            instance = new GdSpinneys();

        return instance;
    }

    public int action0(int spec) {
        logger.debug("Enter spec: " + spec);

        if (tra.isActive()) {
            return 7;
        }
        int sts = 0;
        phoneNumber = new StringBuffer("");
        String cardNumber = "";
        service = WsLoyaltyService.getInstance();

        for (int i = 0; i < service.getCustomerRegistrationRetries(); i++) {
            //inserisco la tessera
            dspLine.init(Mnemo.getMenu(84)).show(1); //CUSTOMER REGIST
            boolean result = readCard(Mnemo.getMenu(23), 83, 10, 13, 0, 0);

            if (!result) {
                return 0;
            }

            dspLine.init(Mnemo.getText(81)).show(1); //please wait
            sts = service.checkBadCard(input.pb);
            dspLine.init("").show(1);

            if (sts > 0) {
                break;
            }

            cardNumber = input.pb;

            //inserisco il mobile
            sts = insertMobile("");

            if (sts == 0) {
                String url = service.getCustomerRegistrationWsBaseAddress();

                sts = customerOperation(url, new LoyaltyCustomer(cardNumber, phoneNumber.toString()), 1, i);

                if (sts == 116) {
                    continue;
                }
            }
            break;
        }

        dspLine.init("").show(1);
        if (sts == 0 || sts == 116) {
            if (sts == 116) {
                panel.clearLink(Mnemo.getInfo(sts), 1);
            }
            Itmdc.IDC_write('F', 0, 0, tra.number, tra.cnt, tra.amt);
            tblWrite();
            return GdTrans.tra_clear();
        }
        return sts;
    }

    static boolean readCard(String hdr, int txt, int min, int max, int msk, int dec) {
        for (int sts; ; panel.clearLink(Mnemo.getInfo(sts), 1)) {
            ModDlg dlg = new ModDlg(hdr);
            dlg.block = false;
            dlg.input.prompt = Mnemo.getText(txt);
            oplToggle(2, hdr);
            input.reset("");
            input.init(0x00, max, msk, dec);
            dlg.show("NBR");
            oplToggle(0, null);

            if (input.key == 0)
                input.key = input.CLEAR;
            if (input.key == input.CLEAR) {
                dspLine.init("").show(1);

                return false;
            }
            if (input.num < 1 || (input.key != input.ENTER && input.key != 0x4f4f))
                sts = 5;
            else
                sts = input.adjust(input.pnt);
            if (sts != 0)
                continue;
            if (input.num >= min)
                return true;
            sts = 3;
        }
    }

    // Card Replacement
    public int action1(int spec) {
        logger.debug("Enter spec: " + spec);
        if (tra.isActive()) {
            return 7;
        }

        int sts = 0;
        String cardNumber = "";
        phoneNumber = new StringBuffer("");
        service = WsLoyaltyService.getInstance();

        dspLine.init(Mnemo.getMenu(89)).show(1); //Card Replacement
        sts = insertMobile("");

        if (sts != 0) {
            return 8;
        }

        if (sts == 0) {
            String request = service.getIdentificationByPhoneWsBaseAddress() + "?phoneNumber=" + phoneNumber.toString();
            sts = service.identificationCustomer(request);
            logger.debug("identificationByPhone: " + sts);
        }

        if (sts == 0) {
            if (service.getLoyaltyCustomer().isActive()) {
                return 113;
            }
            //visualizzo il vecchio cardId e permetto l'inserimento del nuovo
            //String oldCardNumber = service.getOldCardNumber();

            for (int i = 0; i < service.getReplaceCardRetries(); i++) {
                dspLine.init(Mnemo.getText(93).trim() + " " + service.getLoyaltyCustomer().getCardCode()).show(1);  //OLD CARD
                boolean result = readCard(Mnemo.getMenu(89), 94, 10, 13, 0, 0);
                logger.debug("result insert new card id: " + result);

                if (!result) {
                    sts = 0;
                    break;
                } else {
                    //invio al service.checkBadCard la carta inserita
                    cardNumber = input.pb;

                    dspLine.init(Mnemo.getText(81)).show(1); //please wait
                    sts = service.checkBadCard(cardNumber);  //GET
                    dspLine.init("").show(1);

                    logger.debug("result checkBadCard: " + sts);

                    if (sts == 0) {
                        String url = service.getReplaceCardWsBaseAddress();
                        service.getLoyaltyCustomer().setCardCode(cardNumber);
                        sts = customerOperation(url, service.getLoyaltyCustomer(), 3, i);

                        if (sts == 116) {
                            continue;
                        }
                    }
                }

                break;
            }
        }

        dspLine.init("").show(1);
        if (sts == 0 || sts == 116) {
            if (sts == 116) {
                panel.clearLink(Mnemo.getInfo(sts), 1);
            }
            Itmdc.IDC_write('F', 0, 0, tra.number, tra.cnt, tra.amt);
            tblWrite();
            return GdTrans.tra_clear();
        }
        logger.debug("Exit returning " + sts);
        return sts;
    }

    // Update Mobile Number
    public int action2(int spec) {
        logger.debug("Enter spec: " + spec);
        if (tra.isActive()) {
            return 7;
        }

        int sts = 0;
        String cardNumber = "";
        phoneNumber = new StringBuffer("");
        service = WsLoyaltyService.getInstance();
        String customerCard = "";

        for (int i = 0; i < service.getReplaceMobileRetries(); i++) {
            //inserisco la carta
            dspLine.init(Mnemo.getMenu(90)).show(1);  //UPDATE PHONE NUMBER
            boolean result = readCard(Mnemo.getMenu(23), 83, 10, 13, 0, 0); //CUSTOMER CARD?
            logger.debug("result insert customer card: " + result);

            if (!result) {
                sts = 0;
            } else {
                cardNumber = input.pb;
                dspLine.init(Mnemo.getText(81)).show(1); //please wait

                String request = service.getIdentificationByCardWsBaseAddress() + "?cardCode=" + input.pb;
                sts = service.identificationCustomer(request);
                logger.debug("result identificationByCard: " + sts);

                dspLine.init("").show(1); //please wait

                if (sts == 0) {
                    if (!service.getLoyaltyCustomer().isActive()) {
                        return 111;
                    }

                    //inserisco nuovo numero
                    sts = insertMobile(service.getLoyaltyCustomer().getPhoneNumber());
                    logger.debug("result insertMobile: " + sts);

                    //invio al service il vecchio numero, quello nuovo e la carta cliente
                    if (sts == 0) {
                        String url = service.getReplaceMobileWsBaseAddress();

                        service.getLoyaltyCustomer().setPhoneNumber(phoneNumber.toString());
                        sts = customerOperation(url, service.getLoyaltyCustomer(), 2, i);
                        if (sts == 116) {
                            continue;
                        }
                    }
                }
            }
            break;
        }

        dspLine.init("").show(1);
        if (sts == 0 || sts == 116) {
            if (sts == 116) {
                panel.clearLink(Mnemo.getInfo(sts), 1);
            }
            Itmdc.IDC_write('F', 0, 0, tra.number, tra.cnt, tra.amt);
            tblWrite();
            return GdTrans.tra_clear();
        }

        return sts;
    }

    //Identification by customer card
    public int action3(int spec) { //GET
        int sts = 0;
        if (tra.isActive() && (input.lck & 0x14) <= 0)
            return 1;
        boolean result = true;

        if (input.num == 0) {
            dspLine.init(Mnemo.getMenu(93)).show(1);
            result = readCard(Mnemo.getMenu(23), 83, 10, 13, 0, 0); //CUSTOMER CARD?

            if (input.key == ConIo.ENTER && (input.lck & 0x14) <= 0)
                return 1;
        }

        if (result) {
            String request = service.getIdentificationByCardWsBaseAddress() + "?cardCode=" + input.pb;
            sts = handleFunctionalityGet(request, false);
        }

        dspLine.init("").show(1);
        if (sts == 0 && SscoPosManager.getInstance().isUsed()) {
            SscoPosManager.getInstance().loyaltyResponse();
        }
        return sts;
    }

    private String buildPhoneNumber(String input) {
        for (PhoneCode phoneCode : WsLoyaltyService.getInstance().getCountryCodes()) {
            String code = phoneCode.getPhoneCode();
            if (input.startsWith(code)) {
                return code + "-" + input.substring(code.length());
            }
        }
        return "";
    }

    //Identification by mobile
    public int action4(int spec) { //GET
        int sts = 0;
        if (tra.isActive() && (input.lck & 0x14) <= 0)
            return 1;

        phoneNumber = new StringBuffer("");
        service = WsLoyaltyService.getInstance();

        if (input.num == 0) {
            //inserisco il mobile
            dspLine.init(Mnemo.getMenu(94)).show(1);
            sts = insertMobile("");
        } else {
            phoneNumber = new StringBuffer(buildPhoneNumber(input.pb));
        }

        if (sts == 0) {
            String request = service.getIdentificationByPhoneWsBaseAddress() + "?phoneNumber=" + phoneNumber.toString();
            sts = handleFunctionalityGet(request, true);
        }

        dspLine.init("").show(1);
        if (sts == 0 && SscoPosManager.getInstance().isUsed()) {
            SscoPosManager.getInstance().loyaltyResponse();
        }
        return sts;
    }


    private void fillCustomerData() {
        logger.debug("Enter");

        cus.setNumber(service.getLoyaltyCustomer().getCardCode());
        cus.setMobile(service.getLoyaltyCustomer().getPhoneNumber());
        cus.setName(service.getLoyaltyCustomer().getFirstName());
        cus.setNam2(service.getLoyaltyCustomer().getLastName());
        cus.setCusId(service.getLoyaltyCustomer().getCustomerCode());

        cus.setAdrs("");
        cus.setCity("");
        cus.setDtbl("");
        cus.setFiscalId(null);
        cus.setPnt(0);

        logger.debug("Exit");
    }

    private ResponseMessage checkResultJson(String responseJson) {
        ResponseMessage responseCustomer = new ResponseMessage();

        try {
            responseCustomer = gson.fromJson(responseJson, ResponseMessage.class);
        } catch (Exception e) {
            logger.error("Exception checkResultJson: " + e.getMessage());
        }

        return responseCustomer;
    }

    private String writeRequestJson(LoyaltyCustomer customer) {
        logger.debug("ENTER");
        if (customer != null) {
            logger.info("cardNumber: " + customer.getCardCode());
            logger.info("mobile: " + customer.getPhoneNumber());
        }


        String request = "";

        try {
            request = gson.toJson(customer).toString();
        } catch (Exception e) {
            logger.info("Exception: " + e.getMessage());
        }

        logger.debug("EXIT - return request: " + request);
        return request;
    }

    private int insertMobile(String oldPhoneNumber) {
        logger.debug("Enter insertMobile");
        logger.debug("oldPhoneNumber: " + oldPhoneNumber);

        int sts = 0;

        int code = selectPhoneCode(service.getCountryCodes(), 4);
        logger.debug("country code: " + code);

        if (code < 0 || code > 9999) {
            sts = 8;
        } else {
            phoneNumber = new StringBuffer(String.valueOf(code));

            int min = 0;
            int max = 0;
            boolean trimLeadingZero = false;

            for (PhoneCode phCode : service.getCountryCodes()) {
                if (phCode.getPhoneCode().equals(String.valueOf(code))) {
                    min = phCode.getRangeMin();
                    max = phCode.getRangeMax();
                    trimLeadingZero = phCode.isTrimLeadingZero();
                    break;
                }
            }
            logger.debug("min: " + min);
            logger.debug("max: " + max);
            logger.debug("trimLeadingZero: " + trimLeadingZero);

            if (oldPhoneNumber == null || oldPhoneNumber.equals("")) {
                dspLine.init("").show(1);
            } else {
                dspLine.init(Mnemo.getText(95).trim() + " " + oldPhoneNumber).show(1);
            }

            boolean result = acceptNbr(Mnemo.getMenu(87), 92, min, max, max, 0);
            logger.debug("result insert phone number: " + result);

            dspLine.init("").show(1);

            if (!result) {
                sts = 8;
            } else {
                String number = input.pb;
                if (trimLeadingZero && number.charAt(0) == '0') {
                    number = number.substring(1);
                }
                phoneNumber.append("-" + number);
                logger.debug("full phoneNumber: " + phoneNumber);
            }
        }

        logger.debug("Exit insertMobile - return " + sts);
        return sts;
    }

    private int handleFunctionalityGet(String request, boolean rqsByPhone) {
        int sts = 0;

        if (cus.getNumber() != null && cus.getNumber().length() > 0) {
            return 117;
        }

        dspLine.init(Mnemo.getText(81)).show(1); //please wait
        sts = service.identificationCustomer(request);
        logger.debug("result identification: " + sts);

        if (!rqsByPhone && !service.isInactiveCustomerEnabled()) {
            LoyaltyCustomer customer = service.getLoyaltyCustomer();
            if (customer != null && !customer.isActive()) {
                return 111;
            }
        }
        dspLine.init("").show(1);

        if (sts == 0) {
            fillCustomerData();

            if (rqsByPhone) {
                input.pb = cus.getNumber();
                input.num = cus.getNumber().length();
            }

            sts = GdCusto.getInstance().action1(9000);

            if (service.getLoyaltyCustomer().getPromovars() != null) {
                for (Promovar pv : service.getLoyaltyCustomer().getPromovars()) {
                    if (pv.getCode() != null && !pv.getCode().equals("")) {
                        Promo.setPromovar(Long.parseLong(pv.getCode()), pv.getValue());
                    }
                }
            }

            functionLoyalty = 0;
            Itmdc.IDC_write('c', 0, tra.spf3, tra.number, tra.cnt, 0l);
            functionLoyalty = -1;
        }

        return sts;
    }

    private void writeCustomerIdc(LoyaltyCustomer customer, int mode, int offline) {
        cus.setNumber(customer.getCardCode());
        cus.setMobile(phoneNumber.toString());
        cus.setCusId(service.getLoyaltyCustomer().getCustomerCode());

        functionLoyalty = mode;

        Itmdc.IDC_write('c', 0, offline, tra.number, tra.cnt, 0l);

        functionLoyalty = -1;
        cus.setNumber("");
        cus.setMobile("");
        cus.setCusId("");
    }

    private int couponOperation(String code, String type, int retries) {
        if (SscoPosManager.getInstance().isTestEnvironment()) {
            return 0;
        }
        RemoteCouponOperationRequest request = new RemoteCouponOperationRequest(type, String.valueOf(ctl.tran), cus.getNumber(), String.valueOf(ctl.sto_nbr), String.valueOf(ctl.reg_nbr), new Date(), code);

        String url = service.getCouponWsBaseAddress();

        dspLine.init(Mnemo.getText(81)).show(1);
        int ris = service.sendPostRequest(url, gson.toJson(request));
        dspLine.init("").show(1);

        if (ris == 0) {
            ResponseMessage response = checkResultJson(service.getResponseJson());
            if (response.getCode() != 0) {
                if (retries < service.getCouponOperationRetries()) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        logger.error("Error: ", e);
                    }

                    logger.debug("Error description: " + response.getDescription());
                    msgError = response.getDescription();   //SPINNEYS-13032018-CGA#A

                    return 120;
                } else {
                    return response.getCode() % 100 == 1 ? 118 : 119;
                }
            }
        }
        return ris;
    }

    private int customerOperation(String url, LoyaltyCustomer customer, int mode, int retries) {
        String requestJson = writeRequestJson(customer);

        dspLine.init(Mnemo.getText(81)).show(1); //please wait
        int ris = service.sendPostRequest(url, requestJson);
        dspLine.init("").show(1); //please waitwriteRequestJson

        if (ris == 0) { //nessun errore di comunicazione
            //controllo la risposta del Server
            ResponseMessage registration = checkResultJson(service.getResponseJson());
            if (registration.getCode() == 0) {
                writeCustomerIdc(customer, mode, 0);
                panel.clearLink(Mnemo.getMenu(95), 1);
            } else { //errore dal server, lo visualizzo
                int choose = panel.clearLink(registration.getDescription() + " " + Mnemo.getInfo(110).trim(), 3);
                input.reset("");

                if (choose == 1) { //ESC
                    return registration.getCode() % 100 == 1 ? 114 : 115;
                } else { //retry
                    if (retries == service.getCustomerRegistrationRetries() - 1) {
                        writeCustomerIdc(customer, mode, 1);
                    }
                    return 116;
                }
            }
        }

        return ris;
    }

    public int getFunctionLoyalty() {
        logger.debug("EnterExit functionLoyalty: " + functionLoyalty);
        return functionLoyalty;
    }

    private int selectPhoneCode(List<PhoneCode> codesList, int len) {
        logger.debug("Enter codeList.size: " + codesList.size());

        int code = 0;
        SelDlg dlg = new SelDlg(Mnemo.getText(22));

        for (PhoneCode phoneCode : codesList) {
            dlg.add(8, phoneCode.getPhoneCode(), " " + phoneCode.getCountry());
        }

        input.reset("");
        input.prompt = Mnemo.getMenu(91); //INTERNATIONAL CODE
        input.init(0x00, len, len, 0);
        input.key = input.CLEAR;

        dlg.show("MNU");

        if (dlg.code > 0)
            return dlg.code;
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

    public int handleCoupon(Itemdata ptr) {
        int sts = 0;

        if (service.isWsLoyaltyEnabled() && isCoupon(ptr)) {
            sts = 120;
            String code = ptr.number.trim();
            ptr.number = "          " + ptr.number.trim().substring(0, 6);

            if (ptr.qty > 1) {
                sts = 7;
            } else {
                if ((ptr.spf1 & M_VOID) > 0) {
                    removeCoupon(new Coupon(code, false));
                    sts = 0;
                } else {
                    int retries = 0;
                    while (sts == 120 && retries < service.getCouponOperationRetries()) {
                        sts = couponOperation(code, "R", retries++);
                    }
                    if (sts == 0) {
                        ptr.coupon = true;
                        coupons.add(new Coupon(code, false));
                    }
                }
            }
        }
        return sts;
    }

    private boolean isCoupon(Itemdata ptr) {
        String input = ptr.number.trim();
        for (CouponCode couponCode : WsLoyaltyService.getInstance().getCouponCodes()) {
            if (input.startsWith(couponCode.getCode()) && input.length() >= couponCode.getRangeMin() && input.length() <= couponCode.getRangeMax()) {
                return true;
            }
        }
        return false;
    }

    private void removeCoupon(Coupon removed) {
        for (Coupon coupon : coupons) {
            if (coupon.getCode().equals(removed.getCode())) {
                coupons.remove(coupon);
                break;
            }
        }
    }

    public void checkCoupon(String udd, boolean reversalFlag) {
        for (Coupon coupon : coupons) {
            if (coupon.getCode().startsWith(udd)) {
                coupon.setTriggered(!reversalFlag);
            }
        }
    }

    public int confirmCoupons(Transact tra) {
        int sts = 120;
        if (tra.mode == M_GROSS) {
            for (Coupon coupon : coupons) {
                if (coupon.isTriggered()) {
                    int retries = 0;
                    while (sts == 120 && retries < service.getCouponOperationRetries()) {
                        sts = couponOperation(coupon.getCode(), "C", retries++);
                    }
                }
            }
        }
        clearCoupons();
        return sts;
    }

    public String getMsgError() {
        logger.debug("EnterExit - return " + msgError);
        return msgError.length() > 20 ? msgError.substring(0, 17) + "" : msgError;
    }

    public void clearCoupons() {
        coupons.clear();
    }
}