package com.ncr;

import java.io.*;

class BcrIo extends Basis {
	static BcrIo bcr1 = null;

	static boolean isActive() {
		return bcr1.dev.state == 0;
	}

	static void initialize() {
		bcr1 = getInstance("BCR");
		bcr1.dev.state = 2;
	}

	static void terminate() {
		if (bcr1 != null)
			bcr1.stop();
	}

	static void bcrOpen() {
		bcr1.ckrOpen();
	}

	static void bcrClose() {
		bcr1.ckrClose();
	}

	static void bcrTrans() {
		bcr1.endTrans();
	}

	static long getDeposit(int ind) {
		return ind > 0 ? bcr1.deposit : bcr1.stock;
	}

	static int bcrOrder(long value) {
		return bcr1.payOrder(value);
	}

	static int service(int sc) {
		return bcr1.srvFuncs(sc);
	}

	static void watch(int ind) {
		if (panel.modal != null) {
			if (!(panel.modal instanceof BcrDlg))
				return;
			if (bcr1.cashcount() < 1)
				return;
			BcrDlg dlg = (BcrDlg) panel.modal;
			stsLine.init(tnd[ind].text).upto(20, editMoney(ind, getDeposit(ind))).show(1);
			dlg.info[0].setText(stsLine.toString());
			stsLine.init(Mnemo.getText(24)).upto(20, editMoney(ind, getDeposit(0))).show(2);
			dlg.info[1].setText(stsLine.toString());
			for (ind = 0; ind < dnom_tbl.length; ind++) {
				int val = dnom_tbl[ind];
				dlg.setText(ind, Integer.toString(val), val < 3);
			}
		} else
			stsLine.init(tnd[ind].text).upto(20, editMoney(ind, getDeposit(ind))).show(1);
	}

	static BcrIo getInstance(String env) {
		Device d = new Device(env);
		if (d.version > 0) {
			BcrIo bcrio = (BcrIo) d.loadProtocol();
			if (bcrio != null) {
				stsLine.init(env + '=' + System.getProperty(env)).show(1);
				bcrio.dev = d;
				bcrio.start();
				return bcrio;
			}
		}
		return new BcrIo();
	}

	Device dev = new Device("???");
	FileWriter BCRL = null; /* tcp/ip communication log */

	int bcr_tnd = 0; /* tender number (type 'H') */
	int bcr_err = 0; /* most recent device error code */
	int bcr_prv = 0; /* most recent log data type (status/recv/send) */
	String bcr_log = System.getProperty("BCRLOG");
	long deposit = 0, stock = 0;

	/***************************************************************************
	 * open communications and log
	 ***************************************************************************/
	void start() {
		bcr_tnd = TndMedia.find('H');
		if (bcr_log != null) {
			File f = new File(bcr_log);
			try {
				BCRL = new FileWriter(f.getAbsolutePath(), true);
			} catch (IOException e) {
				logError(e);
			}
		}
	}

	/***************************************************************************
	 * close communications and log
	 ***************************************************************************/
	void stop() {
		if (BCRL != null)
			try {
				BCRL.close();
				BCRL = null;
			} catch (IOException e) {
				logError(e);
			}
	}

	/***************************************************************************
	 * open operator at BCR device
	 ***************************************************************************/
	void ckrOpen() {
		return;
	}

	/***************************************************************************
	 * terminate session with BCR device
	 ***************************************************************************/
	void ckrClose() {
		if (dev.state > 0)
			return;
	}

	/***************************************************************************
	 * inform BCR device about end of transaction
	 ***************************************************************************/
	void endTrans() {
		if (dev.state > 0)
			return;
	}

	/***************************************************************************
	 * freeze deposit dispense change
	 *
	 * @param value
	 *            amount to pay (back if < 0)
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int payOrder(long value) {
		return 7;
	}

	/***************************************************************************
	 * service menu
	 ***************************************************************************/
	int srvFuncs(int sc) {
		return 7;
	}

	/***************************************************************************
	 * fill cashcount array
	 ***************************************************************************/
	int cashcount() {
		return -1;
	}

	/***************************************************************************
	 * end of eft transaction
	 *
	 * @param sts
	 *            0 = ok, -1 = device offline, >0 = device error
	 * @return state of bcr transaction (0=done, 23=failed)
	 ***************************************************************************/
	int endOrder(int sts) {
		if (BCRL != null)
			try {
				BCRL.flush();
			} catch (IOException e) {
				logError(e);
			}
		if (sts > 0)
			return 0;
		comError(sts, false);
		return 23;
	}

	/***************************************************************************
	 * show bcr error to operator
	 *
	 * @param sts
	 *            communication error, if 0 then BCR status in bcr_err
	 * @param abort
	 *            operator sees two keys if true (clear and abort)
	 * @return operator decision (<2=clear 2=abort)
	 ***************************************************************************/
	int comError(int sts, boolean abort) {
		if (sts == 0)
			stsLine.init("BCR STATUS").upto(20, editInt(bcr_err));
		else
			stsLine.init(dev.protocol + " OFFLINE");
		return gui.clearLink(stsLine.toString(), abort ? 5 : 1);
	}

	static void logError(Exception e) {
		logConsole(0, "BCRLOG", e.toString());
	}

	/***************************************************************************
	 * log io data/status at serial communication port
	 *
	 * @param fnc
	 *            data type (0=status 1=recv 2=send)
	 * @param data
	 *            array of data bytes to be logged
	 * @param size
	 *            number of data bytes
	 ***************************************************************************/
	void bcr_log(int fnc, byte data[], int size) {
		if (BCRL == null)
			return;
		try {
			StringBuffer sb = new StringBuffer(20 + (size << 1));
			if (fnc == 0 || fnc != bcr_prv) {
				ctl.setDatim();
				String txt[] = { "sts ", "rcv ", "snd " };
				sb.append("\r\n").append(editNum(ctl.date, 6)).append(' ').append(editNum(ctl.time, 6)).append('.')
						.append(editNum(ctl.msec, 3)).append(' ').append(txt[bcr_prv = fnc]);
			}
			for (int ind = 0; ind < size; ind++)
				sb.append(editHex(data[ind], 2));
			BCRL.write(sb.toString());
		} catch (IOException e) {
			logError(e);
		}
	}
}
