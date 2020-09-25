package com.ncr;

class ZVT0700 extends EftIo {
	int ec_msk = 0x10;
	int ec_copies;
	byte ec_msg[] = new byte[19];
	byte ec_rsp[] = new byte[256];
	byte snd_frame[] = new byte[128];

	String ec_pan, ec_amt;
	String ec_cur = "0978"; // 0280 = DM, 0978 = EUR;
	String ec_pwd = System.getProperty("ECP", "123456");

	/***************************************************************************
	 * open communications and log
	 ***************************************************************************/
	void start() {
		super.start();
		try {
			port = dev.open(dev.baud, port.DATABITS_8, port.STOPBITS_2, port.PARITY_NONE);
		} catch (Exception e) {
			dev.error(e);
			return;
		}
		if (ec_pwd.length() > 8) {
			ec_cur = ec_pwd.substring(8);
			ec_pwd = ec_pwd.substring(0, 8);
		}
		if (ec_pwd.length() > 6) {
			ec_msk = Integer.parseInt(ec_pwd.substring(6), 16);
			ec_pwd = ec_pwd.substring(0, 6);
		}
		while ((dev.state = eft_open()) != 0) {
			if (comError(dev.state, true) > 1) {
				stop();
				break;
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
	 * debit / credit / void a monetary amount
	 *
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int authorize() {
		int sts = (tra.spf1 & M_TRVOID) > 0 ^ itm.mark != ' ' ? 1 : 0;
		long value = Math.abs(itm.pos + itm.pov);

		if (port == null)
			return 7;
		lBOF.open(null, "FORM_T" + editNum(itm.tnd, 2) + ".TMP", 1);
		lBOF.recno = lBOF.getSize();
		lREG.read(reg.findTnd(itm.tnd, 1), lREG.LOCAL);
		ec_copies = (lREG.tflg & 0x20) > 0 ? 2 : 1;
		ec_pan = null;
		ec_amt = leftFill(Long.toString(value), 12, '0');
		if (itm.cnt > 0)
			sts = eft_autho();
		else
			sts = sts > 0 ? eft_void() : eft_credit();
		lBOF.close();
		if (sts == 0) {
			if (ec_pan != null) {
				if (ec_pan.length() > 19) {
					if (ec_pan.length() > 20) /* omit 59 or 67 */
					{
						ecn.bank = ec_pan.substring(2, 10);
						ecn.acct = ec_pan.substring(11, 21);
						itm.stat = ecn.seqno;
					} else
						ec_pan = null;
				} else
					itm.serial = ec_pan;
			}
			itm.flag |= T_BNKREF;
			if (ec_pan == null) {
				ec_err = 101;
				sts = FAIL_EFT;
			}
			if (value != Long.parseLong(ec_amt)) {
				ec_err = 154;
				sts = FAIL_EFT;
			}
		}
		return endTrans(sts);
	}

	void details() {
		return;
	}

	/***************************************************************************
	 * generation change at EFT device
	 *
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int dayReset() {
		if (port == null)
			return 0;
		int sts = eft_enday();
		return endTrans(sts);
	}

	/***************************************************************************
	 * close communications and log
	 ***************************************************************************/
	void stop() {
		if (port != null)
			if (dev.state == 0) {
				int sts = eft_close();
				if (sts != 0)
					comError(sts, false);
			}
		super.stop();
	}

	/*******************************************************************/

	private int eft_recv(byte data[], int size, int timo) {
		int sts;

		if ((sts = dev.read(port, data, size, timo)) < 0) {
			eft_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
			return sts;
		}
		eft_log(1, data, size);
		if (size > 1 || data[0] != 0x10)
			return 0;
		if ((sts = dev.read(port, data, size, 1)) < 0) {
			eft_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
			return sts;
		}
		eft_log(1, data, size);
		return data[0] == 0x10 ? 0 : data[0];
	}

	private int eft_send(byte data[], int size) {
		int sts = 0;

		dev.write(port, data, size);
		eft_log(2, data, size);
		if (sts < 0)
			eft_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
		return sts;
	}

	//
	// modulo (x ** 16 + x ** 12 + x ** 5 + x ** 0) CCITT-Polynom
	//
	private int crc_16(byte chr, int crc) {
		int x = crc ^= chr & 0xff;

		x = ((x ^ crc << 4) & 0xff) << 8;
		return crc >> 8 ^ x >> 12 ^ x >> 5 ^ x;
	}

	private int msg_send(byte msg[], int len) {
		int crc = 0;
		int ind = 0, retry = 3, sts;
		byte ack[] = new byte[1];

		gui.display(2, Mnemo.getInfo(32));
		snd_frame[ind++] = 0x10;
		snd_frame[ind++] = 0x02;
		for (int i = 0; i < len; i++) {
			if (msg[i] == 0x10)
				snd_frame[ind++] = 0x10;
			crc = crc_16(snd_frame[ind++] = msg[i], crc);
		}
		snd_frame[ind++] = 0x10;
		crc = crc_16(snd_frame[ind++] = 0x03, crc);
		snd_frame[ind++] = (byte) crc;
		snd_frame[ind++] = (byte) (crc >> 8);
		while (retry-- > 0) {
			if ((sts = eft_send(snd_frame, ind)) != 0)
				return sts;
			if ((sts = eft_recv(ack, 1, 2)) >= 0)
				if (ack[0] == 0x06)
					return 0;
		}
		return -105;
	}

	private int msg_recv(byte msg[], int size, int timo) {
		int crc = 0, ind = 0, sts;
		byte chr[] = new byte[1], tmp[] = new byte[2];

		while ((sts = eft_recv(chr, 1, timo)) >= 0) {
			crc = ind = 0;
			timo = 10;
			if (sts == 0x02) {
				while ((sts = eft_recv(chr, 1, 1)) == 0) {
					if (ind == size)
						break;
					crc = crc_16(msg[ind++] = chr[0], crc);
				}
			}
			while (sts == 0) {
				sts = eft_recv(chr, ind = 1, 1);
			}
			if (sts == 0x03) {
				if (eft_recv(tmp, 2, 1) == 0) {
					if (crc_16(chr[0], crc) == (tmp[0] & 0xff) + ((tmp[1] & 0xff) << 8))
						if (ind > 2)
							if ((msg[2] & 0xff) == ind - 3)
								if (eft_send(new byte[] { 0x06 }, 1) == 0)
									return ind;
				}
			}
			eft_send(new byte[] { 0x15 }, 1);
		}
		return sts;
	}

	/*******************************************************************/

	private int eft_pack(int ind, String src, int fcd) {
		int len = src.length();

		ec_msg[ind++] = (byte) fcd;
		for (int i = 0; i < len; i += 2) {
			ec_msg[ind++] = (byte) Integer.parseInt(src.substring(i, i + 2), 16);
		}
		return len + 2 >> 1;
	}

	private int eft_scan(int ind) {
		return ec_rsp[ind] & 0xff;
	}

	private String eft_unpk(int ind, int len) {
		StringBuffer sb = new StringBuffer(len << 1);

		while (len-- > 0) {
			sb.append((char) ((eft_scan(++ind) >> 4) | '0'));
			sb.append((char) ((eft_scan(ind) & 0x0F) | '0'));
		}
		return sb.toString();
	}

	private int eft_lvar(int ind, int len) {
		int cnt = 0;

		while (len-- > 0) {
			cnt *= 10;
			cnt += eft_scan(++ind) & 15;
		}
		return cnt;
	}

	private int eft_rcv_rsp(int len) {
		int sts = msg_recv(ec_rsp, len, 5);
		if (sts < 0)
			return sts;
		sts = eft_scan(0);
		ec_err = eft_scan(1);
		if (sts == 0x80 || sts == 0x84 && ec_err == 0)
			return 0;
		return FAIL_EFT;
	}

	private int eft_snd_rsp(int sts) {
		int ind = eft_pack(0, "0000", 0x84);

		if (sts == 0)
			ec_msg[0] = (byte) 0x80;
		else
			ec_msg[1] = (byte) sts;
		return msg_send(ec_msg, ind);
	}

	private int eft_datim() {
		int ind = eft_pack(0, "0008", 0x80);

		ind += eft_pack(ind, editNum(ctl.date, 6), 0xAA);
		ind += eft_pack(ind, editNum(ctl.time, 6), 0x0C);
		return msg_send(ec_msg, ind);
	}

	private void eft_idc_eod(int ind, int len) {
		len -= 4;
		len /= 7;
		for (ind += 4; len-- > 0; ind += 7) {
			itm.cnt = eft_scan(ind);
			String s = eft_unpk(ind, 6);
			itm.pos = Long.parseLong(s.substring(1));
			if (s.charAt(0) > '9')
				itm.pos = 0 - itm.pos;
			GdTndrs.tnd_wridc('T', 5, 0, itm.cnt, itm.pos);
		}
	}

	private void eft_ticket(int ctrl, String text) {
		if (lBOF.recno == lBOF.getSize()) {
			lBOF.init("$copies=" + ec_copies).skip(lBOF.dataLen());
			lBOF.write();
		}
		lBOF.init(' ');
		if ((ctrl & 0x20) > 0)
			lBOF.push('>');
		lBOF.skip(ctrl & 0x0f).push(text);
		lBOF.index = lBOF.dataLen();
		lBOF.write();
	}

	private int eft_trend() {
		int cla, ind, sts;

		if ((sts = eft_rcv_rsp(3)) < 0)
			return sts;
		do {
			if ((sts = msg_recv(ec_rsp, 256, 240)) < 0)
				return sts;
			cla = eft_scan(ind = 0);
			int cmd = eft_scan(++ind), len = eft_scan(++ind);
			if (cla == 0x05 && cmd == 0x02) // date/time request
			{
				if ((sts = eft_datim()) != 0)
					return sts;
				continue;
			}
			if (cla == 0x06 && cmd == 0x1E) // transaction abort
			{
				if ((sts = eft_snd_rsp(0x00)) != 0)
					return sts;
				ec_err = eft_scan(3);
				return FAIL_EFT;
			}
			if (cla == 0x06 && cmd == 0xD1) // document line
			{
				sts = eft_scan(3);
				if (sts < 0xff)
					eft_ticket(sts, new String(ec_rsp, 4, len - 1));
				else
					for (sts = eft_scan(4); sts-- > 0; eft_ticket(0, ""))
						;
				if ((sts = eft_snd_rsp(0)) != 0)
					return sts;
				cla = 0;
				continue;
			}
			if (cla != 0x04 && cla != 0x06 || cmd != 0x0F) // unknown
			{
				if ((sts = eft_snd_rsp(0x9B)) != 0)
					return sts;
				cla = 0;
				continue;
			}
			if ((sts = eft_snd_rsp(0x00)) != 0)
				return sts;
			if (len < ++ind - 2)
				continue;
			if (eft_scan(ind) == 0x27) // interim status
			{
				if (eft_scan(++ind) != 0)
					continue;
				if (len < ++ind - 2)
					continue;
			}
			if (eft_scan(ind) == 0x04) // monetary amount
			{
				ec_amt = eft_unpk(ind, 6);
				ind += 7;
				if (len < ind - 2)
					continue;
			}
			if (eft_scan(ind) == 0x19)
				ind += 2; // status
			if (eft_scan(ind) == 0x29)
				ind += 5; // TID
			if (eft_scan(ind) == 0x49)
				ind += 3; // currency
			if (eft_scan(ind) == 0x60) // eod totals
			{
				sts = eft_lvar(ind, 3);
				eft_idc_eod(ind += 4, sts);
				ind += sts;
				if (len < ind - 2)
					continue;
			}
			if (eft_scan(ind) == 0x0C)
				ind += 4; // time
			if (eft_scan(ind) == 0x0D)
				ind += 3; // date
			if (eft_scan(ind) == 0x22) // pan
			{
				sts = eft_lvar(ind, 2);
				ind += 3 + sts;
				if (sts < 12) {
					ec_pan = eft_unpk(ind - sts - 1, sts);
					if (ec_pan.charAt(sts += sts - 1) > '9')
						ec_pan = ec_pan.substring(0, sts - 1);
				}
			}
			if (eft_scan(ind) == 0x17) // card seqno
			{
				ecn.seqno = Integer.parseInt(eft_unpk(ind + 1, 1));
				ind += 3;
			}
			if (eft_scan(ind) == 0x87) // transaction
			{
				itm.number = eft_unpk(ind, 2);
				ind += 3;
			}
			if (eft_scan(ind) == 0x3B) // authorization id
				ind += 9;
			if (eft_scan(ind) == 0xBA) // AID parameters (EC)
				ind += 6;
			if (eft_scan(ind) == 0x0B) // trace number
				ind += 4;
			if (eft_scan(ind) == 0x19) // condition code
				ind += 2;
			if (eft_scan(ind) == 0x29) // terminal id
				ind += 5;
			if (eft_scan(ind) == 0x0E) // expiration date
			{
				ecn.yymm = Integer.parseInt(eft_unpk(ind, 2));
				ind += 3;
			}

			if (eft_scan(ind) == 0xA7) // chip card
			{
				sts = eft_lvar(ind, 2);
				if (eft_scan(ind += 3) == 0xE9) {
					ec_pan = eft_unpk(ind + 22, 5);
					ec_amt = eft_unpk(ind + 32, 3);
				}
				ind += sts;
			}
		} while (cla != 0x06);
		return 0;
	}

	private int eft_autho() {
		int ind = eft_pack(0, "010A", 0x06);

		ind += eft_pack(ind, ec_amt, 0x04);
		ind += eft_pack(ind, ec_cur, 0x49);
		int sts = msg_send(ec_msg, ind);
		return sts == 0 ? eft_trend() : sts;
	}

	private int eft_credit() {
		int ind = eft_pack(0, "31", 0x06);

		ind += eft_pack(ind, ec_pwd, 13);
		ind += eft_pack(ind, ec_amt, 0x04);
		ind += eft_pack(ind, ec_cur, 0x49);
		int sts = msg_send(ec_msg, ind);
		return sts == 0 ? eft_trend() : sts;
	}

	private int eft_void() {
		int ind = eft_pack(0, "30", 0x06);

		ind += eft_pack(ind, ec_pwd, 16);
		ind += eft_pack(ind, leftFill(itm.number, 4, '0'), 0x87);
		ind += eft_pack(ind, ec_amt, 0x04);
		ind += eft_pack(ind, ec_cur, 0x49);
		int sts = msg_send(ec_msg, ind);
		return sts == 0 ? eft_trend() : sts;
	}

	private int eft_open() {
		int ind = eft_pack(0, "00", 0x06);

		ind += eft_pack(ind, ec_pwd, 6);
		ind += eft_pack(ind, ec_cur, ec_msk);
		int sts = msg_send(ec_msg, ind);
		return sts == 0 ? eft_trend() : sts;
	}

	private int eft_enday() {
		int ind = eft_pack(0, "50", 0x06);

		ind += eft_pack(ind, ec_pwd, 3);
		int sts = msg_send(ec_msg, ind);
		return sts == 0 ? eft_trend() : sts;
	}

	private int eft_close() {
		int ind = eft_pack(0, "0200", 0x06);

		int sts = msg_send(ec_msg, ind);
		return sts == 0 ? eft_rcv_rsp(3) : sts;
	}
}
