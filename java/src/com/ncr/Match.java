package com.ncr;

import java.io.*;

abstract class Match extends Action {

	/*******************************************************************/
	/*                                                                 */
	/* mix/match discount */
	/*                                                                 */
	/*******************************************************************/

	private static char idQuery = ' ';  //INSTASHOP-RECORD-CGA#A


	static int scan_mxm(int code) {
		if ((itm.spf2 & M_PRCRED) > 0)
			return 0;
		if ((itm.spf1 & M_RETURN + M_TRRTRN) > 0)
			return 0;
		plu = new Itemdata();
		plu.number = "MM" + editTxt(code, 14);
		if (GdPrice.src_plu(plu.number) > 0)
			return ERROR;
		if (plu.unit < 10)
			return ERROR;
		if (plu.spec == '/')
			if (plu.price < 1)
				return ERROR;
		if (plu.spec != '.' && plu.spec != '/') {
			if (reg.find(3, sc_value(M_SPCMXM)) < 1)
				return 0;
			if (plu.dpt_nbr < 1)
				return ERROR;
		} else
			plu.cmp_nbr = keyValue(String.valueOf(plu.link));
		return M_SPCMXM;
	}

	static void mix_match() {
		int ind = scan_mxm(itm.mmt), prv, cnt = 0;

		if (ind < 0)
			panel.clearLink(Mnemo.getInfo(35), 0x81);
		if (ind < 1)
			return;

		if ((plu.flag & F_NEGSLS) > 0)
			plu.price = -plu.price;
		long total = set_price(plu, itm.prpos);
		cnt = Itmdc.chk_match(plu.spec != '%' && plu.spec != '/') / 100;
		if (plu.spec == '/') {
			plu.spec = '.';
			plu.price = (int) (total / plu.price);
		} else
			plu.amt = plu.price - total;
		if (plu.spec == '%')
			plu.amt = roundBy(total * plu.price, 1000);
		if (plu.spec == '$')
			plu.amt += total;
		prv = cnt - itm.cnt * itm.unit;
		plu.cnt = cnt / plu.unit - prv / plu.unit;
		if (plu.cnt == 0)
			return;
		plu.spf1 = itm.spf1;
		Itemdata sav = itm;
		itm = plu;
		if (itm.spec == '.') {
			itm.amt = itm.cnt * total;
			itm.pnt = itm.cnt * itm.price;
			GdSales.pnt_line(sc_points(0));
		} else
			mxm_line(ind);
		itm = sav;
	}

	static void mxm_line(int ind) {
		int sc = sc_value(ind);

		itm.spf2 |= ind;
		itm.amt *= itm.cnt;
		tra.vItems.addElement('C', itm);
		Itmdc.IDC_write('C', sc, 0, itm.number, itm.cnt, itm.amt);
		accumReg(3, sc, itm.cnt, itm.amt);
		accumDpt(2, itm.cnt, itm.amt);
		accumSlm(2, itm.cnt, itm.amt);
		itm.dsc = getDiscount();
		Itmdc.IDC_write('C', 9, itm.sit, itm.number, 0, itm.dsc);
		itm.idc = lTRA.getSize();
		if (itm.dsc != 0)
			GdSales.itm_discount();
		GdSales.itm_tally(0, itm.amt);
		tra.amt += itm.amt;
		TView.append('C', 0x01, itm.text, editInt(itm.cnt), editMoney(0, itm.amt / itm.cnt), editMoney(0, itm.amt), "");
		GdSales.itm_edit().book(3);
		Promo.updateItems(itm.amt);
	}

	/*******************************************************************/
	/*                                                                 */
	/* special price itemization */
	/*                                                                 */
	/*******************************************************************/

	static boolean chk_special(char spec, String rule) {
		int lim, max, min;

		switch (spec) {
		case 'X':
			plu.spf3 = 4;
			if (plu.prm > 0)
				break;
			return plu.qty >= Integer.parseInt(rule);
		case 'D':
			plu.spf3 = 5;
			min = Integer.parseInt(rule.substring(0, 6));
			if (cmpDates(min, ctl.date) > 0)
				break;
			max = Integer.parseInt(rule.substring(6));
			return cmpDates(max, ctl.date) >= 0;
		case 'T':
			plu.spf3 = 6;
			lim = Integer.parseInt(rule.substring(0, 4));
			if (lim > 0)
				if (lim != weekDay(ctl.date))
					break;
			lim = ctl.time / 100;
			min = Integer.parseInt(rule.substring(4, 8));
			max = Integer.parseInt(rule.substring(8));
			return lim >= min && lim < max;
		case 'P':
			plu.spf3 = 7;
			if ((plu.spf1 & M_VOID) > 0)
				break; /* positive only */
			lim = tra.spf1 > 0 ? -tra.pnt : tra.pnt;
			min = Integer.parseInt(rule.substring(4));
			if (min * plu.qty > lim)
				break;
			itm.prpnt = -min;
			itm.cmp_nbr = keyValue(rule.substring(0, 4));
			return true;
		case 'G':
			for (min = 0; min < 12; min = max) {
				lim = keyValue(rule.substring(min, max = min + 3));
				if (lim == ctl.reg_nbr || lim == ctl.grp_nbr)
					return true;
			}
		}
		return false;
	}

	static int scan_pov(char spec) {
		plu = itm.copy();
		plu.number = " " + spec + plu.number.substring(2);
		int sts = lPLU.find(plu.number, rMNT.recno > 0);
		if (sts < 1) {
			return sts == 0 || ctl.lan > 2 ? 29 : 16;
		}
		lPLU.skip(18);
		while (lPLU.index < lPLU.dataLen()) {
			String rule = lPLU.scan(12);
			plu.price = lPLU.scanNum(8);
			if (plu.price > 0) {
				if (chk_special(spec, rule.replace(' ', '0')))
					plu.prpov = plu.price;
			}
		}
		if ((itm.prpov = plu.prpov) > 0)
			itm.spf3 = plu.spf3;
		return 0;
	}

	static int byLane(char spec) {
		String key = " " + spec + plu.number.substring(2);
		int sts = lPLU.find(key, rMNT.recno > 0);
		if (sts < 1) {
			return sts == 0 || ctl.lan > 2 ? 29 : 16;
		}
		lPLU.skip(18);
		while (lPLU.index < lPLU.dataLen()) {
			String rule = lPLU.scan(12);
			int price = lPLU.scanNum(8);
			if (price > 0) {
				if (chk_special(spec, rule.replace(' ', '0')))
					plu.price = price;
			}
		}
		plu.spec = ' ';
		return 0;
	}

	/*******************************************************************/
	/*                                                                 */
	/* automatic rebate */
	/*                                                                 */
	/*******************************************************************/

	static boolean chk_rebate(String rule, long total) {
		int lim, max, min;

		rule = rule.replace(' ', '0');
		switch (rule.charAt(0)) {
		case 'D':
			min = Integer.parseInt(rule.substring(1, 7));
			if (cmpDates(min, ctl.date) > 0)
				break;
			max = Integer.parseInt(rule.substring(8));
			return cmpDates(max, ctl.date) >= 0;
		case 'T':
			lim = Integer.parseInt(rule.substring(1, 4));
			if (lim > 0)
				if (lim != ctl.wday)
					break;
			lim = ctl.time / 100;
			min = Integer.parseInt(rule.substring(5, 9));
			max = Integer.parseInt(rule.substring(10));
			return lim >= min && lim < max;
		case '<':
			return total < Long.parseLong(rule.substring(1));
		case '>':
			return total > Long.parseLong(rule.substring(1));
		}
		return false;
	}

	static boolean scan_rebate(BinIo io) {
		String rule;

		try {
			plu.dpt_nbr = io.skip(16).scanKey(4);
			plu.flag = io.scanHex(2);
			rule = io.scan(14);
			plu.text = io.scan(20);
			plu.ptyp = io.skip(10).scan(3);
			plu.spec = io.scan();
			plu.price = io.scanNum(8);
		} catch (NumberFormatException e) {
			io.error(e, false);
			return false;
		}
		int ind = (tra.spf2 ^ 3) % 3;
		if (Character.toUpperCase(plu.ptyp.charAt(ind)) == 'X')
			return false;
		if (plu.spec == '%') {
			plu.crd = roundBy(plu.amt * plu.price, 1000);
		}
		if (plu.spec == '/') {
			if (plu.price > 0)
				plu.price = (int) (plu.com / plu.price);
			plu.spec = '.';
		}
		return chk_rebate(rule, plu.spec == '.' ? plu.com : plu.amt);
	}

	static void rbt_match(BinIo io) {
		int nxt = itm.dpt, sts;
		String key = "DP          ";

		if (tra.spf1 > 0 || (itm.spf1 & M_RETURN) > 0)
			return;
		if ((itm.spf2 & M_PRCRED) > 0)
			return;
		if (!Promo.isEnabled(Promo.PROMO_STD))
			return;

		plu = itm.copy();
		if (!itm.isPlu()) {
			plu.number = key + editKey(plu.dpt_nbr, 4);
		}
		if (dpt.key[nxt - 1] == itm.dpt_nbr)
			nxt = dpt.grp[nxt - 1];
		plu.amt = plu.com = set_price(itm, itm.prpos);
		for (;;) {
			if ((sts = io.start(plu.number)) > 0)
				do {
					if (scan_rebate(io))
						break;
				} while ((sts = io.next(plu.number)) > 0);
			if (sts > 0)
				break;
			if (sts < 0)
				return;
			if (plu.number.charAt(0) == ' ') {
				plu.number = key + editKey(itm.dpt_nbr, 4);
				continue;
			}
			if (nxt == 0)
				return;
			plu.number = key + editKey(dpt.key[nxt - 1], 4);
			nxt = dpt.grp[nxt - 1];
		}
		if (plu.spec == '.') {
			if ((itm.flag & F_XPROMO) > 0)
				return;
			Itemdata sav = itm;
			itm = sav.copy();
			itm.cmp_nbr = plu.dpt_nbr;
			itm.text = plu.text;
			itm.prpnt = (plu.flag & F_NEGSLS) > 0 ? -plu.price : plu.price;
			itm.pnt = itm.cnt * itm.prpnt;
			GdSales.pnt_line(sc_points(0));
			sav.pnt += itm.pnt;
			itm = sav;
			return;
		}
		if (itm.sit < 1 || itm.crd != 0)
			return;
		if (reg.find(3, sc_value(M_REBATE)) < 1)
			return;
		if (plu.spec != '%')
			plu.crd = set_price(itm, plu.price);
		if (plu.crd >= plu.amt)
			return;
		if (plu.spec == '=')
			plu.crd -= plu.amt;
		if ((plu.crd *= itm.qty) == 0)
			return;
		Itemdata sav = itm;
		itm = sav.copy();
		itm.spf2 |= M_REBATE;
		itm.rate = plu.spec == '%' ? plu.price : 0;
		itm.text = plu.text;
		itm.cnt = itm.qty;
		itm.amt = (plu.flag & F_NEGSLS) > 0 ? -plu.crd : plu.crd;
		itm.sign();
		GdSales.crd_line();
		sav.crd += itm.amt;
		Promo.updateItems(itm.amt);
		itm = sav;
	}

	static void rbt_total(BinIo io) {
		int sts;

		if (!Promo.isEnabled(Promo.PROMO_STD))
			return;
		plu = itm.copy();
		plu.amt = itm.amt < 0 ? -itm.amt : itm.amt;
		plu.com = itm.com < 0 ? -itm.com : itm.com;
		if ((sts = io.start(itm.number)) > 0)
			do {
				if (scan_rebate(io))
					break;
			} while ((sts = io.next(itm.number)) > 0);
		if (sts < 1)
			return;
		if (plu.spec == '.') {
			if (itm.com == 0)
				return;
			itm.prpnt = (plu.flag & F_NEGSLS) > 0 ? -plu.price : plu.price;
			itm.pnt = pts_valid(itm.com < 0 ? -itm.prpnt : itm.prpnt);
			if (itm.pnt == 0)
				return;
			itm.cmp_nbr = plu.dpt_nbr;
			itm.text = plu.text;
			itm.amt = itm.rew_amt = itm.com;
			itm.spf3 = sc_points(1);
			tra.vTrans.addElement('G', itm.copy());
			return;
		}
		if (itm.spf2 > 0)
			return;
		if (reg.find(4, sc_value(M_TOTRBT)) < 1)
			return;
		if (plu.spec != '%')
			plu.crd = plu.price;
		if (plu.crd >= plu.amt)
			return;
		if (plu.spec == '=')
			plu.crd -= plu.amt;
		if (plu.crd == 0)
			return;
		itm.crd = (plu.flag & F_NEGSLS) > 0 ? -plu.crd : plu.crd;
		if (itm.amt < 0)
			itm.crd = -itm.crd;
		itm.rate = plu.spec == '%' ? plu.price : 0;
		itm.text = plu.text;
		itm.rew_amt = itm.amt;
		itm.spf2 |= M_TOTRBT;
	}

	/*******************************************************************/
	/*                                                                 */
	/* short codes direct / list selection */
	/*                                                                 */
	/*******************************************************************/

	static int chk_short(int ind) {
		String s = plu_tbl[ind].substring(12);
		int dpt_no = keyValue(s), sts = 0;

		panel.display(1, plu_txt[ind] + "  ");
		panel.dspPicture("REF_" + editKey(dpt_no, 4));
		if (input.num == 0) {
			File f = localFile("gif", "TCH_" + s + ".GIF");
			input.prompt = Mnemo.getText(15);
			if (f.exists()) {
				input.init(0x00, 2, 2, 0);
				TchDlg dlg = new TchDlg(plu_tbl[ind]);
				dlg.area.setImage(f);
				dlg.show("TCH");
				dlg.area.setPicture(null);
				if (dlg.code > 0)
					return dlg.code;
			} else {
				input.init(0x00, 4, 4, 0);
				SelDlg dlg = new SelDlg(plu_tbl[ind]);
				dlg.sorted = options[O_ElJrn] > 0x0f;
				for (int rec = 0; lREF.read(++rec) > 0;)
					try {
						if (dpt_no != lREF.scanKey(4))
							continue;
						String key = lREF.scan(':').scan(4);
						if (key.endsWith(" "))
							continue;
						dlg.add(10, key, " " + lREF.skip(23).scan(20));
					} catch (NumberFormatException e) {
						lREF.error(e, false);
					}
				if (dlg.list.getItemCount() < 1)
					return 7;
				dlg.show("REF");
				if (dlg.code > 0)
					return dlg.code;
			}
			if (input.key == 0)
				input.key = input.CLEAR;
			if (input.num < 1 || input.key != input.ENTER)
				return 5;
			sts = input.adjust(input.pnt);
			if (sts > 0)
				return sts;
		}
		if (lREF.find(editKey(dpt_no, 4) + ':' + editTxt(input.pb, 4)) < 1)
			return 8;
		s = lREF.skip(10).scan(16);
		if (!s.startsWith("SET"))
			sts--;
		else
			s = s.substring(8);
		input.prompt = "";
		input.reset(s.trim());
		return sts;
	}

	/**
	 * Search for a shortcode in REF
	 *
	 * @param key
	 *            list identifier
	 * @param ind
	 *            index of picture in list
	 * @return 0=found (plu in input.pb), 8=not found
	 **/
	static int chk_item(String key, int ind) {
		if (!input.isEmpty())
			return 5;
		if (lREF.find(key + ":  " + editNum(ind, 2)) < 1) {
			stsLine.init(Mnemo.getText(15)).upto(20, editNum(ind, 2)).show(2);
			return 8;
		}
		String s = lREF.skip(10).scan(16);
		input.prompt = "";
		input.reset(s.trim());
		return s.charAt(0) == ' ' ? 0 : 7;
	}

	/*******************************************************************/
	/*                                                                 */
	/* sales qualifications */
	/*                                                                 */
	/*******************************************************************/

	static int chk_quali(Itemdata ptr) {
		int nxt = ptr.dpt, sts;
		String key = "DP          ";

		if (lQLU.getSize() < 1)
			return 0;
		if (tra.code > 6 || ctl.mode == M_RENTRY)
			return 0;
		if (tra.spf1 > 0)
			return 0;
		if ((ptr.spf1 & (M_VOID | M_RETURN)) > 0)
			return 0;

		plu = ptr.copy();
		if (!plu.isPlu()) {
			plu.number = key + editKey(ptr.dpt_nbr, 4);
			if (dpt.key[nxt - 1] == ptr.dpt_nbr)
				nxt = dpt.grp[nxt - 1];
		}
		plu.amt = plu.com = set_price(ptr, ptr.price);
		for (;; nxt = dpt.grp[nxt - 1]) {
			if ((sts = lQLU.start(plu.number)) > 0)
				do {
					if (scan_rebate(lQLU))
						break;
				} while ((sts = lQLU.next(plu.number)) > 0);
			if (sts > 0)
				break;
			if (sts < 0 || nxt == 0)
				return 0;
			plu.number = key + editKey(dpt.key[nxt - 1], 4);
		}
		ptr.flag |= plu.flag & F_ONSLIP;
		if ((plu.flag & F_SPCSLS) > 0)
			ptr.qual = plu.text;
		if (tra.res > 0)
			return 0;
		panel.dspPicture("QUA_" + editKey(plu.dpt_nbr, 4));
		panel.display(1, ptr.text);
		panel.display(2, plu.text);
		if (plu.spec == '!')
			panel.clearLink(Mnemo.getInfo(42), 0x22);
		if (plu.spec == '?') {
			if (panel.clearLink(Mnemo.getInfo(40), 0x23) < 2)
				return 7;
		}
		if (plu.spec == 'S')
			if ((sts = GdSigns.chk_autho(Mnemo.getInfo(38))) > 0)
				return sts;
		return plu.spec == 'X' ? 7 : 0;
	}

	/*******************************************************************/
	/*                                                                 */
	/* reason codes */
	/*                                                                 */
	/*******************************************************************/

	static int chk_reason(int code) {
		int sts;

		for (;; panel.clearLink(Mnemo.getInfo(sts), 1)) {
			if (lRCD.find(editNum(code * 100, 4)) < 1)
				return ERROR;
			panel.dspPicture("RCD_" + lRCD.scan(4));
			panel.display(1, lRCD.skip(4).scan(20));
			SelDlg dlg = new SelDlg(Mnemo.getText(22));
			while (lRCD.read(++lRCD.recno) > 0) {
				if (lRCD.scanNum(2) > code)
					break;
				String key = lRCD.scan(2);
				dlg.add(11, key, " " + lRCD.skip(4).scan(20));
			}
			if (dlg.list.getItemCount() < 1)
				return ERROR;
			input.prompt = Mnemo.getText(12);
			input.init(0x00, 2, 2, 0);
			dlg.show("LBS");
			if ((sts = dlg.code) < 1) {
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.num < 1 || input.key != input.ENTER)
					sts = 5;
				else
					sts = input.adjust(input.pnt);
			}
			if (input.key == input.CLEAR)
				return 7;
			if (sts > 0)
				continue;
			if ((sts = input.scanNum(input.num)) > 0)
				if (lRCD.find(editNum(code * 100 + sts, 4)) > 0)
					break;
			sts = 8;
		}
		rcd_tbl[code - 1] = sts;
		return 0;
	}

	/*******************************************************************/
	/*                                                                 */
	/* demographic data queries */
	/*                                                                 */
	/*******************************************************************/

	static int dd_query(char id, int code) {
		for (; lDDQ.read(lDDQ.recno) > 0; lDDQ.recno++) {
			if (dd_check(id, code)) {
				panel.jrnPicture("QUERY");
				itm.mark = id;
				itm.spf1 = code;

				idQuery = id;  //INSTASHOP-RECORD-CGA#A

				if (dd_next() == 0)
					return 0;
			}
		}
		return ERROR;
	}

	static boolean dd_check(char id, int code) {
		if (lDDQ.scan() != id)
			return false;
		char c = lDDQ.scan();
		if (!Character.isDigit(c))
			return true;
		return code == c - '0';
	}
//T0:0001:N19:M1...:Queue              *
	static int dd_next() {
		int ind = 20, max = 0, min, dec = 0;

		try {
			itm.number = lDDQ.scan(':').scan(4);
			min = lDDQ.skip(2).scanNum(1);
			itm.qty = lDDQ.scan();
			itm.spec = lDDQ.scan(':').scan();
			itm.flag = lDDQ.scanHex(1);
			input.prompt = lDDQ.skip(4).scan(20);
		} catch (NumberFormatException e) {
			lDDQ.error(e, false);
			return 8;
		}
		for (; ind > 10; max++) {
			char c = input.prompt.charAt(--ind);
			if (c == ' ')
				break;
			if (c == input.POINT)
				dec = max--;
		}

		if (idQuery != 'A' && idQuery != 'I'  && idQuery != 'S') {   //INSTASHOP-RECORD-CGA#A
			event.onto(0, editNum(max, 3)).push(',').push(editNum(max, 3)).push(',').push(editNum(dec, 3));
			event.rewrite(event.nxt + 1, 28);
			event.onto(4, editNum(min, 3)).skip(4);
			event.rewrite(event.nxt + 2, 28);
		}

		oplLine.init(input.prompt);

		return 0;
	}

	/*******************************************************************/
	/*                                                                 */
	/* select department / salesperson / account */
	/*                                                                 */
	/*******************************************************************/

	static int lb_group(int type, int key) {
		TableSls sls = type == 5 ? slm : dpt;
		int rec = type < 13 ? sls.grp[sls.find(key) - 1] : 0;
		return rec > 0 ? sls.key[rec - 1] : 0;
	}

	static int lb_money(int ind, int rec) {
		if (rec > 0)
			while (lALU.read(++ind) > 0) {
				if ((dlu.dpt_nbr = lALU.scanKey(4)) == 0)
					continue;
				if (GdMoney.src_alu(dlu.dpt_nbr) < 1)
					return ind;
			}
		else
			while (ind < 8) {
				if ((rec = reg.find(tra.code, ++ind)) < 1)
					continue;
				lREG.read(rec, lREG.LOCAL);
				dlu.text = lREG.text;
				dlu.dpt = 0;
				dlu.dpt_nbr = ind;
				if (lALU.getSize() > 0)
					dlu.dpt_nbr += 0xfff0;
				return ind;
			}
		return 0;
	}

	static void lb_add(SelDlg dlg, int key, int sel, String text) {
		int cnt = dlg.list.getItemCount();
		dlg.add(10, editKey(key, 4), " " + text);
		if (cnt < 1 || key == sel)
			dlg.list.select(cnt);
	}

	static int lb_show(int type, int key, int sel) {
		int rec = 0, grp;
		String text = "", bmap = type == 5 ? "SLM_" : "DPT_";
		SelDlg dlg = new SelDlg(Mnemo.getText(22));

		if (type == 5) {
			if (key > 0) {
				if ((grp = slm.find(key)) < 1)
					return ERROR;
				lSLM.read(grp, lSLM.LOCAL);
				text = lSLM.text;
			}
			while (rec < slm.key.length) {
				if ((grp = slm.grp[rec++]) > 0)
					grp = slm.key[grp - 1];
				if (grp != key)
					continue;
				lSLM.read(rec, lSLM.LOCAL);
				lb_add(dlg, lSLM.key, sel, lSLM.text);
			}
			for (rec = 0; lSLU.read(++rec) > 0;) {
				grp = lSLU.scanKey(4);
				if (lSLU.scanKey(4) != key)
					continue;
				lb_add(dlg, grp, sel, lSLU.skip(2).scan(20));
			}
		}
		if (type == 6) {
			if (key > 0) {
				if ((grp = dpt.find(key)) < 1)
					return ERROR;
				lDPT.read(grp, lDPT.LOCAL);
				text = lDPT.text;
			}
			while (rec < dpt.key.length) {
				if ((grp = dpt.grp[rec++]) > 0)
					grp = dpt.key[grp - 1];
				if (grp != key)
					continue;
				lDPT.read(rec, lDPT.LOCAL);
				lb_add(dlg, lDPT.key, sel, lDPT.text);
			}
			for (rec = 0; lDLU.read(++rec) > 0;) {
				grp = lDLU.scanKey(4);
				if (lDLU.scanKey(4) != key)
					continue;
				lb_add(dlg, grp, sel, lDLU.skip(14).scan(20));
			}
		}
		if (type == 13)
			if (key < 1 || key > 0xfff0) {
				bmap = tra.code == 5 ? "ROA_" : "PDO_";
				while ((rec = lb_money(rec, key &= 0x000f)) > 0) {
					if (dlu.dpt != key)
						continue;
					lb_add(dlg, dlu.dpt_nbr, sel, dlu.text);
				}
			}
		if (dlg.list.getItemCount() < 1)
			return ERROR;
		panel.display(1, text);
		panel.dspPicture(bmap + editKey(key, 4));
		input.prompt = Mnemo.getText(type);
		input.init(0x00, 0, 4, 0);
		dlg.itemEcho(-1);
		dlg.show("LBS");
		if (dlg.code > 0)
			return dlg.code;
		if (input.key == input.ENTER)
			return 0;
		if (input.key != 0)
			return 5;
		input.key = input.CLEAR;
		return ERROR;
	}

	static int lb_select(int type, int base) {
		int key = base, sel = 0, sts;

		while ((sts = lb_show(type, key, sel)) >= 0) {
			if (input.key == input.CLEAR) {
				if (key == base)
					return 7;
				key = lb_group(type, sel = key);
				continue;
			}
			sel = input.scanKey(input.num);
			if (sts < 1)
				key = sel;
			else
				panel.clearLink(Mnemo.getInfo(sts), 1);
		}
		if (input.key != input.ENTER)
			return 7;
		for (int ind = 0; ind < input.num; ind++) {
			if (!Character.isDigit(input.pb.charAt(ind)))
				return 8;
		}
		return input.index = 0;
	}
}
