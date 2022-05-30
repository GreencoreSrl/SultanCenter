package com.ncr;

import com.google.gson.*;
import com.ncr.ecommerce.data.Basket;
import com.ncr.ecommerce.data.HeartBeatTimerTask;
import com.ncr.ecommerce.data.Item;
import com.ncr.restclient.HttpClientFactory;
import com.ncr.restclient.IHttpClient;
import com.ncr.ssco.communication.entities.TenderType;
import com.ncr.ssco.communication.entities.TenderTypeEnum;
import com.ncr.ssco.communication.manager.TenderTypeManager;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.ws.rs.core.MultivaluedMap;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.Timer;

import static com.ncr.GdRegis.set_tra_top;

public class ECommerceManager extends Action {
    private static final String ECOMMERCE = "ECOMMERCE";
    private static final String READY = "Ready";
    private static final String CANCELED = "Canceled";
    private static ECommerceManager instance = null;
    private static final Logger logger = Logger.getLogger(ECommerceManager.class);
    private static final String ECOMMERCE_PROPERTIES = "conf/ecommerce.properties";
    private static final String ENABLED = "enabled";
    private static final String TENDER = "tender.";
    private static final String TENDER_DEFAULT = "tender.Default";
    private static final String PRINTERCOPIESNUMBER = "printedCopiesNumber";
    private static final String SHOW_POPUP = "showPopupOnPOS";
    private static final String FINALIZE = "finalize";
    private static final String NOT_SOLD_WARNING = "notSoldWarning";

    private Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    private Properties props = new Properties();
    private Boolean transactionStarted = false;
    private Basket basket;
    private int indexPrinterLine = 0;
    private boolean enabled = false;

    public static ECommerceManager getInstance() {
        if (instance == null) {
            instance = new ECommerceManager();
        }
        return instance;
    }

    private ECommerceManager() {
        loadProperties();
    }

    private void loadProperties() {
        try {
            props.load(new FileInputStream(ECOMMERCE_PROPERTIES));
            enabled = Boolean.parseBoolean(props.getProperty(ENABLED, "false"));
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
        }
    }

    public boolean checkForNewBasket(String terminal, String id) {
        logger.debug("checkForNewBasket - Enter");

        boolean result = false;

        if (isEnabled()) {
            basket = null;
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("authorize", "false");
            IHttpClient client = new HttpClientFactory().getClient(props);
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("type", "Sale");
            if (terminal != null) params.add("TerminalId", terminal);
            if (id != null) params.add("BasketId", id);

            try {
            /*String response = "{\n" +
                    "  \"BasketID\": \"12345\",\n" +
                    "  \"Status\": \"Processing\",\n" +
                    "  \"TerminalID\": \"001\",\n" +
                    "  \"CustomerID\": \"4444000000000\",\n"+
                    "  \"Type\": \"Sale\",\n" +
                    "  \"Receipt\": \"\",\n" +
                    "  \"TotalAmount\": 0.00,\n" +
                    "  \"Items\": [\n" +
                    "    {\"Code\": \"100520162\", \"Price\":  11.97,  \"Barcode\": \"9921463000004\",\n" +
                    "        \"Qty\": \"1\",\n" +
                    "        \"UnitPrice\":5.985}\n" +
                    "  ],\n " +
                    " \"TenderType\": \"Online\" " +
                    "}";*/
                String response = client.get(params).getEntity(String.class);
                logger.debug("Response: " + response);
                if (response != null) {
                    JsonObject jsonObject = (new JsonParser()).parse(response).getAsJsonObject();
                    basket = gson.fromJson(jsonObject, Basket.class);
                    transactionStarted = false;
                    result = true;
                }
            } catch (Exception e) {
                logger.error("Error: " + e.getMessage());
                basket = null;
            }
        }
        logger.debug("checkForNewBasket - Exit - result: " + result);
        return result;
    }

    public boolean updateBasket(Basket basket) {
        logger.debug("updateBasket- Enter");

        boolean result;
        result = false;

        try {
            if (basket != null) {
                IHttpClient client = new HttpClientFactory().getClient(props);
                MultivaluedMap<String, String> params = new MultivaluedMapImpl();
                params.add("fromSource", "POS");
                String response = client.post(params, gson.toJson(basket).toString()).getEntity(String.class);
                result = true;
            }
        } catch (Exception e) { //ECOMMERCE-TSC Handling all exceptions
            logger.error("Error: " + e.getMessage());
        }

        logger.debug("updateBasket - Exit - result: " + result);
        return result;
    }

    public Basket getBasket() {
        return basket;
    }

    public void setBasket(Basket basket) {
        this.basket = basket;
    }

    public void savePrinterInfo(String data) {

        if (isEnabled()) {
            if (transactionStarted && basket != null) {
                String prnOldInfo = basket.getReceipt() != null ? basket.getReceipt() : "";
                String prnInfo = prnOldInfo.concat(data);
                basket.setReceipt(prnInfo);
            }
        }
    }

    public Boolean mustPrinterInfo() {
        if (isEnabled()) {
            return false;
        }

        return true;
    }

    public String addPrinterInfo() {
        if (isEnabled()) {
            return props.getProperty("printedCopies." + indexPrinterLine + ".Text");
        }

        return "";
    }

    public int action0(int spec) {
        int sts = 0;

        if (spec == 1) {
            checkForNewBasket(editKey(ctl.reg_nbr, 3), input.pb.trim());
        }
        if (basket == null || basket.getItems() == null) {
            sts = 136;
        } else {
            sts = basketExplode();
        }

        return sts;
    }

    public int action1(int spec) {
        int sts = 0;
        enabled = !enabled;

        try {
            //Reloading the S_PLURCD.DAT file if is necessary...
            if (!enabled) {
                lRCD.open(null, "S_PLU" + "RCD" + ".DAT", 0);
            } else {
                if (lRCD != null) {
                    lRCD.close();
                }
            }
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
            sts = -1;
        }

        // Diplayed the Ecommerce status on screen...
        if (!enabled) {
            dspLine.init(Mnemo.getMenu(118)).show(1);
        } else {
            dspLine.init(Mnemo.getMenu(117)).show(1);
        }

        return sts;
    }

    private int basketExplode() {
        int sts = 0;
        logger.debug("basketExplode - Enter");
        startBasketTransaction();
        sellBasketItems();
        endBasketTransaction();
        logger.debug("basketExplode - Exit - sts: " + sts);
        return sts;
    }

    private void startBasketTransaction() {
        logger.debug("startBasketTransaction - Enter");

        if (basket.RETURN.equals(basket.getType())) {
            tra.spf1 = M_TRRTRN;
        }

        basket.reset();
        transactionStarted = true;
        tra.eCommerce = true;


        if (!tra.isActive())
            set_tra_top();
        prtTitle(108);

        prtLine.init(Mnemo.getMenu(basket.RETURN.equals(basket.getType()) ? 110 : 109))
                .onto(20, basket.getBasketID()).book(3);
        //int amt = basket.getTotalAmount() != null ? basket.getTotalAmount().multiply(new BigDecimal(tnd[0].dec)).intValue() : 0;
        int amt = basket.getTotalAmount() != null ? basket.getTotalAmount().multiply(new BigDecimal(Math.pow(10, tnd[0].dec))).intValue() : 0;
        Itmdc.IDC_write('B', sc_value(tra.spf1), 0, basket.getBasketID(), basket.getItems().size(), amt);
        Action.input.reset(basket.getCustomerID());
        int sts = group[2].action1(0);
        if (sts > 0) {
            logger.debug("CustomerID is not valid - sts: " + sts);
            tra.stat = 1;
        }

        logger.debug("startBasketTransaction - Exit");
    }

    private void endBasketTransaction() {
        logger.debug("endBasketTransaction - Enter");

        // If there is almost one NoSoldItem in the basket then transaction must be abort...
        if (basket.getNotSoldItems().size() > 0) {
            basket.setStatus(CANCELED);
            basket.setErrorCode(101);
            Action.input.lck = 0xFF;
            group[3].action5(M_CANCEL);
            logger.debug("endBasketTransaction - Transaction state canceled");
            return;
        }

        Action.input.reset(String.valueOf(Math.abs(tra.bal)));
        group[3].action3(0);

        // Barcode formatting
        String barcode = editNum(ctl.date, 4) + editKey(ctl.reg_nbr, 3) + editNum(ctl.tran, 4);
        basket.setBarcodeID(barcode);
        basket.setTransactionID(GdPsh.getUniqueTransactionId());
        //basket.setCustomerID(tra.number);
        basket.setEarnedLoyaltyPoints(tra.pnt);

        //basket.setTotalAmount((double)tra.bal / tnd[0].dec);
        basket.setTotalAmount(removeDecimals(new BigDecimal(tra.bal)));

        payWithTender();

        logger.debug("endBasketTransaction - Exit");
    }

    private void payWithTender() {
        logger.debug("payWithTender - Enter");

        itm.amt = itm.pos = tra.bal;
        if (basket.getTenderId() != null && basket.getTenderId().trim().length() > 0) {
            itm.tnd = Integer.parseInt(basket.getTenderId().trim());
        } else if (props.getProperty(TENDER + basket.getTenderType().trim()) != null) {
            logger.debug("payWithDefaultTender - Valid TenderType found");
            itm.tnd = Integer.parseInt(props.getProperty(TENDER + basket.getTenderType().trim()));
        } else if (Boolean.parseBoolean(props.getProperty(FINALIZE, "false"))) {
            logger.debug("payWithDefaultTender - Using default TenderType configured");
            itm.tnd = Integer.parseInt(props.getProperty(TENDER_DEFAULT));
        }

        int sts = group[7].action3(0);

        // Setting errorcode only case basket sale...
        if (sts > 0 && !basket.RETURN.equals(basket.getType())) {
            // Updating the basket status...
            tra.mode = M_CANCEL;
            basket.setErrorCode(103);
            endOfTransaction();

            // Calling Abort on POS...
            Action.input.lck = 0xFF;
            group[3].action5(M_CANCEL);

            logger.debug("payWithTender - Error occurred into payment step - sts: " + sts);
        } else basket.setErrorCode(0);

        logger.debug("payWithTender - Exit");
    }

    private void sellBasketItems() {
        logger.debug("sellBasketItems - Enter");

        for (Item item : basket.getItems()) {
            int result = sellItem(item);

            if (result > 0) {
                basket.update(item, result > 0);
            } else {
                // Update price with possible DP active...
                int coefficient = 1000;
                Item updItem = new Item(item.getCode(), new BigDecimal(pit.amt + pit.crd).divide(new BigDecimal(coefficient)), item.getQty(), item.getUnitPrice(), item.getBarcode());
                basket.update(updItem, result > 0);
            }
        }

        if (Boolean.parseBoolean(props.getProperty(NOT_SOLD_WARNING, "false")) && basket.getNotSoldItems().size() != 0) {
            panel.clearLink(Mnemo.getInfo(136), 1);
        }

        logger.debug("sellBasketItems - Exit");
    }

    private int sellItem(Item item) {
        logger.debug("sellItem - Enter");

        int sts = 0;
        input.prompt = "";
        input.reset(item.getBarcode().trim());

        itm = new Itemdata();

        //if (item.getUnitPrice() !=  null && item.getQty() > 1) {
        if (item.getUnitPrice() != null) {
            //itm.price = (int) (item.getUnitPrice() * Math.pow(10, tnd[0].dec));
            //itm.prpov = (int)(item.getUnitPrice() * Math.pow(10, tnd[0].dec));
            itm.ecommerceInfo.setUnitPrice(item.getUnitPrice().multiply(new BigDecimal(Math.pow(10, tnd[0].dec))).intValue());
        }

        if (item.getPrice() != null) {
            itm.ecommerceInfo.setPrice(item.getPrice().multiply(new BigDecimal(Math.pow(10, tnd[0].dec))).intValue());

        }

        /*if (item.getQty() != 0) {
             //itm.qty = item.getQty();
            itm.ecommerceInfo.setQty(item.getQty());
        }*/

        //itm.prpov = (int)(item.getPrice() * Math.pow(10, tnd[0].dec));
        //itm.prlbl = (int)(item.getPrice() * Math.pow(10, tnd[0].dec));
        //itm.ecommerceInfo.setPrice(itm.prlbl);


        if ((sts = group[5].action2(0)) == 0) {
            if (event.spc == 0) {
                //TODO: Check if this is needed
                sts = group[5].action7(1);
            }
        }

        logger.debug("sellItem - Exit - sts: " + sts);
        return sts;
    }

    public void endOfTransaction() {
        logger.debug("endOfTransaction - Enter");

        Set<Integer> allowedModes = new HashSet<Integer>(Arrays.asList(M_GROSS, M_CANCEL, M_SUSPND));

        if (allowedModes.contains(tra.mode)) {
            printNotSoldList();
            basket.setTerminalID(editKey(ctl.reg_nbr, 3));
            basket.setStatus(tra.mode == M_GROSS ? READY : CANCELED);
            transactionStarted = false;
            int numPrinterCopies = Integer.parseInt(props.getProperty(PRINTERCOPIESNUMBER));
            for (indexPrinterLine = 1; indexPrinterLine <= numPrinterCopies; indexPrinterLine++) {
                ElJrn.second_cpy(2, ctl.tran, 1);
            }

            basket.setEarnedLoyaltyPoints(tra.pnt);
            updateBasket(basket);
            input.reset("");
        }

        logger.debug("endOfTransaction - Exit");
    }

    public boolean abortTransaction() {
        if (!isEnabled()) return false;
        if (tra.mode == M_CANCEL) return true;

        tra.mode = M_CANCEL;
        basket.setErrorCode(102);
        group[3].action5(M_CANCEL);
        return true;
    }

    private void printNotSoldList() {
        if (basket.getNotSoldItems().size() > 0) {
            for (Item item : basket.getNotSoldItems()) {
                logger.debug("Not sold item: " + item.getBarcode());
                Struc.prtLine.init(Mnemo.getMenu(111)).onto(20, item.getBarcode()).type(2);
            }

            GdRegis.set_trailer();
            Struc.prtLine.type(2);
            GdRegis.hdr_print();
        }
    }

   /* public void sendHeartBeatMessage() {
        int errorCode = 0;
       Timer timer = new Timer();

        try {
            if (isEnabled() && !(Struc.ctl.ckr_nbr == 0)) {
                if (panel.modal != null) {
                    if (panel.modal instanceof ClrDlg) {
                        ClrDlg dlg = (ClrDlg) panel.modal;
                        if (dlg.info.text.toUpperCase(Locale.ENGLISH).contains("PRINTER")) errorCode = 102;
                        else errorCode = 0;
                    }
                }

                timer.schedule(new HeartBeatTimerTask(editKey(ctl.reg_nbr, 3), errorCode),
                        Long.parseLong(props.getProperty("heartBeatSignal", "5000")));
            }
        } catch (Exception ex) {
            logger.error("Error: ", ex);
        }
    }*/

    public void sendHeartBeatMessage() {
        int errorCode = 0;
        try {
            if (isEnabled() && !(Struc.ctl.ckr_nbr == 0)) {
                if (panel.modal != null) {
                    if (panel.modal instanceof ClrDlg) {
                        ClrDlg dlg = (ClrDlg) panel.modal;
                        if (dlg.info.text.toUpperCase(Locale.ENGLISH).contains("PRINTER")) errorCode = 102;
                        else errorCode = 0;
                    }
                }
               new HeartBeatTimerTask(editKey(ctl.reg_nbr, 3), errorCode).sendRequest();
            }
        } catch (Exception ex) {
            logger.error("Error: ", ex);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
    public boolean isTransactionStarted() {
        return transactionStarted;
    }

    public boolean hidePopup() {
        logger.debug("CHR_NBR : " + Struc.ctl.ckr_nbr);
        return (isEnabled() && !Boolean.parseBoolean(props.getProperty(SHOW_POPUP, "true")) && Struc.ctl.ckr_nbr > 0);
    }

    // TODO: Method addDecimals  Math.pow(10, tnd[0].dec)
    // Input :amt
    // OutPut:amt
    public BigDecimal addDecimals(BigDecimal amt) {
        return amt.multiply(new BigDecimal(Math.pow(10, tnd[0].dec)));
    }

    // TODO: Method removeDecimals  Math.pow(10, tnd[0].dec)
    // Input :amt
    // OutPut:amt
    public BigDecimal removeDecimals(BigDecimal amt) {
        return amt.divide(new BigDecimal(Math.pow(10, tnd[0].dec)));
    }

}
