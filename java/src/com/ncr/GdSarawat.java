package com.ncr;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA. User: Administrator Date: 25/05/15 Time: 12.15 To change this template use File |
 * Settings | File Templates.
 */

public class GdSarawat extends Action {
	private static final Logger logger = Logger.getLogger(GdSarawat.class);
	private static GdSarawat instance = null;
	private boolean customerCardRequestEnabled = false;
	private boolean cardOnlyStartTransaction = false;
	private boolean askForCustomerRegistration = false;
	private boolean customerMenuEnabled = false;
	private int manualEntryCustomerCard = 0;
	private boolean capillaryEnabled = false;
	private boolean printFailureTransaction = false;
	private String customerMobile = "";
	private boolean haveCustomerCard = false;
	private boolean isRegistration = false;
	private boolean isInCommunicationWhitCapillary = false;
	private boolean alreadyRedeem = false;
	private boolean ckrClose = false;
	private long discountPoints = 0;
	private boolean appliedDiscountPoints = false;
	private boolean appliedDiscountCoupons = false;
	private long promovarDiscount = 0;
	private long points = 0;
	private boolean enableChkAuthoScan = false;  // CHKAUTHO-CGA#A
	private static boolean enableDsc = false;   // SPINNEYS-ENH-DSC-SBE#A

    // AMZ-2017-004-001#BEG -- roundReturnsCustomerFavour
    private static boolean roundReturnsCustomerFavour = false;
	private boolean itemSortLogic = false;

	public static boolean getRoundReturnsCustomerFavour(){
        return roundReturnsCustomerFavour;
    }
    // AMZ-2017-004-001#END

    // AMZ-2017-004-002#BEG -- quantityBySupervisor
    private static int quantityBySupervisor = 0;
    // 0 : default behaviour
    // 1 : Request supervisor to use one shot quantity key, ignoring the plu flag2  F_NONQTY
    // 2 : Request supervisor to use in all transaction the quantity key, ignoring the plu flag2  F_NONQTY
    public static int getQuantityBySupervisor(){
        return quantityBySupervisor;
    }
    private static boolean forceAcceptQuantityKey = false;
    public static boolean getForceAcceptQuantityKey(){
        return forceAcceptQuantityKey;
    }
    public static void quantityKeyForcedAccept(){
        if (quantityBySupervisor==1){
            forceAcceptQuantityKey = false;
        }
    }
    public static void activateQuantityKeyForcedAccept(){
        forceAcceptQuantityKey = true;
    }
    public static void deactivateQuantityKeyForcedAccept(){
        forceAcceptQuantityKey = false;
    }
    // AMZ-2017-004-002#END


	public long getDiscountPoints() {
		return discountPoints;
	}

	public void setDiscountPoints(long discountPoints) {
		this.discountPoints = discountPoints;
	}

	public boolean isAppliedDiscountPoints() {
		return appliedDiscountPoints;
	}

	public void setAppliedDiscountPoints(boolean appliedDiscountPoints) {
		this.appliedDiscountPoints = appliedDiscountPoints;
	}

	public long getPromovarDiscount() {
		return promovarDiscount;
	}

	public void setPromovarDiscount(long promovarDiscount) {
		this.promovarDiscount = promovarDiscount;
	}

	public long getPoints() {
		return points;
	}

	public void setPoints(long points) {
		this.points = points;
	}

	public boolean isAppliedDiscountCoupons() {
		return appliedDiscountCoupons;
	}

	public void setAppliedDiscountCoupons(boolean appliedDiscountCoupons) {
		this.appliedDiscountCoupons = appliedDiscountCoupons;
	}

	public static GdSarawat getInstance() {
		if (instance == null)
			instance = new GdSarawat();

		return instance;
	}

	public void resetAllSarawat() {
		logger.info("reset all into Sarawat");

        customerMobile = "";
		haveCustomerCard = false;
		isRegistration = false;
		isInCommunicationWhitCapillary = false;
		alreadyRedeem = false;
		appliedDiscountPoints = false;
		appliedDiscountCoupons = false;
	}

	public void setAlreadyRedeem(boolean alreadyRedeem) {
		this.alreadyRedeem = alreadyRedeem;
	}

	public int loadCapillaryParams(String txt) {
		logger.debug("ENTER loadCapillaryParams");
		logger.info("read params record PSAR0: " + txt);

		try {
			customerCardRequestEnabled = txt.substring(0, 1).equals("1");
			cardOnlyStartTransaction = txt.substring(1, 2).equals("1");
			askForCustomerRegistration = txt.substring(2, 3).equals("1");
			customerMenuEnabled = txt.substring(3, 4).equals("1");
			manualEntryCustomerCard = Integer.parseInt(txt.substring(4, 5));
			capillaryEnabled = txt.substring(5, 6).equals("1");
			printFailureTransaction = txt.substring(6, 7).equals("1");
			enableChkAuthoScan = txt.substring(7, 8).equals("1");      // CHKAUTHO-CGA#A
			enableDsc = txt.substring(8, 9).equals("1");  // SPINNEYS-ENH-DSC-SBE#A
            ExtResume.enabled = txt.substring(9, 10).equals("1"); // AMZ-2017#ADD
            ExtResume.supervisor = txt.substring(10, 11).equals("1"); // AMZ-2017#ADD
            roundReturnsCustomerFavour = txt.substring(11, 12).equals("1"); // AMZ-2017-004-001#ADD
            quantityBySupervisor = Integer.parseInt(txt.substring(12, 13)); // AMZ-2017-004-002#ADD
			itemSortLogic = txt.substring(13, 14).equals("1");
		} catch (Exception e) {
			logger.info("Exception " + e.getMessage());
		}

		logger.debug("EXIT loadCapillaryParams");
		return 0;
	}

	public boolean isCustomerCardRequestEnabled() {
		return customerCardRequestEnabled;
	}

	public void setCustomerCardRequestEnabled(boolean customerCardRequestEnabled) {
		this.customerCardRequestEnabled = customerCardRequestEnabled;
	}

	public boolean isCardOnlyStartTransaction() {
		return cardOnlyStartTransaction;
	}

	public void setCardOnlyStartTransaction(boolean cardOnlyStartTransaction) {
		this.cardOnlyStartTransaction = cardOnlyStartTransaction;
	}

	public boolean isAskForCustomerRegistration() {
		return askForCustomerRegistration;
	}

	public void setAskForCustomerRegistration(boolean askForCustomerRegistration) {
		this.askForCustomerRegistration = askForCustomerRegistration;
	}

	public boolean isCustomerMenuEnabled() {
		return customerMenuEnabled;
	}

	public void setCustomerMenuEnabled(boolean customerMenuEnabled) {
		this.customerMenuEnabled = customerMenuEnabled;
	}

	public int getManualEntryCustomerCard() {
		return manualEntryCustomerCard;
	}

	public void setManualEntryCustomerCard(int manualEntryCustomerCard) {
		this.manualEntryCustomerCard = manualEntryCustomerCard;
	}

	public boolean isCapillaryEnabled() {
		return capillaryEnabled;
	}

	public void setCapillaryEnabled(boolean capillaryEnabled) {
		this.capillaryEnabled = capillaryEnabled;
	}

	public String getCustomerMobile() {
		return customerMobile;
	}

	public void setCustomerMobile(String customerMobile) {
		this.customerMobile = customerMobile;
	}

	public boolean getHaveCustomerCard() {
		return haveCustomerCard;
	}

	public void setHaveCustomerCard(boolean haveCustomerCard) {
		this.haveCustomerCard = haveCustomerCard;
	}

	public boolean isRegistration() {
		return isRegistration;
	}

	public void setIsRegistration(boolean isRegistration) {
		this.isRegistration = isRegistration;
	}

	public boolean getInCommunicationWhitCapillary() {
		return isInCommunicationWhitCapillary;
	}

	public void setCkrClose(boolean closed) {
		this.ckrClose = closed;
	}

	public boolean isCkrClose() {
		return this.ckrClose;
	}

	// CHKAUTHO-CGA#A BEG
	public boolean isEnableChkAuthoScan() {
		return enableChkAuthoScan;
	}

	public void setEnableChkAuthoScan(boolean enableChkAuthoScan) {
		this.enableChkAuthoScan = enableChkAuthoScan;
	}
	// CHKAUTHO-CGA#A END

	// SPINNEYS-ENH-DSC-SBE#A BEG
	public static boolean isEnableDsc() {
		return enableDsc;
	}    // SPINNEYS-ENH-DSC-SBE#A END

	public boolean isItemSortLogic() {
		return itemSortLogic;
	}

	public int action0(int spec) {
		logger.debug("ENTER action0 - Registration customer");

		logger.debug("EXIT action0 - Registration customer - return OK");
		return 0;
	}

	public int action1(int spec) {
		logger.debug("ENTER action1 - check customer number");
		int sts = 0;

		// PSH-ENH-005-AMZ#BEG -- customer id
		String oldInput = input.pb;

        if (GdPsh.getInstance().isSmashEnabled()) { // AMZ-2017-003-004#ADD
            if (GdPsh.getInstance().isEnabled() && input.num == 13 && (sts = GdCusto.chk_cusspc(12)) == 0) {
                input.pb = oldInput;
                input.num = 13;
                return GdCusto.getInstance().action1(0);
            }
        } // AMZ-2017-003-004#ADD
		// PSH-ENH-005-AMZ#END -- customer id

		logger.debug("EXIT action1 - return -1");
		return -1;
	}

	public int action2(int spec) {
		logger.debug("ENTER action2 - insert customer");

		if (cus.getNumber() != null && cus.getNumber() != "" || (cardOnlyStartTransaction && tra.mode == M_GROSS)) {
			logger.debug("EXIT action2 - customer card refused");
			return 7;
		}

		if (manualEntryCustomerCard == 0) { // disabled
			logger.debug("EXIT action2 - manualEntryCustomerCard disabled");

			return 83;
		} else if (manualEntryCustomerCard == 1 && (input.lck & 0x14) <= 0) { // allowed only for supervisor, but the
																				// operator isn't a supervisor

			logger.debug("EXIT action2 - manualEntryCustomerCard enabled only for supervisor");

			return 1;
		}
        // AMZ-2017-005#BEG
        if(spec==100){

            int ret = GdCusto.getInstance().action1(0);
            isInCommunicationWhitCapillary = false;

            if (event.key == 0x093) {
                event.nxt = event.alt;
            }

            logger.debug("EXIT action2 - checked customer card - return " + ret);
            return ret;
        }
        // AMZ-2017-005#END

		if (input.num == 8) {
			int ret = GdCusto.getInstance().action1(0);
			isInCommunicationWhitCapillary = false;

			if (event.key == 0x093) {
				event.nxt = event.alt;
			}

			logger.debug("EXIT action2 - checked customer card - return " + ret);
			return ret;
		} else if (input.num > 0) {
			logger.debug("EXIT action2 - return error");

			return 3;
		}


		logger.debug("EXIT action2 - return Ok");
		return 0;
	}

	public int action3(int spec) {
		logger.debug("ENTER action3 - Points");

		logger.debug("EXIT action3 - redeem points OK");
		return 0;
	}

	public int action4(int spec) {
		logger.debug("ENTER action4 - insert coupon");

		dspLine.init(Mnemo.getMenu(85));

		logger.debug("EXIT action4 - redeem coupon OK");
		return 0;
	}

	public int action5(int spec) {
		logger.debug("ENTER action5 - customer menu");

		return 5;
	}

    public int action6(int spec) {
        return 0;
    }

}