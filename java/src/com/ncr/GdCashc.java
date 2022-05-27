package com.ncr;

class GdCashc extends Action {
	int cc_cnt_dsp() {
		dspLine.init(tnd[itm.tnd].dnom[itm.dpt].text).upto(20, editInt(dnom_tbl[itm.dpt]));
		dspBmap = "CSH_" + editNum(itm.tnd, 2) + editNum(itm.dpt, 2);
		return itm.spf1 = 0;
	}

	int cc_amt_dsp() {
		int rec = reg.findTnd(itm.tnd, tra.stat);

		dspLine.init(tnd[itm.tnd].text).upto(20, editInt(reg.sales[rec - 1][0].items));
		dspBmap = "TND_" + editNum(itm.tnd, 4);
		event.dpos = tnd[itm.tnd].dec;
		itm.qty = itm.prm = 1;
		return itm.spf1 = 0;
	}

	void next_tender(int sc, int spec) {
		int ind = itm.tnd, rec;

		do {
			if (spec > 0) {
				if (itm.tnd == 1)
					itm.tnd = tnd.length;
				itm.tnd -= 2;
			}
			if (++itm.tnd == tnd.length) {
				if (ind == 0)
					return;
				itm.tnd = 1;
			}
		} while ((rec = reg.findTnd(itm.tnd, sc)) < 1);
		panel.dspSymbol(tnd[itm.tnd].symbol);
		if (reg.sales[rec - 1][0].total == 0) {
			lREG.read(rec, LOCAL);
			if ((lREG.tflg & 0x20) > 0)
				if (tnd[itm.tnd].dnom[0].value != 0) {
					itm.dpt = dnom_tbl.length;
					while (itm.dpt > 0)
						dnom_tbl[--itm.dpt] = 0;
					return;
				}
		}
		itm.dpt = -1;
		event.nxt = event.alt;
	}

	/**
	 * cc item clear
	 **/
	public int action0(int spec) {

		return spec > 0 ? cc_amt_dsp() : cc_cnt_dsp();
	}

	/**
	 * start checker cash count
	 **/
	public int action1(int spec) {
		int sts;

		if (spec > 100) {
			event.key = spec / 100;
			spec %= 100;
		}
		if ((sts = sc_checks(10, spec)) > 0)
			return sts;
		next_tender(spec, itm.tnd = 0);
		if (itm.tnd == 0)
			return 7;
		GdRegis.set_ac_ctl(event.key);
		prtTitle(tra.head = event.dec);
		tra.tim = sec_time();
		tra.stat = spec;
		tra.number = editNum(ctl.ckr_nbr, 3);
		DevIo.drwPulse(0);
		lCTL.read(ctl.ckr, lCTL.LOCAL);
		prtLine.init(Mnemo.getText(1)).upto(17, editNum(lCTL.key, 3)).onto(20, lCTL.text).book(3);
		prtLine.init(' ').book(2);
		return action0(itm.dpt < 0 ? 1 : 0);
	}

	/**
	 * abort cash count
	 **/
	public int action2(int spec) {
		prtAbort(23);
		accumReg(9, 3, 0, sec_diff(tra.tim));
		return GdRegis.prt_trailer(1);
	}

	/**
	 * count entry
	 **/
	public int action3(int spec) {
		long limit = 100000000;

		itm.qty = input.scanNum(input.num);
		if (itm.qty == 0)
			return 8;
		itm.pos = itm.qty * tnd[itm.tnd].dnom[itm.dpt].value;
		if (itm.pos >= limit)
			return 2;
		itm.cnt = itm.qty;
		itm.sign();
		if ((tnd[itm.tnd].flag & T_NEGTND) > 0)
			itm.pos = 0 - itm.pos;
		int rec = reg.findTnd(itm.tnd, tra.stat);
		long amt = reg.sales[rec - 1][0].total;
		if (Math.abs(amt + itm.pos) >= limit)
			return 2;
		dnom_tbl[itm.dpt] += itm.cnt;
		tra.amt -= tnd[itm.tnd].fc2hc(amt);
		accumTnd(tra.stat, 0, -amt);
		tra.amt += tnd[itm.tnd].fc2hc(amt += itm.pos);
		accumTnd(tra.stat, 0, amt);
		GdTrans.tra_balance();
		itm.text = tnd[itm.tnd].dnom[itm.dpt].text;
		prtLine.init(itm.text).upto(28, editInt(itm.cnt)).upto(40, editMoney(itm.tnd, itm.pos)).book(3);
		TView.append('$', 0x40, itm.text, editInt(itm.cnt), editMoney(itm.tnd, itm.pos / itm.cnt),
				editMoney(itm.tnd, itm.pos), "");
		return cc_cnt_dsp();
	}

	/**
	 * amount entry
	 **/
	public int action4(int spec) {
		long limit = 100000000;

		itm.pos = input.scanNum(input.num);
		if (itm.pos == 0)
			return 8;
		if (itm.pos != tnd[itm.tnd].round(itm.pos))
			return 8;
		if (itm.prm == 0)
			itm.pos *= itm.qty;
		if (itm.pos >= limit)
			return 2;
		itm.cnt = itm.qty;
		itm.sign();
		if ((tnd[itm.tnd].flag & T_NEGTND) > 0)
			itm.pos = 0 - itm.pos;
		int rec = reg.findTnd(itm.tnd, tra.stat);
		long amt = reg.sales[rec - 1][0].total;
		if (Math.abs(amt + itm.pos) >= limit)
			return 2;
		tra.amt -= tnd[itm.tnd].fc2hc(amt);
		accumTnd(tra.stat, 0, -amt);
		tra.amt += tnd[itm.tnd].fc2hc(amt += itm.pos);
		accumTnd(tra.stat, itm.cnt, amt);
		GdTrans.tra_balance();
		prtLine.init(tnd[itm.tnd].text).upto(17, editInt(itm.cnt));
		if (itm.prm == 0)
			prtLine.push(" x").upto(28, editMoney(itm.tnd, itm.pos / itm.cnt));
		prtLine.upto(40, editMoney(itm.tnd, itm.pos)).book(3);
		TView.append('$', 0x40, tnd[itm.tnd].text, editInt(itm.cnt),
				itm.prm < 1 ? editMoney(itm.tnd, itm.pos / itm.cnt) : "", editMoney(itm.tnd, itm.pos), "");
		return cc_amt_dsp();
	}

	/**
	 * next denom/tender
	 **/
	public int action5(int spec) {
		if (spec > 1) {
			if (spec == 2) {
				itm.spf1 = M_VOID;
				dspLine.onto(12, editTxt("- ", 8));
				return 0;
			}
			int qty = input.scanNum(input.num);
			if (qty < 1)
				return 8;
			if (spec == 3)
				itm.prm = 0;
			if (spec == 4) {
				if (tnd[itm.tnd].icnt < 2)
					return 5;
			}
			itm.qty = qty;
			dspLine.onto(12, editTxt(itm.prm > 0 ? "/ " : "* ", 8)).upto(17, editInt(itm.spf1 > 0 ? -qty : qty));
			return 0;
		}
		if (itm.dpt >= 0) {
			if (spec > 0) {
				if (itm.dpt-- > 0)
					return cc_cnt_dsp();
			} else if (++itm.dpt < dnom_tbl.length) {
				if (tnd[itm.tnd].dnom[itm.dpt].value > 0)
					return cc_cnt_dsp();
			}
		}
		next_tender(tra.stat, spec);
		return action0(itm.dpt < 0 ? 1 : 0);
	}

	/**
	 * total/finish
	 **/
	public int action6(int spec) {
		int rec;

		prtLine.init(' ').book(2);
		tra.amt = 0;
		for (itm.tnd = 0; ++itm.tnd < tnd.length;) {
			if ((rec = reg.findTnd(itm.tnd, tra.stat)) < 1)
				continue;
			Sales sls = reg.sales[rec - 1][0];
			lREG.read(rec, lREG.LOCAL);
			if (!sls.isZero()) {
				int flg = lREG.flag & 3;
				if (tnd[itm.tnd].unit > 0) {
					prtLine.init(tnd[itm.tnd].text).onto(20, tnd[itm.tnd].editXrate(true)).book(2);
					stsLine.init(tnd[itm.tnd].symbol).upto(17, editMoney(itm.tnd, sls.total));
					GdSuper.report_prt(flg, stsLine.toString(), 0, 0, sls.items, tnd[itm.tnd].fc2hc(sls.total));
				} else
					GdSuper.report_prt(flg | 0x200, tnd[itm.tnd].text, 0, 0, sls.items, sls.total);
			}
			GdTndrs.tnd_wridc('T', tra.stat, tnd[itm.tnd].ctrl, sls.items, sls.total);
			if (tra.stat == 4 || tra.stat == 5) {
				if (ctl.ability > 0)
					lPOT.readSls(rec, lPOT.blk);
				accumTnd(tra.stat, -lREG.block[0].items, -lREG.block[0].total);
			}
		}
		// TSC-MOD2014-AMZ#ADD
		if (tra.code != 15) {
			dspLine.init (Mnemo.getText (24)).upto (20, editMoney (0, tra.amt = tra.bal));
			prtLine.init (Mnemo.getText (24)).upto (40, editMoney (0, tra.amt)).book (3);
		} else {
			tra.amt = tra.bal;
		}
		// TSC-MOD2014-AMZ#END
		accumReg(9, 3, 0, sec_diff(tra.tim));
		return GdRegis.prt_trailer(2);
	}

	/**
	 * tender preselect by tender key
	 **/
	public int action7(int spec) {
		int ind = tnd_tbl[spec];

		if (ind < 1 || ind >= tnd.length)
			return 5;
		if (reg.findTnd(ind, tra.stat) < 1)
			return 7;
		itm.tnd = ind - 1;
		next_tender(tra.stat, 0);
		return action0(itm.dpt < 0 ? 1 : 0);
	}
}
