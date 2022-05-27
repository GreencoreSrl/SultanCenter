package com.ncr;

import com.ncr.ssco.communication.entities.AdditionalProcessType;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class GdPrice extends Action {

	/*******************************************************************/
	/*                                                                 */
	/* dlu / plu access */
	/*                                                                 */
	/*******************************************************************/
	private static final Logger logger = Logger.getLogger(GdPrice.class);
	final static SimpleDateFormat expDateParser = new SimpleDateFormat("ddMMyy"); // TSC-ENH2014-1-AMZ#ADD

    static int src_dlu(int key) {
		if (lDLU.find(editKey(key, 4)) < 1)
			return 28;
		try {
			dlu.dpt_nbr = lDLU.skip(4).scanKey(4);
			dlu.flag = lDLU.scanHex(2);
			dlu.sit = lDLU.scanNum(1);
			dlu.vat = lDLU.scanNum(1);
			dlu.cat = lDLU.scanNum(2);
			dlu.halo = lDLU.scanHex(4);
			dlu.flg2 = lDLU.scanHex(2);
			dlu.ages = lDLU.scanNum(1);
			dlu.type = lDLU.scanNum(1);
			dlu.text = lDLU.scan(20);
			dlu.xtra = lDLU.scan(10);
		} catch (NumberFormatException e) {
			lDLU.error(e, false);
			return 28;
		}
		return 0;
	}

	static int src_dpt(int key) {
		if (src_dlu(key) > 0)
			dlu.dpt_nbr = key;
		else if (dlu.dpt_nbr < 1)
			return 28;
		if ((dlu.dpt = dpt.find(dlu.dpt_nbr)) < 1)
			return 28;
		if (key == dlu.dpt_nbr)
			getMemdpt(dlu.dpt);
		else
			dlu.dpt_nbr = key;
		return 0;
	}

	// TSC-ENH2014-1-AMZ#BEG
	static int src_plu (String key){
		int ret = src_plu_aux(key);

		input.aux1 = "";
		return ret;
	}

	static int src_plu_aux (String key) {
	// TSC-ENH2014-1-AMZ#END
	// static int src_plu (String key) // TSC-ENH2014-1-AMZ#DEL
		for (plu.chaino = key;;) {
			int sts = lPLU.find(key, rMNT.recno > 0);
			if (sts < 1) {
				return sts == 0 || ctl.lan > 2 ? 27 : 16;
			}
			if (!lPLU.pb.substring(36).startsWith("@@@@"))
				break;
			if (key != plu.chaino)
				return 29;
			key = lPLU.skip(40).scan(16);
		}
		try {
			plu.number = lPLU.scan(16);
			plu.dpt_nbr = lPLU.scanKey(4);
			plu.flag = lPLU.scanHex(2);
			plu.sit = lPLU.scanNum(1);
			plu.vat = lPLU.scanNum(1);
			plu.mmt = lPLU.scanNum(2);
			plu.ptyp = lPLU.scan(2);
			plu.unit = lPLU.scanNum(4);
			plu.link = lPLU.scanNum(4);
			plu.text = lPLU.scan(20);
            //10020048000000  00100000A1                            000 00001000
			// PSH-ENH-001-AMZ#BEG
			plu.gCard = lPLU.scan();
			try {
				plu.discountFlag = lPLU.scanHex(1);
			} catch (Exception e) {
				logger.error("Wrong field: ", e);
			}
			plu.xtra = lPLU.scan(8);
			// PSH-ENH-001-AMZ#END
			// plu.xtra = lPLU.scan (10); // PSH-ENH-001-AMZ#DEL
			plu.flg2 = lPLU.scanHex(2);
			plu.ages = lPLU.scanNum(1);
			plu.spec = lPLU.scan();
			plu.originalPrice = plu.price = lPLU.scanNum(8);
			// TSC-ENH2014-1-AMZ#BEG
			if (input.aux1.length() > 0) {
				plu.price = Integer.parseInt(input.aux1.substring(0, 6));
				plu.expdate = input.aux1.substring(6, 12);
			} else {
				plu.expdate = "";
			}
			// TSC-ENH2014-1-AMZ#END
		} catch (NumberFormatException e) {
			lPLU.error(e, false);
			return 27;
		}
		if (plu.dpt_nbr > 0) {
			dlu = new Itemdata();
			if (src_dpt(plu.dpt_nbr) > 0)
				return 28;
			plu.dpt = dlu.dpt;
			plu.halo = dlu.halo;
			plu.cat = dlu.cat;
			plu.type = dlu.type;
		}
		if (key.charAt(1) != ' ')
			return 0;
		if ((plu.flag & F_WEIGHT + F_DECQTY) == F_WEIGHT + F_DECQTY) /* weight item */
		{
			plu.tare = plu.mmt;
			plu.mmt = 0;
		}
		if (plu.spec == 'G') {
			int sts = Match.byLane(plu.spec);
			if (sts > 0)
				return sts;
		}
		if (options[O_PluEx] > 0) {
			int sts = src_pluex(key);
			if (sts > 0)
				return sts;
		}

		// SURCHARGEPRICE-SSAM#A BEG
		if (!GdPsh.getInstance().isGiftCard(plu) || !SurchargeManager.getInstance().isEnabledNetSurcharge()) // PSH-ENH-001-AMZ#ADD -- do not rely on past input price on gift cards
		// SURCHARGEPRICE-SSAM#A END
			if (plu.price != 0)
				Itmdc.chk_local(plu);

        //PSH-ENH-20151120-CGA#A BEG
        /*if (GdPsh.isUtility(plu)) {
            String descr = GdPsh.readDescriptionUtility(plu.number);

            int res = GdPsh.srv_buyUtility(itm, descr);
            if (res > 0) {
                return res;
            }

            GdPsh.getLstUtilities().add(itm.copy());
        }*/
        //PSH-ENH-20151120-CGA#A END

		return 0;
	}

	static int src_pluex(String key) {
		int sts = lPLU.find(" E" + key.substring(2), rMNT.recno > 0);
		if (sts < 1) {
			if (options[O_PluEx] < 2)
				return 0;
			return sts == 0 || ctl.lan > 2 ? 27 : 16;
		}
		try {
			lPLU.skip(16);
			plu.ePack = lPLU.scanNum(4);
			plu.eUref = lPLU.scanNum(4);
			plu.eUofm = lPLU.scan(2);
			plu.eFamily = lPLU.scanNum(3);
			plu.eXline = lPLU.scan(40);
			plu.eFiller = lPLU.scan();
			plu.ePromo = lPLU.scanNum(8);
		} catch (NumberFormatException e) {
			lPLU.error(e, false);
			return 27;
		}
		return 0;
	}

	static int src_sar(String nbr, char stat) {
		if (ctl.mode > 0)
			return 0;
		rSAR.stat = stat; /* W = working / V = voiding */
		rSAR.reg = ctl.reg_nbr;
		rSAR.tran = ctl.tran;
		rSAR.ckr = ctl.ckr_nbr;
		rSAR.date = ctl.date;
		rSAR.time = ctl.time;
		int sts = rSAR.find('S' + nbr);
		if (sts < 1)
			return sts < 0 || stat != 'W' ? 0 : 8;
		if (stat == 'W')
			if (rSAR.stat == 'S')
				return 0;
		if (stat == rSAR.stat)
			if (rSAR.reg == ctl.reg_nbr)
				return 0;
		stsLine.init(Mnemo.getText(3)).onto(10, editKey(rSAR.reg, 3) + '/' + editNum(rSAR.tran, 4) + '/')
				.push(rSAR.stat).show(2);
		return 7;
	}

	static int chk_plumsk(String rule) {
		int ind, info = 0;

		if (plu.number.equals(plu.chaino)) /* if not chained */
		{
			StringBuffer sb = new StringBuffer(plu.number);
			for (ind = 0; ind < sb.length(); ind++) {
				char c = plu.eanupc.charAt(ind);
				switch (rule.charAt(4 + ind)) {
				case '%':
					c = '0';
				case '+':
				case '#':
					sb.setCharAt(ind, c);
				}
			}
			plu.number = sb.toString();
			if (rule.charAt(19) == '%')
				plu.number = cdgSetup(plu.number, ean_weights, 10);
		}
		for (ind = 0; ind < plu.eanupc.length(); ind++) {
			switch (rule.charAt(4 + ind)) {
			case '+':
			case '-':
				info = info * 10 + (plu.eanupc.charAt(ind) - '0');
			}
		}
		for (ind = rule.charAt(2) & 15; ind-- > 0; info *= 10)
			;
		return info;
	}

	static int chk_databar() {
		String[] ai = new String[3];
		for (int ind = 0; (ind = input.parseGS1(ai, ind)) > 0;) { // System.out.println ("aid=" + ai[0] + " fix=" +
																	// ai[1] + " var=" + ai[2]);
			if (ai[0].equals("15")) /* best before date */
				if (cmpDates(Integer.parseInt(ai[1]), ctl.date) <= 0)
					plu.flag |= F_SKPSKU;
			if (ai[0].equals("17")) /* expiration date */
			{
				if (cmpDates(Integer.parseInt(ai[1]), ctl.date) <= 0)
					return 47;
				plu.flg2 &= ~F_EXPIRY;
			}
			if (ai[0].equals("21")) /* serial number */
				plu.serial = ai[2];
		}
		return 0;
	}

	static int chk_serial() {
		if (plu.serial.length() > 0)
			return 0;
		panel.display(1, plu.text);
		panel.dspPicture("DPT_" + editKey(plu.dpt_nbr, 4));
		if (!acceptNbr(Mnemo.getInfo(64), 76, 1, 20, 0, 0))
			return 5;
		plu.serial = input.pb;
		return 0;
	}

	static int chk_pluspc() {
		int sts;
		String rule = ean_special('P', plu.eanupc);

		if (input.key == 0x4f4f)
			if ((sts = chk_databar()) > 0)
				return sts;
		if (rule == null)
			return 0;
		if (rule.charAt(19) == '%')
			if (cdgCheck(plu.eanupc, ean_weights, 10) > 0)
				return 9;
		int ind = rule.lastIndexOf('%', 18) - 4;
		if (ind > 0)
			if (cdgPrice(plu.eanupc, ind) != 0)
				return 9;
		int info = chk_plumsk(rule);
		if (tra.res > 0 || tra.code == 7)
			return 0;


		switch (rule.charAt(1)) {
		case '*':
			if (plu.qty > 0)
				return 5;
			plu.qty = info;
			break;
		case '&':
			info = (int) tnd[tnd_tbl[K_AltCur]].fc2hc(info);
		case '$':
			// TSC-MOD2014-AMZ#BEG
			if (GdTsc.isQuantityCheckEnabled()) {
				plu.flag |= F_QTYPRH;
			}
			// TSC-MOD2014-AMZ#END

			//ECOMMERCE-SSAM#A BEG
			// Setting for scale item barcode
			if (plu.ecommerceInfo.getUnitPrice() != 0) {
				plu.price = plu.ecommerceInfo.getPrice();
				plu.prlbl = plu.ecommerceInfo.getUnitPrice();
				break;
			}
			//ECOMMERCE-SSAM#A END

			if ((plu.flag & F_WEIGHT + F_DECQTY) != F_WEIGHT + F_DECQTY) {
				plu.prlbl = plu.price = plu.qrcode && plu.prpov != 0 ? plu.prpov : info;
				break;
			}
			if ((plu.prlbl = plu.price) == 0)
				return 8;
			plu.price = info;
			break;
		case '.':
			// TSC-MOD2014-AMZ#BEG
			if (GdTsc.isQuantityCheckEnabled()) {
				plu.flag |= F_QTYPRH;
			}
			// TSC-MOD2014-AMZ#END
			if (info > 0) {
				if (plu.prm++ > 0)
					return 4;
				if ((plu.dec = info) > 9999999)
					return 2;
				plu.flag &= ~F_WEIGHT;
			} else if (plu.prm < 1)
				return 4;
		}

		if ((long) plu.qty * plu.dec > 9999999)
			return 2;
		return 0;
	}

	static int chk_extend(Itemdata ptr) {
		int sts, spf1 = tra.spf1 | ptr.spf1, clim = 100000;
		long amt, ilim = 100000000, tlim = 1000000000;

		if ((ptr.flag & F_WEIGHT) > 0) {
			if (ptr.prlbl > 0) { /* if unit price changed after labelling, weight wrong */
				long dec = ptr.price * 100000L / ptr.unit / ptr.prlbl;
				ptr.dec = (int) roundBy(dec, 10);
				ptr.prm++;
				ptr.ext = 1;
				//ptr.flag &= ~F_WEIGHT;	//TAU-20160816-SBE#A
			} else if ((sts = GdScale.weigh(ptr)) > 0)
				return sts;
		}
		int cnt = ptr.qty, qty = ptr.ext > 0 ? 1 : cnt;
		amt = set_price(ptr, ptr.price) * qty;
		if (amt >= ilim)
			return 2;
		if (ptr.prpos != 0)
			if (set_price(ptr, ptr.prpos) * qty >= ilim)
				return 2;
		if (ptr.prpov != 0)
			if (set_price(ptr, ptr.prpov) * qty >= ilim)
				return 2;
		while ((spf1 >>= 1) > 0)
			if ((spf1 & 1) > 0) {
				amt = 0 - amt;
				cnt = 0 - cnt;
			}
		if ((cnt += tra.cnt) >= clim)
			return 34;
		if (cnt <= 0 - clim)
			return 34;
		if ((amt += tra.amt) >= tlim)
			return 34;
		if (amt <= 0 - tlim)
			return 34;
		if ((amt += ctl.gross) >= tlim)
			return 34;
		if (amt <= 0 - tlim)
			return 34;

		if ((ptr.spf1 & M_VOID) > 0) {
			if (Itmdc.chk_void(ptr))
				return 7;
		}
		if ((sts = Match.chk_quali(ptr)) > 0)
			return sts;
		return 0;
	}

	static int chk_quantity(Itemdata ptr) {
		int sts;

		ptr.flag ^= F_WEIGHT;
		logger.info("ptr.qty");
		if (ptr.qty > 0)
			return 0;
		DevIo.alert(0);
		panel.display(1, ptr.text);
		panel.dspPicture("DPT_" + editKey(ptr.dpt_nbr, 4));
		for (;; panel.clearLink(Mnemo.getInfo(sts), 1)) {
			input.prompt = Mnemo.getText(35);
			input.init(0x00, 3, 3, 0);
			ModDlg dlg = new ModDlg(Mnemo.getInfo(43));
			oplToggle(2, Mnemo.getInfo(43));
			dlg.show("QTY");
			oplToggle(0, null);
			if ((sts = dlg.code) > 0)
				continue;
			if (input.key == 0)
				input.key = input.CLEAR;
			if (input.key == input.CLEAR)
				return 43;
			if (input.num < 1 || input.key != input.ENTER)
				sts = 5;
			else
				sts = input.adjust(input.pnt);
			if (sts > 0)
				continue;
			if ((ptr.qty = input.scanNum(input.num)) > 0)
				return 0;
			sts = 8;
		}
	}

	static int chk_flags(Itemdata ptr) {
		int sts;

		if (tra.res > 0)
			if (ptr.stat != 0x02) {
				if (tra.spf1 > 0) {
					ptr.spec = ' ';
					ptr.sit = dci.sit;
					ptr.vat = dci.vat;
					ptr.cat = dci.cat;
					ptr.flag = dci.flag;
					ptr.flg2 = dci.flg2;
					ptr.mmt = dci.mmt;
					ptr.ages = dci.ages;
				}
				ptr.flag &= F_DPOSIT | F_DECQTY | F_XPROMO;
				ptr.unit = dci.unit;
				ptr.price = dci.price;
			}
		if (ptr.unit == 0)
			return 8;
		if (rbt[ptr.sit].text == null)
			return 8;
		if (ptr.vat >= vat.length || vat[ptr.vat].text == null)
			return 8;
		if (tra.code == 7)
			ptr.ext = 1;
		// TSC-MOD2014-AMZ#BEG
		if ((ptr.flag & F_QTYPRH) > 0) {
			if (ptr.qty > 0) {
				return 5;
			}
		}
		// TSC-MOD2014-AMZ#END
		if ((ptr.flag & F_WEIGHT) > 0) {
			logger.info("spf1: " + ptr.spf1);
			logger.info("flag: " + ptr.flag);
			if ((ptr.flag & F_DECQTY) > 0) {
				if (ptr.qty > 0)
					return 5;
				logger.info("tra.spf1: " + tra.spf1 + "ptr.spf1: " + ptr.spf1 + "ptr.ext: " + ptr.ext);
				if (ctl.mode == M_RENTRY) {
				//if ((tra.spf1 | ptr.spf1 | ptr.ext) > 0 || ctl.mode == M_RENTRY) {
					logger.info("ptr.prm: " + ptr.prm + "ptr.prlbl: " + ptr.prlbl);
					if (ptr.prm < 1) {
						logger.info("Exiting with 43 - 1");
						return 43;
					}
					ptr.flag ^= F_WEIGHT;
				} else if (ptr.prm++ > 0)
					return 5;
			} else if (chk_quantity(ptr) > 0) {
				logger.info("Exiting with 43 - 2");
				return 43;
			}
		}
		if (ptr.qty == 0)
			ptr.qty = 1;
		if (ptr.prm > 0) {
			if ((ptr.flag & F_DECQTY) == 0 || ptr.unit != 10)
				return 4;
			ptr.dec *= ptr.qty;
			ptr.qty = 1;
		}
		if ((ptr.flag & F_NEGSLS) > 0) {
			if ((ptr.spf1 & M_RETURN) > 0 || (tra.spf1 & M_TRRTRN) > 0)
				return 7;
			int sc = sc_value((ptr.flag & F_DPOSIT) > 0 ? M_REFUND : M_EXPNSE);
			if (reg.find(2, sc) == 0)
				return 7;
			ptr.spf1 ^= 256 >> sc;
		}
		if (ctl.mode == M_RENTRY || tra.code > 4)
			return 0;

        if(!GdSarawat.getForceAcceptQuantityKey()) { // AMZ-2017-004-002#ADD
            if ((ptr.flg2 & F_NONQTY) > 0)
                if (ptr.qty != 1)
                    return 59;
            // AMZ-2017-004-002#BEG
        }else{
            GdSarawat.quantityKeyForcedAccept();
        }
        // AMZ-2017-004-002#END

		if (tra.code == 0)
			if (tra.res == 0) {
				if ((ptr.flg2 & F_LOCKED) > 0)
					return 7;
				if ((ptr.flg2 & F_MEMBER) > 0)
					return 58;
				if ((ptr.flg2 & F_EXPIRY) > 0) {
					if (panel.clearLink(Mnemo.getInfo(65), 3) < 2)
						return 7;
				}
			}
		if (ctl.ckr_age > 0) {
			if (ctl.tooYoung(ckr_age[ptr.ages], ctl.ckr_age)) {
				panel.display(2, ptr.text);
				sts = GdSigns.chk_autho(Mnemo.getInfo(57));
				if (sts > 0)
					return sts;
			}
		}
		if (cus_age[ptr.ages] > 0) {
			if (tra.age == 0)
				DevIo.alert(0);
			for (; tra.age == 0; panel.clearLink(Mnemo.getInfo(sts), 1)) {
				panel.display(1, Mnemo.getInfo(57));
				input.prompt = Mnemo.getText(72);
				input.init(0x00, 6, 6, 0);
				ModDlg dlg = new ModDlg(ptr.text);
				dlg.line[0].setText(Mnemo.getText(8));
				sts = ctl.date + (100 - cus_age[ptr.ages]) * 10000;
				dlg.line[2].setText("< " + editDate(sts) + "  ");
				oplToggle(2, ptr.text);
				dlg.show("AGE");
				oplToggle(0, null);
				if ((sts = dlg.code) > 0)
					continue;
				sts = 5;
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.key == input.CLEAR)
					return 7;
				if (input.num < 5 || input.key != input.ENTER)
					continue;
				if ((sts = input.adjust(input.pnt)) > 0)
					continue;
				cus.setAge(input.scanDate(input.num));
				if (dat_valid(cus.getAge())) {
					dspLine.show(1);
					break;
				}
				sts = 8;
			}
			if (ctl.tooYoung(cus_age[ptr.ages], cus.getAge())) {
				panel.display(2, ptr.text);
				return 57;
			}
		}
		return 0;
	}

	static boolean chk_verify(int txt) {
		if (tra.res > 0)
			return false;
		if (itm.price == 0 || itm.prlbl > 0)
			return false;
		panel.dspPicture("DPT_" + editKey(itm.dpt_nbr, 4));
		panel.display(1, itm.text);
		stsLine.init(Mnemo.getText(36)).upto(20, editDec(itm.price, tnd[0].dec)).show(2);
		return panel.clearLink(Mnemo.getInfo(txt), 0x23) < 2;
	}

	static void scan_ean() {
		int ind, top = 16 - input.num;
		String rule;

		plu.number = editTxt(input.pb, 16);
		if ((rule = ean_special('U', plu.number)) != null) {
			plu.number = plu.number.substring(1) + '0';
			if (--top == 8 && rule.charAt(1) == 'E')
				plu.number = editTxt(upcSpreadE(plu.number.substring(top)), 16);
			plu.number = cdgSetup(plu.number, ean_weights, 10);
			plu.number = editTxt(input.pb + plu.number.charAt(15), 16);
		}
		while ((rule = ean_special('S', plu.number)) != null) {
			if ((ind = top + rule.charAt(2) & 0x0f) >= 15)
				break;
			plu.number = editTxt(plu.number.substring(top = ind), 16);
		}
		plu.eanupc = plu.number;
		ind = input.key;
		input.reset(plu.number.trim());
		input.key = ind;
		while ((rule = ean_special('Z', plu.number)) != null) {
			if (top == 8 && rule.charAt(1) == 'E')
				plu.number = editTxt(upcSpreadE(plu.number.substring(8)), 16);
			if ((ind = top - (rule.charAt(2) & 0x0f)) < 0)
				break;
			StringBuffer sb = new StringBuffer(plu.number);
			while (sb.charAt(ind) == ' ')
				sb.setCharAt(ind++, '0');
			plu.number = sb.toString();
		}
		if ((rule = ean_special('P', plu.number)) != null) {
			StringBuffer sb = new StringBuffer(plu.number);
			for (ind = 0; ind < sb.length(); ind++) {
				char c = rule.charAt(4 + ind);
				if (c > ' ' && c < '0')
					sb.setCharAt(ind, '0');
			}
			plu.number = sb.toString();
			if (rule.charAt(19) == '%')
				plu.number = cdgSetup(plu.number, ean_weights, 10);
		}
	}

	static int scan_ref() {
		int ind = 16, nbr = ean_16spec.length;
		String rule;

		plu = new Itemdata();
		plu.number = editTxt(input.pb, 16);
		while (nbr > 0) {
			if ((rule = ean_16spec[--nbr]) == null)
				continue;
			if (rule.charAt(0) != 'L')
				continue;
			StringBuffer sb = new StringBuffer(rule.substring(4));
			while (ind > 0) {
				char c = sb.charAt(--ind);
				if (c > ' ' && c < '0')
					c = '0';
				if (c > '9')
					c = input.num > 0 ? input.pb.charAt(--input.num) : '0';
				sb.setCharAt(ind, c);
			}
			plu.number = sb.toString();
			if (rule.charAt(19) == '%')
				plu.number = cdgSetup(plu.number, ean_weights, 10);
			input.reset(plu.number.trim());
			break;
		}
		scan_ean();
		if ((ind = src_plu(plu.number)) > 0)
			return 29;
		if (plu.dpt_nbr < 1)
			return 29;
		plu.stat = 2;
		if ((ind = chk_flags(plu)) > 0)
			return 29;
		ref = plu;
		return 0;
	}

	//PSH-ENH-20151120-CGA#A BEG
    int menuUtility() {
        logger.debug("ENTER menuUtility");

        String response = "";
        int code = 0;
        SelDlg dlg = new SelDlg(Mnemo.getText(22));

        logger.info("call function getCompanies");
        response = GdPsh.getPpController().getCompanies();
        logger.info("response from server: " + response);

        String replyC[] = response.split(";");
		ArrayList<String> companies = new ArrayList<String>();
		if (Integer.parseInt(replyC[0]) > 0) {
			logger.debug("EXIT return code error: " + replyC[0]);
			return 98;
		} else {
			for (int index = 1; index < replyC.length; index++) {
				if (replyC[index].length() > 0 && !replyC[index].equals("\r\n")) {
					companies.add(replyC[index]);
					dlg.add(8, editNum(index, 3), " " + replyC[index]);
				}
			}
			if (companies.size() == 0) {
				logger.debug("EXIT menuUtility - return message list empty");
				return 100;
			}
		}

		input.reset("");
        //dlg.input.prompt = "";
        input.key = input.CLEAR;

        dlg.show("MNU");

        if (dlg.code > 0)
            return dlg.code;
        if (input.key == 0)
            input.key = input.CLEAR;
        if (input.num < 1 || input.key != input.ENTER)
            return 5;
        if ((code = input.adjust(input.pnt)) > 0)
            return code;

        code = input.scanNum(input.num);
        if (code < 1 || replyC[code] == null)
            return 8;
        if (mon.snd_mon != null)
            return 7;


        logger.info("call function getProducts");
        response = GdPsh.getPpController().getProducts(replyC[code]);
        logger.info("response from server: " + response);

		String replyP[] = response.split(";");
		logger.info("error code: " + replyP[0]);

		ArrayList<String> products = new ArrayList<String>();
		if (Integer.parseInt(replyP[0]) > 0) {
			logger.debug("EXIT return code error: " + replyP[0]);
			return 98;
		} else {
			for (int index = 1; index < replyP.length; index++) {
				if (replyP[index].length() > 0 && !replyP[index].equals("\r\n")) {
					products.add(replyP[index]);
					dlg.add(8, editNum(index, 3), " " + replyP[index]);
				}
			}
			if (products.size() == 0) {
				logger.debug("EXIT menuUtility - return message list empty");
				return 101;
			}
		}

        dlg = new SelDlg(Mnemo.getText(22));

        for (int i = 1; i < replyP.length-1; i++) {
            dlg.add(8, editNum(i, 3), " " + replyP[i]);
        }

		input.reset("");
        //dlg.input.prompt = "";
        input.key = input.CLEAR;

        dlg.show("MNU");
        //input.key = input.CLEAR;

        if (dlg.code > 0)
            return dlg.code;
        if (input.key == 0)
            input.key = input.CLEAR;
        if (input.num < 1 || input.key != input.ENTER)
            return 5;
        if ((code = input.adjust(input.pnt)) > 0)
            return code;

        code = input.scanNum(input.num);
        if (code < 1 || replyP[code] == null)
            return 8;
        if (mon.snd_mon != null)
            return 7;

        int retCodePrepay = GdPsh.readCodePrepay(replyP[code]);
        if (retCodePrepay > 0) {
            return retCodePrepay;
        }

        logger.info("utility Code: " + itm.utilityCode);
        input.pb = itm.utilityCode;

        logger.debug("EXIT return ok");
        return 0;
    }
    //PSH-ENH-20151120-CGA#A END

    /**
	 * view plu
	 **/
	public int action0(int spec) {
		int sts;

		if (input.num == 0)
			return GdRegis.prt_trailer(2);
		plu = new Itemdata();
		scan_ean();
		if ((sts = src_plu(plu.number)) > 0)
			return sts;
		if (spec > 0) {
			itm.number = input.pb;
			dspLine.init(plu.text);
			oplLine.init(Mnemo.getText(36)).upto(20, editDec(plu.price, tnd[0].dec));
			return 0;
		}
		if ((sts = chk_pluspc()) > 0)
			return sts;
		if (plu.qty == 0)
			plu.qty = 1;
		itm = plu;
		if (itm.spec > ' ')
			if (itm.prlbl == 0) {
				sts = Match.scan_pov(itm.spec);
				if (sts > 0)
					return sts;
			}
		oplLine.init(Mnemo.getText((itm.flag & F_SPCSLS) > 0 || itm.prpov > 0 ? 41 : 36));
		if (itm.spf3 > 0) {
			itm.price = itm.prpov;
			if ((sts = reg.find(8, 21 + itm.spf3)) > 0) {
				lREG.read(sts, lREG.LOCAL);
				oplLine.init(lREG.text);
			}
		}
		oplLine.upto(20, editMoney(0, set_price(itm, itm.price)));
		if (itm.qty > 1)
			oplLine.init(' ').push(editInt(itm.qty)).skip().push(itm.ptyp).skip().push(Mnemo.getText(60).charAt(0))
					.upto(20, editMoney(0, set_price(itm, itm.price)));
		if (itm.prm > 0)
			oplLine.init(' ').push(editDec(itm.dec, 3)).skip().push(itm.ptyp).upto(20,
					editMoney(0, set_price(itm, itm.price)));
		stsLine.init(itm.text).show(1);
		oplLine.show(2);
		panel.dspPicture("DPT_" + editKey(itm.dpt_nbr, 4));
		PluDlg dlg = new PluDlg(Mnemo.getMenu(57));
		dlg.labl.setBars(input.pb);
		prtLine.init(' ').push('x' + editHex(itm.flag, 2)).push(editTxt(itm.sit, 3) + editTxt(itm.vat, 4))
				.onto(13, editNum(itm.mmt, 2)).upto(24, editDec(itm.unit, 1) + ' ' + itm.ptyp).skip(2)
				.push(editNum(itm.link, 4)).skip().push(editHex(itm.flg2, 2) + '-' + editNum(itm.ages, 1)).skip()
				.push(editKey(itm.dpt_nbr, 4));
		dlg.data.setText(prtLine.toString(0, 40));
		input.init(0x80, 0, 0, 0);
		DevIo.oplSignal(15, 1);
		oplToggle(2, Mnemo.getMenu(57));
		dlg.show("PLU");
		oplToggle(0, null);
		DevIo.oplSignal(15, 0);
		if (ctl.ckr_nbr < 1)
			return 0;
		return group[4].action0(0);
	}

	/**
	 * plu number
	 **/
	public int action2(int spec) {
		int ind;

		//QRCODE-SELL-CGA#A BEG
		logger.info("input.qrcode: " + input.qrcode);
		//if (spec == 0) input.qrcode = "29043361;48.20\r\n222695019154;19.15";
		if (!input.qrcode.equals("")) {
			QrCodeManager.getInstance().handleQrCode();

			return 0;
		}
		//QRCODE-SELL-CGA#A END

		//INSTASHOP-VOID-CGA#A BEG
		if (itm.spf1 == M_VOID && ECommerce.getEanItemComm().trim().equals(input.pb.trim())) {
			//non si può stornare l'articolo commissione
			return 7;
		}
		//INSTASHOP-VOID-CGA#A END
		if ((ind = pre_valid(0x11)) > 0)
			return ind;

        //PSH-ENH-20151120-CGA#A BEG
        if (spec == 1) {
            logger.info("pressed utility key");
            // if (GdPsh.isEnabled()) { // AMZ-2017-003-004#DEL
            if (GdPsh.getInstance().isEnabled() && GdPsh.getInstance().isPxlEnabled()) { // AMZ-2017-003-004#ADD
                logger.info("isEnabled");

                int ret = menuUtility();
                logger.info("ret menu: " + ret);

                if (ret > 0) {
                    logger.debug("EXIT ret > 0 - return error message");

                    return ret;
                }
            } else {
                logger.debug("EXIT disabled - return error message");

                return 7; // unavailable
            }
        }
        //PSH-ENH-20151120-CGA#A END

		//AMAZON-COMM-CGA#A BEG
		if (ECommerce.isAutomaticVoidAmazonItem()) {
			itm.spf1 = M_VOID;
			logger.info("ECommerce Void Item");
		}
		//AMAZON-COMM-CGA#A END
		plu = itm.copy();
		scan_ean();
		if (input.key == 0x4f4f)
			plu.stat |= 1;
		if ((ind = GdSpinneys.getInstance().handleCoupon(plu)) > 0) {

			logger.debug("Response code error: " + ind);
			//SPINNEYS-13032018-CGA#A BEG
			if (ind == 120) {
				panel.clearLink(GdSpinneys.getInstance().getMsgError(), 1);
				return 0;
			}
			//SPINNEYS-13032018-CGA#A END
			return ind;
		}
		if ((ind = src_plu(plu.number)) > 0)
			return ind;

		//ECOMMERCE-SSAM#A BEG
		// Setting about item base on type
		if (plu.ecommerceInfo.getUnitPrice() != 0) {
			if ((plu.flag & F_WEIGHT) > 0)
			{
				plu.prlbl = plu.ecommerceInfo.getUnitPrice();
				plu.price = plu.ecommerceInfo.getPrice();
			}
			else {
				plu.qty = (plu.ecommerceInfo.getPrice() / plu.ecommerceInfo.getUnitPrice());
				plu.prpov = plu.ecommerceInfo.getUnitPrice();
			}
		}
		//ECOMMERCE-SSAM#A END

		// TSC-ENH2014-1-AMZ#BEG
		if (plu.expdate.length() > 0) {
			try {
				Date pluDate = expDateParser.parse(plu.expdate);
				Date now = new Date();
				Date posDate = expDateParser.parse(expDateParser.format(now));
				if (posDate.after(pluDate)) {
					return 146; // Expired product
				}

			} catch (Exception e) {
				return 146; // Expired product
			}
		}
		// TSC-ENH2014-1-AMZ#END
		//AMAZON-COMM-CGA#A BEG
		if (ECommerce.isAutomaticVoidAmazonItem()) {
			plu.amt = ECommerce.getAmtAutomaticItem();
			logger.info("ECommerce Void Item");
		}
		//AMAZON-COMM-CGA#A END

		// CHKINPUT-CGA#A BEG
		int sts;
		if ((sts = ItemAuthManager.getInstance().getItemAuth(input, plu)) > 0) return sts;
		// CHKINPUT-CGA#A END

		if (plu.dpt_nbr < 1)
			return 28;
		if ((ind = chk_pluspc()) > 0)
			return ind;
		if ((ind = chk_flags(plu)) > 0)
			return ind;
		if (vat[plu.vat].flat > 0)
			return 7;
		if ((plu.flg2 & F_SERIAL) > 0)
			if ((ind = chk_serial()) > 0)
				return ind;
		// PSH-ENH-001-AMZ#BEG -- gdprice.action2
		if (GdPsh.getInstance().isGiftCard(plu)) {
			ind = GdPsh.getInstance().readSerial32(plu);
			if (ind == -1) {
				event.nxt = event.alt; // do not ask the price cancelling operation
				return 0; // no dialog box, cancel operation
			}
			if (ind > 0) {
				return ind;
			}
		} else {
			if (plu.gCardTopup) {
				return 7; // not available. can't topup a normal item, must be a gift card
			}
		}
		// PSH-ENH-001-AMZ#END

        //PSH-ENH-20151120-CGA#A BEG
        if (GdPsh.getInstance().isUtility(plu)) {
            logger.info("inserted item code - the item is utility");
            logger.info("code: " + plu.number);

			//UTILITY-WITHOUTPR-CGA#A BEG
			if (GdPsh.isEnableUtilityWithoutPr()) {
				plu.utilityName = plu.text;
				logger.info("plu.utilityName: " + plu.utilityName);
			} else { //UTILITY-WITHOUTPR-CGA#A END
				if (plu.utilityName.equals("")) {
					logger.info("utility name is empty");
					int ret = GdPsh.readDescriptionUtility(plu.number);
					if (ret > 0) {
						logger.debug("EXIT ret > 0 - return error message");

						return ret;
					}
				}

				plu.text = plu.utilityName;
			}

            int res = 0;
            if (plu.utilityName.startsWith("DIR")) {//online
                logger.info("utility name start with DIR - is online");
                ind = GdPsh.insertMobile();
                if (ind == -1) {
                    event.nxt = event.alt; // do not ask the price cancelling operation

                    logger.debug("EXIT clear - cancel operation");
                    return 0; // no dialog box, cancel operation
                } else if (ind > 0) {
                    logger.debug("EXIT ind > 0 - return error message");
                    return ind;
                }
            } else {
                logger.info("utility name don't start with DIR - is offline");

                try {
                    logger.info("call the getValue for item price");
                    String[] prc = GdPsh.getPpController().getValue(plu.utilityName).split(";");

					if ("0".equals(prc[0])) {
						plu.price = Integer.parseInt(prc[1]);

                        logger.info("item price: " + plu.price);
                    } else {
                        logger.debug("EXIT get price returned error: " + prc[1]);
                        return 98;
                    }

                  //  res = GdPsh.srvBuyUtility(false);
                } catch(Exception e){
                    logger.info("EXCEPTION: " + e.getMessage());
                    return 98;
                }

                logger.info("response from server: " + res);
            }
        }
        //PSH-ENH-20151120-CGA#A END
        dlu = itm;

		// SURCHARGEPRICE-SSAM#A BEG
        SurchargeManager.getInstance().applySurcharge(plu, cus);
        // SURCHARGEPRICE-SSAM#A END

		itm = plu;
		if (ctl.mode == M_RENTRY || tra.code == 7)
			itm.price = 0;
		if ((itm.flg2 & F_GRATIS) == 0) {
			if (itm.price == 0) {
				if ((!GdPsh.getInstance().priceAskDisabled() || !GdPsh.getInstance().isUtility(itm))){
					if (SscoPosManager.getInstance().isUsed()) {
						SscoPosManager.getInstance().getProcessor().setAdditionalProcessType(AdditionalProcessType.PRICE);
						SscoPosManager.getInstance().getProcessor().additionalProcess();
					}
					return GdSales.itm_displ();
				}
			}
		}
		if (itm.gCardTopup) // PSH-ENH-001-AMZ#ADD -- topup price request
			return GdSales.itm_displ(); // PSH-ENH-001-AMZ#ADD -- topup price request
		if ((tra.spf1 & M_TRRTRN) > 0 || (itm.spf1 & M_RETURN) > 0) {
			if (!tra.eCommerce)
				if (chk_verify(40))
		        	return GdSales.itm_displ();
		} else if (tra.mode <= M_GROSS)
			if (itm.prpov == 0 && itm.prpos == 0) {
				if ((itm.flag & F_SKPSKU) > 0) {
					itm.spf3 = 1; /* visual verify */
					if (chk_verify(30))
                            if(!GdPsh.getInstance().priceAskDisabled() || !GdPsh.getInstance().isUtility(itm)) // AMZ-2017-003-004#ADD
						        return GdSales.itm_displ();
				}
				if (itm.spec > ' ') {
					if (itm.prlbl == 0)
						if ((ind = Match.scan_pov(itm.spec)) > 0) {
							itm = dlu;
							return ind;
						}
					if (itm.spec == 'P')
						if (itm.spf3 == 0) {
							itm = dlu;
							return 7;
						}
				}
			}
		if ((ind = chk_extend(itm)) > 0) {
			itm = dlu;
			return ind;
		}
		event.nxt = event.alt;

		if (GdPsh.getInstance().isGiftCard(itm) && GdPsh.getUniqueTransactionId().equals("")){
			GdPsh.setUTID();
		}

		return action7(event.spc = 1);
	}

	/**
	 * dpt/sku/price label
	 **/
	public int action3(int spec) {
		int ind, dpt_no;

		// PSH-ENH-007-AMZ#BEG -- ext. topup
		// Check if a code is directly keyed as topup card request
		if ((ind = GdPsh.checkDirectCodeTopup(input, itm)) != 0) {
			if (ind == -1) {
				event.nxt = event.alt;
				return group[4].action0(0); // GdSales.action0 == ItmClear
			}
			return ind;
		}
		// PSH-ENH-007-AMZ#END -- ext. topup
		if (input.num < event.max)
			return ERROR;
		if ((ind = pre_valid(0x11)) > 0)
			return ind;
		if ((cdgCheck(input.pb, lbl_weights, 10)) > 0)
			return 9;
		if (spec == 1) {
			dlu = itm.copy();
			if ((dpt_no = input.scanKey(4)) == 0)
				return 28;
			dlu.number = input.scan(8) + editTxt("", 8);
			dlu.price = input.scanNum(6);
			if ((ind = src_dpt(dpt_no)) > 0)
				return ind;
			if (event.key == 0x4f4f)
				dlu.stat |= 1;
			if ((ind = chk_flags(dlu)) > 0)
				return ind;
			if (dlu.price > 0) {
				if ((ind = chk_extend(dlu)) > 0)
					return ind;
			}
			itm = dlu;
		}

		if (itm.price == 0)
			return GdSales.itm_displ();
		event.nxt = event.alt;
		return action7(1);
	}

	/**
	 * keyed department
	 **/
	public int action4(int spec) {
		int ind, dpt_no;

		if ((ind = pre_valid(0x11)) > 0)
			return ind;
		String s = plu_tbl[spec - 1];
		if (s != null) {
			if (dir_tbl[spec - 1] < -1) /* DYNA */
			{
				if (input.num > 0)
					return 5;
				if (dir_tbl[spec - 1] == -2) /* DESK */
				{
					input.sel = -1;
					panel.dyna.setTouch(s.substring(12));
				} else
					input.sel = Integer.parseInt(s.substring(14));
				event.nxt = event.base;
				return 0;
			}
			if (dir_tbl[spec - 1] == -1) /* LIST */
			{
				ind = Match.chk_short(spec - 1);
				if (ind > 0)
					return ind;
				if (ind < 0)
					return action2(0);
				if (event.find(0xbf, event.base) == 0)
					return 5;
				if ((itm.prpov | itm.prpos) > 0)
					return 5;
				event.next(event.nxt);
				return action8(0);
			}
			if (input.num == 0) {
				boolean isPlu = s.charAt(0) != '$';
				input.reset(s.replace('$', ' ').trim());
				if (input.num == 0)
					return 7;
				if (isPlu)
					return action2(0);
			}
		}
		if ((dpt_no = dir_tbl[spec - 1]) == 0)
			return 5;
		dlu = itm.copy();
		dlu.price = input.scanNum(input.num);
		if ((ind = src_dpt(dpt_no)) > 0)
			return ind;
		stsLine.init(dlu.text).show(1);
		if ((ind = chk_flags(dlu)) > 0)
			return ind;
		if (dlu.price == 0)
			return 8;
		if (chk_halos(dlu.halo, dlu.price))
			return 5;
		if ((ind = chk_extend(dlu)) > 0)
			return ind;
		itm = dlu;
		event.nxt = event.alt;
		return action7(spec);
	}

	/**
	 * department number
	 **/
	public int action5(int spec) {
		int ind, dpt_no;

		if ((ind = pre_valid(0x11)) > 0)
			return ind;
		if (input.num == 0)
			if ((ind = Match.lb_select(6, 0xffff)) > 0)
				return ind;
		if ((dpt_no = input.scanKey(input.num)) == 0)
			return 8;
		dlu = itm.copy();
		if ((ind = src_dpt(dpt_no)) > 0)
			return ind;
		if ((ind = reg.find(8, 8)) > 0) {
			lREG.read(ind, lREG.LOCAL);
			if ((lREG.tflg & 0x10) > 0)
				if (spec == 0) {
					stsLine.init(Mnemo.getText(6)).upto(20, editKey(dpt_no, 4)).show(2);
					if ((ind = GdSigns.chk_autho(Mnemo.getInfo(38))) > 0)
						return ind;
				}
		}
		if ((ind = chk_flags(dlu)) > 0)
			return ind;
		itm = dlu;
		if ((itm.flag & F_SKPSKU) > 0)
			event.nxt = event.alt;
		if (SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().getProcessor().setAdditionalProcessType(AdditionalProcessType.PRICE);
			SscoPosManager.getInstance().getProcessor().additionalProcess();
		}
		return GdSales.itm_displ();
	}

	/**
	 * sku
	 **/
	public int action6(int spec) {
		if (input.num > 0) {
			if (spec == 1)
				itm.number = rightFill(input.pb, 16, ' ');
			else
				itm.number = itm.number.substring(0, 10) + editTxt(input.pb, 6);
		} else
			event.nxt = event.alt;
		return GdSales.itm_displ();
	}

	/**
	 * item price
	 **/
	public int action7(int spec) {
		int ind = 0;


		if (spec == 0) {
			dlu = itm.copy();
			if (input.num > 0)
				dlu.price = input.scanNum(input.num);
			if (dlu.price == 0)
				return 8;
			if (chk_halos(dlu.halo, dlu.price))
				return 5;
			if (dlu.spf3 == 1) {
				dlu.prpov = dlu.price;
				dlu.price = itm.price;
			}
			if ((ind = chk_extend(dlu)) > 0)
				return ind;
			itm = dlu;
		}

		//UMNIA-20180109-CGA#A BEG
		if (GdUmniah.getInstance().isAutoSellItem()) {
			logger.info("automatic sale amount: " + GdUmniah.getInstance().getAmountAutoSellItem());
			itm.price = GdUmniah.getInstance().getAmountAutoSellItem()/10;
		}
		//UMNIA-20180109-CGA#A END
		//AMAZON-COMM-CGA#A BEG
		if (ECommerce.isAutomaticAmazonItem()) {
			itm.price = ECommerce.getAmtAutomaticItem();
			logger.info("ECommerce Item, price: " + itm.price);
		}
		//AMAZON-COMM-CGA#A END

		// PSH-ENH-001-AMZ#BEG
		if (itm.prpnt != 0) {
			itm.gCardTopup = false;

			if ((itm.spf1 & M_VOID) > 0) {
				ind = GdPsh.getInstance().cancelRedemption(itm);
			} else {
				ind = GdPsh.pointsRedemption(itm);
			}

			if (ind > 0) {
				panel.clearLink(Mnemo.getInfo(ind), 0x81);
				return group[4].action0(0); // GdSales.action0 == ItmClear
			}
		}
		if (GdPsh.getInstance().isGiftCard(itm)) {
			if ((itm.spf1 & M_RETURN) > 0) {
				panel.clearLink(Mnemo.getInfo(7), 0x81);
				return group[4].action0(0); // GdSales.action0 == ItmClear
			}
			if ((itm.spf1 & M_VOID) > 0) {
				ind = GdPsh.getInstance().cancelGiftCard(itm);
			} else {
				ind = GdPsh.sellGiftCard(itm);
				if (ind > 0 && itm.prpnt != 0) {
					ind = GdPsh.getInstance().cancelRedemption(itm);
				}
			}
			if (ind > 0) {
				panel.clearLink(Mnemo.getInfo(ind), 0x81);
				return group[4].action0(0); // GdSales.action0 == ItmClear
			}
		}
		// PSH-ENH-001-AMZ#END

        //PSH-ENH-20151120-CGA#A BEG
        logger.info("code: " + itm.number);

        if (GdPsh.getInstance().isUtility(itm)) {
            logger.info("item is utility");

            if ((itm.spf1 & M_RETURN) > 0) {
                logger.info("return item - error message to display");

                panel.clearLink(Mnemo.getInfo(7), 0x81);
                return group[4].action0(0); // GdSales.action0 == ItmClear
            }
            if ((itm.spf1 & M_VOID) > 0) {
                logger.info("void item");

                ind = GdPsh.getInstance().cancelBuyUtility(itm);

                if (ind > 0) {
                    logger.info("response > 0 - error message to display");
                    panel.clearLink(Mnemo.getInfo(ind), 0x81);

                    return group[4].action0(0); // GdSales.action0 == ItmClear
                }
            } else {
                if (itm.utilityMaxPrice > 0 && itm.price > itm.utilityMaxPrice) {
                    logger.debug("EXIT utility item price > utility maxPrice");
                    return 7;
                }

                if (itm.utilityName.startsWith("DIR")) {//online
                    logger.info("Utility online");
                    ind = GdPsh.srvBuyUtility(true);
                } else {
                    logger.info("Utility offline");
                    ind = GdPsh.srvBuyUtility(false);
                }

                logger.info("response from server: " + ind);

                if (ind > 0) {
                    logger.info("response > 0 - error message to display");
                    panel.clearLink(Mnemo.getInfo(ind), 0x81);

                    return group[4].action0(0); // GdSales.action0 == ItmClear
                } else {
                    logger.info("response from server: ok");
                    logger.info("add item to list utilities");
                    GdPsh.getLstUtilities().add(itm.copy());
                }
            }
        }
        //PSH-ENH-20151120-CGA#A END

        // EMEA-UPB-DMA#A BEG
		String key = "DP          ";
		logger.info("cerco itm.number: >" + itm.number + "<");
		if (lUPB.find(itm.number) > 0 || lUPB.find(key + editKey(itm.dpt_nbr, 4)) > 0) {
			itm.operationType = lUPB.skip(17).scanNum(2);
			itm.providerID = lUPB.skip().scanNum(2);
			logger.info("itm.operationType: " + itm.operationType);
			logger.info("itm.providerID: " + itm.providerID);

			int res = WinUpb.getInstance().upb_autho(itm);
			if (res != 0)
				switch (res) {
				case UPB_TIMEOUT_ERROR:
					return 78;
				default:
					return 75;
				}
		}
		// EMEA-UPB-DMA#A END

		// SPINNEYS-ENH-DSC-SBE#A BEG
		if (itm.prpov != 0 || itm.prpos != 0) {
			itm.prchange = true;
		}
		// SPINNEYS-ENH-DSC-SBE#A END
		if (itm.prpov == 0)
			itm.prpov = itm.price;
		if (itm.prpos == 0)
			itm.prpos = itm.prpov;

		if (tra.mode == 0)
			tra.mode = M_GROSS;
		if (!tra.isActive())
			GdRegis.set_tra_top();
		itm.spf1 ^= tra.spf1;
		itm.slm = tra.slm;
		itm.slm_nbr = tra.slm_nbr;
		if (ctl.mode != M_RENTRY && tra.code != 7) {
			if (itm.link > 0) {
				input.reset(String.valueOf(itm.link));
				if ((ind = scan_ref()) > 0) {
					itm.link = 0;
					panel.clearLink(Mnemo.getInfo(ind), 0x81);
				} else if (vat[ref.vat].flat > 0) /* environmental tax */
				{
					itm.flat = ref.vat;
					itm.flatax = set_price(ref, ref.price);
					itm.link = 0;
				}
			}
		} else
			itm.link = 0;
		if (itm.eXline.length() < 1) {
			if (lDBL.find("Dpt" + editKey(itm.dpt_nbr, 4)) > 0)
				itm.eXline = lDBL.pb.substring(8);
		}
		return GdSales.itm_register();
	}

	/**
	 * box/set number
	 **/
	public int action8(int spec) {
		int cnt = tra.cnt, rec = 0, sts;
		long amt = tra.amt;

		if (ctl.mode == M_RENTRY || tra.code == 7)
			return 5;
		if ((tra.spf1 & M_TRRTRN) > 0 || (itm.spf1 & M_RETURN) > 0)
			return 5;
		if ((sts = ean_reduce('B', 8)) > 0)
			return sts;

		String nbr = input.pb;
		lBOX.open("inq", nbr + ".SET", 0);
		if (lBOX.file == null) {
			if (ctl.lan > 2)
				return 7;
			if (itm.spf1 > 0)
				return 7;
			String name = "LAST_BOX.TMP";
			String path = System.getProperty("BOX", "inq") + "\\" + nbr;
			panel.display(2, Mnemo.getInfo(26));
			if ((sts = netio.copyF2f(path + ".BOX", name, true)) != 0)
				return sts < 0 ? 7 : sts;
			lBOX.open(null, name, 0);
		}
		if (!tra.isActive())
			GdRegis.set_tra_top();
		prtLine.init(Mnemo.getMenu(58)).book(3);
		spec = itm.spf1;
		while (lBOX.read(++rec) > 0) {
			try {
				itm.dpt_nbr = lBOX.scanNum(4);
				itm.number = lBOX.scan(':').scan(16);
				itm.qty = lBOX.scan(':').scanNum(5);
				itm.price = lBOX.scan('*').scanNum(8);
				if (itm.qty < 1 || itm.price < 0)
					throw new NumberFormatException("invalid qty/price");
			} catch (NumberFormatException e) {
				lBOX.error(e, false);
				continue;
			}
			event.spc = input.msk = 0;
			if (itm.isPlu()) {
				input.prompt = "";
				input.reset(itm.number.trim());
				if ((itm.prpov = itm.price) > 0)
					itm.spf3 = 2;
				if ((sts = action2(0)) == 0)
					if (event.spc == 0) {
						itm.price = itm.prpov;
						sts = action7(input.num = 0);
					}
			} else {
				input.prompt = Mnemo.getText(6);
				input.reset(editNum(itm.dpt_nbr, 4));
				if ((sts = action5(7)) == 0) {
					if (itm.number.charAt(0) < '0')
						itm.number = "";
					sts = action7(input.num = 0);
				}
			}
			Magic.nxt_item(sts);
			itm.spf1 = spec;
		}
		lBOX.close();
		itm.cnt = tra.cnt - cnt;
		Itmdc.IDC_write('B', sc_value(tra.spf1), 0, nbr, itm.cnt, tra.amt - amt);
		dspLine.init(Mnemo.getText(15)).upto(17, nbr);
		prtLine.init(Mnemo.getText(21)).upto(17, editInt(itm.cnt)).onto(20, dspLine.toString()).book(3);
		dspBmap = "DPT_0000";
		return GdTrans.itm_clear();
	}

	/**
	 * resume trans number
	 **/
	public int action9(int spec) {
		int rec, sts = tid_reduce();
		int ind = tra.spf1 > 0 ? 12 : 11;

		if (sts > 0)
			return sts;
		int sel = input.scanKey(3), tran = input.scanNum(4);
		String nbr = input.pb;

		if (sel == ctl.reg_nbr)
			sel = LOCAL;
		if ((rec = Magic.src_hrec(tran, sel)) < 1)
			return rec < 0 ? 16 : 7;
		dct = new Transact();
		Magic.IDC_scan();
		if (dct.mode < M_GROSS || (dct.spf1 & M_TRVOID) > 0)
			return 7;
		if (dct.mode == M_CANCEL)
			if (reg.find(1, M_SUSPND) > 0)
				return 23;
		if (dct.mode == M_SUSPND)
			dct.mode = M_CANCEL;
		if (dct.mode > M_CANCEL)
			return 7;
		if (dct.code != tra.code)
			return 7;
		if (tra.spf1 > 0) {
			if (dct.mode == M_CANCEL)
				return 23;
			if (tra.spf2 != dct.spf2)
				return 7;
			if (!tra.number.equals(dct.number.trim()))
				return 7;
			if ((sts = src_sar(nbr, 'V')) > 0)
				return sts;
			if (tra.code == 3)
				ind++;
		} else {
			if (dct.mode != M_CANCEL)
				return 7;
			if (reg.find(1, M_SUSPND) > 0)
				if ((sts = src_sar(nbr, 'W')) > 0)
					return sts;
		}
		tra.res = spec;
		if (!tra.isActive())
			GdRegis.set_tra_top();
		prtLine.init(Mnemo.getMenu(47)).book(3);
		while ((sts = lIDC.read(rec++, sel)) > 0) {
			char type = lIDC.pb.charAt(32);
			if (type == 'F')
				break;
			dci = new Itemdata();
			Magic.IDC_scan();
			if (type == 'B')
				if (dci.stat == 0) {
					itm = dci;
					if (tra.spf1 > 0) {
						itm.cnt = -itm.cnt;
						itm.amt = -itm.amt;
					}
					Itmdc.IDC_write('B', sc_value(tra.spf1), itm.stat, itm.number, itm.cnt, itm.amt);
					prtLine.init(Mnemo.getText(21)).upto(17, editInt(itm.cnt)).onto(20, Mnemo.getText(15))
							.upto(38, itm.number.trim()).book(3);
				}
			if (type == 'P') {
				if (dci.stat == 1) {
					input.prompt = Mnemo.getText(5);
					input.reset(editKey(dct.slm_nbr, 4));
					if ((sts = group[3].action6(0)) > 0) {
						dspLine.init(' ').show(1);
						panel.clearLink(Mnemo.getInfo(sts), 1);
					}
				} else if (dci.stat == 2) {
					input.prompt = Mnemo.getText(15);
					input.reset(dci.number.trim());
					if ((sts = group[2].action0(15)) > 0) {
						dspLine.init(' ').show(1);
						panel.clearLink(Mnemo.getInfo(sts), 1);
					}
				} else {
					if ((sts = lIDC.read(rec++, sel)) < 1)
						break;
					type = lIDC.pb.charAt(32);
					Magic.IDC_scan();
				}
			}
			if ("SILRX".indexOf(type) >= 0) {
				for (; (sts = lIDC.read(rec, sel)) > 0; rec++) {
					type = lIDC.pb.charAt(32);
					if (type == 'C' && lIDC.pb.charAt(35) > '1')
						break;
					if (type != 'C' && type != 'Q') /* credits and reason codes */
						if (type != 'W')
							break;
					Magic.IDC_scan();
				}
				if ((dci.stat & 2) > 0)
					continue; /* linked item */
				itm = dci;
				event.spc = input.msk = 0;
				if (itm.isPlu()) {
					if (itm.eanupc.length() == 0)
						itm.eanupc = itm.number;
					input.prompt = "";
					input.reset(itm.eanupc.trim());
					if ((sts = action2(0)) == 0)
						if (event.spc == 0) {
							itm.price = dci.price;
							sts = action7(1);
						}
				} else {
					input.prompt = Mnemo.getText(6);
					input.reset(editKey(itm.dpt_nbr, 4));
					if (itm.number.charAt(0) < '0')
						itm.number = "";
					if ((sts = action5(0)) == 0) {
						sts = action7(1);
					}
				}
				Magic.nxt_item(sts);
				continue;
			}
			if (type == 'G') {
				if ((sts = lIDC.read(rec, sel)) > 0) {
					if (lIDC.pb.charAt(32) == 'K') /* reward details */
					{
						rec++;
						Magic.IDC_scan();
					}
				}
				if (tra.spf1 == 0) {
					if (dci.spf2 > 0 || (dci.spf3 - 1 & 3) < 3)
						continue;
				}
				itm = dci;
				if (tra.spf1 > 0) {
					itm.cnt = -itm.cnt;
					itm.amt = -itm.amt;
					itm.pnt = -itm.pnt;
					itm.rew_qty = -itm.rew_qty;
					itm.rew_amt = -itm.rew_amt;
				}
				if (itm.spf2 == 0) /* points */
				{
					itm.text = Mnemo.getText(63);
					if ((itm.spf3 - 1 & 3) != 1)
						GdSales.pnt_line(itm.spf3);
					else
						tra.vTrans.addElement('G', itm);
				} else {
					Itmdc.IDC_write('G', itm.spf3, itm.spf2, itm.number, itm.pnt, itm.amt);
					Itmdc.IDC_write('K', trx_pres(), itm.flag, itm.promo, itm.rew_qty, itm.rew_amt);
				}
			}
			if (type == 'D' && tra.spf1 > 0) /* trans discounts */
			{
				if ((sts = lIDC.read(rec, sel)) > 0) {
					if (lIDC.pb.charAt(32) == 'K') /* reward details */
					{
						rec++;
						Magic.IDC_scan();
					}
				}
				if (dci.spf3 != 3)
					continue;
				itm = dci;
				itm.cnt = -itm.cnt;
				itm.rew_qty = -itm.rew_qty;
				itm.crd = -itm.crd;
				itm.rew_amt = -itm.rew_amt;
				if (itm.dpt_nbr != 0) {
					dlu = itm.copy();
					if (src_dpt(itm.dpt_nbr) != 0)
						continue;
					itm = dlu;
				}
				lREG.read(reg.find(4, itm.spf3), lREG.LOCAL);
				itm.text = lREG.text;
				GdTrans.rbt_distrib();
				GdTrans.tra_balance();
			}
			if (type != 'C')
				continue;
			int sc = lIDC.pb.charAt(35) - '0';
			if (type == 'C' && sc < 4) /* manual credits */
			{
				if ((sts = lIDC.read(rec, sel)) > 0) {
					if (lIDC.pb.charAt(32) == 'Q') /* reason code */
					{
						rec++;
						Magic.IDC_scan();
					}
				}
				if (pit != null) {
					pit.spf2 = dci.spf2;
					itm = pit.copy();
					GdSales.crd_register(Math.abs(dci.crd));
				}
			}
			if (sc == 4 && tra.spf1 > 0) /* auto discounts */
			{
				if ((sts = lIDC.read(rec, sel)) > 0) {
					if (lIDC.pb.charAt(32) == 'K') /* promo details */
					{
						rec++;
						Magic.IDC_scan();
					}
				}
				if (dci.isPlu()) {
					plu = dci.copy();
					input.prompt = "";
					input.reset(plu.number.trim());
					scan_ean();
					if (src_plu(plu.number) > 0)
						continue;
					itm = plu;
				} else {
					dlu = dci.copy();
					if (src_dpt(dlu.dpt_nbr) > 0)
						continue;
					itm = dlu;
				}
				lREG.read(reg.find(3, sc), lREG.LOCAL);
				itm.text = lREG.text;
				itm.cnt = itm.qty;
				itm.amt = itm.crd;
				if (dci.spec != '-') {
					itm.cnt = -itm.cnt;
					itm.rew_qty = -itm.rew_qty;
					itm.amt = -itm.amt;
					itm.rew_amt = -itm.rew_amt;
				}
				GdSales.crd_line();
			}
		}
		if (sts < 1) {
			if (sts < 0)
				panel.clearLink(Mnemo.getInfo(16), 1);
			prtLine.init(Mnemo.getInfo(23)).book(3);
		}
		itm = new Itemdata();
		itm.cnt = tra.cnt;
		itm.amt = tra.amt + tra.dsc_amt;
		accumReg(8, ind, itm.cnt, itm.amt);
		Itmdc.IDC_write('B', sc_value(tra.spf1), tra.res, nbr, itm.cnt, itm.amt);
		dspLine.init(Mnemo.getText(30)).upto(17, nbr);
		prtLine.init(Mnemo.getText(21)).upto(17, editInt(itm.cnt)).onto(20, dspLine.toString()).book(3);
		if (tra.spf1 > 0) {
			if (event.find(0x19, event.nxt) > 0) /* total key */
				return group[3].action3(0);
		}
		dspBmap = "DPT_0000";
		//INSTASHOP-RESUME-CGA#A BEG
		/*if (GdTrans.isInstanshopResume()) {
			automaticFinalizeInstanshop();
		} else {
			GdTrans.searchInstashopSuspend(tran);
		}*/
		//INSTASHOP-RESUME-CGA#A END
		return tra.res = GdTrans.itm_clear();
	}
	//INSTASHOP-RESUME-CGA#A BEG
	/*private void automaticFinalizeInstanshop() {
		Action.group[3].action3(0);

		itm.tnd = ECommerce.getAutomaticResumeTnd();

		itm.amt = itm.pos = tra.amt;

		GdTndrs.tnd_print();
	}*/
	//INSTASHOP-RESUME-CGA#A END

}
