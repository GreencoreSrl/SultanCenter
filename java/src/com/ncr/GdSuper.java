package com.ncr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
class GdSuper extends Action {
	static int sel_ckrnbr(int txt) {
		if (ctl.lan == 0)
			lCTL.update();
		for (int sts;; panel.clearLink(Mnemo.getInfo(sts), 1)) {
			panel.display(1, Mnemo.getMenu(txt));
			input.prompt = Mnemo.getText(1);
			input.init(0x00, 3, 3, 0);
			SelDlg dlg = new SelDlg(Mnemo.getText(22));
			for (int rec = 0; lCTL.read(++rec, LOCAL) > 0;) {
				if (lCTL.key > 799)
					continue;
				dlg.add(7, editNum(lCTL.key, 3), ' ' + lCTL.text);
			}
			dlg.show("LBS");
			if ((sts = dlg.code) == 0) {
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.num < 1 || input.key != input.ENTER)
					sts = 5;
				else
					sts = input.adjust(input.pnt);
			}
			if (input.key == input.CLEAR)
				return 0;
			if (sts == 0) {
				int ckr = input.scanNum(input.num);
				if (ckr > 0 && ckr < 800)
					if ((ctl.ckr = lCTL.find(ckr)) > 0)
						return ckr;
				sts = 8;
			}
		}
	}

	static int sel_regnbr(int ckr, int ac) {
		int sts;

		if (ctl.lan > 2)
			tra.comm = ctl.reg_nbr;
		else
			while (tra.mode < 9) {
				if (ckr > 0)
					dspLine.init(Mnemo.getText(1)).upto(20, editNum(ckr, 3));
				else
					dspLine.init(Mnemo.getMenu(ac - 12));
				dspLine.show(1);
				input.prompt = Mnemo.getText(3);
				input.init(0x00, 3, 3, 0);
				SelDlg dlg = new SelDlg(Mnemo.getText(22));
				for (int rec = 0; lLAN.read(++rec, 0) > 0;) {
					if (lLAN.type != 'R' && lLAN.type != 'M')
						continue;
					String s = Mnemo.getText(48).substring(0, 8) + editKey(0xF00 + lLAN.grp, 3);
					if (lLAN.ckr > 0 || lLAN.sts > 0)
						s += " (" + editNum(lLAN.ckr, 3) + ' ' + editNum(lLAN.sts, 2) + ")";
					dlg.add(7, editKey(lLAN.key, 3), ' ' + s);
				}
				dlg.show("SEL");
				if ((sts = dlg.code) == 0) {
					if (input.key == 0)
						input.key = input.CLEAR;
					if (input.key == input.CLEAR)
						return ERROR;
					if (input.key != input.ENTER)
						sts = 5;
					else if (input.num == 1)
						sts = 3;
					else
						sts = input.adjust(input.pnt);
				}
				if (sts == 0) {
					tra.comm = input.scanKey(input.num);
					if (input.num == 2)
						tra.comm += 0xf00;
					if (tra.comm != ctl.reg_nbr) {
						if (ctl.lan > 0)
							sts = 16;
						else
							sts = netio.regckr(tra.comm, ckr, ac);
						if (sts == 0 && tra.comm > 0) {
							if (ac < 21 || ac > 27)
								sts = 7;
						}
					} else
						sts = ckr > 0 && lCTL.sts == 0 ? 7 : 0;
					if (sts == 0)
						break;
				}
				if (input.key == input.CLEAR)
					continue;
				panel.clearLink(Mnemo.getInfo(sts), 1);
			}
		report_hdr(ckr, ac);
		if (tra.comm >= 0xf00) {
			oplLine.init(Mnemo.getText(48)).upto(20, editKey(tra.comm, 2));
			prtLine.init(Mnemo.getText(48)).upto(17, editKey(tra.comm, 2)).book(3);
		} else if (tra.comm > 0) {
			oplLine.init(Mnemo.getText(3)).upto(20, editKey(tra.comm, 3));
			prtLine.init(Mnemo.getText(3)).upto(17, editKey(tra.comm, 3)).book(3);
			if (tra.comm == ctl.reg_nbr)
				tra.comm = LOCAL;
		} else
			oplLine.init(Mnemo.getText(11));
		oplLine.show(2);
		return 0;
	}

	static void sup_print(int lfs) {
		int station = tra.mode == 9 ? 3 : 2;

		if (lfs-- > 0) {
			while (lfs-- > 0)
				panel.print(ELJRN + 2, "");
			prtLine.book(station);
		}
	}

	static void report_hdr(int ckr, int ac) {
		tra.code = ac;
		prtTitle(tra.code - 12);
		if (ckr > 0) {
			if (ckr > 0)
				lCTL.read(ctl.ckr, lCTL.LOCAL);
			dspLine.init(Mnemo.getText(1)).upto(20, editNum(ckr, 3));
			prtLine.init(Mnemo.getText(1)).upto(17, editNum(ckr, 3)).onto(20, lCTL.text).book(3);
		}
		if (tra.subc > 0) {
			prtLine.init(Mnemo.getMenu(tra.subc + 64));
			if (tra.xtra > 0)
				prtLine.upto(17, editKey(tra.xtra, 4));
			prtLine.book(3);
		}
		dspLine.show(1);
	}

	void report_sc0(String text) {
		if (text.trim().length() == 0)
			return;
		text = center(text, 20, '*');
		prtLine.init(rightFill(text, 40, '*')).book(2);
		if (tra.code == 23)
			panel.display(1, text);
	}

	static void report_prt(int flag, String text, int rate, int trans, int items, long total) {
		if (text.trim().length() == 0)
			return;
		prtLine.init(text);
		if ((flag & 0x40) > 0)
			if (trans == 0)
				return;
		if (rate > -1000 && rate < 10000)
			if ((flag & 8) > 0)
				prtLine.upto(14, editRate(rate));
		if ((flag & 4) > 0)
			prtLine.upto(20, editInt(trans));
		if ((flag & 2) > 0)
			prtLine.upto(28, editInt(items));
		if ((flag & 1) > 0) {
			if ((flag & 0x100) > 0) {
				total += total / 3600 * 4000 + total / 60 * 40;
				prtLine.upto(37, editDec(total / 100, 2)).push(':').push(editNum((int) Math.abs(total), 2)).onto(34,
						':');
			} else
				prtLine.upto(40, editMoney((flag & 0x200) > 0 ? itm.tnd : 0, total));
		}
		sup_print(1);
	}

	void report_CTL() {
		prtLine.init(Mnemo.getText(3)).upto(17, editKey(lCTL.reg, 3));
		if (lCTL.sts == 2)
			prtLine.onto(20, Mnemo.getInfo(lCTL.lck == 2 ? 55 : 0));
		if (lCTL.sts == 19)
			prtLine.onto(20, Mnemo.getInfo(24));
		sup_print(1);
		prtLine.init(Mnemo.getText(38)).onto(9, editDate(lCTL.dateOpn)).onto(20, editTime(lCTL.timeOpn)).push(" - ")
				.push(editTime(lCTL.timeCls)).onto(35, '#').push(editNum(lCTL.sec + ctl.ckr_sec, 4));
		sup_print(1);
	}

	void report_ACT(long major[]) {
		int ind = 0, rate;

		String time = editKey(lACT.key >> 8, 2) + ':' + editKey(lACT.key & 0xff, 2);
		dspLine.init(Mnemo.getText(4)).upto(20, time).show(1);
		if (major[ind] != 0)
			rate = (int) roundBy(lACT.block[ind].total * 10000 / major[ind], 10);
		else
			rate = 10000;
		report_prt(0x4f, time, rate, lACT.block[ind].trans, lACT.block[ind].items, lACT.block[ind].total);
	}

	void report_DPT(long major[]) {
		int ind = 0, rate;

		dspLine.init(Mnemo.getText(6)).upto(20, editKey(lDPT.key, 4)).show(1);
		while (lDPT.block[ind].trans == 0)
			if (++ind == major.length)
				return;
		prtLine.init(editKey(lDPT.key, 4)).onto(6, lDPT.text);
		sup_print(2);
		for (ind = 0; ind < major.length; ind++) {
			if (major[ind] != 0)
				rate = (int) roundBy(lDPT.block[ind].total * 10000 / major[ind], 10);
			else
				rate = 10000;
			report_prt(0x4f, Mnemo.getHead(ind), rate, lDPT.block[ind].trans, lDPT.block[ind].items,
					lDPT.block[ind].total);
		}
	}

	void report_SLM(long major[]) {
		int ind = 0, rate;

		dspLine.init(Mnemo.getText(5)).upto(20, editKey(lSLM.key, 4)).show(1);
		while (lSLM.block[ind].trans == 0)
			if (++ind == major.length)
				return;
		prtLine.init(editKey(lSLM.key, 4)).onto(6, lSLM.text);
		sup_print(2);
		for (ind = 0; ind < major.length; ind++) {
			if (major[ind] != 0)
				rate = (int) roundBy(lSLM.block[ind].total * 10000 / major[ind], 10);
			else
				rate = 10000;
			report_prt(0x4f, Mnemo.getHead(ind), rate, lSLM.block[ind].trans, lSLM.block[ind].items,
					lSLM.block[ind].total);
		}
	}

	void report_REG() {
		int flag = lREG.flag, ic = lREG.key / 100, type = lREG.key % 10;

		if (type < 1) {
			if ((flag & 0x40) == 0)
				prtLine.init(' ').book(2);
			report_sc0(lREG.text);
			return;
		}
		if (type > 8) {
			if ((flag & 0x40) > 0)
				flag ^= 0x40;
			else
				prtLine.init(stl_line).book(2);
			lREG.block[0].trans = 0;
			lREG.block[0].items = tra.sub_cnt;
			tra.sub_cnt = 0;
			lREG.block[0].total = tra.sub_amt;
			tra.sub_amt = 0;
		}
		if ((flag & 0x10) > 0) {
			tra.sub_cnt += lREG.block[0].items;
			tra.sub_amt += lREG.block[0].total;
		}
		if ((flag & 0x20) > 0) {
			tra.sub_cnt -= lREG.block[0].items;
			tra.sub_amt -= lREG.block[0].total;
		}
		if (ic == 9)
			flag |= 0x100;
		else if (ic > 9) {
			flag |= 0x200;
			itm.tnd = ic - 10;
			if (type == 8)
				if ((flag & 0x40) == 0) {
					prtLine.init(tnd[itm.tnd].editXrate(true));
					sup_print(1);
					return;
				}
		}
		report_prt(flag, lREG.text, lREG.rate, lREG.block[0].trans, lREG.block[0].items, lREG.block[0].total);
	}

	void report_RSP() {
		int rec, sc, sts = 0, tcnt;

		prtLine.init(' ').book(2);
		for (itm.tnd = 0; itm.tnd < tnd.length; itm.tnd++) {
			itm.amt = itm.cnt = tcnt = 0;
			for (sc = 7; sc > 0; sc--) {
				if (sc > 3 && sc < 7)
					continue;
				if ((rec = reg.findTnd(itm.tnd, sc)) < 1)
					continue;
				if (ctl.ability > 0)
					sts = ckrRead(rec, tra.comm);
				else
					sts = lREG.read(rec, tra.comm);
				if (sts < 1)
					break;
				if ((lREG.flag & 0x10) > 0) {
					lREG.block[0].items = -lREG.block[0].items;
					lREG.block[0].total = -lREG.block[0].total;
				}
				tcnt += lREG.block[0].trans;
				itm.cnt += lREG.block[0].items;
				itm.amt += lREG.block[0].total;
			}
			if (sts < 0) {
				prtAbort(16);
				break;
			}
			if (itm.tnd == 0)
				lREG.text = Mnemo.getText(24);
			if (itm.tnd == 1 && tnd[1].limit[L_MaxDrw] > 0)
				if (itm.amt > tnd[1].limit[L_MaxDrw])
					lREG.text += "  ***";
			report_prt(lREG.flag & 0x03 | 0x240, lREG.text, 0, tcnt, itm.cnt, itm.amt);
		}
	}

	static boolean filter_sls(int rec, TableSls tbl) {
		if (tra.subc == 0)
			return true;
		if (tra.subc < 4)
			return editKey(tbl.key[rec], 4).startsWith("****".substring(4 - tra.subc));
		if (tbl.key[rec] == tra.xtra)
			return true;
		if (tra.subc > 4)
			while ((rec = tbl.grp[rec]) > 0) {
				if (tbl.key[--rec] == tra.xtra)
					return true;
				if (tra.subc == 5)
					break;
			}
		return false;
	}

	/**
	 * AC20 checker list
	 **/
	public int action0(int spec) {
		int rec = 0, sts;

		if (sel_regnbr(0, spec) < 0)
			return GdTrans.tra_clear();
		while ((sts = lCTL.read(++rec, tra.comm)) > 0) {
			if (lCTL.key > 799)
				continue;
			if (lCTL.sts == 0 || lCTL.sts == 18)
				continue;
			prtLine.init(Mnemo.getText(1)).upto(17, editNum(lCTL.key, 3)).onto(20, lCTL.text);
			sup_print(2);
			report_CTL();
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC21/19 this checker
	 **/
	public int action1(int spec) {
		int rec = 0, sts;

		lCTL.read(ctl.ckr, lCTL.LOCAL);
		if (spec == 19) {
			if (ctl.mode > 0)
				return 7;
			if (ctl.lan > 1)
				tra.comm = LOCAL;
			else {
				if (lCTL.read(ctl.ckr, tra.comm = 0) <= 0)
					return 16;
				if ((sts = netio.regckr(0, lCTL.key, spec)) > 0)
					return sts;
			}
			if (lCTL.sts == 19)
				return 24;
			tra.mode = 9;
			report_hdr(lCTL.key, spec);
		} else if (sel_regnbr(lCTL.key, spec) < 0)
			return GdTrans.tra_clear();
		tra.number = editNum(lCTL.key, 3);
		while ((sts = ckrRead(++rec, tra.comm)) > 0) {
			int ic = lREG.key / 100, sc = lREG.key % 100;
			if (ctl.ability == 0)
				if (ic > 9) {
					if (sc > 1)
						continue;
					if (ic > 10 && sc == 0)
						continue;
				}
			if (ic > 0 && lREG.flag < 0x80)
				report_REG();
			if (tra.code == 19 && ic > 10) {
				itm.tnd = ic - 10;
				itm.dpt = sc;
				if (sc == 1) {
					if (tnd[itm.tnd].ctrl == 0)
						continue;
					itm.dpt = 6; /* uncontrolled onhand */
				} else if (sc != 5)
					continue; /* cash count */
				GdTndrs.tnd_wridc('O', 0, tnd[itm.tnd].ctrl, lREG.block[0].items, lREG.block[0].total);
			}
		}

		//INSTASHOP-REPORT-CGA#A BEG
		if (spec == 21) {
			ECommerce.printInstashopReport();
		}
		//INSTASHOP-REPORT-CGA#A END


		if (sts < 0)
			prtAbort(16);
		else
			prtLine.init(' ').book(2);
		if (tra.code == 19 && tra.mode != M_CANCEL) {
			lCTL.read(ctl.ckr, lCTL.LOCAL);
			lCTL.sts = spec;
			lCTL.dateBal = ctl.date;
			lCTL.timeBal = ctl.time / 100;
			lCTL.rewrite(ctl.ckr);
			if (ctl.ckr_nbr < 800) {
				event.nxt = event.alt;
				return group[0].action4(spec);
			}
		}
		return GdRegis.prt_trailer(1);
	}

	/**
	 * AC22 office report
	 **/
	public int action2(int spec) {
		return 0;
	}

	/**
	 * AC23 financial report
	 **/
	public int action3(int spec) {
		int rec = 0, sts;

		if (sel_regnbr(0, spec) < 0)
			return GdTrans.tra_clear();
		while ((sts = lREG.read(++rec, tra.comm)) > 0) {
			int ic = lREG.key / 100, sc = lREG.key % 100;
			if (ctl.ability > 0)
				if (ic > 9 && ctl.lan < 3) {
					if (sc > 1)
						continue;
					if (ic > 10 && sc == 0)
						continue;
				}
			if (ic > 0 || (tra.comm & 0xff) != 0)
				report_REG();
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC24 activity report
	 **/
	public int action4(int spec) {
		int grp, ind, prv = 0, rec = 0, sts = 0;
		long major[] = new long[lACT.block.length];

		if (sel_regnbr(0, spec) < 0)
			return GdTrans.tra_clear();
		prtLine.init(' ').book(2);
		while (rec < act.key.length) {
			grp = act.grp[rec++];
			if (grp > 0 && grp != prv) {
				if ((sts = lACT.read(prv = grp, tra.comm)) <= 0)
					break;
				for (ind = major.length; ind-- > 0; major[ind] = lACT.block[ind].total)
					;
			}
			if ((sts = lACT.read(rec, tra.comm)) <= 0)
				break;
			if (grp == 0) {
				prv = rec;
				for (ind = major.length; ind-- > 0; major[ind] = lACT.block[ind].total)
					;
				prtLine.init(stl_line).book(2);
			}
			report_ACT(major);
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC25 salesperson totals
	 **/
	public int action5(int spec) {
		int grp, ind, prv = 0, rec = 0, sts = 0;
		long major[] = new long[lSLM.block.length];

		if (tra.mode != 9) {
			if (tra.subc > 3) {
				input.pb = leftFill(input.pb, 4, '*');
				if ((tra.xtra = input.scanKey(4)) == 0)
					return 8;
				if (slm.find(tra.xtra) == 0)
					return 8;
			}
			if (tra.subc == 0 && event.key < 7)
				tra.subc = event.key;
			if (spec == 0) {
				dspLine.init(Mnemo.getMenu(event.dec));
				return 0;
			}
		}
		if (sel_regnbr(0, spec) < 0)
			return GdTrans.tra_clear();
		while (rec < slm.key.length) {
			grp = slm.grp[rec];
			if (!filter_sls(rec++, slm))
				continue;
			if (grp > 0 && grp != prv) {
				if ((sts = lSLM.read(prv = grp, tra.comm)) <= 0)
					break;
				for (ind = major.length; ind-- > 0; major[ind] = lSLM.block[ind].total)
					;
			}
			if ((sts = lSLM.read(rec, tra.comm)) <= 0)
				break;
			if (grp == 0) {
				prv = rec;
				for (ind = major.length; ind-- > 0; major[ind] = lSLM.block[ind].total)
					;
			}
			report_SLM(major);
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC26 department totals
	 **/
	public int action6(int spec) {
		int grp, ind, prv = 0, rec = 0, sts = 0;
		long major[] = new long[lDPT.block.length];

		if (tra.mode != 9) {
			if (tra.subc > 3) {
				input.pb = leftFill(input.pb, 4, '*');
				if ((tra.xtra = input.scanKey(4)) == 0)
					return 8;
				if (dpt.find(tra.xtra) == 0)
					return 8;
			}
			if (tra.subc == 0 && event.key < 7)
				tra.subc = event.key;
			if (spec == 0) {
				dspLine.init(Mnemo.getMenu(event.dec));
				return 0;
			}
		}
		if (sel_regnbr(0, spec) < 0)
			return GdTrans.tra_clear();
		while (rec < dpt.key.length) {
			grp = dpt.grp[rec];
			if (!filter_sls(rec++, dpt))
				continue;
			if (grp > 0 && grp != prv) {
				if ((sts = lDPT.read(prv = grp, tra.comm)) <= 0)
					break;
				for (ind = major.length; ind-- > 0; major[ind] = lDPT.block[ind].total)
					;
			}
			if ((sts = lDPT.read(rec, tra.comm)) <= 0)
				break;
			if (grp == 0) {
				prv = rec;
				for (ind = major.length; ind-- > 0; major[ind] = lDPT.block[ind].total)
					;
			}
			report_DPT(major);
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC27 ckr/reg responsability
	 **/
	public int action7(int spec) {
		int ckr = 0, rec = 0, sts = 0;

		if (ctl.ability > 0) {
			if (ctl.ckr_nbr < 800)
				ckr = ctl.ckr_nbr;
			if (sel_regnbr(ckr, spec) < 0)
				return GdTrans.tra_clear();
			if (ckr > 0)
				report_RSP();
			else
				while ((sts = lCTL.read(++rec, tra.comm < 0 ? tra.comm : 0)) > 0) {
					if (lCTL.key == 0 || lCTL.key > 799)
						continue;
					if (lCTL.sts == 0 || lCTL.sts > 2)
						continue;
					if (tra.comm < 0) {
						if ((lPOT.blk = ckrBlock(lCTL.key)) < 0)
							continue;
					} else if (netio.regckr(tra.comm, lCTL.key, spec) > 0)
						continue;
					dspLine.init(Mnemo.getText(1)).upto(20, editNum(lCTL.key, 3)).show(1);
					prtLine.init(Mnemo.getText(1)).upto(17, editNum(lCTL.key, 3)).onto(20, lCTL.text);
					sup_print(2);
					if (lCTL.sts == 1) {
						prtLine.init(Mnemo.getText(3)).upto(17, editKey(lCTL.reg, 3));
						sup_print(1);
					}
					report_RSP();
					if (tra.mode == M_CANCEL)
						break;
				}
		} else {
			if (ctl.ckr_nbr < 800)
				tra.comm = LOCAL;
			else if (sel_regnbr(ckr, spec) < 0)
				return GdTrans.tra_clear();
			if (tra.comm != 0 && tra.comm < 0xf00)
				report_RSP();
			else
				while ((sts = lLAN.read(++rec, 0)) > 0) {
					if (lLAN.type != 'R' && lLAN.type != 'M')
						continue;
					if (lLAN.tbl[0] == 0)
						continue;
					if (tra.comm > 0)
						if ((tra.comm & 0xff) != lLAN.grp)
							continue;
					dspLine.init(Mnemo.getText(3)).upto(20, editKey(lLAN.key, 3)).show(1);
					prtLine.init(Mnemo.getText(3)).upto(17, editKey(lLAN.key, 3));
					if (lLAN.ckr < 800 && lLAN.sts != 2)
						prtLine.onto(20, Mnemo.getText(1)).upto(37, editNum(lLAN.ckr, 3));
					else
						prtLine.onto(20, Mnemo.getInfo(0));
					sup_print(2);
					report_RSP();
					if (tra.mode == M_CANCEL)
						break;
				}
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC28 LAN status
	 **/
	public int action8(int spec) {
		int rec = 0, sts;

		if (sel_regnbr(0, spec) < 0)
			return GdTrans.tra_clear();
		while ((sts = lLAN.read(++rec, tra.comm)) > 0) {
			if (lLAN.type != 'R')
				if (lLAN.type != 'S' && lLAN.type != 'B')
					continue;
			prtLine.init(lLAN.text);
			sup_print(lLAN.text.trim().length() > 0 ? 2 : 1);
			String txt = Mnemo.getText(lLAN.type == 'R' ? 3 : 71);
			dspLine.init(txt).upto(20, editKey(lLAN.key, 3)).show(1);
			prtLine.init(txt).upto(17, editKey(lLAN.key, 3));
			if (lLAN.key == ctl.srv_nbr) {
				if (netio.srv > 0)
					prtLine.onto(20, Mnemo.getInfo(54));
			} else if ((lLAN.lan & 1) > 0)
				prtLine.onto(20, Mnemo.getInfo(16));
			sup_print(1);
			if (lLAN.ckr > 0) {
				prtLine.init(Mnemo.getText(lLAN.ckr < 800 ? 1 : 2)).upto(17, editNum(lLAN.ckr, 3));
				if ((lLAN.lan & 2) > 0)
					prtLine.onto(20, Mnemo.getInfo(50));
				sup_print(1);
				prtLine.init(Mnemo.getText(14)).upto(17, editNum(lLAN.sts, 2)).onto(20, lIDC.id)
						.upto(29, Integer.toString(lLAN.idc)).onto(31, lJRN.id).upto(40, Integer.toString(lLAN.jrn));
				sup_print(1);
				prtLine.init(Mnemo.getText(75)).upto(17, editInt(lLAN.dat)).onto(20, lDTL.id)
						.upto(29, Integer.toString(lLAN.dtl)).onto(31, lGPO.id).upto(40, Integer.toString(lLAN.gpo));
				sup_print(1);
			}
			if (lLAN.mnt > 0) {
				prtLine.init(mnt_line.substring(0, 20)).onto(31, rMNT.id).upto(40, Integer.toString(lLAN.mnt));
				sup_print(1);
			}
		}
		if (sts < 0)
			prtAbort(16);
		return GdRegis.prt_trailer(2);
	}

	/**
	 * AC19/21 any checker
	 **/
	public int action9(int spec) {
		int ckr = sel_ckrnbr(event.dec);

		if (ckr < 1)
			return 8;
		if (ctl.lan > 1 && lCTL.sts == 0)
			return 7;
		tra.code = spec;
		lPOT.blk = ckrBlock(ckr);
		return action1(tra.code);
	}

}
