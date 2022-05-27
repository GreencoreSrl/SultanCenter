package com.ncr; /*******************************************************************/
/*                                                                 */
/*     module name: lload.c            creation date 09/jul/93     */
/*     author name: h. bufe            ncr augsburg ssd-retail     */
/*                                                                 */
/*     load pos image data from server                                   */
/*     setup CTL and LAN file from IDC                                  */
/*                                                                 */
/*******************************************************************/

import java.awt.*;
import java.awt.event.*;

/*******************************************************************/

class LanLdataPanel extends Panel implements Runnable {
	int abort = 1; /* 0 = forever, 1 = runonce, -1 = aborted */
	Thread a2a = new Thread(this);

	LanIo net = new LanIo("LAN", 0, Struc.S_MSG);

	static LocalACT lACT = new LocalACT("ACT", 1);
	static LocalDPT lDPT = new LocalDPT("DPT", Struc.S_MOD);
	static LocalREG lREG = new LocalREG("REG", 1);
	static LocalSLM lSLM = new LocalSLM("SLM", Struc.S_MOD);

	static LocalCTL lCTL = new LocalCTL("CTL");
	static LocalLAN lLAN = new LocalLAN("LAN", Struc.S_CKR);
	static LocalPOT lPOT = new LocalPOT("POT", Struc.S_CKR, lREG.block[0]);
	static LocalPOS lPOS = new LocalPOS("POS");

	static SeqIo lIDC = new SeqIo("IDC", 0, 80);
	static SeqIo lJRN = new SeqIo("JRN", 0, 44);
	static SeqIo lGPO = new SeqIo("GPO", 0, 44);
	static SeqIo lDTL = new SeqIo("DTL", 0, 34);

	Button b1 = new Button(lIDC.id + lIDC.REG);
	Button b2 = new Button("000000");
	Button b3 = new Button("000000");

	static Terminal ctl = new Terminal();

	LanLdataPanel() {
		super(new BorderLayout());
		net.net = net;
		Panel pnl = new Panel(new GridLayout());
		pnl.add(b1);
		pnl.add(b2);
		pnl.add(b3);
		add(net.lbl, BorderLayout.NORTH);
		add(pnl, BorderLayout.CENTER);
		setEnabled(false);
		setFont(Config.getFont(Font.BOLD, 23));
	}

	void ctlUpdate(int nbr, int sts) {
		int rec = lCTL.find(nbr);

		if (rec < 1)
			return;
		if (sts == 1)
			if (lLAN.sts != 8 || lLAN.ckr != nbr) {
				lCTL.dateOpn = lLAN.date;
				lCTL.timeOpn = lLAN.time / 100;
				lCTL.reg = ctl.reg_nbr;
			}
		if (sts == 2 || sts == 19 && ctl.ckr_nbr == nbr) {
			lCTL.dateCls = lLAN.date;
			lCTL.timeCls = lLAN.time / 100;
		}
		if (sts == 1 || sts == 76)
			if (ctl.ckr_sec != lCTL.sec) {
				lCTL.sec = ctl.ckr_sec;
				lCTL.datePwd = lLAN.date;
				lCTL.timePwd = lLAN.time / 100;
			}
		if (sts == 19) {
			lCTL.dateBal = lLAN.date;
			lCTL.timeBal = lLAN.time / 100;
		}
		if (sts != 76)
			lCTL.sts = sts;
		lCTL.rewrite(rec);
	}

	void cpyCmos(int rec) {
		lREG.read(rec, lREG.LOCAL);
		int ic = lREG.key / 100, sc = lREG.key % 100;
		if (ic == 0) {
			if (sc == 2)
				lLAN.dat = lREG.block[0].trans;
			if (sc > 0 && sc < 9) {
				lPOS.trans = lREG.block[0].trans;
				lPOS.total = lREG.block[0].total;
				lPOS.rewrite(sc);
			}
		}
		if (ic == 1) {
			if (sc == 1)
				lLAN.org = lREG.rate;
		}
		if (rec > lPOT.getSize()) {
			lPOT.key = lREG.key;
			if (ic < 10 || sc < 7)
				lREG.block[0].reset();
			lPOT.write();
		}
	}

	void cpyData(SlsIo io) {
		char id = io.id.charAt(0);

		b1.setLabel(io.id + io.REG);
		b2.setLabel(id == 'P' ? "CKR" + net.editNum(ctl.ckr_nbr, 3) : "------");
		for (io.recno = 0;;) {
			show(b3, io.recno++);
			if (net.readSls(io.recno, ctl.reg_nbr, io) < 0)
				abort = net.ERROR;
			if (net.sts < 1)
				return;
			if (id == 'P') {
				lPOT.onto(0, io.pb.substring(io.fixSize));
				lPOT.rewrite(io.recno, lPOT.fixSize + lPOT.blk * Total.length);
			} else {
				io.onto(0, io.pb);
				io.rewrite(io.recno, 0);
				if (id == 'R')
					cpyCmos(io.recno);
			}
		}
	}

	public void run() {
		while (abort > 0) {
			net.open(DatIo.REG);
			if (net.state == 0)
				break;
			try {
				a2a.sleep(5000);
			} catch (InterruptedException e) {
				return;
			}
		}
		lCTL.update();
		while (lIDC.read(++lIDC.recno) > 0) {
			if (lIDC.pb.charAt(32) != 'F')
				continue;
			ctl.date = lIDC.skip(9).scanNum(6);
			ctl.time = lIDC.scan(':').scanNum(6);
			ctl.tran = lIDC.scan(':').scanNum(4);
			ctl.mode = lIDC.skip(7).scanNum(1);
			ctl.ckr_nbr = lIDC.skip(4).scanNum(3);
			String nbr = lIDC.skip().scan(16);
			int sts = lIDC.skip().scanNum(2);
			ctl.ckr_sec = lIDC.skip().scanNum(5);

			show(b2, ctl.date);
			show(b3, ctl.time);
			lLAN.date = ctl.date;
			lLAN.time = ctl.time;
			if (sts == 1 || sts == 2)
				ctlUpdate(ctl.ckr_nbr, sts);
			if (sts == 19)
				if (ctl.mode != 2)
					ctlUpdate(Integer.parseInt(nbr.substring(13)), sts);
			lLAN.sts = sts;
			if (lLAN.ckr != ctl.ckr_nbr) {
				if ((lLAN.ckr = ctl.ckr_nbr) < 800) {
					for (int ind = 0; ind < lLAN.tbl.length; ind++) {
						if (lLAN.tbl[ind] == 0)
							lLAN.tbl[ind] = lLAN.ckr;
						if (lLAN.tbl[ind] == lLAN.ckr)
							break;
					}
				}
			}
		}
		lLAN.idc = lIDC.getSize();
		lLAN.jrn = lJRN.getSize();
		lLAN.dtl = lDTL.getSize();
		lLAN.gpo = lGPO.getSize();

		cpyData(lACT);
		cpyData(lDPT);
		cpyData(lREG);
		cpyData(lSLM);
		lREG.id = lPOT.id;
		for (lPOT.blk = 0; lPOT.blk < lLAN.tbl.length; lPOT.blk++) {
			if ((ctl.ckr_nbr = lLAN.tbl[lPOT.blk]) < 1)
				break;
			if (net.regckr(ctl.reg_nbr, ctl.ckr_nbr, 21) < 1)
				abort = net.ERROR;
			else
				cpyData(lREG);
			if (abort < 0)
				break;
		}
		net.kill();
		if (lLAN.getSize() < 1)
			lLAN.write();
		lLAN.rewrite(1);
		lLAN.close();
		lPOT.close();
		System.exit(abort < 0 ? 255 : 0);
	}

	void show(Button b, int value) {
		b.setLabel(net.editNum(value, 6));
	}

	void stop() {
		try {
			abort = -1;
			a2a.interrupt();
			a2a.join();
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}
}

public class LanLdata {
	static LanLdataPanel pnl = new LanLdataPanel();

	public static void main(String args[]) {
		Frame f = new Frame("LAN load data");

		f.add(pnl, BorderLayout.CENTER);
		f.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				pnl.a2a.start();
			}

			public void windowClosing(WindowEvent e) {
				pnl.stop();
				System.exit(1);
			}
		});
		f.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == e.VK_ESCAPE || code == e.VK_F4 && e.isShiftDown()) {
					pnl.stop();
					System.exit(1);
				}
			}
		});
		f.setLocation(200, 200);
		f.pack();
		f.show();
	}
}
