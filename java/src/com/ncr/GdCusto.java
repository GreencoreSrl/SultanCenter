package com.ncr;

import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

class GdCusto extends Action {

	/*******************************************************************/
	/*                                                                 */
	/* clu access */
	/*                                                                 */

	/**
	 * ***************************************************************
	 */

	// SARAWAT-ENH-20150507-CGA#A BEG
	private static GdCusto instance = null;
	private static final Logger logger = Logger.getLogger(GdCusto.class);

	public static GdCusto getInstance() {
		if (instance == null)
			instance = new GdCusto();

		return instance;
	}
	// SARAWAT-ENH-20150507-CGA#A END

	static int src_clu(String nbr, int id) {
		String key = "C" + id + editTxt(nbr, 14);

		int sts = lCLU.find(key, rMNT.recno > 0);
		if (sts < 1) {
			return sts == 0 || ctl.lan > 2 ? 7 : 16;
		}
		try {
			lCLU.skip(lCLU.fixSize);
			if (id == 1) {
				cus.spec = lCLU.scanNum(2);
				cus.branch = lCLU.scanNum(2);
				cus.rate = lCLU.scanNum(4);
				cus.dscnt = lCLU.scanNum(4);
				cus.extra = lCLU.scanNum(4);
				cus.limchk = lCLU.scanNum(8);
				cus.limcha = lCLU.scanNum(8);
				cus.name = lCLU.scan(30);
			} else
				lCLU.skip(2);
			if (id == 2) {
				cus.adrs = lCLU.scan(30);
				cus.city = lCLU.scan(30);
			}
			if (id == 3) {
				cus.nam2 = lCLU.scan(30);
				cus.dtbl = lCLU.scan(30);
			}
			if (id == 0)
				cus.number = lCLU.skip(46).scan(14).trim();
		} catch (NumberFormatException e) {
			lCLU.error(e, false);
			return 8;
		}
		return 0;
	}

	/*******************************************************************/
	/*                                                                 */
	/* reduce customer number to <= max digits */
	/*                                                                 */

	/**
	 * ***************************************************************
	 */

	static int chk_cusspc(int max) {
		int ind, nbr = 32, sts;

		if (input.key == 0x4d4d)
			while (nbr > 0) {
				String msr = editTxt(input.pb, 20);
				String rule = msr_20spec[--nbr];
				if (rule == null)
					continue;
				for (ind = 0; ind < 20; ind++) {
					char c = rule.charAt(ind);
					if (msr.charAt(ind) != c) {
						if (c == ' ' || msr.charAt(ind) == ' ')
							break;
						if (Character.isDigit(c))
							break;
					}
				}
				if (ind < 20)
					continue;
				for (ind -= input.num; ind < 20; ind++) {
					char c = rule.charAt(ind);
					if (c < ':')
						continue;
					if (c == 'Z')
						if (input.index == 0)
							if (msr.charAt(ind) == '0')
								continue;
					input.push(msr.charAt(ind));
				}
				if (input.index < 1)
					return 3;
				input.reset(input.toString(0, input.index));
				break;
			}
		if (input.num > 16)
			return 2;
		if ((sts = ean_reduce('C', max)) > 0)
			return sts;
		return 0;
	}

	static void cus_print(int station) {
		int sc = sc_value(tra.spf2);
		String s = Mnemo.getText(sc);

		prtDwide(ELJRN + station, Mnemo.getMenu(15 + sc));

		// SARAWAT-ENH-20150507-CGA#A BEG
		if (!CapillaryService.getInstance().isEnabled()) {
			prtLine.init(' ').book(station & 2);
			prtLine.init(s).onto(20, tra.number).book(station);
		}
		// SARAWAT-ENH-20150507-CGA#A END

		if (cus.branch > 0) {
			prtLine.init(Mnemo.getText(54)).onto(20, editNum(cus.spec, 2) + '/' + editNum(cus.branch, 2)).book(station);
		}
		if (cus.fiscalId != null) {
			prtLine.init(Mnemo.getText(74)).onto(20, cus.fiscalId).book(station);
		}
		if (tra.stat > 2) {
			// SARAWAT-ENH-20150507-CGA#A BEG
			if (!CapillaryService.getInstance().isEnabled()) {
				if (WsLoyaltyService.getInstance().isWsLoyaltyEnabled()) {
					prtLine.init(WsLoyaltyService.getInstance().getCustomerInfo(cus)).book(station & 2);
				} else {
					prtLine.init(cus.nam2).book(station & 2);
					prtLine.init(cus.name).book(station & 2);
					prtLine.init(cus.adrs).book(station & 2);
					prtLine.init(cus.city).book(station & 2);
				}
			} else {
				prtLine.init(Mnemo.getText(31)).onto(20, cus.number).book(station & 2);
			}
			// SARAWAT-ENH-20150507-CGA#A END

			if (cus.pnt != 0) {
				prtLine.init(Mnemo.getText(39)).onto(20, editPoints(cus.pnt, true)).book(station & 2);
			}
		}
		prtLine.init(' ').book(station & 2);
	}

	void spc_open(BinIo io, String file) {
		String name = "LAST_CIN.TMP";
		String path = System.getProperty("SPC", "spc");

		io.open("spc", file += ".DAT", 0);
		if (io.file != null)
			return;
		if (ctl.lan > 2 || io == lCGR)
			return;
		int sts = netio.copyF2f(path + "\\" + file, name, false);
		if (sts > 0)
			panel.clearLink(Mnemo.getInfo(sts), 2);
		else if (sts == 0)
			io.open(null, name, 0);
	}

	/*******************************************************************/
	/*                                                                 */
	/* discount after total for paying cash */
	/*                                                                 */

	/**
	 * ***************************************************************
	 */

	static int dsc_line(int sc, long credit, long amount) {
		int ind, rec;

		itm.spf3 = sc;
		itm.crd = credit;
		itm.text = lREG.text;
		itm.rew_amt = amount;
		pit = itm;
		if (tra.tnd == 0)
			GdTrans.tra_total();
		itm = pit;
		itm.index = tra.vTrans.size();
		GdTrans.rbt_distrib();
		itm = tra.vTrans.getElement(itm.index);
		GdTrans.rbt_trans();
		prtDline("SlsBD" + editNum(tra.code, 2));
		prtLine.init(Mnemo.getText(26)).upto(40, editMoney(0, tra.amt)).book(3);
		TView.append(' ', 0x80, Mnemo.getText(26), "", "", editMoney(0, tra.amt), "");
		Promo.updateTotals(credit);
		GdTrans.tra_balance();
		return GdTndrs.tnd_clear(26);
	}

	/*******************************************************************/
	/*                                                                 */
	/* employee / customer discount after late recognition */
	/*                                                                 */

	/**
	 * ***************************************************************
	 */
	static void dsc_late() {
		int rec = tra.vItems.size();

		while (rec-- > 0) {
			itm = tra.vItems.getElement(rec);
			if (itm.id != 'S' && itm.id != 'C')
				continue;
			itm.dsc = getDiscount();
			if (itm.dsc != 0)
				GdSales.itm_discount();
		}
		GdTrans.tra_balance();
		itm = new Itemdata();
	}

	/**
	 * cust/trans id number
	 */
	int action0(int spec) {
		int sts;

		if (spec == 0) {
			if ((sts = pre_valid(0x10)) > 0)
				return sts;
			dspLine.init(' ');
			dspBmap = "NBR_0000";
			return 0;
		}
		if ((sts = forceCard(0x04)) > 0)
			return sts;
		if ((sts = chk_cusspc(16)) > 0)
			return sts;
		itm.number = input.pb;
		if (!tra.isActive())
			GdRegis.set_tra_top();
		prtLine.init(Mnemo.getText(spec)).onto(20, itm.number).book(3);
		dspLine.init(Mnemo.getText(spec)).upto(20, " " + itm.number);
		Itmdc.IDC_write('P', trx_pres(), 2, itm.number, 0, 0l);
		if (sts == 0 && SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().loyaltyResponse();
		}
		return GdTrans.itm_clear();
	}

	/**
	 * pers/cust number
	 */
	int action1(int spec) {

		int sts, stat = 2;
		String nbr = cus.number = input.pb;

        // AMZ-2017-005#NOTA -- ci puo' essere il nuovo caso mnemo[92]="PHONE    "
        // Dove si cerca il cliente tramite il numero di telefono
		if (spec >= 9000) {
			spec -= 9000;
			stat = 3;
		}
		spec = input.prompt.equals(Mnemo.getText(7)) ? M_EMPDSC : M_CUSDSC;
		if ((sts = forceCard(spec)) > 0)
			return sts;
		if ((sts = chk_cusspc(12)) > 0)
			return sts;
		if (spec == M_EMPDSC) {
			lCTL.read(ctl.ckr, lCTL.LOCAL);
			if (lCTL.pers == Integer.parseInt(leftFill(nbr, 8, '0')))
				return 39;
		}
		int rec = reg.find(3, sc_value(spec));
		lREG.read(rec, lREG.LOCAL);

		if ((options[O_Custo] & 0x10) > 0) {
			HoCus.append(cus.number);
			if (HoCus.finish() > 1)
				return 23;
		}
		// PSH-ENH-005-AMZ#BEG -- customer id
		if (GdPsh.isEnabled()) {
            if(GdPsh.isEnabledSMASH()) {// AMZ-2017-003-004#ADD
                // AMZ-2017-003-006#NOTA passaggio tessera
                // la customerCheck vendera' automaticamente gli articoli
                // che il server comunica assieme alla tessera
                sts = GdPsh.customerCheck(cus);
                if (sts != 0) {
                    return sts;
                }
                stat = 3;
            }// AMZ-2017-003-004#ADD
		} // first check philoshopic customer

		// PSH-ENH-005-AMZ#END -- customer id

		// SARAWAT-ENH-20150507-CGA#A BEG
		if (CapillaryService.getInstance().isEnabled()) {
			logger.info("Capillary is enabled");

			GdSarawat.getInstance().setInCommunicationWhitCapillary(true);
			//boolean success = false;
            int success = 0;

			if (GdSarawat.getInstance().isRegistration()) {
				/*String customerMobile = input.pb;
				logger.info("mobile: " + customerMobile);

				if (customerMobile.startsWith("0")) {
					customerMobile = customerMobile.substring(1);
					logger.info("mobile: " + customerMobile);
				}

                success = CommunicationCapillaryForCustomer.getInstance().customerAdd(GdSarawat.getInstance().getCodeCustomerCard(), customerMobile);
                */

                String customerCard = input.pb;
                success = CommunicationCapillaryForCustomer.getInstance().customerAdd(customerCard, GdSarawat.getInstance().getCustomerMobile());

				logger.info("Communication with Capillary for registration customer - success: " + success);
			} else {
                // AMZ-2017-005#BEG --  basta verificare che input inizi con PHONE nmemo[92]
                // E tirargli via gli zeri iniziali.

                logger.info("Searching customer with input = " + input);
                if(input.prompt.equals(Mnemo.getText(92))){
                    String number = input.pb;
                    number = number.replaceFirst("0*","");
                    logger.info("Searching customer by phone number: " + number);
                    success = CommunicationCapillaryForCustomer.getInstance().customerGet(number, true);

                }else{
                    logger.info("Searching customer by Id: " + input.pb);
                    success = CommunicationCapillaryForCustomer.getInstance().customerGet(input.pb, false);
                }
                // AMZ-2017-005#END


                // success = CommunicationCapillaryForCustomer.getInstance().customerGet(input.pb, false); // AMZ-2017-005#DEL
				logger.info("Communication with Capillary for identification customer - success: " + success);
			}

			//if (success) {
            if (success == 0) {
                DevIo.alert(1);

				cus.number = nbr = CommunicationCapillaryForCustomer.getInstance().getCustomer().getExternalId();
				cus.mobile = CommunicationCapillaryForCustomer.getInstance().getCustomer().getMobile();
				cus.name = CommunicationCapillaryForCustomer.getInstance().getCustomer().getName();
				cus.adrs = "";
				cus.city = "";
				cus.nam2 = "";
				cus.dtbl = "";
				cus.fiscalId = null;
				cus.pnt = CommunicationCapillaryForCustomer.getInstance().getCustomer().getLoyaltyPoints();
				logger.info("Points into customer card: " + cus.pnt);
			} else {
                DevIo.alert(0);
				cus.number = "";

				if (CommunicationCapillaryForCustomer.getInstance().isAlreadyRegistered()) {
					logger.info("customer already registered - return error");

					return 85;
				}

				logger.info("communication with capillary for customer failed - return error");
				//return 82;
                return success;
			}

			stat = 3;
		} else {
			logger.info("Capillary is disabled");
			// SARAWAT-ENH-20150507-CGA#A END

			if (lCLU.getSize() > 0) {
				if ((sts = src_clu(cus.number, 1)) > 0) {
					if (src_clu(cus.number, 0) == 0)
						sts = src_clu(cus.number, 1);
				}
				if (sts > 0) {
					if ((lREG.tflg & 0x20) == 0)
						return sts;
					if (panel.clearLink(Mnemo.getInfo(40), 3) < 2)
						return sts;
				} else {
					if (cus.branch > 2 && cus.branch < 10) {
						oplLine.init('*').onto(2, editNum(cus.branch, 2)).upto(20, cus.number).show(2);
						return 25;
					}
					if (src_clu(cus.number, 2) > 0) {
						cus.city = cus.adrs = leftFill("", 30, '.');
					}
					if (src_clu(cus.number, 3) > 0)
						cus.nam2 = "";
					stat = 3;
				}
			}
		}
		if ((options[O_Custo] & 0x02) > 0) {
			if ((sts = FiscalId.accept(16)) > 0)
				return sts;
			else
				cus.fiscalId = input.pb;
		}
		tra.spf2 = spec;
		tra.number = nbr;
		if (!tra.isActive())
			GdRegis.set_tra_top();
		if ((tra.stat = stat) > 2) {
			//if (!GdPsh.isEnabled() && !CapillaryService.getInstance().isEnabled()) { // AMZ-2017-003-004#DEL
            if ((!GdPsh.isEnabled() || !GdPsh.isEnabledSMASH()) && !CapillaryService.getInstance().isEnabled()) { // AMZ-2017-003-004#ADD
               if (ctl.mode != M_RENTRY) {
                    for (int ind = 1; ind < 5; ind++) {
                        if (rCLS.find("CP" + ind, cus.number) > 0)
                            cus.pnt += rCLS.block.items;
                    }
                }
            }
			if (cus.pnt != 0)
				showPoints(tra.pnt += cus.pnt);
			tra.rate = cus.rate;
			if ((tra.spf3 & 4) > 0)
				tra.xtra = cus.extra;
			Promo.identifyCustomer();
		} else
			tra.rate = lREG.rate;
		cus_print(3);
		TView.append('>', 0x00, Mnemo.getMenu(15 + sc_value(tra.spf2)), "", "", "", "");
		if (tra.stat > 2) {
			if ((options[O_Custo] & 0x01) > 0)
				showShopper();
			if (ctl.lan == 1)
				prtBlock(ELJRN + 2, offl_txt, 0, offl_txt.length);
		}
		if (sc_checks(3, sc_value(M_REBATE)) > 0)
			return 0;
		if (tra.number.length() < 9 && !GdSarawat.getInstance().getInCommunicationWhitCapillary()) // SARAWAT-ENH-20150507-CGA#A
			spc_open(lCIN, tra.number);
		if (cus.branch > 0)
			spc_open(lCGR, "GROUP_" + editNum(cus.branch, 2));

		if (GdPsh.isEnabledPXL()){
			dspLine.init(cus.name);
		}else {
			dspLine.init(Mnemo.getText(sc_value(tra.spf2))).upto(20, tra.number);
		}
		if (GdPsh.isEnabled() && SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().loyaltyResponse();
		}
		if (tra.rate != 0) {
			if ((lREG.tflg & 0x40) > 0) {
				event.nxt = event.alt;
				oplLine.init(Mnemo.getText(20)).upto(20, editDec(tra.rate, 1));
			} else
				dsc_late();
		}
		// SARAWAT-ENH-20150507-CGA#A BEG
		if (CapillaryService.getInstance().isEnabled()) {
			Itmdc.IDC_write('c', 0, tra.spf3, tra.number, tra.cnt, 0l);
			GdSarawat.getInstance().setIsRegistration(false);
		}
		// SARAWAT-ENH-20150507-CGA#A END

		return 0;
	}

	/**
	 * pers/cust discount rate
	 */
	int action2(int spec) {
		if (input.num > 0) {
			int rate = input.scanNum(input.num);
			if (rate > 499)
				return 8;
			tra.rate = rate;
		}
		dspLine.init(Mnemo.getText(20)).upto(20, editRate(tra.rate));
		dsc_late();
		return 0;
	}

	/**
	 * total discount rate
	 */
	int action3(int spec) {
		int rate = 0, sts;
		long amount = tra.amt + tra.dsc_amt + tra.chg_amt + tra.tld_amt;

		if (input.num > 0) {
			input.dec++;
			if ((sts = input.adjust(1)) > 0)
				return sts;
			if ((rate = input.scanNum(input.num)) == 0)
				return 8;
		}
		if ((sts = sc_checks(4, spec)) > 0)
			return sts;
		int limit = tra.stat > 2 ? cus.dscnt : lREG.rate;
		if (rate == 0)
			rate = limit;
		else if (rate > limit)
			if ((lREG.tflg & 0x40) == 0)
				return 46;
		if ((lREG.tflg & 0x20) > 0)
			amount = tra.dsc_sls;
		long credit = roundBy(amount * rate, 1000);
		if (credit == 0 || credit == amount)
			return 8;
		itm.rate = rate;
		return dsc_line(spec, -credit, amount);
	}

	/**
	 * total discount amount
	 */
	int action4(int spec) {
		int sts;
		long credit = input.scanNum(input.num);
		long amount = tra.amt + tra.dsc_amt + tra.chg_amt + tra.tld_amt;

		if (credit == 0 || credit >= Math.abs(amount))
			return 8;
		if ((sts = sc_checks(4, spec)) > 0)
			return sts;
		if ((lREG.tflg & 0x20) > 0)
			if (credit >= Math.abs(amount = tra.dsc_sls))
				return 46;
		return dsc_line(spec, amount < 0 ? credit : -credit, amount);
	}

	/**
	 * local invoice / slip
	 */
	int action6(int spec) {
		int sel = (options[O_copy2] & 1) > 0 ? 4 : 2;

		if (input.num == 0) {
			if (!DevIo.station(4))
				return 7;
			if (slpFind(tra.code) < 0)
				return 7;
			slpStatus(1, 1);
			event.nxt = event.alt;
			return 0;
		}
		if (!DevIo.station(sel))
			return 7;
		if (input.num < 4)
			return 3;
		int nbr = input.scanNum(input.num);
		return ElJrn.second_cpy(sel, nbr, 1);
	}

	/**
	 * remote invoice
	 */
	int action7(int spec) {
		int sts, type = 0;

		if (spec == 0) {
			if (ctl.lan > 1)
				return 7;
			dspLine.init(Mnemo.getMenu(48));
			return 0;
		}
		if (spec == 1) {
			if ((sts = tid_reduce()) > 0)
				return sts;
			rBIL.reg = input.scanKey(3);
			rBIL.tran = input.scanNum(4);
			if ((sts = Magic.src_frec(rBIL.tran, rBIL.reg)) < 1)
				return sts < 0 ? 16 : 8;
			if (lIDC.skip(34).scanNum(1) != M_GROSS)
				return 7;
			return 0;
		}
		if (input.num > 0) {
			type = input.scanNum(input.num);
			if (type >= tnd.length)
				return 46;
			if (tnd[type].unit == 0)
				return 8;
		}
		if ((sts = rBIL.write(type)) < 1)
			return sts < 0 ? 16 : 7;
		prtLine.init(Mnemo.getMenu(48)).onto(20, Mnemo.getText(13)).upto(37, editNum(type, 2) + tnd[type].symbol)
				.book(1);
		prtLine.init(Mnemo.getText(3)).upto(17, editKey(rBIL.reg, 3)).onto(20, Mnemo.getText(30))
				.upto(37, editNum(rBIL.tran, 4)).book(1);
		return GdTrans.tra_clear();
	}

	/**
	 * demographic data query
	 */
	int action8(int spec) {
		logger.info("key: " + input.key + " spec: " + spec);
		if (spec == 9999)
			return 5;

		if (input.num == input.max)
			if (input.pb.charAt(0) > itm.qty)
				return 8;
		itm.amt = input.num > 0 ? Long.parseLong(input.pb) : 0;
		Itmdc.IDC_write('Q', itm.spf1, 0, itm.number, itm.cnt, itm.amt);
		prtLine.init(input.prompt);
		for (int ind = 20; input.num > 0;) {
			if (prtLine.peek(--ind) == input.POINT)
				continue;
			prtLine.poke(ind, input.pb.charAt(--input.num));
		}
		prtLine.book(itm.flag & 3);
		if ((itm.flag & 2) > 0) {
			itm.text = prtLine.toString(0, 20);
			tra.vItems.addElement('Q', itm.copy());
		}
		while (lDDQ.read(++lDDQ.recno) > 0) {
			if (Match.dd_check(itm.mark, itm.spf1))
				if (Match.dd_next() == 0)
					return 0;
		}
		event.nxt = event.alt;
		return action9(ERROR);
	}

	/**
	 * exit data query
	 */
	int action9(int spec) {
		if (spec == 0) {
			if (itm.spec != 'C')
				return 5;
		}
		panel.jrnPicture(null);
		if (tra.stat > 2) {
			if ((options[O_Custo] & 0x01) > 0)
				showShopper();
		}
		return GdTndrs.tnd_clear(0);
	}
}
