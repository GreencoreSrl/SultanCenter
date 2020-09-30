package com.ncr;

import java.io.*;

class GdMaint extends Action {
	static boolean hot_maint(DatIo io) {
		File f = io.pathfile, hot = localFile("hot", f.getName());
		if (!hot.exists())
			return false;
		io.close();
		logConsole(2, "use " + hot.getPath(), null);
		if (localMove(hot, f))
			io.open(f.getParent(), f.getName(), 0);
		return true;
	}

	static boolean hot_maint(String fname) {
		File hot = localFile("hot", fname);
		if (!hot.exists())
			return false;
		File f = localFile(null, hot.getName());
		logConsole(2, "use " + hot.getPath(), null);
		localMove(hot, f);
		return true;
	}

	static void hot_maint() {
		Promo.checkForNewFile();
		hot_maint(lQLU); /* sales qualifications */
		hot_maint(lREF); /* lists of short codes */
		hot_maint(lRLU); /* auto rebate / points */
		if (hot_maint(lSET))
			UpSet.init();
		if (hot_maint("P_REGTXT.DAT"))
			Param.readTXT();
		hot_maint(lDBL); /* 2nd language lines */
	}

	/**
	 * start price mnt
	 */
	int action0(int spec) {
		GdRegis.set_ac_ctl(spec);
		prtTitle(tra.head = event.dec);
		return 0;
	}

	/**
	 * change plu price
	 */
	int action1(int spec) {
		int dev = 2, ind, price = plu.price;

		if (input.num > 0) {
			if (ctl.lan < 3)
				return 7;
			plu.price = input.scanNum(input.num);
		}
		if (price != plu.price) {
			prtLine.init(' ').book(2);
			prtLine.init(editIdent(plu.number, false)).upto(40, editMoney(0, price)).book(3);
			lPLU.onto(0, editNum(plu.price, 8));
			lPLU.rewrite(lPLU.recno, 70);
			prtLine.init(plu.text).onto(20, editKey(plu.dpt_nbr, 4)).upto(40, editMoney(0, plu.price)).book(3);
		}
		if (spec > 0) {
			DevIo.tpmPrint(dev, 1, "");
			prtDwide(dev, plu.text);
			DevIo.tpmLabel(dev, itm.number);
			stsLine.init(tnd[0].symbol).upto(20, editMoney(0, plu.price));
			prtDwide(dev, stsLine.toString());
			if ((ind = tnd_tbl[K_AltCur]) > 0) {
				stsLine.init(tnd[ind].symbol).upto(20, editMoney(ind, tnd[ind].hc2fc(plu.price)));
				prtDwide(dev, stsLine.toString());
			}
		}
		dspLine.init(Mnemo.getMenu(tra.head));
		return 0;
	}

	/**
	 * ready state monitor
	 */
	int action2(int spec) {
		int ind = mon.opd_sts, pause = options[O_Pause];

		if (ctl.ckr_nbr > 0) {
			if (pause > 0)
				if (input.tic > pause * 60) {
					return ctl.ckr_nbr < 800 ? 0xCDA8 : 0x00A2;
				}
			while (++ind < 5) {
				if (ind == 1)
					if (ctl.lan == 1)
						break;
				if (ind == 2)
					if (ctl.lan == 2)
						break;
				if (ind == 3)
					if ((input.lck & 0x20) > 0)
						break;
				if (ind == 4)
					if (mon.rcv_mon != null)
						break;
			}
			if (ind < 5)
				DevIo.oplDisplay(0, Mnemo.getInfo(48 + ind));
			else if ((ind = 0) < mon.opd_sts)
				DevIo.oplDisplay(0, dspLine.toString());
			mon.opd_sts = ind;
		}
		if ((input.tic & 3) == 1)
			hot_maint();

		//ECOMMERCE-SBE#A BEG
		if (ECommerceManager.getInstance().checkForNewBasket(editKey(ctl.reg_nbr, 3))) {
			return 0xabcd;
		}
		//ECOMMERCE-SBE#A END
		return 0;
	}

	/**
	 * device services menu
	 */
	int action5(int spec) {
		String[] env = { "EFT", "BCR" };
		int ind, sts = 0, sel = 0, tbl[] = new int[env.length];

		input.prompt = Mnemo.getText(15);
		input.init(0x00, 1, 1, 0);
		panel.display(1, Mnemo.getMenu(event.dec));
		SelDlg dlg = new SelDlg(Mnemo.getText(22));
		while (sts < env.length) {
			String s = System.getProperty(env[sts++]);
			if (s == null)
				continue;
			if ((ind = s.lastIndexOf('/')) > 0) {
				tbl[sel++] = sts;
				dlg.add(9, editNum(sel, input.max), " " + s.substring(ind + 1));
			}
		}
		if (sel < 1)
			return 7;
		if (sel > 1) {
			dlg.show("LBS");
			if ((sts = dlg.code) < 1) {
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.num < 1 || input.key != input.ENTER)
					sts = 5;
				else
					sts = input.adjust(input.pnt);
			}
			if (sts > 0)
				return sts;
			ind = input.scanNum(input.num);
			if (ind < 1 || ind > sel)
				return 46;
			sel = ind;
		}
		GdRegis.set_ac_ctl(spec);
		prtTitle(event.dec);
		tra.tim = sec_time();
		for (sel = tbl[sel - 1] * 10;;) {
			input.init(0x00, 1, 1, 0);
			panel.display(1, Mnemo.getDiag(ind = sel - 10));
			dlg = new SelDlg(Mnemo.getText(22));
			while (++ind < sel) {
				if (Mnemo.getDiag(ind).trim().length() < 1)
					continue;
				dlg.add(9, editNum(ind % 10, input.max), " " + Mnemo.getDiag(ind));
			}
			dlg.show("LBS");
			if ((sts = dlg.code) == 0) {
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.key == input.CLEAR)
					break;
				if (input.num < 1 || input.key != input.ENTER)
					sts = 5;
				else if ((sts = input.adjust(input.pnt)) == 0) {
					if ((ind = input.scanNum(input.num)) < 1)
						sts = 8;
				}
			}
			if (sts == 0) {
				String txt = Mnemo.getDiag(sel - 10 + ind);
				if (txt.trim().length() < 1)
					sts = 7;
				else {
					panel.display(2, txt);
					prtLine.init(txt);
					if (sel == 10)
						sts = EftIo.service(ind);
					if (sel == 20)
						sts = BcrIo.service(ind);
					if (sts == 0)
						prtLine.book(3);
					continue;
				}
			}
			panel.clearLink(Mnemo.getInfo(sts), 0x81);
		}
		accumReg(9, 3, 0, sec_diff(tra.tim));
		return GdRegis.prt_trailer(1);
	}

	/**
	 * journal watch in supervisor ready state
	 */
	int action6(int spec) {
		int len, sts;

		if (ctl.lan > 2)
			return 7;
		dspLine.init(Mnemo.getInfo(spec)).show(1);
		for (;; panel.clearLink(Mnemo.getInfo(sts), 1)) {
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
				if (input.key != input.ENTER)
					sts = 5;
				else if (input.num < input.max)
					sts = 3;
				else
					sts = input.adjust(input.pnt);
				if (sts == 0) {
					tra.comm = input.scanKey(input.num);
					if (tra.comm == 0)
						sts = 7;
				}
			}
			if (input.key == input.CLEAR)
				return GdTrans.tra_clear();
			if (sts > 0)
				continue;
			len = lLAN.read(0, tra.comm);
			if (len < 1)
				sts = len < 0 ? 16 : 8;
			else if (lLAN.type != 'R' && lLAN.type != 'M')
				sts = 7;
			if (sts == 0)
				break;
		}
		SpyDlg spy = new SpyDlg(key_txt[1]);
		mon.watch = lLAN.jrn - spy.area.rows + 1;
		if (mon.watch < 1)
			mon.watch = 1;
		dspLine.show(1);
		input.init(0x80, 0, 0, 0);
		spy.show("SPY");
		mon.watch = 0;
		return GdTrans.tra_clear();
	}

	/**
	 * price inquiry in closed state
	 */
	int action7(int spec) {
		ModDlg dlg = new ModDlg(Mnemo.getMenu(spec));
		dlg.block = false;
		input.prompt = Mnemo.getText(9);
		input.init(0x00, 16, 0, 0);
		oplToggle(2, Mnemo.getMenu(spec));
		dlg.show("INQ");
		oplToggle(0, null);
		if (dlg.code > 0)
			return dlg.code;
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.key != input.ENTER && input.key != 0x4f4f)
			return 5;
		return group[5].action0(0);
	}

	/**
	 * start dpt key mnt
	 */
	int action8(int spec) {
		if (ctl.mode > 0)
			return 7;
		GdRegis.set_ac_ctl(spec);
		prtTitle(event.dec);
		dspLine.init(Mnemo.getText(10)).upto(20, "01 - 15");
		return 0;
	}

	/**
	 * preset dpt / end
	 */
	int action9(int spec) {
		int dpt_no;

		if (spec == 0) {
			while (spec < dir_tbl.length) {
				if ((dpt_no = dir_tbl[spec++]) <= 0)
					continue;
				prtLine.init(editNum(spec, 2) + '.' + Mnemo.getText(10)).onto(15, editKey(dpt_no, 4))
						.onto(20, dir_txt[spec - 1]).book(3);
			}
			return GdRegis.prt_trailer(2);
		}
		if (dir_tbl[spec - 1] < 0)
			return 7;
		if (input.num > 0) {
			if ((dpt_no = input.scanKey(input.num)) > 0) {
				if (GdPrice.src_dpt(dpt_no) > 0)
					return 28;
				dir_txt[spec - 1] = dlu.text.substring(0, 18);
			}
			dir_tbl[spec - 1] = dpt_no;
			if (input.dky > 0)
				input.dky = 0;
		}
		dspLine.init(editNum(spec, 2) + '.' + Mnemo.getText(10)).upto(20, editKey(dir_tbl[spec - 1], 4));
		return 0;
	}
}
