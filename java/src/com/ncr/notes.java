package com.ncr;

import java.io.*;

class HotProc {
	static class Redirect extends Thread {
		PrintStream out; /* Java console */
		BufferedInputStream bin;

		Redirect(PrintStream out, InputStream in) {
			this.out = out;
			bin = new BufferedInputStream(in);
			start();
		}

		public void run() {
			try {
				for (int b; (b = bin.read()) > 0; out.write(b))
					;
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}

	static int exec(String cmd) {
		int sts = -1;
		try {
			Process pro = Runtime.getRuntime().exec(cmd);
			new Redirect(System.out, pro.getInputStream());
			new Redirect(System.err, pro.getErrorStream());
			sts = pro.waitFor();
		} catch (Exception e) {
		}
		return sts;
	}
}

class HotCare extends Basis implements Runnable {
	int mnt_add = 0, mnt_chg = 0, mnt_del = 0;
	int mnt_tic = -1;

	HshIo sPLU = new HshIo(lPLU.id, lPLU.fixSize, lPLU.record.length);
	HshIo sCLU = new HshIo(lCLU.id, lCLU.fixSize, lCLU.record.length);
	HshIo sGLU = new HshIo(lGLU.id, lGLU.fixSize, lGLU.record.length);
	LocalREG sREG = new LocalREG("REG", 1);

	void rfc_apply() {
		int rec, ic = 0, sc = 0;

		try {
			ic = rMNT.skip(20).scanNum(2);
			sc = rMNT.scanNum(2);
		} catch (NumberFormatException e) {
			rMNT.error(e, false);
		}
		if (ic < 11 || sc != 8)
			return;
		if ((rec = reg.find(ic, sc)) == 0)
			return;
		sREG.onto(0, rMNT.pb.substring(rMNT.index));
		sREG.rewrite(rec, 4);
		sREG.read(rec, LOCAL);
		TndMedia ptr = tnd[ic -= 10];
		ptr.init(sc, sREG);
		String s = ptr.editXrate(true);
		logConsole(2, "rfc " + editNum(ic, 2) + ":" + s, null);
		panel.dspStatus(0, s, true, false);
	}

	void fxf_apply() {
		int ind, sts = -1;
		String type = rMNT.skip().scan(3);

		if (type.equals("RFC")) {
			rfc_apply();
			return;
		}
		String src = localPath(rMNT.pb.substring(++rMNT.index).trim());
		String tar = null, tmp = "LAST_F2F.TMP";
		if (src.length() < 1)
			return;
		if ((ind = src.indexOf(' ')) > 0) {
			tar = src.substring(ind).trim();
			src = src.substring(0, ind);
		}
		if (type.equals("F2D")) {
			File f = localFile(null, src);
			if (!f.exists())
				return;
			panel.dspStatus(0, src + "--->nul", true, false);
			logConsole(2, "del " + src, null);
			localMove(null, f);
		}
		if (type.equals("F2X")) {
			panel.dspStatus(0, "run " + src, true, false);
			if (tar != null)
				src += " " + tar;
			sts = HotProc.exec("7052_F2X.BAT " + src);
			logConsole(2, "run " + src + " rc=" + sts, null);
		}
		if (type.equals("T2F")) {
			if ((ind = src.indexOf("REG")) >= 0) {
				String s = src.substring(0, ind) + REG + src.substring(ind + 3);
				if ((sts = netio.copyF2f(s, tmp, false)) > 0)
					return;
			}
		} else if (!type.equals("F2F"))
			return;
		if (sts < 0)
			if (netio.copyF2f(src, tmp, false) != 0)
				return;
		src = src.substring(src.lastIndexOf(File.separatorChar) + 1);
		File f = localFile(tar, src);
		logConsole(2, "add " + f.getPath(), null);
		localMove(new File(tmp), f);
		if (tar == null)
			tar = ".";
		panel.dspStatus(0, src + "--->" + tar, true, false);
	}

	void cnt_show(int tic) {
		if (mnt_tic != tic) {
			mnt_tic = tic;
			cntLine.init(mnt_line.substring(20)).onto(1, editNum(mnt_add, 5)).onto(8, editNum(mnt_chg, 5)).onto(15,
					editNum(mnt_del, 5));
			panel.dspStatus(0, cntLine.toString(), true, false);
		}
	}

	public void run() {
		for (;; Thread.yield()) {
			if (netio.readMnt('A', rMNT.recno, rMNT) > 0) {
				rMNT.recno++;
				try /* validate 10 bytes header */
				{
					int nbr = rMNT.skip().scanNum(4);
					if (nbr > 0 && nbr != ctl.sto_nbr)
						continue;
					nbr = rMNT.scan(':').scanKey(3);
					if (nbr != ctl.reg_nbr && nbr != ctl.grp_nbr)
						if (nbr > 0)
							continue;
					rMNT.scan(':');
				} catch (NumberFormatException e) {
					rMNT.error(e, false);
					continue;
				}
				char id = rMNT.pb.charAt(rMNT.index);
				if (id == '*') {
					fxf_apply();
					continue;
				}
				HshIo io = id == 'C' ? sCLU : sPLU;
				if (id == 'G')
					io = sGLU;
				String data = rMNT.pb.substring(rMNT.index);
				if (data.charAt(io.fixSize - 1) < '0')
					continue;
				int sts = io.find(rMNT.scan(io.fixSize));
				if (sts < 0)
					continue;
				if (rMNT.scan() != '-') {
					io.push(data);
					io.rewrite(io.recno, 0);
					if (sts == 0)
						mnt_add++;
					else
						mnt_chg++;
				} else if (sts > 0) {
					io.delete(io.recno);
					mnt_del++;
				}
				cnt_show(ctl.time % 100);
				continue;
			}
			if (rMNT.recno > 0) {
				sPLU.sync();
				sCLU.sync();
				sGLU.sync();
				cnt_show(-1);
				cntLine.recno = rMNT.recno - 1;
				rMNT.recno = 0;
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}

class HotNote extends Basis implements Runnable {
	int recRcv = 0, recSnd = 0;
	RmoteNEW sNEW = new RmoteNEW(rNEW.id);

	public void run() {
		while (true) {
			if (netio.newsReset) {
				netio.newsReset = false;
				recRcv = recSnd = 0;
				mon.alert = -1;
				mon.snd_mon = null;
			}
			if (mon.alert < 0) {
				mon.alert = 0;
				mon.rcv_mon = null;
			}
			if (mon.snd_mon != null) {
				if (sNEW.read(recSnd) > 0) {
					if (sNEW.reg2 == 0 || sNEW.reg2 >= 0xf00 || sNEW.sts == 0) {
						mon.snd_dsp = 0;
						mon.snd_mon = null;
					}
				}
			}
			for (; mon.rcv_mon == null; recRcv++) {
				if (sNEW.read(recRcv + 1) < 1)
					break;
				if (mon.snd_mon == null)
					if (recRcv == recSnd)
						if (sNEW.reg1 != ctl.reg_nbr || sNEW.sts == 0)
							recSnd++;
				if (sNEW.sts == 0)
					continue;
				if (sNEW.reg2 != ctl.reg_nbr) {
					if (sNEW.reg1 == ctl.reg_nbr)
						continue;
					if (sNEW.reg2 > 0)
						if (sNEW.reg2 != ctl.grp_nbr)
							continue;
				}
				if (sNEW.text.endsWith("!"))
					mon.alert++;
				mon.rcv_ckr = sNEW.ckr;
				mon.rcv_msg = sNEW.text;
				mon.rcv_mon = editKey(sNEW.reg1, 3) + '/' + editNum(sNEW.ckr, 3) + "  " + editTime(sNEW.tim1);
			}
			for (; mon.snd_mon == null; recSnd++) {
				if (mon.rcv_mon == null)
					if (recSnd == recRcv)
						break;
				if (sNEW.read(recSnd + 1) < 1)
					break;
				if (sNEW.reg1 != ctl.reg_nbr || sNEW.sts == 0)
					continue;
				mon.snd_mon = "#" + sNEW.sts + "->" + editKey(sNEW.reg2, 3) + "  " + editTime(sNEW.tim1);
			}
			if (mon.rcv_dsp != recRcv) {
				mon.rcv_dsp = recRcv;
				panel.dspNotes(0, mon.rcv_mon);
			}
			if (mon.snd_dsp != recSnd) {
				mon.snd_dsp = recSnd;
				panel.dspNotes(1, mon.snd_mon);
			}
			int lamp = mon.snd_mon == null ? 0 : 1;
			DevIo.oplSignal(3, mon.rcv_mon == null ? lamp : 2);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}

abstract class Notes extends Action {
	static int ack_note(int rec) {
		if (rNEW.read(rec) < 1)
			return 16;
		if (rNEW.reg2 == ctl.reg_nbr && rNEW.sts > 0) {
			rNEW.dat2 = ctl.date;
			rNEW.tim2 = ctl.time / 100;
			rNEW.sts = 0;
			if (rNEW.write(rec) < 1)
				return 16;
		}
		panel.dspNotes(0, null);
		DevIo.oplSignal(3, mon.snd_mon == null ? 0 : 1);
		mon.alert = ERROR;
		return 0;
	}

	static int snd_note(int nbr) {
		rNEW.reg1 = ctl.reg_nbr;
		rNEW.ckr = ctl.ckr_nbr;
		rNEW.dat1 = ctl.date;
		rNEW.tim1 = ctl.time / 100;
		rNEW.reg2 = note_tbl[nbr];
		rNEW.dat2 = rNEW.tim2 = 0;
		rNEW.text = note_txt[rNEW.sts = nbr];
		return rNEW.write(0) > 0 ? 0 : 16;
	}

	static int show_me() {
		int code = 0;

		if (ctl.lan > 2)
			return showHelp();
		panel.display(1, Mnemo.getMenu(63));
		if (mon.rcv_mon != null) {
			if (lCTL.find(mon.rcv_ckr) > 0)
				panel.display(2, lCTL.text);
			panel.clearLink(mon.rcv_msg, 2);
			return ack_note(mon.rcv_dsp);
		}
		input.prompt = Mnemo.getText(15);
		input.init(0x00, 1, 1, 0);
		SelDlg dlg = new SelDlg(Mnemo.getText(22));
		while (++code < note_txt.length)
			dlg.add(1, Integer.toString(code), note_txt[code]);
		dlg.show("MSG");
		if (dlg.code > 0)
			return dlg.code;
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.num < 1 || input.key != input.ENTER)
			return 5;
		if ((code = input.adjust(input.pnt)) > 0)
			return code;

		code = input.scanNum(input.num);
		if (code < 1 || note_txt[code] == null)
			return 8;
		if (mon.snd_mon != null)
			return 7;
		if (note_tbl[code] == 0)
			return 7;
		return snd_note(code);
	}

	static String getMessTxt(int ind) {
		String txt = mess_txt[ind];
		if (txt != null)
			switch (txt.charAt(0)) {
			case '@':
				return null;
			case '>':
				return txt.substring(1);
			}
		return txt;
	}

	static void advertize() {
		int pos, size = 20;
		String txt;

		if (mon.adv_rec < 1)
			mon.adv_dsp = 60;
		if ((pos = mon.adv_dsp) == 60) {
			if (ctl.ckr_nbr < 1 || ctl.ckr_nbr > 799 || ctl.mode > 0) {
				txt = Mnemo.getInfo(ctl.mode > 0 ? ctl.mode + 17 : 0);
				mon.adv_rec = 16;
				pos = 40 - size;
			} else {
				if (mon.adv_rec > 15)
					mon.adv_rec = 0;
				if ((txt = getMessTxt(mon.adv_rec++)) == null)
					return;
				pos = 20 - size;
			}
			mon.adv_txt = rightFill(editTxt(txt, 59), 79, ' ');
			panel.cid.hotMaint();
		}

		if (pos == 40)
			if (mon.adv_rec < 16) {
				if ((txt = getMessTxt(mon.adv_rec)) != null) {
					mon.adv_rec++;
					pos = 0;
					mon.adv_txt = mon.adv_txt.substring(40, 59) + rightFill(txt, 60, ' ');
				}
			}
		txt = mon.adv_txt.substring(pos, pos + size);
		DevIo.cusDisplay(0, txt);
		panel.cid.display(0, txt);
		mon.adv_dsp = pos + 1;
	}

	static void watch(int rec) {
		SpyDlg dlg = (SpyDlg) panel.modal;
		GdElJrn area = dlg.area;
		int rows = area.rows;

		if (rec == 0) {
			if (area.bar.getValue() < area.bar.getMaximum() - rows)
				return;
			rec = mon.watch;
			while (lJRN.read(mon.watch, tra.comm) > 0) {
				dlg.add(lJRN.pb);
				rec = mon.watch++;
			}
			if (rec == mon.watch)
				return;
			area.bar.setValues(rec - rows, rows, 0, rec);
		} else
			for (int ind = 0; ind < rows; ind++) {
				dlg.add(lJRN.read(rec + ind, tra.comm) > 0 ? lJRN.pb : null);
			}
		area.repaint();
	}
}
