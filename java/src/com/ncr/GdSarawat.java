package com.ncr;

import com.ncr.capillary.CouponDetails.RedeemCoupon;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

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
		} catch (Exception e) {
			logger.info("Exception " + e.getMessage());
		}

		if (capillaryEnabled) {
			CapillaryService.getInstance().init();
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

	public void setInCommunicationWhitCapillary(boolean isInCommunicationWhitCapillary) {
		this.isInCommunicationWhitCapillary = isInCommunicationWhitCapillary;
	}

	public boolean isPrintFailureTransaction() {
		return printFailureTransaction;
	}

	public void setPrintFailureTransaction(boolean printFailureTransaction) {
		this.printFailureTransaction = printFailureTransaction;
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

	int action0(int spec) {
		logger.debug("ENTER action0 - Registration customer");

		if ((cus.number != null && cus.number != "")
				|| !CapillaryService.getInstance().isEnabled() && (tra.mode == M_TRRTRN || tra.mode == M_TRVOID)) {
			logger.debug("EXIT action0 - Registration customer - return 7");
			return 7; // UNAVAILABLE
		}

		switch (spec) {
            case 0:
                logger.info("push registration button or answer to question about registration");

                break;
            case 1:
                logger.info("inserted customer card number by keyboard");

                if (input.num == 13) {
                    if (manualEntryCustomerCard == 0) {
                        logger.info("manualEntryCustomerCard disabled - return error");
                        logger.debug("EXIT action0 - Registration customer");

                        return 83;
                    } else if (manualEntryCustomerCard == 1 && (input.lck & 0x14) <= 0) {
                        logger.info("manualEntryCustomerCard enabled only for supervisor - return error");
                        logger.debug("EXIT action0 - Registration customer");

                        return 1;
                    }

                    if (GdCusto.chk_cusspc(12) == 0) { // if the code passed belongs to a customer card
                        logger.info("customer card accepted: " + input.pb);

                        //codeCustomerCard = input.pb;
                        isRegistration = true;

                        int ret = GdCusto.getInstance().action1(0);
                        isInCommunicationWhitCapillary = false;

                        logger.debug("EXIT action0 - return ret: " + ret);
                        return ret;
                    } else {
                        logger.info("customer card refused: " + input.pb);
                        logger.debug("EXIT action0 - Registration customer");

                        return 31;
                    }
                } else {
                    logger.debug("EXIT action0 - Registration customer - return -1");
                    return -1;
                }

                //break;

            case 2:
                logger.info("inserted customer card number by scanner");

                if (input.num == 13) {
                    if (GdCusto.chk_cusspc(12) == 0) { // if the code passed belongs to a customer card
                        logger.info("customer card accepted: " + input.pb);


                        //codeCustomerCard = input.pb;
                        isRegistration = true;

                        int ret = GdCusto.getInstance().action1(0);
                        isInCommunicationWhitCapillary = false;

                        logger.debug("EXIT return ret: " + ret);
                        return ret;
                    } else {
                        logger.info("customer card refused: " + input.pb);
                        logger.debug("EXIT action0 - Registration customer");

                        return 31;
                    }
                } else {
                    logger.debug("EXIT action0 - Registration customer - return -1");
                    return -1;
                }

                //break;
            case 99:
                logger.info("inserted customer mobile");

                if (CapillaryService.getInstance().isEnabled()) {
                    /*isRegistration = true;

                    int ret = GdCusto.getInstance().action1(0);
                    isInCommunicationWhitCapillary = false;
                    logger.debug("EXIT action0 - Registration customer - return " + ret);

                    return ret; */
                    customerMobile = input.pb;
                    logger.info("mobile: " + customerMobile);

                    if (customerMobile.startsWith("0")) {
                        customerMobile = customerMobile.substring(1);
                        logger.info("mobile: " + customerMobile);
                    }
                    int success = CommunicationCapillaryForCustomer.getInstance().customerGet(customerMobile, true);

					if (success == 0 && CommunicationCapillaryForCustomer.getInstance().isAlreadyRegistered()) {
						logger.debug("EXIT action0 - Registration customer - return already registered");

						return 85;
					}
                }

                break;
            default:
                break;
		}

		logger.debug("EXIT action0 - Registration customer - return OK");
		return 0;
	}

	int action1(int spec) {
		logger.debug("ENTER action1 - check customer number");
		int sts = 0;

		// PSH-ENH-005-AMZ#BEG -- customer id
		String oldInput = input.pb;

        if (GdPsh.isEnabledSMASH()) { // AMZ-2017-003-004#ADD
            if (GdPsh.isEnabled() && input.num == 13 && (sts = GdCusto.chk_cusspc(12)) == 0) {
                input.pb = oldInput;
                input.num = 13;
                return GdCusto.getInstance().action1(0);
            }
        } // AMZ-2017-003-004#ADD
		// PSH-ENH-005-AMZ#END -- customer id

		if (CapillaryService.getInstance().isEnabled() && input.num == 13 && (sts = GdCusto.chk_cusspc(12)) == 0) {
			// Fixed to 8 digits
			if (cus.number != null && cus.number != "" || (cardOnlyStartTransaction && tra.mode == M_GROSS)) {

				logger.debug("EXIT action1 - customer card refused");
				return 7;
			}

			if (spec == 0) { // x000d
				if (manualEntryCustomerCard == 0) { // disabled
					logger.debug("EXIT action1 - manualEntryCustomerCard disabled");

					return 83;
				} else if (manualEntryCustomerCard == 1 && (input.lck & 0x14) <= 0) { // allowed only for supervisor,
																						// but the operator isn't a
																						// supervisor
					logger.debug("EXIT action1 - manualEntryCustomerCard enabled only for supervisor");

					return 1;
				}
			}

			isInCommunicationWhitCapillary = true;

			int ret = GdCusto.getInstance().action1(0);
			isInCommunicationWhitCapillary = false;

			logger.debug("EXIT action1 - checked the customer card - return " + ret);
			return ret;
		}

		logger.debug("EXIT action1 - return -1");
		return -1;
	}

	int action2(int spec) {
		logger.debug("ENTER action2 - insert customer");

		if (cus.number != null && cus.number != "" || (cardOnlyStartTransaction && tra.mode == M_GROSS)
				|| !CapillaryService.getInstance().isEnabled()) {

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

	int action3(int spec) {
		logger.debug("ENTER action3 - Points");

		if (cus.number != null && cus.number != "" && CapillaryService.getInstance().isEnabled() && tra.mode != M_TRRTRN
				&& tra.mode != M_TRVOID) {
			int nPoint = cus.pnt;
			//boolean ris = true;
            int ris = 0;

			switch (spec) {
			case 0:
				logger.info("points inquiry - request customer/get");

				ris = CommunicationCapillaryForPoints.getInstance().customerGet(cus.number);

				//if (ris) {
                if (ris == 0) {
                    DevIo.alert(1);
					nPoint = CommunicationCapillaryForPoints.getInstance().getNPointsCustomer();
                    cus.pnt = nPoint;
					logger.info("points customer: " + nPoint);
				} else {
                    DevIo.alert(0);
					logger.debug("EXIT action3 - request customer/get failed - return: " + ris);
					//return 82; // CAPILLARY FAILED
                    return ris;
				}
				panel.clearLink(Mnemo.getText(87) + ": " + nPoint, 0x081);

				break;
			case 1:
				logger.info("pressed button for points redeem");

				if (alreadyRedeem) {
					logger.debug("EXIT already redeem");
					return 80; // ALREADY REDEEM
				}

				logger.info("request customer/get");
				ris = CommunicationCapillaryForPoints.getInstance().customerGet(cus.number);

				//if (ris) {
                if (ris == 0) {
                    DevIo.alert(1);
					nPoint = CommunicationCapillaryForPoints.getInstance().getNPointsCustomer();
                    cus.pnt = nPoint;

                    logger.info("points customer: " + nPoint);
				} else {
                    DevIo.alert(0);
					logger.debug("EXIT action3 - request customer/get failed - return: " + ris);
					//return 82;
                    return ris;
				}

				dspLine.init(Mnemo.getText(87) + ": " + nPoint);

				break;
			case 2:
				logger.info("inserted points for redeem");

				if (Integer.parseInt(input.pb) > nPoint) {
					logger.debug("EXIT too many points to be redeemed");
					return 8;
				}

				//boolean success = CommunicationCapillaryForPoints.getInstance().pointsIsRedeemable(input.pb, cus.mobile);
                int success = CommunicationCapillaryForPoints.getInstance().pointsIsRedeemable(input.pb, cus.mobile);

				//if (!success) {
                if (success != 0) {
                    DevIo.alert(0);
					logger.debug("EXIT action3 - redeem points failed - return: " + success);
					//return 79; // REDEEM POINTS FAILED
                    return success;
				} else {
                    DevIo.alert(1);
					pointsDiscountApplies();
					alreadyRedeem = true;
				}

				break;
			default:
				break;
			}
		} else {
			logger.debug("EXIT action3 - redeem points not allowed");
			return 7;
		}

		logger.debug("EXIT action3 - redeem points OK");
		return 0;
	}

	int action4(int spec) {
		logger.debug("ENTER action3 - insert coupon");

		dspLine.init(Mnemo.getMenu(85));
		if (cus.number != null && cus.number != "" && CapillaryService.getInstance().isEnabled() && tra.mode != M_TRRTRN
				&& tra.mode != M_TRVOID) {
			if (spec == 1) {
				//boolean success = false;
                int success = 0;

				ArrayList<RedeemCoupon> listCouponPassed = CommunicationCapillaryForCoupon.getInstance()
						.getListCouponIsRedeemable();

				for (int i = 0; i < listCouponPassed.size(); i++) {
					if (listCouponPassed.get(i).getCode().equals(input.pb)) {
						return 84; // RETURN COUPON
					}
				}

				success = CommunicationCapillaryForCoupon.getInstance().couponIsRedeemable(input.pb, cus.mobile);

				//if (!success) {
                if (success != 0) {
					logger.debug("EXIT action4 - redeem coupon failed - return: " + success);
                    DevIo.alert(0);
					//return 81; // RED. COUP. FAILED
                    return success;
				} else {
                    DevIo.alert(1);
                    prtLine.init(Mnemo.getText(86).trim() + ": " + input.pb).book(3);

					if (CommunicationCapillaryForCoupon.getInstance().getRedeemCoupon().getDiscount() == 0D) {
						couponDiscountApplies(CommunicationCapillaryForCoupon.getInstance().getRedeemCoupon());

						ArrayList<RedeemCoupon> listCoupon = CommunicationCapillaryForCoupon.getInstance()
								.getListCouponIsRedeemable();
						listCoupon.get(listCoupon.size() - 1).setApplied(true);
					}

					/*
					 * ArrayList<Long> listRange = CommunicationCapillaryForCoupon.getInstance().getListRangeCode();
					 * 
					 * for (int i = 0; i < listRange.size(); i += 2) { if (Long.parseLong(input.pb) >= listRange.get(i)
					 * && Long.parseLong(input.pb) < listRange.get(i + 1)) {
					 * couponDiscountApplies(CommunicationCapillaryForCoupon.getInstance().getRedeemCoupon());
					 * 
					 * ArrayList<RedeemCoupon> listCoupon = CommunicationCapillaryForCoupon.getInstance()
					 * .getListCouponIsRedeemable(); listCoupon.get(listCoupon.size() - 1).setApplied(true);
					 * 
					 * break; } }
					 */
				}
			}
		} else {
			logger.debug("EXIT action4 - redeem coupon not allowed");
			return 7;
		}

		logger.debug("EXIT action4 - redeem coupon OK");
		return 0;
	}

	int action5(int spec) {
		logger.debug("ENTER action5 - customer menu");

		if (!GdSarawat.getInstance().isCustomerMenuEnabled() || !CapillaryService.getInstance().isEnabled()) {
			logger.debug("EXIT action5 - not allowed");
			return 7;
		}

		int code, line = event.read(event.nxt);

		input.prompt = Mnemo.getText(event.alt);
		input.init(0x00, event.max, event.min, event.dec);
		panel.display(1, Mnemo.getMenu(event.act));

		SelDlg dlg = new SelDlg(Mnemo.getText(22));

		int act = event.act;
		for (code = event.next(line); event.key > 0; code = event.next(code)) {
			dlg.add(8, editNum(event.key, input.max), " " + (Mnemo.getMenu(act + event.max)));
		}
		dlg.show("MNU");

		if (dlg.code > 0) {
			return dlg.code;
		}
		if (input.key == 0) {
			input.key = input.CLEAR;
		}

		if (input.num < 1 || input.key != input.ENTER) {
			return 5;
		}

		code = input.adjust(input.pnt);
		if (code > 0) {
			return code;
		}

		code = input.scanNum(input.num);
		for (line = event.next(line); event.key > 0; line = event.next(line)) {
			if (event.key != code)
				continue;
			if ((event.lck & input.lck) == 0)
				return 1;
			input.num = 0;
			return group[event.act / 10].exec();
		}

		return 5;
	}


    // AMZ-2017-005#BEG
    /**
     * Search customer from Capillary by phone number
     * @param spec
     * @return
     */
    int action6(int spec) {
        return 0;
    }
    // AMZ-2017-005#END

	public boolean couponDiscountApplies(RedeemCoupon coupon) {
		logger.debug("ENTER couponDiscountApplies");

        //String code = coupon.getCode();
        String code = coupon.getDiscountCode();
        logger.info("code coupon: " + code);

		try {
			appliedDiscountCoupons = true;
			long flag = Long.parseLong("1" + code);
			logger.info("flag: " + flag);

			Promo.setPromovar(flag, 1);
		} catch (Exception e) {
			logger.info("Exception " + e.getMessage());
			logger.debug("EXIT couponDiscountApplies - return false");
			return false;
		}

		logger.debug("EXIT couponDiscountApplies - return true");
		return true;
	}

	/*
	 * public boolean couponDiscountApplies(RedeemCoupon coupon) { logger.debug("ENTER couponDiscountApplies");
	 * 
	 * String type = coupon.getDiscountType(); long discount = 0L; String code = coupon.getCode(); long promovar = 0L;
	 * 
	 * logger.info("type discount: " + type); logger.info("value discount: " + discount); logger.info("code coupon: " +
	 * code);
	 * 
	 * try { appliedDiscountCoupons = true; long flag = Long.parseLong("1" + code); logger.info("flag: " + flag);
	 * 
	 * if (type.equals("PERC")) { promovar = Long.parseLong("2" + code); discount = (long) coupon.getDiscount(); } else
	 * { promovar = Long.parseLong("3" + code); discount = (long) (coupon.getDiscount() * 100); }
	 * 
	 * logger.info("promovar: " + promovar);
	 * 
	 * Promo.setPromovar(flag, 1); Promo.setPromovar(promovar, discount); } catch (Exception e) { logger.info(
	 * "Exception " + e.getMessage()); logger.debug("EXIT couponDiscountApplies - return false"); return false;
	 * }
	 * 
	 * logger.debug("EXIT couponDiscountApplies - return true"); return true; }
	 */

	public void applyManualCoupon() {
		logger.debug("ENTER applyManualCoupon");

		ArrayList<RedeemCoupon> listCoupon = CommunicationCapillaryForCoupon.getInstance().getListCouponIsRedeemable();

		for (int i = 0; i < listCoupon.size(); i++) {
			if (!listCoupon.get(i).isApplied()) {
				logger.info("code coupon not applied: " + listCoupon.get(i).getCode());

				long amount = tra.amt + tra.dsc_amt + tra.chg_amt + tra.tld_amt;
				logger.info("amount: " + amount);

				int ris = 1;

				if (listCoupon.get(i).getDiscountType().equals("PERC")) {
					logger.info("type coupon PERC");

					if ((ris = sc_checks(4, 8)) == 0) {
						long valueDiscount = (long) ((listCoupon.get(i).getDiscount() * amount) / 100);
						logger.info("valueDiscount calculated: " + valueDiscount);

						if (valueDiscount == 0) {
							ris = 8;
						} else {
							ris = GdCusto.dsc_line(8, -valueDiscount, amount);
						}
					}
				} else if (listCoupon.get(i).getDiscount() <= tra.bal) {
					logger.info("type coupon: " + listCoupon.get(i).getDiscountType());

					if ((ris = sc_checks(4, 7)) == 0) {
						long valueDiscount = -(long) (listCoupon.get(i).getDiscount() * 100);
						if (valueDiscount == 0 || Math.abs(valueDiscount) >= Math.abs(amount)) {
							ris = 8;
						} else {
							ris = GdCusto.dsc_line(7, valueDiscount, amount);
						}
					}
				}

				if (ris == 0) {
					logger.info("discount coupon applied");

					listCoupon.get(i).setApplied(true);
				} else {
					logger.info("discount coupon not applied");
				}
			}
		}

		logger.debug("EXIT applyManualCoupon");
	}

	public boolean pointsDiscountApplies() {
		logger.debug("ENTER pointsDiscountApplies");

		try {
			appliedDiscountPoints = true;
			promovarDiscount = Long.parseLong(CommunicationCapillaryForPoints.getInstance().getRedemptionPvDiscount());

			discountPoints = CommunicationCapillaryForPoints.getInstance().getDiscountValue();

			// if (discountPoints.startsWith("0.")) {
			// discountPoints = discountPoints.substring(2);
			// }

			logger.info("promovarDiscount: " + promovarDiscount);
			logger.info("discount: " + discountPoints);

			Promo.setPromovar(promovarDiscount, discountPoints);

			long promovarPoints = Long.parseLong(CommunicationCapillaryForPoints.getInstance().getRedemptionPvPoints());
			points = CommunicationCapillaryForPoints.getInstance().getNPointsRedeem();

			logger.info("promovarPoints: " + promovarPoints);
			logger.info("points: " + points);

			Promo.setPromovar(promovarPoints, points);
		} catch (Exception e) {
			logger.info("Exception " + e.getMessage());
			logger.debug("EXIT pointsDiscountApplies - return false");
			return false;
		}

		logger.debug("EXIT pointsDiscountApplies - return true");
		return true;
	}

	public static void printCapillaryVoucher() {
		logger.debug("ENTER printCapillaryVoucher");

		prtLine.init(' ').book(3);
		prtLine.init("####################").type(2);
		prtLine.init(' ').book(3);

		for (int i = 0; i < GdTrans.getListFailedCoupon().size(); i++) {
			logger.info("print coupon redeem failed: " + GdTrans.getListFailedCoupon().get(i));

			prtLine.init(Mnemo.getInfo(81).trim() + ": " + GdTrans.getListFailedCoupon().get(i)).book(3);
		}

		prtLine.init(' ').book(3);

		if (!tra.successRedeemPoint) {
			logger.info("print points redeem failed");

			prtLine.init(Mnemo.getInfo(79).trim()).type(2);
		}

		prtLine.init(' ').book(3);

		if (!tra.successTransaction) {
			logger.info("print Capillary failed for send data transaction");

			prtLine.init(Mnemo.getInfo(82).trim()).book(3);
		}

		prtLine.init(' ').book(3);
		prtLine.init("####################").type(2);
		prtLine.init(' ').book(3);

		GdRegis.hdr_print();

		logger.debug("EXIT printCapillaryVoucher");
	}

    public String readErrorCodeCapillary(int code) {
        logger.debug("ENTER readErrorCodeCapillary");
        logger.info("code: " + code);

        Properties prop = new Properties();
        String msg = "";

        try {
            prop.load(new FileInputStream("conf/errorCodes.properties"));

            msg = prop.getProperty(String.valueOf(code));
            if (msg == null || msg.equals("")) {
                logger.info("default value");

                msg = prop.getProperty(String.valueOf("default"));
            }
        }catch (Exception e) {
            logger.info("EXCEPTION: " + e.getMessage());
            logger.debug("ENTER return: " + Mnemo.getText(82));
            return Mnemo.getText(82);
        }

        logger.debug("ENTER return: " + msg);
        return msg;
    }
}