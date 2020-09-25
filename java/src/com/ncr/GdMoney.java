package com.ncr;

class GdMoney extends Action {
	static int src_alu(int key) {
		if (lALU.find(editKey(key, 4)) < 1)
			return 7;
		try {
			dlu.dpt = lALU.skip(4).scanNum(2);
			dlu.tnd = lALU.scanNum(2);
			dlu.flag = lALU.scanHex(2);
			dlu.sit = lALU.scanNum(1);
			dlu.vat = lALU.scanNum(1);
			dlu.cat = lALU.scanNum(2);
			dlu.halo = lALU.scanHex(4);
			dlu.text = lALU.scan(20);
			dlu.xtra = lALU.scan(10);
		} catch (NumberFormatException e) {
			lALU.error(e, false);
			return 8;
		}
		if (tra.code == 6)
			dlu.dpt = dlu.tnd;
		return 0;
	}

	static void mny_print(int station) {
		if (itm.number.length() > 0) {
			prtLine.init(Mnemo.getText(30)).onto(12, itm.number).book(station);
		}
		if (itm.eXline.length() > 0)
			prtLine.init(itm.eXline).book(station);
		prtLine.init(itm.text).onto(20, editKey(itm.dpt_nbr, 4)).upto(40, editMoney(0, itm.amt)).push(itm.mark)
				.book(station);
	}

	static int mny_line() {
		int sc = sc_value(itm.spf1);

		itm.amt = itm.qty * (long) itm.price;
		itm.cnt = itm.qty;
		itm.spf1 |= tra.spf1;
		itm.sign();
		if ((itm.stat = tra.code) == 6)
			itm.amt = -itm.amt;
		tra.vItems.addElement('M', itm);
		pit = itm.copy();
		Itmdc.IDC_write('M', sc, itm.dpt, itm.number, itm.cnt, itm.amt);
		TView.append('M', 0x00, itm.text, "", "", editMoney(0, itm.amt), "");
		accumReg(tra.code, itm.dpt, itm.cnt, itm.amt);
		tra.cnt += itm.cnt;
		tra.amt += itm.amt + itm.dsc;
		GdTrans.tra_balance();
		oplLine.init(itm.text);
		dspLine.init(editKey(itm.dpt_nbr, 4)).upto(20, editMoney(0, itm.amt));
		mny_print(3);
		cusLine.init(itm.text).show(10);
		cusLine.init(tnd[0].symbol).upto(20, editMoney(0, itm.amt));
		if ((itm.flag & F_ONSLIP) > 0)
			if (itm.spf1 == 0)
				DevIo.tpmCheque(0, itm.number, itm.amt);
		return GdTrans.itm_clear();
	}

	/**
	 * money clear
	 **/
	int action0(int spec) {
		dspLine.init(Mnemo.getMenu(tra.head));
		if (!tra.isActive())
			event.nxt = event.alt;
		dspBmap = (tra.code == 6 ? "PDO_" : "ROA_") + "0000";
		return GdTrans.itm_clear();
	}

	/**
	 * money type
	 **/
	int action1(int spec) {
		int key = lALU.getSize() > 0 ? 0xffff : 0, sts;

		if (input.num == 0)
			if ((sts = Match.lb_select(13, 0)) > 0)
				return sts;
		itm.dpt_nbr = input.scanKey(input.num);
		dlu = itm.copy();
		if (key > 0) {
			if ((sts = src_alu(itm.dpt_nbr)) > 0)
				return sts;
		} else
			dlu.dpt = itm.dpt_nbr;
		if (dlu.dpt < 1 || dlu.dpt > 8)
			return 8;
		if ((sts = sc_checks(tra.code, dlu.dpt)) > 0)
			return sts;
		if (key == 0) {
			dlu.text = lREG.text;
			dlu.sit = 1;
		}
		itm = dlu;
		dspLine.init(itm.text);
		dspBmap = (tra.code == 6 ? "PDO_" : "ROA_") + editKey(itm.dpt_nbr, 4);
		if ((itm.flag & F_SKPSKU) > 0)
			event.nxt = event.alt;
		if ((itm.flag & F_NEGSLS) > 0)
			itm.spf1 |= M_REFUND;
		return 0;
	}

	/**
	 * account number
	 **/
	int action2(int spec) {
		int rec, sel, tran;

		if (input.num == 13) {
			if (input.scan() != '1')
				return 5;
			if (cdgCheck(input.pb, ean_weights, 10) > 0)
				return 9;
			if (!dat_valid(input.scanNum(4)))
				return 8;
			input.reset(input.scan(7));
		}
		if (input.num > 8)
			return 2;
		itm.number = input.pb;
		lREG.read(reg.find(tra.code, itm.dpt), lREG.LOCAL);
		if ((lREG.tflg & 0x20) == 0) {
			if (input.num > 0)
				dspLine.init("#" + itm.number);
			return 0;
		}
		if (input.num > 7)
			return 2;
		if (input.num < 4)
			return 3;
		sel = input.scanKey(input.num - 4);
		tran = input.scanNum(4);
		if (sel == 0)
			sel = ctl.reg_nbr;
		itm.number = editKey(sel, 3) + editNum(tran, 4);
		if (sel == ctl.reg_nbr)
			sel = LOCAL;
		if ((rec = Magic.src_frec(tran, sel)) < 0) {
			if (panel.clearLink(Mnemo.getInfo(16), 3) < 2)
				return 7;
			dspLine.init(Mnemo.getText(30)).upto(20, itm.number);
			return 0;
		}
		while (rec > 1) {
			if (lIDC.read(--rec, sel) < 1)
				return 16;
			if (lIDC.skip(23).scanNum(4) != tran)
				break;
			if (lIDC.skip(5).scan() != 'M')
				continue;
			if (lIDC.skip(27).scanNum(2) > 0)
				break;
			if ((itm.amt = 0 - lIDC.skip(6).scanDec(10)) <= 0)
				break;
			input.reset(Long.toString(itm.amt));
			event.nxt = event.alt;
			return action3(0);
		}
		return 7;
	}

	/**
	 * money amount
	 **/
	int action3(int spec) {
		itm.price = input.scanNum(input.num);

		if (itm.price == 0)
			return 8;
		if (chk_halos(itm.halo, itm.price))
			return 5;
		if (!tra.isActive())
			GdRegis.set_tra_top();
		itm.spf1 |= tra.spf1;
		itm.qty = 1;
		if (itm.eXline.length() < 1) {
			String key = tra.code == 6 ? "Pdo" : "Roa";
			if (lDBL.find(key + editKey(itm.dpt_nbr, 4)) > 0)
				itm.eXline = lDBL.pb.substring(8);
		}
		return mny_line();
	}

	/**
	 * error correct
	 **/
	int action4(int spec) {
		if ((pit.flag & F_ONSLIP) > 0)
			return 7;
		if (TView.syncIndex(pit.index) != pit.index)
			return 7;
		itm = pit;
		itm.spf1 |= M_ERRCOR;
		itm.mark = Mnemo.getText(60).charAt(6);
		return mny_line();
	}

	/**
	 * collective sales
	 **/
	int action9(int spec) {
		int ic = spec / 10, sc = spec % 10, sts;
		String nbr = editKey(ctl.reg_nbr, 3) + editNum(ctl.tran, 4);

		if (tra.mode < M_GROSS)
			return 5;
		if ((tra.bal < 0) ^ ((tra.spf1 & M_TRVOID) > 0))
			return 7;
		if ((sts = sc_checks(ic, sc)) > 0)
			return sts;
		if (tra.tnd == 0)
			GdTrans.tra_total();
		if (tra.tnd == 2)
			GdTrans.tra_taxes();
		else {
			showTotal(0);
			GdTndrs.tnd_drawer();
		}
		itm = new Itemdata();
		itm.cnt = -tra.cnt;
		itm.amt = -tra.bal;
		accumReg(ic, sc, itm.cnt, itm.amt);
		Itmdc.IDC_write('M', sc_value(tra.spf1), sc, nbr, itm.cnt, itm.amt);
		lREG.read(reg.find(ic, sc), lREG.LOCAL);
		TView.append('M', 0x80, lREG.text, "", "", editMoney(0, tra.bal), "");
		dspLine.init(lREG.text).upto(20, editMoney(0, tra.bal));
		cusLine.init(lREG.text).show(10);
		cusLine.init(tnd[0].symbol).upto(20, editMoney(0, tra.bal));
		prtLine.init('#' + nbr).onto(20, lREG.text).upto(40, editMoney(0, tra.bal)).book(3);
		GdTrans.tra_finish();
		lREG.read(reg.find(ic, sc), lREG.LOCAL);
		if ((lREG.tflg & 0x20) == 0)
			return 0;
		stsLine.init(lREG.text).upto(20, nbr);
		prtDwide(2, stsLine.toString());
		String ean = 1 + editNum(ctl.date, 4) + nbr + 0;
		DevIo.tpmLabel(2, cdgSetup(ean, ean_weights, 10));
		GdRegis.hdr_print();
		return 0;
	}
}
