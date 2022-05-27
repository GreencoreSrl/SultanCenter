package com.ncr;

abstract class UpSet extends Action {
	static HshIo lSEX = new HshIo("SET", 20, 32); /* set references */
	static Itemdata mxs; /* set discount data (of 1st item) */

	static void scan_set() {
		dlu.number = lSET.scan(':').scan(16);
		dlu.qty = lSET.skip(6).scanNum(7);
		dlu.cnt = (int) lSET.scanDec(8);
		if (ctl.tran != lSET.scan(':').scanNum(4))
			dlu.cnt = 0;
	}

	static void SET_update(int rec, int cnt) {
		lSET.index = 0;
		lSET.pushDec(cnt, 8).push(':').push(editNum(ctl.tran, 4));
		lSET.rewrite(rec, 39);
	}

	static int setCheck(String sid, int cnt) {
		int newSets = Integer.MAX_VALUE, oldSets = newSets, sets;

		if (lSET.find(sid.substring(0, 5) + "0000") < 1)
			return 0;
		mxs.serial = lSET.skip(lSET.fixSize + 1).scan(16);
		mxs.qual = lSET.skip(6).scan(20);
		while (lSET.read(++lSET.recno) > 0) {
			if (!sid.startsWith(lSET.scan(5)))
				break;
			String key = lSET.scan(4);
			scan_set();
			if (dlu.qty == 0)
				continue;
			sets = dlu.cnt / dlu.qty;
			if (newSets > sets)
				newSets = sets > 0 ? sets : 0;
			if (sid.endsWith(key)) {
				sets = (dlu.cnt - cnt) / dlu.qty;
				mxs.rpt = dlu.qty;
			}
			if (oldSets > sets)
				oldSets = sets > 0 ? sets : 0;
			if (key.equals("0001")) {
				mxs.number = dlu.number;
				mxs.qty = dlu.qty;
			}
		}
		return newSets - oldSets;
	}

	static void setAccum(String key, int cnt, String skip) {
		for (int ind = 0; lSEX.find(key + editNum(++ind, 4)) > 0;) {
			String sid = lSEX.skip(lSEX.fixSize + 1).scan(lSET.fixSize);
			if (skip != null)
				if (sid.startsWith(skip))
					continue;
			if (lSET.find(sid) < 1)
				break;
			lSET.index += lSET.fixSize;
			scan_set();
			SET_update(lSET.recno, dlu.cnt + cnt);
		}
	}

	static void setProperties() {
		int rec = tra.vItems.size();

		while (rec-- > 0) {
			plu = tra.vItems.getElement(rec);
			if (plu.id != 'S')
				continue;
			if ((plu.spf1 & M_RETURN) > 0)
				continue;
			if ((plu.spf2 & M_PRCRED) > 0)
				continue;
			if (!plu.number.equals(mxs.number))
				continue;
			mxs.dpt_nbr = plu.dpt_nbr;
			mxs.dpt = plu.dpt;
			mxs.flag = plu.flag;
			mxs.flg2 = plu.flg2;
			mxs.type = plu.type;
			mxs.sit = plu.sit;
			mxs.vat = plu.vat;
			mxs.cat = plu.cat;
			mxs.unit = plu.unit;
			mxs.price = plu.price;
			break;
		}
	}

	static void setEngine(String upc, int cnt) {
		for (int ind = 0; lSEX.find(upc + editNum(++ind, 4)) > 0;) {
			String sid = lSEX.skip(lSEX.fixSize + 1).scan(lSET.fixSize);
			mxs = new Itemdata();
			if ((mxs.cnt = setCheck(sid, cnt)) == 0)
				continue;
			setProperties(); /* first in set */
			mxs_line();
			if (mxs.qual != null)
				continue;
			sid = sid.substring(0, 5);
			if (lSET.find(sid + "0000") < 1)
				continue;
			cnt -= mxs.rpt * mxs.cnt;
			int undo = 0 - mxs.cnt, rpt;
			for (int rec = lSET.recno; lSET.read(++rec) > 0;) {
				if (!lSET.scan(lSET.fixSize).startsWith(sid))
					break;
				scan_set();
				setAccum(dlu.number, rpt = undo * dlu.qty, sid);
				if (undo > 0)
					if (!upc.equals(dlu.number))
						setEngine(dlu.number, rpt);
			}
			if (cnt == 0)
				break;
		}
	}

	static void setTitle() {
		if (mxs.qual == null)
			return;
		prtLine.init(mxs.qual).onto(20, Mnemo.getText(35)).upto(38, editInt(mxs.cnt)).book(3);
		mxs.qual = null;
	}

	static void mxs_match(BinIo io) {
		int ind = M_REBATE, sts;

		mxs.amt = mxs.com = set_price(mxs, mxs.price);
		plu = mxs.copy();
		if ((sts = io.start(mxs.serial)) > 0)
			do {
				if (Match.scan_rebate(io))
					break;
			} while ((sts = io.next(mxs.serial)) > 0);
		if (sts < 1)
			return;
		if (plu.spec == '.') {
			itm = mxs.copy();
			itm.cmp_nbr = plu.dpt_nbr;
			itm.text = plu.text;
			itm.prpnt = (plu.flag & F_NEGSLS) > 0 ? -plu.price : plu.price;
			itm.amt *= itm.cnt * itm.qty;
			itm.pnt = itm.cnt * itm.qty * itm.prpnt;
			setTitle();
			GdSales.pnt_line(sc_points(2));
			return;
		}
		if (mxs.spf2 > 0)
			return;
		if (reg.find(3, sc_value(ind)) < 1)
			return;
		if (plu.spec != '%')
			plu.crd = set_price(mxs, plu.price);
		if (plu.crd > plu.amt)
			return;
		if (plu.spec == '=')
			plu.crd -= plu.amt;
		if ((plu.crd *= mxs.qty) == 0)
			return;
		mxs.spf2 |= ind;
		mxs.crd = (plu.flag & F_NEGSLS) > 0 ? -plu.crd : plu.crd;
		mxs.rate = plu.spec == '%' ? plu.price : 0;
		mxs.text = plu.text;
	}

	static void mxs_line() {
		mxs_match(lCIN);
		mxs_match(lCGR);
		mxs_match(lRLU);
		itm = mxs;
		if (itm.spf2 == 0)
			return;
		int sc = sc_value(itm.spf2);
		itm.amt = itm.cnt * itm.crd;
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
		setTitle();
		TView.append('C', 0x01, itm.text, editInt(mxs.cnt), itm.rate > 0 ? editRate(itm.rate) : "",
				editMoney(0, itm.amt), "");
		GdSales.crd_print(3);
		Promo.updateItems(itm.amt);
	}

	static void mix_match() {
		if ((itm.spf2 & M_PRCRED) > 0)
			return;
		if (tra.spf1 > 0 || (itm.spf1 & M_RETURN) > 0)
			return;

		Itemdata sav = itm;
		setAccum(itm.number, itm.cnt, null);
		setEngine(itm.number, itm.cnt);
		itm = sav;
	}

	static void init() {
		int ind = lSET.getSize() * 3 + 15 >> 1 & ~7;

		lSEX.close();
		lSEX.open("inq", "S_HSHSET.DAT", 2);
		lSEX.init(' ').skip(lSEX.dataLen());
		while (ind-- > 0)
			lSEX.write();
		lSEX.close();
		lSEX.open("inq", lSEX.pathfile.getName(), 1);
		if (lSEX.getSize() < 1)
			return;
		lSET.close();
		lSET.open(null, lSET.pathfile.getName(), 1);
		for (int rec = 0; lSET.read(++rec) > 0;) {
			String sid = lSET.scan(lSET.fixSize);
			if (sid.endsWith("0000"))
				continue;
			scan_set();
			SET_update(rec, ind = 0);
			while (ind++ < 10000) {
				String key = dlu.number + editNum(ind, 4);
				if (lSEX.find(key) < 1) {
					lSEX.onto(0, key + ':' + sid);
					lSEX.rewrite(lSEX.recno, 0);
					break;
				}
			}
		}
		lSEX.sync();
	}
}