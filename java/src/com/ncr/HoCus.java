package com.ncr;

import java.io.*;

/***************************************************************************
 *
 * access to Highorder CustomerTransaction Accounts provides methods to initiate requests, monitor and possibly cancel
 *
 ***************************************************************************/
abstract class HoCus extends Basis {
	/***************************************************************************
	 * add request record to HCA file
	 *
	 * @param customer
	 *            number (13 digits, right justified)
	 ***************************************************************************/
	static void append(String cid) {
		if (ctl.lan > 2)
			return;
		rHCA.reg1 = ctl.reg_nbr;
		rHCA.ckr = ctl.ckr_nbr;
		rHCA.dat1 = ctl.date;
		rHCA.tim1 = ctl.time;
		rHCA.reg2 = rHCA.dat2 = rHCA.tim2 = rHCA.sts = 0;
		rHCA.key = cid;
		if (rHCA.write(rHCA.recno = 0) != 0)
			mon.hocus = 0;
	}

	/***************************************************************************
	 * cancel pending request
	 *
	 * @param relative
	 *            record number in HCA file
	 ***************************************************************************/
	static void cancel(int rec) {
		int sts = ERROR;

		if (rec > 0)
			if (rHCA.read(rec) > 0) {
				if ((sts = rHCA.sts) == 0) {
					rHCA.reg2 = ctl.reg_nbr;
					rHCA.dat2 = ctl.date;
					rHCA.tim2 = ctl.time;
					rHCA.sts = 2;
					sts = rHCA.write(rec) < 1 ? ERROR : 0;
				}
			}
		if (sts != 0)
			rHCA.error(new IOException("CancelStatus=" + sts), false);
	}

	/***************************************************************************
	 * check status of HCA request
	 *
	 * @return false=pending, true=completed
	 ***************************************************************************/
	static boolean isReady() {
		if (rHCA.recno < 1) {
			append(rHCA.key);
			return false;
		}
		if (rHCA.read(rHCA.recno) < 1)
			return false;
		return rHCA.sts > 0;
	}

	/***************************************************************************
	 * Wait for completion
	 *
	 * @return -1=operator abort, 0=no service, >0=HCA status
	 ***************************************************************************/
	static int finish() {
		if (mon.hocus < 0)
			return 0;
		mon.hocus = 1;
		int sts = panel.clearLink(Mnemo.getInfo(26), 0x81);
		mon.hocus = -1;
		if (sts >= 0) {
			cancel(rHCA.recno);
			return ERROR;
		}
		logConsole(2, rHCA.id, rHCA.pb);
		return rHCA.sts;
	}
}
