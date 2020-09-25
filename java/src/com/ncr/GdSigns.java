package com.ncr;

import com.ncr.ssco.communication.entities.pos.SscoCashier;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.io.*;

import static com.ncr.Itmdc.IDC_write;
class GdSigns extends Action {
	/**
	 * enter and validate authorization
	 **/

	static private boolean isEod = false;	// FIX-20170413-CGA#A BEG
	private static final Logger logger = Logger.getLogger(GdSigns.class);

	static int chk_autho(String text) {
		int nbr, rec, sts;

		if (tra.res > 0)
			return 0;
		if ((input.lck & 0x10) > 0)
			return 0;
		if (!input.isLockPos(0x04)) {
			if (text != null)
				stsLine.init(text).show(1);
			panel.clearLink(Mnemo.getInfo(56), mon.autho = 0x21);
			mon.autho = 0;
			if ((input.lck & 0x04) == 0)
				return 1;
			input.key = input.ENTER;
		}
		if (input.optAuth < 2)
			return 0;
		if (ctl.lan == 0)
			lCTL.update();
		input.prompt = Mnemo.getText(2);
		input.init(0x00, 3, 3, 0);
		panel.display(1, Mnemo.getInfo(38));
		SelDlg dlg = new SelDlg(Mnemo.getText(22));
		for (rec = 0; lCTL.read(++rec, lCTL.LOCAL) > 0;) {
			if (lCTL.key < 800 || lCTL.key > 999)
				continue;
			dlg.add(9, editNum(lCTL.key, 3), " " + lCTL.text);
		}
		dlg.block = false;
		dlg.show("AUT");
		if (dlg.code > 0)
			return dlg.code;
		if (input.key > 0x4000) {
			if ((sts = chk_persno(input.num)) > 0)
				return sts;
			input.key = input.ENTER;
		} else if (GdSarawat.getInstance().isEnableChkAuthoScan()){  // CHKAUTHO-CGA#A BEG
			return 83;
		}  // CHKAUTHO-CGA#A END
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.num < 1 || input.key != input.ENTER)
			return 5;
		if ((sts = input.adjust(input.pnt)) > 0)
			return sts;
		nbr = input.scanNum(input.num);
		if (nbr < 800)
			return 46;
		if ((rec = lCTL.find(nbr)) == 0)
			return 7;
		if (ctl.lan < 1) {
			if (lCTL.read(rec, 0) < 1)
				return 16;
			if (lCTL.key != nbr) {
				stsLine.init(Mnemo.getInfo(15)).onto(15, "SRV").show(2);
				return 7;
			}
		}
		if ((sts = chk_passwd(Mnemo.getInfo(38))) > 0)
			return sts;
		prtLine.init(Mnemo.getText(2)).upto(17, editNum(nbr, 3));
		prtLine.onto(20, Mnemo.getInfo(38)).book(1);
		ctl.sup = rec;
		input.lck |= 0x10;
		showAutho();
		return 0;
	}

	static int key_autho() {
		if (input.optAuth < 2 || ctl.ckr_nbr > 799)
			return 5;
		if ((input.lck & 0x10) == 0)
			return chk_autho(Mnemo.getInfo(38));
		input.lck &= ~0x10;
		showAutho();
		return ctl.sup = 0;
	}

	static int chk_pause() {
		if (ctl.ckr_nbr > 799 || ctl.ckr_sec < 1)
			return 5;
		if ((options[O_SecNo] & 8) == 0)
			return 5;
		lCTL.read(ctl.ckr, lCTL.LOCAL);
		for (int sts; (sts = chk_passwd(Mnemo.getMenu(27))) > 0;) {
			if (input.key != input.CLEAR)
				panel.clearLink(Mnemo.getInfo(sts), 1);
			else if ((input.lck & 2) > 0)
				break;
		}

		return 0;
	}

	static int chk_passwd(String msg) {
		if (SscoPosManager.getInstance().isEnabled()) {
			return 0;
		}
		if (lCTL.sec == 0)
			return 0;
		panel.display(1, msg);
		int sts = BirIo.verify("CKR_" + editNum(lCTL.key, 4));
		if (sts >= 0)
			return sts;
		if ((sts = get_passwd(null)) > 0)
			return sts;
		return lCTL.sec == input.scanNum(input.num) ? 0 : 8;
	}

	static boolean hot_passwd(int sec) {
		if (sec < 1)
			return true;
		return lPWD.find(editNum(sec, lPWD.fixSize)) > 0;
	}

	static int get_passwd(String msg) {
		int len = 4;

		if (msg != null)
			panel.display(1, msg);

		input.prompt = Mnemo.getText(73);
		input.init(0x40, len, len, 0);
		ModDlg dlg = new ModDlg(lCTL.text);
		msg = Mnemo.getText(lCTL.key < 800 ? 1 : 2);
		dlg.line[0].setText(msg + "     " + editNum(lCTL.key, 3));
		if (ctl.date % 10000 == lCTL.age % 10000)
			dlg.line[2].setPicture("BIRTHDAY");
		if (dlg.line[2].image == null)
			dlg.line[2].setText(Mnemo.getText(7) + editNum(lCTL.pers, 8));
		oplToggle(2, lCTL.text);
		dlg.show("PWD");
		oplToggle(0, null);
		if (dlg.code > 0)
			return dlg.code;
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.num < 1 || input.key != input.ENTER)
			return 5;
		if (input.num < options[O_SecNo] >> 4)
			return 3;
		return input.adjust(input.pnt);
	}

	static int new_passwd() {
		int sec, sts;

		while (true) {
			sts = get_passwd(Mnemo.getInfo(48));
			if (input.key == input.CLEAR)
				return ERROR;
			if (sts == 0) {
				sec = input.scanNum(input.num);
				if (sec == lCTL.sec)
					sts = 7;
				else if (hot_passwd(sec))
					sts = 8;
				if (sts == 0)
					for (;; panel.clearLink(Mnemo.getInfo(sts), 1)) {
						sts = get_passwd(Mnemo.getInfo(40));
						if (input.key == input.CLEAR)
							break;
						if (sts == 0) {
							if (sec == input.scanNum(input.num))
								return sec;
							sts = 8;
						}
					}
			}
			if (input.key != input.CLEAR) {
				panel.clearLink(Mnemo.getInfo(sts), 1);
			}
		}
	}

	/**
	 * derive checker/secret nbr from pers. nbr
	 **/
	static int chk_persno(int ind) {
		if (ind < 4) {
			if ((options[O_xCaRd] & 8) == 0)
				return ERROR;
			if ((input.lck & 0x04) > 0)
				return ERROR;
			return (input.optAuth & 2) > 0 ? 9 : 1;
		}
		if ((ind = GdCusto.chk_cusspc(8)) > 0)
			return ind;

		int nbr = input.scanNum(input.num);
		while (lCTL.read(++ind, lCTL.LOCAL) > 0) {
			if (lCTL.key < 1 || lCTL.key > 999)
				continue;
			if (lCTL.pers == nbr) {
				input.reset(editNum(lCTL.key, 3));
				return 0;
			}
		}
		return 8;
	}

	static void reg_eoday() {
		lPOS.read(1);
		long total = lPOS.total;
		lPOS.read(2);
		lPOS.trans = ++lPOS.trans % 10000;
		lPOS.total = total;
		lPOS.rewrite(2);
	}

	static void eod_print(int spec, int sel, int type) {
		tra.comm = sel;
		tra.tnd = type;
		if (spec == 19)
			group[9].action1(spec);
		if (spec < 20 || spec > 28)
			return;
		tra.mode = 9;
		prtTitle(sel == 0 ? 0 : 18);
		event.act = 90 + (event.spc = spec) % 10;
		group[9].exec();
	}

	int action0(int spec) {
		return 0;
	}

	/**
	 * menu selection
	 **/
	int action1(int spec) {
		int code, line = event.read(event.nxt);

		if (input.num == 0) {
			if (input.dky >= 0)
				if (event.lck < 0xFF) {
					input.dky = event.lck & 15;
					for (code = event.next(line); event.key > 0; code = event.next(code)) {
						if (event.min < 1)
							continue;
						input.rules[input.dky][event.min - 1] = event.act == 75 ? "T" + editNum(event.spc, 2)
								: "M" + editNum(event.dec, 2);
					}
					event.nxt = line;
					return input.dky = 0;
				}
			input.prompt = Mnemo.getText(event.alt);
			input.init(0x00, event.max, event.min, event.dec);
			panel.display(1, Mnemo.getMenu(event.act));
			SelDlg dlg = new SelDlg(Mnemo.getText(22));
			for (code = event.next(line); event.key > 0; code = event.next(code)) {
				dlg.add(8, editNum(event.key, input.max),
						" " + (event.act == 75 ? tnd[event.spc].text : Mnemo.getMenu(event.dec)));
			}
			dlg.show("MNU");
			if (dlg.code > 0)
				return dlg.code;
			if (input.key == 0)
				input.key = input.CLEAR;
			if (input.num < 1 || input.key != input.ENTER)
				return 5;
			code = input.adjust(input.pnt);
			if (code > 0)
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

	/**
	 * exit from PoS
	 **/
	int action2(int spec) {
		if (input.pb.length() > 0) {
			spec = Integer.parseInt(input.pb);
			logger.debug("Setting spec to: " + spec);
		}
		while (ctl.lan < 2) {
			if (netio.isCopied(mon.image = lIDC.getSize()))
				break;
			if (!SscoPosManager.getInstance().isEnabled())
				panel.clearLink(Mnemo.getInfo(26), 0x14);
			if (ctl.lan > 0)
				break;
		}
		mon.image = 0;
		event.nxt = -1;
		if (SscoPosManager.getInstance().isEnabled()) {
			SscoPosManager.getInstance().shuttingDownResponse();
		}

		logger.debug("Exit - spec: " + spec);
		return spec;
	}

	/**
	 * operator open
	 **/
	int action3(int spec) {
		// AMZ-2017#BEG
		int chk_sts = chk_broken();
		if (chk_sts > 0) return chk_sts;
		// AMZ-2017#END

		lLAN.read(1, lLAN.LOCAL);
		if (lLAN.sts >= 90)
			return 26;
		if (ctl.lan == 0)
			lCTL.update();

		int ind = chk_persno(input.num), nbr, rec, sec = 0;

		if (ind > 0)
			return ind;
		if ((nbr = input.scanNum(input.num)) == 0)
			return 8;
		if (nbr >= 800) {
			if (!input.isLockPos(0x04))
				return 1;
		}
		if (lLAN.ckr != nbr)
			if (chk_reboot(lLAN.ckr)) {
				if (nbr < 800)
					return 55;
				lPOT.blk = ckrBlock(lLAN.ckr);
				ctl.work = tra.tim = sec_time();
				event.alt = event.base;
				return action5(input.index = 0);
			}
		if ((ctl.ckr = lCTL.find(nbr)) == 0)
			return 8;
		if (ctl.lan < 2) {
			if (lCTL.read(ctl.ckr, tra.mode = 0) < 0)
				tra.mode = M_CANCEL;
			if (lCTL.key != nbr) {
				stsLine.init(Mnemo.getInfo(15)).onto(15, "SRV").show(2);
				return 7;
			}
		}
		if (ctl.lan == 2) {
			tra.mode = M_CANCEL;
			if (lLAN.read(0, ctl.reg_nbr) > 0)
				if (lLAN.sts == 98) {
					stsLine.init(Mnemo.getMenu(2)).show(2);
					return 7;
				}
		}
		if (lCTL.lck > 0) {
			if (lCTL.lck != 2)
				return 7;
			panel.display(1, Mnemo.getInfo(55));
			if (panel.clearLink(Mnemo.getInfo(40), 0x23) < 2)
				return 7;
		}
		if (lCTL.sts == 1)
			if (lCTL.reg != ctl.reg_nbr) {
				stsLine.init(Mnemo.getInfo(37)).upto(20, editKey(lCTL.reg, 3)).show(1);
				panel.clearLink(Mnemo.getInfo(7), 0x21);
				return 7;
			}
		if (lCTL.pin > 0) {
			if (lCTL.pin != ctl.grp_nbr && lCTL.pin != ctl.reg_nbr)
				return 7;
		}
		if ((ind = chk_passwd(Mnemo.getMenu(20))) > 0)
			return ind;
		if (nbr < 800) {

			if (lCTL.sts == 19)
				return 24;
			if (lCTL.sec > 0) {
				if (!lCTL.pwdOlder(options[O_SecEx])) {
					sec = lCTL.sec;
					if ((options[O_SecNo] & 0x01) < 1)
						if (hot_passwd(sec))
							sec = 0;
				}
			}
			if (sec == 0) {
				if ((options[O_SecNo] & 0x01) > 0)
					sec = (ctl.date + ctl.time) % 10000;
				else if ((sec = new_passwd()) < 0)
					return 7;
			}
			if ((lPOT.blk = ckrBlock(nbr)) < 0)
				return 7;
			if (ctl.lan == 0) {
				if (netio.regckr(0, nbr, spec) == 7)
					return 7;
			}
			tra.amt = lPOT.blk;
			DevIo.drwPulse(0);
			for (ind = 0; ind < tnd.length; ind++) {
				tnd[ind].alert = 0;
				for (int sc = 0; ++sc < 8;) {
					if (sc > 3 && sc < 7)
						continue;
					if ((rec = reg.findTnd(ind, sc)) < 1)
						continue;
					if (ctl.ability > 0)
						ckrRead(rec, lREG.LOCAL);
					else
						lREG.read(rec, lREG.LOCAL);
					Total tb = lREG.block[0];
					tnd[ind].alert += sc == 3 ? -tb.total : tb.total;
				}
			}
		} else
			sec = lCTL.sec;
		ctl.ckr_nbr = nbr;
		ctl.ckr_sec = sec;
		ctl.ckr_age = lCTL.age;
		ctl.block = false;
		ctl.work = sec_time();
		lJRN.recno = nbr < 800 ? lJRN.getSize() : 0;
		GdRegis.set_ac_ctl(spec);
		tra.number = editNum(lCTL.pers, 8);
		prtTitle(20);
		prtLine.init(' ');
		for (ind = 0; ind < 2; ind++) {
			prtLine.onto(ind * 20, vrs_tbl[ind]).push(editVersion(version[ind], true));
		}
		prtLine.book(3);
		if (nbr < 800) {
			EftIo.eftOpen();
			BcrIo.bcrOpen();
			GdScale.version();
			// EMEA-UPB-DMA BEG
			WinUpb.getInstance().loadParam();
			WinUpb.getInstance().start();
			// EMEA-UPB-DMA END
			// PSH-ENH-CHECKER-SBE#BEG
			GdPsh.setCashierId(ctl.reg_nbr, ctl.ckr_nbr);
			// PSH-ENH-CHECKER-SBE#END
		}
		prtLine.init(' ').book(2);
		prtLine.init(Mnemo.getText(nbr < 800 ? 1 : 2)).upto(17, editNum(nbr, 3)).onto(20, lCTL.text).book(3);
		if (nbr > 799 && sec > 0) {
			String name = "CKR_" + editNum(nbr, 4);
			int sts = BirIo.enroll(name);
			if (sts > 0)
				panel.clearLink(Mnemo.getInfo(sts), 0x91);
			if (sts == 0)
				BirIo.collect(name);
		} else
			ElJrn.toggle();
		lCTL.dateOpn = ctl.date;
		lCTL.timeOpn = ctl.time / 100;
		if (sec != lCTL.sec) {
			if ((options[O_SecNo] & 0x01) > 0)
				prtLine.init(Mnemo.getText(73)).upto(17, editNum(sec, 4)).book(2);
			lCTL.datePwd = lCTL.dateOpn;
			lCTL.timePwd = lCTL.timeOpn;
		}
		lCTL.lck = 0;
		lCTL.reg = ctl.reg_nbr;
		lCTL.sec = tra.cnt = ctl.ckr_sec;
		lCTL.sts = tra.code;
		lCTL.rewrite(ctl.ckr);
		if (tra.mode == M_CANCEL)
			prtDwide(ELJRN + 3, Mnemo.getInfo(16));

		GdSarawat.getInstance().setCkrClose(false); // SARAWAT-ENH-20150507-CGA#A
		GdRegis.prt_trailer(2);

		//WINEPTS-CGA#A BEG
		PosGPE.checkPinPad();
		PosGPE.checkEptsUPB(true);
		logger.info("check isEptsVoidFlagPresent()");
		if (PosGPE.isEptsVoidFlagPresent()) {
			logger.info("isEptsVoidFlagPresent true");
			panel.clearLink(Mnemo.getInfo(132), 1);
			PosGPE.deleteEptsVoidFlag();
		}
		//WINEPTS-CGA#A END
		// AMZ-2017#BEG
        String resume = ExtResume.readFlagFile();
        if (resume != null) {
            /* trova l'event.nxt della resume */
            int a = event.find(0x1d, event.nxt);
            event.read(a);
            event.find(0xd, event.nxt);

            /* fa la resume */
            group[3].action1(4);  //GdTrans.trans preselect
            input.reset(resume);
			tra.stat = 1; //setto come anonymous sales
            group[5].action9(1); // GdPrice.resume trans no
            ExtResume.writeFlagFile("");
        }
        // AMZ-2017#END
		if( SscoPosManager.getInstance().isUsed()) {
			SscoCashier cashier = SscoPosManager.getInstance().getCashier();
			if (Integer.parseInt(cashier.getUserId()) > 799) {
				SscoPosManager.getInstance().setCashierSupervisor(true);
			}
			SscoPosManager.getInstance().signOnResponse();
		}
		return 0;
	}

	/**
	 * operator close
	 **/
	int action4(int spec) {
		if (ctl.mode > 0)
			return 7;
		if ((ctl.ckr = lCTL.find(ctl.ckr_nbr)) == 0)
			return 7;

		if (spec == 2)
			if (ctl.ckr_nbr < 800) {
				if ((options[O_SecNo] & 0x02) > 0) {
					int sts = chk_passwd(Mnemo.getMenu(event.dec));
					if (sts > 0)
						return sts;
				}
			}
		lJRN.recno = 0;
		if (spec == 0)
			spec = 2;
		GdRegis.set_ac_ctl(lCTL.sts = spec);
		prtTitle(21);
		if (tra.who > 0) {
			prtLine.init(Mnemo.getText(2)).upto(17, editNum(tra.who, 3)).onto(20, Mnemo.getInfo(55)).book(3);
			lCTL.lck = tra.mode = M_CANCEL;
		}
		lCTL.dateCls = ctl.date;
		lCTL.timeCls = ctl.time / 100;
		lCTL.rewrite(ctl.ckr);
		if (spec != 19)
			tra.number = editNum(lCTL.pers, 8);
		ctl.alert = false;

		GdSarawat.getInstance().setCkrClose(true); // SARAWAT-ENH-20150507-CGA#A

		GdRegis.prt_trailer(2);
		if (spec == 99) {
			reg_eoday();
			return action2(4);
		}
		if (ctl.ckr_nbr < 800) {
			EftIo.eftClose();
			BcrIo.bcrClose();
			DevIo.drwPulse(0);
			if (ctl.view > 0)
				ElJrn.toggle();
			// EMEA-UPB-DMA BEG
			WinUpb.getInstance().stop();
			// EMEA-UPB-DMA END
		}
		ctl.ckr_nbr = 0;
		panel.jrnPicture("CLOSE");
		if (SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().signOffResponse();
		}
		return GdTrans.tra_clear();
	}

	/**
	 * pause / lock
	 **/
	int action5(int spec) {
		lCTL.read(ctl.ckr, lCTL.LOCAL);
		if (spec > 0) {
			GdRegis.set_ac_ctl(spec);
			prtTitle(event.dec);
			tra.tim = sec_time();
			prtLine.init(' ').book(2);
			GdRegis.set_trailer();
			prtLine.book(3);
			tra.number = editNum(lCTL.pers, 8);
			Itmdc.IDC_write('F', input.key > 0xFF ? 1 : 0, 0, tra.number, tra.cnt, tra.amt);
			tblWrite();
			panel.jrnPicture("PAUSE");
			showHeader(false);
			if (ctl.view > 0)
				ElJrn.toggle();
			return ctl.ckr_nbr = 0;
		}
		int ind, nbr = lCTL.key, rec, who;
		if ((ind = chk_persno(input.num)) > 0)
			return ind;
		who = input.scanNum(input.num);
		if (who >= 800) {
			if (!input.isLockPos(0x04))
				return 1;
			if ((rec = lCTL.find(who)) < 1)
				return 8;
			if ((ind = chk_passwd(Mnemo.getInfo(55))) > 0)
				return ind;
			tra.who = who;
			ctl.sup = rec;
			event.nxt = event.alt;
		} else {
			if (who != nbr)
				return 8;
			if ((ind = chk_passwd(Mnemo.getMenu(20))) > 0)
				return ind;
		}
		ctl.ckr_nbr = nbr;
		accumReg(9, 2, 1, sec_diff(tra.tim));
		if (who >= 800)
			return action4(ctl.mode = 0);
		ElJrn.toggle();
		tra.code = 1;
		tra.amt = lPOT.blk;
		GdRegis.prt_trailer(1);
		return 0;
	}

	/**
	 * change password
	 **/
	int action6(int spec) {
		int ckr = ctl.ckr_nbr, sec, sts;

		if (ctl.mode > 0)
			return 7;
		if (ckr > 799) {
			ckr = GdSuper.sel_ckrnbr(event.dec);
			if (ckr < 1)
				return 8;
			if (ctl.lan == 0)
				if (lCTL.read(0, ckr) > 0) {
					if (lCTL.sts == 1) {
						stsLine.init(Mnemo.getInfo(37)).upto(20, editKey(lCTL.reg, 3)).show(1);
						if (panel.clearLink(Mnemo.getInfo(40), 0x23) < 2)
							return 7;
					}
				}
		} else
			lCTL.read(ctl.ckr, lCTL.LOCAL);
		if ((sts = chk_passwd(Mnemo.getMenu(event.dec))) > 0)
			return sts;
		String birName = "CKR_" + editNum(ckr, 4);
		sts = BirIo.enroll(birName);
		if (sts > 0)
			return sts;
		if (sts == 0)
			sec = lCTL.sec;
		else if ((sec = new_passwd()) < 0)
			return 7;
		GdRegis.set_ac_ctl(spec);
		prtTitle(event.dec);
		tra.number = editNum(ckr, 3);
		if (ctl.ckr_nbr < 800)
			ctl.ckr_sec = sec;
		else
			prtLine.init(Mnemo.getText(1)).upto(17, tra.number).onto(20, lCTL.text).book(3);
		if (sec != lCTL.sec) {
			lCTL.read(ctl.ckr, lCTL.LOCAL);
			lCTL.datePwd = ctl.date;
			lCTL.timePwd = ctl.time / 100;
			lCTL.sec = tra.cnt = sec;
			lCTL.rewrite(ctl.ckr);
		} else {
			tra.mode = 2;
			BirIo.collect(birName);
		}
		return GdRegis.prt_trailer(2);
	}

	/**
	 * online EoD
	 **/
	int action7(int spec) {
		int nbr = 0, sec = 0, sts;

		if (spec == 99) {
			setEod(true);
		}

		if (input.key == 0) {
			if ((input.tic &= 3) != 1)
				return 0;
			if (ctl.block) {
				logger.info("EOD blocked by instashop file: " + ctl.block);
				panel.display(1, Mnemo.getInfo(122));
				return 0;
			}
			GdMaint.hot_maint();
			lLAN.read(1, lLAN.LOCAL);
			if (lLAN.sts >= 90) {
				if (ctl.lan == 0)
					nbr = lLAN.ckr;

				panel.display(1, Mnemo.getMenu(18));
			} else if (ctl.lan == 0)
				nbr = netio.eodPoll(ctl.reg_nbr);
			if (nbr == 0)
				return 0;

			ctl.ckr_nbr = nbr;

			logger.debug("returning 0xCDA0");
			return 0xCDA0; /* auto cluster EOD */
		}

		if (input.key != 0xCDA0) {
			if (ctl.lan > 0)
				return 16;
			if (spec > 0) {
				lLAN.read(1, lLAN.LOCAL);
				if (lLAN.sts >= 90)
					return 7;

				dspLine.init(Mnemo.getMenu(18));
				return 0;
			}
			if ((sts = chk_persno(input.num)) > 0) {
				logger.debug("Sts: " + sts);
				return sts;
			}
			if ((nbr = input.scanNum(input.num)) == 0)
				return 8;
			if ((ctl.ckr = lCTL.find(nbr)) == 0)
				return 8;
			if (nbr >= 800) {
				if (!input.isLockPos(0x04))
					return 1;
				if (lCTL.read(ctl.ckr, 0) < 1)
					return 16;
			} else if (lCTL.sts == 0)
				return 7;

			if ((sts = chk_passwd(Mnemo.getMenu(18))) > 0) {
				return sts;
			}
			ctl.ckr_nbr = nbr;
			sec = 9999;
		} else
			tra.slip = 1;
		lPOT.blk = ckrBlock(nbr);

		if (lLAN.sts < 90) {
			//INSTASHOP-EOD-CGA#A BEG
			logger.info("eod, check instashop file");
			if (ECommerce.checkFileInstashop()) {
				return 122;
			}
			//INSTASHOP-EOD-CGA#A END
			tra.mode = 9;
			tra.code = 99;
			prtTitle(18);
			EftIo.eftOrder(0);
			//ECR-CGA#A BEG
			if (PluginHandler.getInstance().isVerifonePluginEnabled() && verifone.isEnableSettle()) {
				logger.info("call settle");
				verifone.settle();
			}
			//ECR-CGA#A END
			GdRegis.prt_trailer(2);
			reg_eoday();
		}
		ctl.ckr_nbr = 0;
		// SBE-BEG TEMPORARY PASSTHROUGH
		try {
			File f = new File("eodpass.tmp");
			f.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// SBE-END TEMPORARY PASSTHROUGH
		logger.debug("Enabled: " + SscoPosManager.getInstance().isEnabled());
		if (SscoPosManager.getInstance().isEnabled()) {
			SscoPosManager.getInstance().sendShutdownRequested();
			return 0;
		} else {
			return action2(sec > 0 ? 2 : 3);
		}
	}

	/**
	 * declare defective
	 **/
	int action8(int spec) {
		int ckr = 0, sel, sts;

		if (spec > 0) {
			tra.code = spec;
			dspLine.init(Mnemo.getMenu(tra.head = event.dec));
			return 0;
		}
		if ((sel = input.scanKey(input.num)) == 0)
			return 8;
		if ((sts = lLAN.read(0, sel)) < 1)
			return sts < 0 ? 16 : 8;
		if (lLAN.sts != 2 && (lLAN.sts != 19 || lLAN.ckr >= 800))
			ckr = lLAN.ckr;
		sts = netio.declare(sel, ctl.ckr_nbr, tra.code);
		if (sts != 0)
			return sts;
		prtTitle(tra.head);
		prtLine.init(Mnemo.getText(3)).upto(17, editKey(sel, 3)).book(3);
		if (ckr > 0) {
			prtDwide(ELJRN + 3, Mnemo.getMenu(21));
			prtLine.init(Mnemo.getText(ckr < 800 ? 1 : 2)).upto(17, editNum(ckr, 3)).book(3);
		}
		tra.number = editKey(sel, 3) + "/" + editNum(ckr, 3) + "=" + editNum(tra.code, 2);
		tra.code = 28;
		return GdRegis.prt_trailer(2);
	}

	/**
	 * standalone EoD
	 **/
	int action9(int spec) {
		int ind = -1, sel = ctl.lan < 3 ? 0 : LOCAL, sts;
		int sc05 = reg.findTnd(0, 5), rec = 0;

		if (ctl.lan == 2)
			return 7;

		// FIX-20170413-CGA#A BEG
		if (spec == 99) {
			setEod(true);
		}
		// FIX-20170413-CGA#A END

		if (sc05 > 0) {
			if (ctl.ability > 0)
				while ((sts = lCTL.read(++rec, sel)) > 0) {
					if (lCTL.key < 1 || lCTL.key > 799)
						continue;
					if (lCTL.sts < 1 || lCTL.sts > 2)
						continue;
					if (sel < 0) {
						if ((lPOT.blk = ckrBlock(lCTL.key)) < 0)
							continue;
					} else if (netio.regckr(sel, lCTL.key, 28) > 0)
						continue;
					if ((sts = ckrRead(sc05, sel)) < 1)
						break;
					if (lREG.block[0].trans > 0)
						continue;
					stsLine.init(Mnemo.getText(1)).upto(20, editNum(lCTL.key, 3)).show(2);
					panel.clearLink(Mnemo.getMenu(28), 0x01);
					return 7;
				}
			else
				while ((sts = lLAN.read(++rec, sel)) > 0) {
					if (lLAN.type != 'R' && lLAN.type != 'M')
						continue;
					if (lLAN.sts >= 90 || lLAN.tbl[0] == 0)
						continue;
					if ((sts = lREG.read(sc05, sel)) < 1)
						break;
					if (lREG.block[0].trans > 0)
						continue;
					stsLine.init(Mnemo.getText(3)).upto(20, editKey(lLAN.key, 3)).show(2);
					panel.clearLink(Mnemo.getMenu(28), 0x01);
					return 7;
				}
			if (sts < 0)
				return 16;
		}
		if (ctl.lan < 3) {
			panel.display(1, Mnemo.getMenu(0));
			for (rec = 0; (sts = lLAN.read(++rec, sel)) > 0;) {
				if (lLAN.type != 'R' && lLAN.type != 'C')
					continue;
				if (lLAN.key == ctl.reg_nbr || lLAN.sts >= 90)
					continue;
				boolean inactive = lLAN.ckr == 0 && lLAN.date == 0;
				sts = netio.declare(lLAN.key, ctl.ckr_nbr, inactive ? spec : 28);
				if (sts > 0)
					return sts;
			}
			if (sts < 0)
				return 16;
			for (rec = 0; (sts = lLAN.read(++rec, sel)) > 0;) {
				if (lLAN.type != 'R' && lLAN.type != 'C')
					continue;
				if (lLAN.key == ctl.reg_nbr || lLAN.sts >= 90)
					continue;
				mon.lan99 = rec;
				stsLine.init(Mnemo.getInfo(37)).upto(20, editKey(lLAN.key, 3));
				panel.clearLink(stsLine.toString(), 0x84);
				mon.lan99 = 0;
				if (lLAN.sts < 90)
					return 23;
			}
			if (sts < 0)
				return 16;
		}
		while (++ind < eod.length) {
			if ((spec = eod[ind].ac) == 0)
				continue;
			int type = eod[ind].type;
			if (type > 3)
				type = 0;
			if (spec == 19 || spec == 21) {
				for (ctl.ckr = 0; lCTL.read(++ctl.ckr, sel) > 0;) {
					if (lCTL.key == 0 || lCTL.key > 799)
						continue;
					if (lCTL.sts == 0 || lCTL.sts > 2)
						continue;
					lPOT.blk = ckrBlock(lCTL.key);
					eod_print(spec, sel, type);
				}
			} else if (eod[ind].sel == 'S') {
				for (rec = 0; lLAN.read(++rec, sel) > 0;) {
					if (lLAN.type != 'R' && lLAN.type != 'M')
						continue;
					if (lLAN.ckr == 0 || lLAN.sts <= 90)
						continue;
					eod_print(spec, lLAN.key, type);
				}
			} else
				eod_print(spec, sel, type);
			if (sel == 0)
				if (ctl.lan > 0)
					return 16;
		}
		GdRegis.set_ac_ctl(spec = 99);
		tra.mode = 9;
		prtTitle(18);
		EftIo.eftOrder(0);
		return action4(spec);
	}

	// FIX-20170413-CGA#A BEG
	static void setEod(boolean eod) {
		isEod = eod;
	}

	static boolean isEod() {
		return isEod;
	}
	// FIX-20170413-CGA#A END
}
