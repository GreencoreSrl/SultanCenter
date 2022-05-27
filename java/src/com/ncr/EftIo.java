package com.ncr;

import java.io.*;
import javax.comm.*;

class EftIo extends Basis {
	static EftIo eft1 = null;
	static final int FAIL_EFT = -10; /* transaction abort by device */
	static final int FAIL_DLG = -20; /* transaction abort by dialog */

	static void initialize() {
		eft1 = getInstance("EFT");
	}

	static void terminate() {
		if (eft1 != null)
			eft1.stop();
	}

	static void eftOpen() {

		eft1.ckrOpen();
	}

	static void eftClose() {
		eft1.ckrClose();
	}

	static void eftTrans(boolean active) {
		eft1.preSwipe(active);
	}

	static int eftOrder(long value) {
		if (value != 0)
			return eft1.authorize();
		itm = new Itemdata();
		itm.tnd = tnd_tbl[K_TndEft];
		int sts = eft1.dayReset();
		if (sts == 0)
			prtForm(3 + ELJRN, "FORM_T" + editNum(itm.tnd, 2));
		return sts;
	}

	static void eftDetails() {
		eft1.details();
	}

	static int service(int sc) {
		return eft1.srvFuncs(sc);
	}

	static EftIo getInstance(String env) {
		Device d = new Device(env);
		if (d.version > 0) {
			EftIo eftio = (EftIo) d.loadProtocol();
			if (eftio != null) {
				stsLine.init(env + '=' + System.getProperty(env)).show(1);
				eftio.dev = d;
				eftio.start();
				return eftio;
			}
		}
		return new EftIo();
	}

	Device dev = null;
	SerialPort port = null;

	FileWriter EFTL = null; /* serial communication log */

	int ec_err = 0; /* most recent EFT terminal error code */
	int ec_prv = 0; /* most recent log data type (status/recv/send) */
	String ec_log = System.getProperty("EFTLOG");

	/***************************************************************************
	 * open communications and log
	 ***************************************************************************/
	void start() {
		if (ec_log != null) {
			File f = new File(ec_log);
			try {
				EFTL = new FileWriter(f.getAbsolutePath(), true);
			} catch (IOException e) {
				logError(e);
			}
		}
	}

	/***************************************************************************
	 * open operator at EFT device
	 ***************************************************************************/
	void ckrOpen() {
		if (port == null)
			return;
	}

	/***************************************************************************
	 * terminate session with EFT device
	 ***************************************************************************/
	void ckrClose() {
		if (port == null)
			return;
	}

	/***************************************************************************
	 * inform EFT device about start/end of transaction
	 * 
	 * @param active
	 *            true=start / false=stop
	 ***************************************************************************/
	void preSwipe(boolean active) {
		if (port == null)
			return;
	}

	/***************************************************************************
	 * debit / credit / void a monetary amount itm.pos = payment amount in tender currency itm.pov = cashback amount in
	 * tender currency
	 *
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int authorize() {
		return 7;
	}

	/***************************************************************************
	 * data collect N-records and print detailled info
	 ***************************************************************************/
	void details() {
		return;
	}

	/***************************************************************************
	 * service menu
	 * 
	 * @param sc
	 *            subcode (1-9)
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int srvFuncs(int sc) {
		return 7;
	}

	/***************************************************************************
	 * generation change at EFT device
	 *
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int dayReset() // generation change at EFT device
	{
		return 7;
	}

	/***************************************************************************
	 * close communications and log
	 ***************************************************************************/
	void stop() {
		if (EFTL != null)
			try {
				EFTL.close();
				EFTL = null;
			} catch (IOException e) {
				logError(e);
			}
		if (port != null) {
			port.close();
			port = null;
		}
	}

	/***************************************************************************
	 * end of eft transaction
	 *
	 * @param sts
	 *            0 = ok, -10 = cancel by device, -20 = cancel by dialog
	 * @return state of eft transaction (0=done, 23=failed)
	 ***************************************************************************/
	int endTrans(int sts) {
		if (EFTL != null)
			try {
				EFTL.flush();
			} catch (IOException e) {
				logError(e);
			}
		if (sts == 0)
			return sts;
		input.prompt = Mnemo.getText(36);
		if (sts != FAIL_DLG)
			comError(sts, false);
		return 23;
	}

	/***************************************************************************
	 * show eft/com error to operator
	 *
	 * @param sts
	 *            communication error, if -10 then EFT status in ec_err
	 * @param abort
	 *            operator sees two keys if true (clear and abort)
	 * @return operator decision (<2=clear 2=abort)
	 ***************************************************************************/
	int comError(int sts, boolean abort) {
		if (sts == FAIL_EFT)
			stsLine.init("EFT STATUS").upto(20, editInt(ec_err));
		else
			stsLine.init("COM STATUS").upto(20, editInt(sts));
		return gui.clearLink(stsLine.toString(), abort ? 5 : 1);
	}

	static void logError(Exception e) {
		logConsole(0, "EFTLOG", e.toString());
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
	void eft_log(int fnc, byte data[], int size) {
		if (EFTL == null)
			return;
		try {
			StringBuffer sb = new StringBuffer(20 + (size << 1));
			if (fnc == 0 || fnc != ec_prv) {
				ctl.setDatim();
				String txt[] = { "sts ", "rcv ", "snd " };
				sb.append("\r\n").append(editNum(ctl.date, 6)).append(' ').append(editNum(ctl.time, 6)).append('.')
						.append(editNum(ctl.msec, 3)).append(' ').append(txt[ec_prv = fnc]);
			}
			for (int ind = 0; ind < size; ind++)
				sb.append(editHex(data[ind], 2));
			EFTL.write(sb.toString());
		} catch (IOException e) {
			logError(e);
		}
	}
}
