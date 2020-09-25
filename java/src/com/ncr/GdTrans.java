package com.ncr;

import com.ncr.capillary.CouponDetails.RedeemCoupon;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;

class GdTrans extends Action {
	private static final Logger logger = Logger.getLogger(GdTrans.class);

	// SARAWAT-ENH-20150507-CGA#A BEG
	private static ArrayList<String> listFailedCoupon = new ArrayList<String>();
	// SARAWAT-ENH-20150507-CGA#A END


	static void chg_auto() {
		int sc = sc_value(M_CHARGE);

		itm = new Itemdata();
		itm.number = editRate(tra.xtra);
		itm.cnt = tra.chg_cnt;
		itm.amt = tra.chg_amt;
		Itmdc.IDC_write('D', sc, itm.dpt_nbr = 0, itm.number, itm.cnt, itm.amt);
		prtLine.init(' ').onto(20, Mnemo.getText(23)).upto(40, editMoney(0, tra.amt)).book(3);
		lREG.read(reg.find(4, sc), lREG.LOCAL);
		prtLine.init(lREG.text).upto(25, itm.number).upto(40, editMoney(0, itm.amt)).book(3);
		TView.append('D', 0x00, lREG.text, "", itm.number, editMoney(0, itm.amt), "");
		tra.amt += itm.amt;
		tra.chg_amt = tra.chg_cnt = 0;
	}

	static void dsc_auto() {
		int sc = sc_value(tra.spf2);

		Itmdc.IDC_write('J', sc, 0, editRate(tra.rate), tra.dsc_cnt, tra.dsc_amt);
		prtLine.init(' ').onto(20, Mnemo.getText(23)).upto(40, editMoney(0, tra.amt)).book(3);
		lREG.read(reg.find(3, sc), lREG.LOCAL);
		prtLine.init(lREG.text).book(3);
		itm = new Itemdata();
		for (itm.sit = 0; itm.sit < rbt.length; itm.sit++) {
			TableRbt ptr = rbt[itm.sit];
			itm.amt = ptr.dsc_sls.total - ptr.amt;
			if (itm.amt != 0) {
				itm.rate = (tra.spf2 & M_EMPDSC) > 0 ? ptr.rate_empl : ptr.rate_cust;
				if (itm.rate > tra.rate)
					itm.rate = tra.rate;
				prtLine.init(Mnemo.getText(58)).upto(17, editDec(itm.amt, tnd[0].dec)).upto(25, editRate(itm.rate))
						.upto(40, editMoney(0, ptr.amt));
				prtLine.onto(1, editNum(itm.sit, 1)).book(3);
				String tags = "  " + Mnemo.getText(58).substring(0, 1) + itm.sit;
				TView.append(' ', 0x00, rbt[itm.sit].text, editDec(itm.amt, tnd[0].dec), editRate(itm.rate),
						editMoney(0, ptr.amt), tags);
			}
		}
		tra.amt += tra.dsc_amt;
		tra.dsc_amt = tra.dsc_cnt = 0;
	}

	static void rbt_auto() {
		if (ctl.mode == M_RENTRY || tra.spf1 > 0)
			return;
		Promo.readItemDiscounts();
		Promo.updateTotals(tra.dsc_amt + tra.chg_amt);
		tra.rbt_amt = -tra.tld_amt;
		itm = new Itemdata();
		itm.spf3 = sc_value(M_TOTRBT);
		for (itm.dpt = 0; itm.dpt++ < dpt.key.length;) {
			itm.amt = dpt.dsc_sls[itm.dpt - 1];
			itm.com = dpt.pnt_sls[itm.dpt - 1];
			if (itm.amt == 0 && itm.com == 0)
				continue;
			itm.spf2 = 0;
			itm.dpt_nbr = dpt.key[itm.dpt - 1];
			itm.number = "SD          " + editKey(itm.dpt_nbr, 4);
			Match.rbt_total(lCIN);
			Match.rbt_total(lCGR);
			Match.rbt_total(lRLU);
			if (itm.spf2 > 0)
				rbt_distrib();
		}
		for (itm.sit = 0; itm.sit < rbt.length; itm.sit++) {
			TableRbt ptr = rbt[itm.sit];
			itm.amt = ptr.dsc_sls.total;
			itm.com = ptr.pnt_sls.total;
			if (itm.amt == 0 && itm.com == 0)
				continue;
			itm.dpt_nbr = itm.spf2 = 0;
			itm.number = "SI" + editTxt(itm.sit, 14);
			Match.rbt_total(lCIN);
			Match.rbt_total(lCGR);
			Match.rbt_total(lRLU);
			if (itm.spf2 > 0)
				rbt_distrib();
		}
		itm.amt = tra.dsc_sls;
		itm.com = tra.pnt_sls;
		itm.sit = itm.dpt_nbr = itm.spf2 = 0;
		if (itm.amt != 0 && itm.com != 0) {
			itm.number = "ST" + editTxt("", 14);
			Match.rbt_total(lCIN);
			Match.rbt_total(lCGR);
			Match.rbt_total(lRLU);
		}
		if (itm.spf2 > 0)
			rbt_distrib();
		Promo.updateTotals(tra.rbt_amt += tra.tld_amt);
		Promo.readTranDiscounts();
		tra_balance();
		Promo.setTransactionAmount(tra.amt + tra.dsc_amt + tra.chg_amt + tra.tld_amt);
	}

	static void pts_trans(int sc) {
		accumPts(sc, itm.pnt, itm.amt);
		Itmdc.IDC_write('G', sc, 0, itm.number, itm.pnt, itm.amt);
		if (itm.promo.length() > 0)
			Itmdc.IDC_write('K', trx_pres(), itm.flag, itm.promo, itm.rew_qty, itm.rew_amt);
		if (sc < 5 || itm.promo.length() == 0)
			showPoints(tra.pnt += itm.pnt);
		prtLine.init(itm.text).book(3);
		if (!itm.number.startsWith("SD")) {
			prtLine.init(Mnemo.getText(58));
			if (itm.number.startsWith("SI"))
				prtLine.onto(1, itm.number.substring(15));
		} else
			prtLine.init(itm.number.substring(12));
		prtLine.upto(17, editDec(itm.amt, tnd[0].dec)).onto(20, editPoints(itm.pnt, false)).book(3);
	}

	static void rbt_distrib() {
		int ind, rec;

		if (itm.rate == 0)
			itm.number = "";
		else
			itm.number = editRate(itm.rate);
		itm.cnt = tra.dsc_cnt;
		itm.amt = tra.dsc_sls;
		if (itm.dpt_nbr > 0) {
			itm.cnt = dpt.dsc_cnt[itm.dpt - 1];
			itm.amt = dpt.dsc_sls[itm.dpt - 1];
			if (itm.amt == 0) /* dp = disastrous proportions */ {
				lDPT.read(itm.dpt, LOCAL);
				accumTax(10, lDPT.vat, 0, itm.crd);
				vat[lDPT.vat].tld_amt += itm.crd;
			}
		} else if (itm.sit > 0) {
			itm.cnt = rbt[itm.sit].dsc_sls.items;
			itm.amt = rbt[itm.sit].dsc_sls.total;
		}
		tra.tld_amt += itm.crd;
		if (itm.rew_amt == 0) /* trx rollback */
			itm.rew_amt = itm.amt;
		tra.vTrans.addElement('D', itm.copy());
		if (itm.amt == 0)
			return;
		itm.dec = (int) roundBy(itm.crd * 10000 / itm.amt, 10);
		for (ind = 0; ind < tra.vItems.size(); ind++) {
			Itemdata ptr = tra.vItems.getElement(ind);
			if (ptr.id != 'S' && ptr.id != 'C')
				continue;
			if (ptr.sit == 0)
				continue;
			if (itm.dpt_nbr > 0) {
				for (rec = ptr.dpt; rec-- > 0; rec = dpt.grp[rec]) {
					if (dpt.key[rec] == itm.dpt_nbr)
						break;
				}
				if (rec < 0)
					continue;
			} else if (itm.sit > 0) {
				if (ptr.sit != itm.sit)
					continue;
			}
			itm.amt = roundBy(itm.dec * (ptr.amt + ptr.dsc), 1000);
			accumTax(10, ptr.vat, 0, itm.amt);
			vat[ptr.vat].tld_amt += itm.amt;
			itm.crd -= itm.amt;
			itm.tnd = ptr.vat;
		}
		accumTax(10, itm.tnd, 0, itm.crd);
		vat[itm.tnd].tld_amt += itm.crd;
	}

	//DMA-VENTILA_IVA_SU_K# BEG
	static void rbt_distrib_k(long sconto) {
		int ind, rec;


		long ventila = 0, totale = 0, itm_amt = 0, ventilato = 0;

		for (ind = 0; ind < tra.vItems_k.size(); ind++) {
			Itemdata ptr = tra.vItems_k.getElement(ind);
			if (ptr.id != 'S' && ptr.id != 'C')
				continue;
			if (ptr.sit == 0)
				continue;

			totale += ptr.amt * ptr.qty;
		}

		Itemdata ptr = new Itemdata();
		for (ind = 0; ind < tra.vItems_k.size(); ind++) {
			ptr = tra.vItems_k.getElement(ind);
			if (ptr.id != 'S' && ptr.id != 'C')
				continue;
			if (ptr.sit == 0)
				continue;
			if (itm.dpt_nbr > 0) {
				for (rec = ptr.dpt; rec-- > 0; rec = dpt.grp[rec]) {
					if (dpt.key[rec] == itm.dpt_nbr)
						break;
				}
				if (rec < 0)
					continue;
			} else if (itm.sit > 0) {
				if (ptr.sit != itm.sit)
					continue;
			}
			itm_amt = roundBy(itm.dec * (ptr.amt + ptr.dsc), 1000);
			ventila = (long)(((double)sconto/totale) * itm_amt);
			ventilato += ventila;

			accumTax(10, ptr.vat, 0, ventila);
		}
		accumTax(10, ptr.vat, 0, sconto - ventilato);
	}
	//DMA-VENTILA_IVA_SU_K# END

	static void rbt_trans() {
		String sBase = "";

		accumReg(4, itm.spf3, itm.cnt, itm.crd);
		Itmdc.IDC_write('D', itm.spf3, itm.sit, itm.number, itm.cnt, itm.crd);
		prtLine.init(itm.text);
		if (itm.promo.length() == 0) {
			if (itm.spf3 != 7) {
				prtLine.book(3);
				prtLine.init(Mnemo.getText(58));
				if (itm.dpt_nbr > 0)
					prtLine.init(editKey(itm.dpt_nbr, 4));
				else if (itm.sit > 0)
					prtLine.onto(1, editNum(itm.sit, 1));
				prtLine.upto(17, sBase = editDec(itm.rew_amt, tnd[0].dec));
			}
		} else {
			Itmdc.IDC_write('K', trx_pres(), itm.flag, itm.promo, itm.rew_qty, itm.rew_amt);
			Promo.writePromoDetails(itm.promo);
		}
		prtLine.upto(25, itm.number).upto(40, editMoney(0, itm.crd)).book(3);
		TView.append('D', 0x00, itm.text, sBase, itm.number, editMoney(0, itm.crd), "");
		tra.amt += itm.crd;
		tra.tld_amt -= itm.crd;
	}

	static void itm_trans(boolean forth) {
		int cnt = forth ? itm.cnt : -itm.cnt;
		long amt = forth ? itm.amt : -itm.amt;
		long dsc = getDiscount();

		if (forth) {
			itm.spf2 = M_REBATE;
			tra.vTrans.addElement('C', itm);
		} else
			dsc = 0 - dsc;
		accumDpt(2, cnt, amt);
		accumDpt(1, 0, dsc);
		GdSales.itm_tally(0, amt + dsc);
		tra.tld_amt += amt;
		tra.dsc_amt += dsc;
	}

	static void vat_auto() {
		int rec;

		if ((tra.spf3 & 1) > 0) {
			prtDwide(ELJRN + 3, Mnemo.getMenu(49));
			if (tra.taxidn.length() > 0)
				prtLine.init(Mnemo.getText(43)).onto(20, tra.taxidn).book(3);
		}
		for (int ind = 0; ind < vat.length; ind++) {
			if ((rec = reg.find(7, 11 + ind)) < 1)
				continue;
			Sales sls = reg.sales[rec - 1][0];
			if (sls.isZero())
				continue;
			itm = new Itemdata();
			itm.cnt = sls.items;
			itm.amt = sls.total;
			itm.number = editRate(vat[ind].rate);
			itm.text = vat[ind].text.substring(0, 8);
			Itmdc.IDC_write('V', ind, 1, itm.number, itm.cnt, itm.amt);
			if ((tra.spf3 & 1) > 0 && reg.find(7, 41 + ind) > 0) {
				itm.dsc = 0;
				accumTax(40, ind, itm.cnt, itm.amt);
				Itmdc.IDC_write('V', ind, 4, tra.taxidn, itm.cnt, itm.amt);
			} else {
				rec = reg.find(7, 51 + ind);
				sls = rec > 0 ? reg.sales[rec - 1][0] : new Sales();
				itm.dsc = vat[ind].collect(itm.amt -= sls.total);
				accumTax(0, ind, itm.cnt, itm.dsc);
				Itmdc.IDC_write('V', ind, 0, itm.number, itm.cnt, itm.dsc);
				if (!sls.isZero()) {
					Itmdc.IDC_write('V', ind, 5, itm.number, sls.items, sls.total);
				}
			}
			prtLine.init(' ').upto(25, itm.text + itm.number).onto(0, editDec(itm.amt, tnd[0].dec).trim())
					.upto(40, editMoney(0, itm.dsc)).book(3);
			TView.append('V', 0x00, itm.text + editDec(itm.amt, tnd[0].dec), editInt(itm.cnt), itm.number,
					editMoney(0, itm.dsc), "");
			tra.amt += itm.dsc;
		}
		itm.dsc = tra.bal - tra.amt;
		if (itm.dsc == 0)
			return;
		int sc = sc_value(M_RNDTAX);
		if ((rec = reg.find(4, sc)) < 1)
			return;
		lREG.read(rec, lREG.LOCAL);
		itm.text = lREG.text;
		itm.number = "";
		accumReg(4, sc, itm.cnt = signOf(itm.dsc), itm.dsc);
		Itmdc.IDC_write('D', sc, 0, "", itm.cnt, itm.dsc);
		prtLine.init(itm.text).upto(40, editMoney(0, itm.dsc)).book(3);
		TView.append('D', 0x00, itm.text, "", "", editMoney(0, itm.dsc), "");
		tra.amt += itm.dsc;
	}

	static void vat_exempt() {
		int rec;

		prtDwide(ELJRN + 3, Mnemo.getMenu(49));
		if (tra.taxidn.length() > 0)
			prtLine.init(Mnemo.getText(43)).onto(20, tra.taxidn).book(3);
		for (int ind = 0; ind < vat.length; ind++) {
			if ((rec = reg.find(7, 11 + ind)) < 1)
				continue;
			Sales sls = reg.sales[rec - 1][0];
			if (sls.isZero())
				continue;
			itm = new Itemdata();
			itm.cnt = sls.items;
			itm.amt = sls.total;
			itm.number = editRate(vat[ind].rate);
			if (reg.find(7, 41 + ind) < 1)
				continue;
			accumTax(10, ind, -itm.cnt, -itm.amt);
			accumTax(40, ind, itm.cnt, itm.amt);
			Itmdc.IDC_write('V', ind, 4, tra.taxidn, itm.cnt, itm.amt);
			itm.dsc = 0 - vat[ind].exempt(itm.amt);
			if (reg.find(7, 91 + ind) > 0) {
				accumTax(90, ind, itm.cnt, itm.dsc);
				Itmdc.IDC_write('V', ind, 9, itm.number, itm.cnt, itm.dsc);
			}
			itm.text = vat[ind].text.substring(0, 8) + editRate(vat[ind].rate);
			prtLine.init(' ').upto(25, itm.text).onto(0, editDec(itm.amt, tnd[0].dec).trim())
					.upto(40, editMoney(0, itm.dsc)).book(3);
			tra.amt += itm.dsc;
		}
		prtLine.init(Mnemo.getText(55)).upto(40, editMoney(0, tra.amt)).book(3);
	}

	/**
	 * clear/end of item
	 */
	static int itm_clear() {
		itm = new Itemdata();
		if ((input.optAuth & 1) == 0 && (input.lck & 0x10) > 0) /* until end of item */
			if ((input.optAuth & 2) > 0 || (input.lck & 0x04) == 0) {
				input.lck &= ~0x10;
				showAutho();
			}
		return 0;
	}

	/**
	 * clear/end of transaction
	 */
	static int tra_clear() {
		if (tra.slip > 0)
			slpStatus(0, tra.slip);
		input.lck &= (input.lck & 4) > 0 ? 0x1F : 0x0F;
		if ((input.optAuth & 2) > 0)
			if (ctl.ckr_nbr < 800)
				input.lck &= ~0x10;
		event.dpos = tnd[0].dec;
		tra = new Transact();
		cus = new Customer();
		ecn = new PayCards();
		itm = new Itemdata();
		pit = null;
		for (int ind = rcd_tbl.length; ind > 0; rcd_tbl[--ind] = 0)
			;
		if (ctl.ckr_nbr > 0) {
			if (ctl.ckr_nbr < 800) {
				BcrIo.bcrTrans();
				dspLine.init(Mnemo.getInfo(ctl.mode > 0 ? ctl.mode + 17 : 53));
				if (ctl.alert)
					input.lck |= 0x20;
			} else {
				event.nxt = event.alt;
				dspLine.init(Mnemo.getText(2));
			}
		} else
			dspLine.init(Mnemo.getInfo(0));
		showHeader(ctl.ckr_nbr > 0);
		showMinus(false);
		GdPsh.reset(); // PSH-ENH-001-AMZ#ADD -- reset
		// SARAWAT-ENH-20150507-CGA#A BEG
		if (CapillaryService.getInstance().isEnabled()) {
			CapillaryService.getInstance().resetAllParam();
		}
		GdSpinneys.getInstance().clearCoupons();
		Struc.verifone.cleanReceiptData();

		if (GdSarawat.getInstance().isCustomerCardRequestEnabled() && CapillaryService.getInstance().isEnabled()
				&& !GdSarawat.getInstance().isCkrClose()
				&& !GdSigns.isEod()) {  // FIX-20170413-CGA#A
			logger.info("tra_clear - show prompt for request customer card");

			stsLine.init("").upto(20, "").show(2);
			if (panel.clearLink(Mnemo.getText(83), 0x23) == 2) { // "DataCustomer Card?"
				GdSarawat.getInstance().setHaveCustomerCard(true);
				dspLine.init(Mnemo.getMenu(23));
			} else {
				if (GdSarawat.getInstance().isAskForCustomerRegistration()) {
					logger.info("tra_clear - show prompt for registration");

					if (panel.clearLink(Mnemo.getText(84), 0x23) == 2) { // "Register customer?"
						if (event.find(0x94, event.nxt) > 0) {
							return GdSarawat.getInstance().action0(0);
						}
					}
				}
			}
		}
		// SARAWAT-ENH-20150507-CGA#A END

		return input.sel = 0;
	}

	static void tra_balance() {
		int ind, opt = options[O_CandC], rec;
		long discount = 0;

		tra.bal = tra.amt + tra.dsc_amt + tra.chg_amt + tra.tld_amt;
		if (opt > 0 && tra.mode == M_GROSS) {
			for (ind = 0; ind < vat.length; ind++) {
				if ((rec = reg.find(7, 11 + ind)) < 1)
					continue;
				Sales sls = reg.sales[rec - 1][0];
				if (sls.isZero())
					continue;
				if ((tra.spf3 & 1) > 0)
					if (reg.find(7, 41 + ind) > 0)
						continue;
				long amt = sls.total + vat[ind].tld_amt;
				if ((rec = reg.find(7, 51 + ind)) > 0)
					amt -= reg.sales[rec - 1][0].total;
				tra.bal += vat[ind].collect(amt);
			}
			tra.bal = roundBy(tra.bal, opt) * opt;
		} else if (tra.mode == M_GROSS && (tra.spf3 & 1) > 0) {
			for (ind = 0; ind < vat.length; ind++) {
				if ((rec = reg.find(7, 11 + ind)) < 1)
					continue;
				Sales sls = reg.sales[rec - 1][0];
				if (sls.isZero())
					continue;
				if (reg.find(7, 41 + ind) < 1)
					continue;
				tra.bal -= vat[ind].exempt(sls.total + vat[ind].tld_amt);
			}
		}
		mon.clock = ERROR;
		ind = (tra.spf3 & 1) > 0 ? 55 : 24;
		hdrLine.init(Mnemo.getText(ind)).upto(20, editMoney(0, tra.bal)).show(0);
		if ((options[O_Custo] & 0x04) > 0)
			ind = 0;
		hdrLine.init(Mnemo.getText(ind)).upto(20, editMoney(0, tra.bal)).show(13);
		for (ind = 0; ind < 8; ind++) {
			if ((rec = reg.find(3, 1 + ind)) < 1)
				continue;
			discount -= reg.sales[rec - 1][0].total;
		}
		if (discount == 0)
			stsLine.init(' ');
		else
			stsLine.init(Mnemo.getText(20)).upto(20, editMoney(0, discount));
		stsLine.show(5);
	}

	static void tra_total() {
		int ind;

		for (ind = 0; ind < tra.vTrans.size(); ind++) {
			itm = tra.vTrans.getElement(ind);
			if (itm.id != 'C')
				continue;
			itm_trans(false);
			GdSales.crd_line();
		}
		if ((tra.slip & 0x40) > 0)
			Clean.print();
		if (tra.dsc_amt != 0)
			dsc_auto();
		if (tra.chg_amt != 0)
			chg_auto();
		for (ind = 0; ind < tra.vTrans.size(); ind++) {
			itm = tra.vTrans.getElement(ind);
			if (itm.id == 'D')
				rbt_trans();
			else if (itm.id == 'G')
				pts_trans(itm.spf3);
		}
		Promo.bookComplexRewards();
		if (tra.mode <= M_GROSS)
			tra.tnd = 2;
		showTotal(0);
		tra.gpo = lGPO.getSize();
		ind = (tra.spf1 & M_TRVOID) > 0 ? 1 : 0;
		if (tra.mode == 0) {
			accumReg(8, 45 + (tra.code == 6 ? 2 : 0) + ind, tra.cnt, tra.amt);
		}
		if (tra.mode == M_GROSS) {
			accumReg(8, 1, tra.cnt, tra.amt);
			if (tra.code == 7) {
				accumReg(8, 4, tra.cnt, tra.amt);
			} else
				accumAct(ctl.time / 100, tra.cnt, tra.amt);
			if (tra.amt < 0) {
				accumReg(8, 2, tra.cnt, tra.amt);
			}
			accumReg(8, 41 + ((tra.spf1 & M_TRRTRN) > 0 ? 2 : 0) + ind, tra.cnt, tra.amt);
		}
		dspLine.init(Mnemo.getText(24)).upto(20, editMoney(0, tra.amt));
		if ((options[O_DWide] & 1) > 0) {
			prtLine.init(Mnemo.getText(21)).upto(17, editInt(tra.cnt)).book(3);
			prtDline("SlsTL" + editNum(tra.code, 2));
			prtDwide(ELJRN + 3, dspLine.toString());
		} else {
			prtDline("SlsTL" + editNum(tra.code, 2));
			prtLine.init(Mnemo.getText(21)).upto(17, editInt(tra.cnt)).onto(20, dspLine.toString()).book(3);
		}
		TView.append(' ', 0x80, Mnemo.getText(24), editInt(tra.cnt), "", editMoney(0, tra.amt), "");
		if (tra.pnt != 0) {
			prtDline("PtsTL" + editNum(tra.code, 2));
            //SARAWAT-ENH-20150507-CGA#A BEG
            if (!CapillaryService.getInstance().isEnabled()) {
                prtLine.init(Mnemo.getText(39)).onto(20, editPoints(tra.pnt, true)).book(3);
            }
            //SARAWAT-ENH-20150507-CGA#A END
            //prtLine.init(Mnemo.getText(39)).onto(20, editPoints(tra.pnt, true)).book(3); //SARAWAT-ENH-20150507-CGA#D

			prtLine.init(' ').book(2);
		}
	}

	static void tra_taxes() {
		tra.tnd = 1;
		if (tra.mode != M_GROSS)
			return;
		if (options[O_CandC] > 0) {
			vat_auto();
			prtLine.init(Mnemo.getText(26)).upto(40, editMoney(0, tra.amt)).book(3);
		} else if ((tra.spf3 & 1) > 0)
			vat_exempt();
	}

	static int tra_finish() {
		// PSH-ENH-005-AMZ#BEG -- customer points
		if (GdPsh.isEnabled()) {
            if (GdPsh.isEnabledSMASH()) {// AMZ-2017-003-004#ADD
                GdPsh.customerUpdate(cus, tra);
                GdPsh.sendDataCollect();//DMA-TLOG_UPLOADING#A
            } // AMZ-2017-003-004#ADD
		}
		GdSpinneys.getInstance().confirmCoupons(tra);
        GdSarawat.deactivateQuantityKeyForcedAccept();// AMZ-2017-004-002#ADD

		//INSTASHOP-RESUME-CGA#A BEG
		/*if (!eanItemComm.trim().equals("")) {
			handleInstashopResume(numberTraResume);
		}*/
		//INSTASHOP-RESUME-CGA#A END
		// PSH-ENH-005-AMZ#BEG -- customer points
		// SARAWAT-ENH-20150507-CGA#A BEG
		if (CapillaryService.getInstance().isEnabled()) {
			logger.info("tra_finish - capillary is enabled");

			long npoints = CommunicationCapillaryForPoints.getInstance().getNPointsRedeem();
			logger.info("tra_finish - npoint used: " + npoints);

			long valuePromo = 0;
			long valuePromoDiscount = 0;

			if (!CommunicationCapillaryForPoints.getInstance().getRedemptionPvDiscount().equals("")) {
				valuePromo = Promo.getPromovar(
						Long.parseLong("9" + CommunicationCapillaryForPoints.getInstance().getRedemptionPvDiscount()));
				valuePromoDiscount = Promo.getPromovar(
						Long.parseLong("1" + CommunicationCapillaryForPoints.getInstance().getRedemptionPvDiscount()));
			}

            int redeemPnt = 0;
			if (valuePromo != 0) {
				if (GdSarawat.getInstance().getDiscountPoints() == -valuePromoDiscount) {
                    redeemPnt = CommunicationCapillaryForPoints.getInstance().pointsRedeem(tra, cus, npoints);
					//successRedeemPoint = CommunicationCapillaryForPoints.getInstance().pointsRedeem(tra, cus, npoints);
                    tra.successRedeemPoint = redeemPnt == 0;
					logger.info("tra_finish - pointsRedeem error code: " + redeemPnt);
				} else {
					tra.successRedeemPoint = false;
				}
			} else {
				if (GdSarawat.getInstance().isAppliedDiscountPoints()) {
					tra.successRedeemPoint = false;
				}
			}

            logger.info("tra_finish - pointsRedeem successRedeemPoint: " + tra.successRedeemPoint);
			if (!tra.successRedeemPoint) {
                if (redeemPnt > 2000) {
                    logger.info("tra_finish - pointsRedeem error code: " + redeemPnt);

                    String msg = GdSarawat.getInstance().readErrorCodeCapillary(redeemPnt);
                    logger.info("tra_finish - pointsRedeem error message: " + msg);

                    panel.clearLink(msg, 0x81);
                } else {
                    logger.info("tra_finish - pointsRedeem error code: " + redeemPnt);
                    logger.info("tra_finish - pointsRedeem error message: " + Mnemo.getInfo(79));

                    panel.clearLink(Mnemo.getInfo(79), 0x81); // REDEEM POINT FAILED
                }
			}

			ArrayList<RedeemCoupon> listCoupon = new ArrayList<RedeemCoupon>();
			listCoupon = CommunicationCapillaryForCoupon.getInstance().getListCouponIsRedeemable();

			boolean notApplied = false;
			boolean failed = false;

			logger.info("tra_finish - size listCoupon used: " + listCoupon.size());

            int capillarySuccess = 0;
			for (int i = 0; i < listCoupon.size(); i++) {
				//boolean success = true;
                capillarySuccess = 0;

				if (listCoupon.get(i).isApplied()) {
					logger.info("tra_finish - coupon " + listCoupon.get(i).getCode() + " applied");

                    //long valuePromoCoupons = -Promo.getPromovar(Long.parseLong("4" + listCoupon.get(i).getCode()));
                    long valuePromoCoupons = -Promo.getPromovar(Long.parseLong("4" + listCoupon.get(i).getDiscountCode()));
                    if(listCoupon.get(i).getDiscount() == 0D && valuePromoCoupons == 0) {
                        notApplied = true;
                        listFailedCoupon.add(listCoupon.get(i).getCode());
                    }
                    else {
                        capillarySuccess = CommunicationCapillaryForCoupon.getInstance().couponRedeem(tra, cus,
                                listCoupon.get(i).getCode());
                        logger.info("tra_finish - couponRedeem success: " + capillarySuccess);
                    }
					/*
					 * long valuePromoCoupons = -Promo.getPromovar(Long.parseLong("4" + listCoupon.get(i).getCode()));
					 * 
					 * if((!listCoupon.get(i).getDiscountType().equals("PERC") && listCoupon.get(i).getDiscount() !=
					 * valuePromoCoupons) || (listCoupon.get(i).getDiscountType().equals("PERC") && valuePromoCoupons ==
					 * 0)) { notApplied = true; listFailedCoupon.add(listCoupon.get(i).getCode()); } else { success =
					 * CommunicationCapillaryForCoupon.getInstance().couponRedeem(tra, cus,
					 * listCoupon.get(i).getCode()); logger.info("tra_finish - couponRedeem success: " + success);
					 * 
					 * }
					 */
				} else {
					logger.info("tra_finish - coupon " + listCoupon.get(i).getCode() + " not applied");

					notApplied = true;
					prtLine.init(Mnemo.getMenu(86)).onto(20, listCoupon.get(i).getCode()).upto(40, "").book(3); // COUPON
																												// NOT
																												// ACCEPTED
				}

				//if (!success) {
                if (capillarySuccess != 0) {
					failed = true;
					listFailedCoupon.add(listCoupon.get(i).getCode());
				}
			}

			if (notApplied) {
				panel.clearLink(Mnemo.getInfo(84), 0x81); // RETURN COUPON
			}

			if (failed) {
                if (capillarySuccess > 2000) {
                    String msg = GdSarawat.getInstance().readErrorCodeCapillary(capillarySuccess);
                    panel.clearLink(msg, 0x81);
                } else {
                    panel.clearLink(Mnemo.getInfo(81), 0x81); // REDEEM COUPON FAILED
                }
			}

			// send transaction data to capillary
            int successTrans = 0;
            successTrans = CommunicationCapillaryForTransaction.getInstance().transactionAdd(tra, cus, pit, ctl);
			//successTransaction = CommunicationCapillaryForTransaction.getInstance().transactionAdd(tra, cus, pit, ctl);
            tra.successTransaction = successTrans == 0;
			logger.info("Communication with Capillary for sending info about the transaction - error code: "
					+ successTrans);

			if (!tra.successTransaction) {
                DevIo.alert(0);
                if (successTrans > 2000) {
                    String msg = GdSarawat.getInstance().readErrorCodeCapillary(successTrans);
                    logger.info("error message: " + msg);

                    panel.clearLink(msg, 0x81);
                } else {
                    logger.info("error message: " + Mnemo.getInfo(82));
                    panel.clearLink(Mnemo.getInfo(82), 0x81); // CAPILLARY FAILED
                }
			}  else {
                DevIo.alert(1);
            }
		}
		// SARAWAT-ENH-20150507-CGA#A END

		if (tra.code == 0)
			Promo.endTransaction();

		// UPB-EMEA-DMA BEG
		// for (int i=0; i<Struc.tra.itemsVsUPB.size(); i++)
		// WinUpb.getInstance().upb_confirm(Struc.tra.itemsVsUPB.get(i), i);
		// UPB-EMEA-DMA END

		ECommerce.resetInstashop();  //INSTASHOP-SELL-CGA#A

		if (tra.mode > 0) {
			if (options[O_CandC] == 0) {
				GdRegis.vat_print();
				//WINEPTS-CGA#A BEG
				logger.info("call print card voucher");

				DevIo.printCreditCardVoucher(0);
				int retvl = GdRegis.prt_trailer(1);
				DevIo.printCreditCardVoucher(1);
				DevIo.removeCreditCardVoucher();

				/*if (GdPos.sscoClient != null && GdPos.sscoClient.isRunningOnFastLane()) {
				} else {
					PosGPE.smartCardStatus(); // BAS-ENH-2130177-AGA#A
				}*/  //questo è quello che c'è in Finiper, e che ho sostituito con quello che segue

				if (!SscoPosManager.getInstance().isUsed()){
					logger.info("call smartCardStatus");
					PosGPE.smartCardStatus();
				}
				//WINEPTS-CGA#A END
				//return GdRegis.prt_trailer(1); //WINEPTS-CGA#D
				return retvl;
			}
		}
		//INSTASHOP-FINALIZE-CGA#A BEG
		ECommerce.setCardTypeTnd(0);
		ECommerce.setCardTypeDesc("");
		//INSTASHOP-FINALIZE-CGA#A END
		int successPrTrl = GdRegis.prt_trailer(2);

		ECommerce.setAccount("");   //INSTASHOP-FINALIZE-CGA#A

		return successPrTrl;
	}

	// SARAWAT-ENH-20150507-CGA#A BEG
	public static ArrayList<String> getListFailedCoupon() {
		return listFailedCoupon;
	}

	public static void setListFailedCoupon(ArrayList<String> listFailedCoupon) {
		GdTrans.listFailedCoupon = listFailedCoupon;
	}

	public static void resetParamCapillaryVoucher() {
		listFailedCoupon = new ArrayList<String>();
		tra.successTransaction = true;
	}
	// SARAWAT-ENH-20150507-CGA#A END

	static void tra_profic(int cnt) {
		if (!tra.isActive())
			GdRegis.set_tra_top();
		int rec = reg.find(9, tra.mode > M_GROSS ? 4 : 5);
		if (rec > 0)
			reg.sales[rec - 1][0].set(cnt, sec_diff(tra.tim));
	}

	/**
	 * transaction clear
	 */
	int action0(int spec) {

		return tra_clear();
	}

	/**
	 * trans preselect
	 */
	int action1(int spec) {
		int sc = 11, sts;

		if (spec == 1) /* void */ {
			int flg = M_TRVOID;
			if ((tra.spf1 & flg) > 0)
				return 5;
			if ((sts = sc_checks(2, sc = sc_value(flg))) > 0)
				return sts;
			if (tra.spf1 > 0)
				rcd_tbl[sc - 1] = 0;
			else if ((sts = Match.chk_reason(sc)) > 0)
				return sts;
			tra.spf1 |= flg;
			if (!tra.isActive())
				GdRegis.set_tra_top();
			prtTitle(44);
			if (rcd_tbl[sc - 1] > 0)
				prtLine.init(Mnemo.getText(12)).upto(17, editReason(sc)).book(3);
		}
		if (spec == 3) /* surcharge on delivery */ {
			if ((tra.spf3 & 4) > 0)
				return 5;
			if ((sts = sc_checks(4, 4)) > 0)
				return sts;
			tra.spf3 |= 4;
			tra.xtra = tra.stat > 2 ? cus.extra : lREG.rate;
			if (!tra.isActive())
				GdRegis.set_tra_top();
			prtTitle(event.dec);
		}
		if (spec == 4) /* transaction recall */ {
			if ((sts = pre_valid(0x10)) > 0)
				return sts;
			if (tra.spf1 > 0) {
				sc++;
				if ((tra.spf1 & M_TRRTRN) > 0) {
					if ((tra.spf1 & M_TRVOID) == 0)
						return 7;
					sc++;
				}
			}
			if ((sts = sc_checks(8, 11)) > 0)
				return sts;
			dspLine.init(Mnemo.getMenu(event.dec));
		}
		//INSTASHOP-RESUME-CGA#A BEG
		/*if (spec == 5) {
			return automaticInstashopResume();
		}*/
		//INSTASHOP-RESUME-CGA#A END
		return 0;
	}

	/**
	 * subtotal
	 */
	int action2(int spec) {
		int lfs = 0;

		if (input.num > 0) {
			if (tra.amt == tra.sub_amt)
				return 5;
			if (!DevIo.station(4))
				return 7;
			lfs = input.scanNum(input.num) + 1;
		}
		if (!tra.isActive())
			GdRegis.set_tra_top();
		if (lfs > 0) /* floor total on slip */ {
			tra.cnt -= tra.sub_cnt;
			tra.amt -= tra.sub_amt;
			dspLine.init(Mnemo.getText(46)).upto(20, editMoney(0, tra.amt));
			cusLine.init(Mnemo.getText(46)).show(10);
			cusLine.init(tnd[0].symbol).upto(20, editMoney(0, tra.amt)).show(11);
			prtLine.init(Mnemo.getText(21)).upto(17, editInt(tra.cnt)).onto(20, dspLine.toString()).book(3);
			DevIo.slpInsert(--lfs);
			prtLine.type(4);
			GdRegis.set_trailer();
			prtLine.type(4);
			DevIo.slpRemove();
			tra.sub_cnt = tra.cnt += tra.sub_cnt;
			tra.sub_amt = tra.amt += tra.sub_amt;
		} else {
			dspLine.init(Mnemo.getText(23)).upto(20, editMoney(0, tra.amt));
			cusLine.init(Mnemo.getText(23)).show(10);
			cusLine.init(tnd[0].symbol).upto(20, editMoney(0, tra.amt)).show(11);
			prtDline("SlsST" + editNum(tra.code, 2));
			prtLine.init(' ').onto(20, dspLine.toString()).book(3);
			TView.append(' ', 0x00, Mnemo.getText(23), editInt(tra.cnt), "", editMoney(0, tra.amt), "");
		}
		dspBmap = "DPT_0000";
		return itm_clear();
	}

	/**
	 * total
	 */
	int action3(int spec) {
		int sts;

		if (spec > 0) {
			if (input.num == 0) {
				panel.jrnPicture(null);
				dspLine.init(' ');
				return 0;
			}

			TableRbt ptr = rbt[itm.sit = input.scanNum(input.num)];
			itm.amt = ptr.dsc_sls.total;
			dspLine.init(Mnemo.getText(23));
			oplLine.init(Mnemo.getText(58).substring(0, 1) + itm.sit).upto(20, editMoney(0, itm.amt));
			itm.number = "SI" + editTxt(itm.sit, 14);
			Match.rbt_total(lCIN);
			Match.rbt_total(lCGR);
			Match.rbt_total(lRLU);
			panel.jrnPicture(itm.spf2 > 0 ? "JRN_SAVE" : null);
			dspBmap = "DPT_0000";
			return itm_clear();
		}
		if (tra.mode == M_GROSS && tra.bal < 0)
			if ((sts = sc_checks(8, 2)) > 0)
				return sts;
		tra_profic(tra.cnt);

		if (tra.code < 5)
			rbt_auto();

		// SARAWAT-ENH-20150507-CGA#A BEG
		if (spec == 0 && CapillaryService.getInstance().isEnabled()) {
			GdSarawat.getInstance().applyManualCoupon();
		}
		// SARAWAT-ENH-20150507-CGA#A END

		dspBmap = "DPT_0000";
		showTotal(24);
		if (tra.mode > M_GROSS || tra.bal == 0)
			tra_total();
		if (ctl.mode > 0 || tra.mode > M_GROSS) {
			int rec = reg.find(1, 1);
			Sales sls = reg.sales[rec - 1][0];
			sls.set(tra.cnt, tra.amt);
			if (tra.mode > M_GROSS)
				return GdRegis.prt_trailer(2);
		}
		if (tra.bal == 0) {
			if (SscoPosManager.getInstance().isEnabled()){
				if (tra.mode == 0){
					SscoPosManager.getInstance().startTransaction(99999);
				}
				SscoPosManager.getInstance().updateTotalAmount((int) 0, (int)tra.bal, tra.cnt, 0 );
				SscoPosManager.getInstance().enterTenderModeResponse();
			}
			return tra_finish();
		}
		for (sts = 0; sts++ < 2;)
			if ((tnd[0].till & sts) > 0)
				DevIo.drwPulse(sts);
		event.nxt = event.alt;
		if (tra.mode < M_GROSS)
			return GdTndrs.tnd_clear(0);
		sts = (tra.spf1 & M_TRVOID) > 0 ? 1 : 0;
		if ((tra.spf1 & M_TRRTRN) > 0)
			sts += 2;
		if (SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().updateTransactionalDiscount((int)(tra.amt - tra.bal));
			SscoPosManager.getInstance().updateTotalAmount((int)tra.amt, (int)tra.bal, tra.cnt, 0);
			SscoPosManager.getInstance().enterTenderModeResponse();
		}

		//INSTASHOP-RESUME-CGA#A BEG
		//if (!GdTrans.isInstanshopResume()) {
			if (Match.dd_query(tra.code < 7 ? 'T' : 'X', sts) < 0) {
				event.next(event.nxt);
				event.nxt = event.alt;
				return group[2].action9(-1);
			}
		//}
		//INSTASHOP-RESUME-CGA#A END

		return 0;
	}

	/**
	 * resume after total
	 */
	int action4(int spec) {
		int ind = tra.vTrans.size();

		if (tra.res > 0)
			return 7;

		//if (tra.tnd > 0 && SscoPosManager.getInstance().isUsed())
		if (tra.tnd > 0 ||
				ECommerce.getAmountInstashop() != 0) {   //INSTASHOP-FINALIZE-CGA#A*/
			return 5;
		}

		// SARAWAT-ENH-20150507-CGA#A BEG
		if (GdSarawat.getInstance().isAppliedDiscountPoints()) {
			GdSarawat.getInstance().pointsDiscountApplies();
		}
		// SARAWAT-ENH-20150507-CGA#A END

		showTotal(0);
		showMinus(false);
		Promo.updateTotals(0 - tra.dsc_amt - tra.chg_amt - tra.rbt_amt);
		for (; ind-- > 0; tra.vTrans.removeElementAt(ind)) {
			itm = tra.vTrans.getElement(ind);
			if (itm.id == 'C')
				itm_trans(false);
		}
		for (ind = vat.length; ind-- > 0; vat[ind].reset())
			accumTax(10, ind, 0, -vat[ind].tld_amt);
		tra.tld_amt = 0;
		panel.jrnPicture(null);
		dspBmap = "DPT_0000";
		dspLine.init(Mnemo.getText(23)).upto(20, editMoney(0, tra.amt));
		cusLine.init(Mnemo.getText(23)).show(10);
		cusLine.init(tnd[0].symbol).upto(20, editMoney(0, tra.amt)).show(11);
		tra_balance();
		if (SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().exitTenderModeResponse();
		}
		return itm_clear();
	}

	/**
	 * transaction abort / suspend
	 */
	int action5(int spec) {
		int sts;
		String nbr = editKey(ctl.reg_nbr, 3) + editNum(ctl.tran, 4);

		if (tra.tnd > 0 && !SscoPosManager.getInstance().isUsed())
			return 5;

		// SARAWAT-ENH-20150507-CGA#A BEG
		if (spec == M_CANCEL || spec == M_SUSPND) {
			if (CapillaryService.getInstance().isEnabled()) {
				logger.info("press abort or suspend - return coupon used");

				ArrayList<RedeemCoupon> listCouponPassed = CommunicationCapillaryForCoupon.getInstance()
						.getListCouponIsRedeemable();

				logger.info("coupon used: " + listCouponPassed.size());
				if (listCouponPassed != null && listCouponPassed.size() > 0) {
					panel.clearLink(Mnemo.getInfo(84), 0x81); // RETURN COUPON

					for (RedeemCoupon redeemCoupon : listCouponPassed) {
						logger.info("print return coupon: " + redeemCoupon.getCode());

						prtLine.init(Mnemo.getInfo(84) + ": ").onto(20, redeemCoupon.getCode()).upto(40, "").book(3);
					}
				}
			}
		}
		// SARAWAT-ENH-20150507-CGA#A END

		if ((sts = sc_checks(1, spec)) > 0)
			return sts;
		if (spec == M_SUSPND) {
			if (tra.spf1 > 0)
				return 7;
			if (ctl.mode > 0 || tra.code > 0)
				return 7;
		} else if ((sts = Match.chk_reason(9)) > 0)
			return sts;
		if ((sts = GdPsh.cancelAll()) > 0) // PSH-ENH-001-AMZ#ADD
			return sts; // PSH-ENH-001-AMZ#ADD

        //PSH-ENH-20151120-CGA#A BEG
        if ((sts = GdPsh.cancelAllUtilities()) > 0) {
            logger.info("abort - response from server: " + sts);
            return sts;
        }
        //PSH-ENH-20151120-CGA#A END

        showTotal(0);
		tra_profic(0);
		tra.mode = spec;
		if (tra.code < 5) {
			if ((tra.slip & 0x40) > 0)
				Clean.print();
			if (tra.dsc_amt != 0)
				dsc_auto();
			if (tra.chg_amt != 0)
				chg_auto();
			Promo.cancelTransaction();
		}
		if (ctl.mode > 0)
			tra.amt = tra.cnt = 0;
		int rec = reg.find(1, 1);
		reg.sales[rec - 1][0].set(tra.cnt, tra.amt);
		prtLine.init(Mnemo.getText(21)).upto(17, editInt(tra.cnt)).onto(20, Mnemo.getText(24))
				.upto(40, editMoney(0, tra.amt)).book(3);
		if (spec == M_CANCEL) {
			dspLine.init(Mnemo.getInfo(23)).show(10);
			if (rcd_tbl[8] > 0) {
				itm.cnt = -tra.cnt;
				GdSales.itm_reason(9, -tra.amt);
				//WINEPTS-CGA#A BEG
				if (PosGPE.isEptsVoidFlagPresent()) {
					PosGPE.getInstance().GPEDataCollect(7);
				}
				//WINEPTS-CGA#A END
				prtLine.init(Mnemo.getText(12)).onto(13, editReason(9)).book(3);
			}
		} else
			dspLine.init(Mnemo.getMenu(77)).show(10);
		//WINEPTS-CGA#A BEG
		if (DevIo.ThereIsVoucher()) {
			panel.clearLink(Mnemo.getInfo(132), 1);
			PosGPE.deleteEptsVoidFlag();
		}
		//WINEPTS-CGA#A END
		prtDwide(ELJRN + 3, dspLine.toString());
		cusLine.init(tnd[0].symbol).upto(20, editMoney(0, tra.amt)).show(11);
		GdRegis.prt_trailer(2);
		lREG.read(reg.find(1, spec), lREG.LOCAL);

		if (SscoPosManager.getInstance().isEnabled()){
			SscoPosManager.getInstance().voidTransaction();
			if (spec == M_SUSPND){
				SscoPosManager.getInstance().suspendTransactionResponse();
			} else {
				SscoPosManager.getInstance().voidTransactionResponse();
			}
		}

		if ((lREG.tflg & 0x20) == 0)
			return 0;
		stsLine.init(lREG.text).upto(20, nbr);
		prtDwide(2, stsLine.toString());
		String ean = 1 + editNum(ctl.date, 4) + nbr + 0;
		DevIo.tpmLabel(2, cdgSetup(ean, ean_weights, 10));
		GdRegis.hdr_print();

		//INSTASHOP-SELL-CGA#A BEG
		ECommerce.resetInstashop();
		ECommerce.setInstashopChoiceType("");
		//INSTASHOP-SELL-CGA#A END

		//INSTASHOP-CGA#A BEG
		/*if (!ECommerce.getInstashopChoice().equals("0") && spec == M_SUSPND) {
			writeInstashopSuspend();
		}*/
		//INSTASHOP-CGA#A END
		return 0;
	}

	/**
	 * salesman number
	 */
	int action6(int spec) {
		int rec, slm_no, sts, team;

		if (input.num == 0)
			if ((sts = Match.lb_select(5, 0xffff)) > 0)
				return sts;
		if ((slm_no = input.scanKey(input.num)) == 0)
			return 8;
		if (lSLU.find(editKey(slm_no, 4)) > 0) {
			try {
				lSLU.skip(lSLU.fixSize);
				if ((team = lSLU.scanKey(4)) == 0)
					return 8;
				lSLM.text = lSLU.skip(2).scan(20);
				lSLM.pers = lSLU.scanNum(8);
			} catch (NumberFormatException e) {
				lSLU.error(e, false);
				return 8;
			}
		} else
			team = slm_no;
		if ((rec = slm.find(team)) == 0)
			return 8;
		if (!tra.isActive())
			GdRegis.set_tra_top();
		if (slm_no == team)
			lSLM.read(rec, lSLM.LOCAL);
		tra.slm = rec;
		tra.slm_nbr = slm_no;
		tra.slm_prs = lSLM.pers;
		dspLine.init(Mnemo.getText(5)).upto(17, editKey(slm_no, 4));
		prtLine.init(dspLine.toString()).onto(20, lSLM.text).book(3);
		Itmdc.IDC_write('P', trx_pres(), 1, editNum(tra.slm_prs, 8), 0, 0l);
		dspBmap = "SLM_" + editKey(slm_no, 4);
		return 0;
	}

	/**
	 * no sale
	 */
	int action7(int spec) {
		int sts;

		if (SscoPosManager.getInstance().isEnabled() ){
			return -1;
		}

		if ((sts = sc_checks(8, 3)) > 0)
			return sts;
		if ((options[O_SecNo] & 4) > 0) {
			lCTL.read(ctl.ckr, lCTL.LOCAL);
			sts = GdSigns.chk_passwd(Mnemo.getInfo(25));
			if (sts > 0)
				return sts;
		}
		dspLine.init(Mnemo.getInfo(25));
		accumReg(8, 3, 1, 0l);
		prtDwide(ELJRN + 3, dspLine.toString());
		DevIo.drwPulse(0);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * no tax / tax
	 */
	int action8(int spec) {
		//WINEPTS-CGA#A BEG
		logger.info("ENTER GdTrans.action8 - spec: " + spec);

		PosGPE.setPreset(-1);
		if (spec >= 9900) {
			int preset = spec - 9900;

			PosGPE.setPreset(preset);
			spec = 7;
			int specGpe = 7107;
			int tenderPosition = specGpe % 100;
			int nextActionCode = specGpe / 100;

			event.spc = tenderPosition;
			event.act = nextActionCode;

			int retStatus = Action.group[event.act / 10].exec();

			logger.info("retStatus: " + retStatus);
			if (retStatus != 0) {
				return retStatus;
			}
		}
		//WINEPTS-CGA#A END
		if (spec < 9999) {
			if (spec > 0) {
				if ((tra.spf3 & 1) == 0) {
					dspLine.init(Mnemo.getMenu(spec));
					event.nxt = event.alt;
					return 0;
				}
			} else
				tra.taxidn = input.scan(input.num);
			tra.spf3 ^= 1;
			tra_balance();
		}
		showTotal(24);
		return 0;
	}
}
