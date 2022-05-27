package com.ncr.ssco.communication.requestprocessors;

import com.ncr.ssco.communication.entities.AdditionalProcessType;
import com.ncr.ssco.communication.entities.ItemExceptionId;
import com.ncr.ssco.communication.entities.ItemExceptionType;
import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.entities.pos.SscoTransaction;
import com.ncr.ssco.communication.manager.SscoMessageHandler;
import com.ncr.ssco.communication.requestdecoder.RequestFromSsco;
import com.ncr.ssco.communication.responseencoder.ResponseToSsco;

import java.util.*;

public class ItemRequestProcessor extends TransactionProcessor {

    private SscoItem itemRequest;
    private SscoItem itemResponse;
    private AdditionalProcessType additionalProcessType = AdditionalProcessType.NONE;
    private Properties processorProperties;

    public ItemRequestProcessor(SscoMessageHandler messageHandler) {
        super(messageHandler);
        processorProperties = messageHandler.getProcessorsProperties();
    }

    @Override
    public void additionalProcess() {
        logger.debug("Enter");
        String info = "";
        String key = "ENTER";

        if (additionalProcessType == AdditionalProcessType.DEPT) {
            info = "" + this.itemRequest.getPrice();
        } else if (additionalProcessType == AdditionalProcessType.QTY) {
            info = "" + this.itemRequest.getUpc();
        } else if (additionalProcessType == AdditionalProcessType.PRICE) {
            if (itemRequest.getPrice() > 0) {
                info = "" + itemRequest.getPrice();
            } else {
                key = "CLEAR";
                getManager().getCurrentSscoItem().setZeroPriced(true);
                sendResponses(new SscoError(SscoError.ZERO_PRICED, "Missing Price"));
            }
        }
        getManager().sendAdditionalProcess(info, key);
        additionalProcessType = AdditionalProcessType.NONE;
        logger.debug("Exit");
    }

    @Override
    public void process(RequestFromSsco requestFromSsco) {
        logger.debug("Enter");

        int price = 0;
        int scanned = -1;
        int qt = 0;
        int weight = 0;
        int tareCode = 0;
        String upc = requestFromSsco.getStringField("UPC");
        String department = requestFromSsco.getStringField("Department");
        if (requestFromSsco.getIntField("Scanned") != null)
            scanned = requestFromSsco.getIntField("Scanned");
        if (requestFromSsco.getIntField("Price") != null)
            price = requestFromSsco.getIntField("Price");
        if (requestFromSsco.getIntField("Quantity") != null)
            qt = requestFromSsco.getIntField("Quantity");

        itemRequest = new SscoItem(upc, scanned, department, 0, "", price, qt);

        if (requestFromSsco.getIntField("Weight") != null) {
            weight = requestFromSsco.getIntField("Weight");
            itemRequest.setWeight(weight);
        }
        if (requestFromSsco.getIntField("TarCode") != null) {
            tareCode = requestFromSsco.getIntField("TarCode");
            itemRequest.setTareCode(tareCode);
        }

        logger.info("ItemRequestProcessor -- START ");
        logger.info("UPC: " + upc);
        logger.info("Department: " + department);
        logger.info("Price: " + price);
        logger.info("Scanned: " + scanned);
        logger.info("Qt: " + qt);
        logger.info("Weight: " + weight);
        logger.info("TareCode: " + tareCode);
        logger.info("ItemRequestProcessor -- END ");

        if (!getManager().itemRequest(itemRequest))
            logger.warn("-- Warning ");

        logger.debug("Exit");
    }

    @Override
    public void sendResponses(SscoError sscoError) {
        logger.debug("Enter");

        if (sscoError.getCode() != SscoError.OK) {
            logger.info("sendItemException 0 " + sscoError.getCode());
            sendItemException(sscoError);
            getMessageHandler().getResponses().add(addEndResponse());
            logger.info("Exit ");
            return;
        }

        if (!getManager().transactionHasStarted()) {
            sendStartTransaction();
        } else {
            if (sscoError.getCode() != SscoError.OK) {
                logger.info("sendItemException: " + sscoError.getCode());
                sendItemException(sscoError);
            } else {
                sendItemSold();
                syncPromotions(itemResponse);
                sendTotalsResponse(sscoError);
            }
            getMessageHandler().getResponses().add(addEndResponse());
        }
        logger.debug("Exit");
    }

    private void sendItemException(SscoError sscoError) {
        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemException");
        switch (sscoError.getCode()) {
            case 16:
            case 27: //itemNotFound
                if (itemRequest != null) {
                    responseToSsco.setStringField("UPC", itemRequest.getUpc());
                } else {
                    responseToSsco.setStringField("UPC", "0");
                    getManager().clearRequest();
                }
                responseToSsco.setIntField("ExceptionType", ItemExceptionType.Event.getCode());
                responseToSsco.setIntField("ExceptionId", ItemExceptionId.ItemNotFound.getCode());
                break;
            case SscoError.ZERO_PRICED:
                responseToSsco.setStringField("UPC", itemRequest.getUpc());
                responseToSsco.setIntField("ExceptionType", 0);
                responseToSsco.setIntField("ExceptionId", ItemExceptionId.PriceRequired.getCode());
                break;
            case 1008:
                responseToSsco.setStringField("UPC", itemRequest.getUpc());
                responseToSsco.setIntField("WeightRequired", 1);
                break;
            default:
                if (itemRequest != null) {
                    if (itemRequest.getUpc() != null) {
                        responseToSsco.setStringField("UPC", itemRequest.getUpc());
                    }

                    if (itemRequest.getDepartment() != null) {
                        responseToSsco.setStringField("Department", itemRequest.getDepartment());
                    }
                } else {
                    responseToSsco.setStringField("UPC", "0");
                }

                responseToSsco.setStringField("Message.1", sscoError.getMessage());
                if (sscoError.getCode() == 190) {
                    responseToSsco.setIntField("TimeRestricted", 1);
                }
                break;
        }
        getMessageHandler().sendResponseToSsco(responseToSsco);

        if (getManager().getCurrentSscoItem().isPriceChanged()) {
            getManager().clearRequest();
        }
    }

    private void checkExtraProperties(ResponseToSsco responseToSsco) {
        Set<String> acceptedValues = new HashSet<String>() {{
            add("0"); add("2"); add("3"); add("4"); add("5");
        }};

        String ean = itemResponse.getUpc().trim();
        String ret = processorProperties.getProperty("EanFlag." + ean.trim());
        logger.info("getEanProperty " + ean + "=" + ret);

        if (ret != null) {
            String tokens[] = ret.split(";");

            for (int index = 0; index <= 1; index++) {
                if (!acceptedValues.contains(tokens[index])) {
                    logger.error("getEanProperty bad flag value [" + index + "] " + tokens[index]);
                    logger.error("flag must be in [0,2,3,4,5] set");
                    ret = null;
                }
            }
            if (ret != null) {
                if (!"0".equals(tokens[0])) {
                    responseToSsco.setIntField("RequiresSubsCheck", Integer.parseInt(tokens[0]));
                }
                if (!"0".equals(tokens[1])) {
                    responseToSsco.setIntField("RequiresSecurityBagging", Integer.parseInt(tokens[1]));
                }
            }
        }
    }

    private void sendItemSold() {
        logger.debug("Enter");
        String encoding = processorProperties.getProperty("Encoding", "CP1256");

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("ItemSold");

        if (itemResponse.getDepartment() != null) {
            responseToSsco.setStringField("Department", itemResponse.getDepartment());
        }
        if (itemResponse.getUpc() != null){
            responseToSsco.setStringField("UPC", itemResponse.getUpc().trim());
        }
        responseToSsco.setStringField("Description", itemResponse.getDescription().trim());
        if (itemResponse.getAdditionalDescription().length() != 0) {
            try {
                String description = itemResponse.getDescription().trim() + "\r\n" + itemResponse.getAdditionalDescription().trim();
                responseToSsco.setByteArrayField("Description", description.getBytes(encoding));
            } catch (Exception e) {
                logger.error("Error converting: ", e);
            }
        }
        responseToSsco.setIntField("ItemNumber", itemResponse.getItemNumber());

        additionalProcessType = AdditionalProcessType.NONE;

        int qty = itemResponse.getQty();
        if (qty <= 1) {
            if(itemResponse.getWeight()!=0 && itemResponse.getWeightPrice()!=0){
                responseToSsco.setIntField("Price", itemResponse.getWeightPrice());
                responseToSsco.setIntField("ExtendedPrice", itemResponse.getPrice());
                responseToSsco.setIntField("Weight", itemResponse.getWeight());
            }else {
                responseToSsco.setIntField("Price", itemResponse.getPrice());
            }
        } else {
            responseToSsco.setIntField("Price", itemResponse.getPrice() / qty);
            responseToSsco.setIntField("ExtendedPrice", itemResponse.getPrice());
            responseToSsco.setIntField("Quantity", qty);
        }

        checkExtraProperties(responseToSsco);
        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    public void sendStartTransaction() {
        logger.debug("Enter");

        SscoTransaction transaction = getManager().getTransaction();
        transaction.resetItemNumber();

        ResponseToSsco responseToSsco = getMessageHandler().createResponseToSsco("StartTransaction");
        responseToSsco.setStringField("Type", "normal");//transaction.getType()
        responseToSsco.setIntField("Id", transaction.getTransactionId());

        getMessageHandler().sendResponseToSsco(responseToSsco);

        logger.debug("Exit");
    }

    public AdditionalProcessType getAdditionalProcessType() {
        return additionalProcessType;
    }

    @Override
    public void setAdditionalProcessType(AdditionalProcessType additionalProcessType) {
        this.additionalProcessType = additionalProcessType;
    }

    public Integer getPrice() {
        if (itemRequest != null)
            return itemRequest.getPrice();

        return null;
    }

    public Integer getWeight() {
        if (itemRequest != null)
            return itemRequest.getWeight();

        return null;
    }

    public void setItemResponse(SscoItem response) {
        this.itemResponse = response;
    }

    public SscoItem getItemResponse() {
        return itemResponse;
    }

    public SscoItem getItemRequest() {
        return itemRequest;
    }

    public void setItemRequest(SscoItem itemRequest) {
        this.itemRequest = itemRequest;
    }
}