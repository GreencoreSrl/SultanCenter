package com.ncr;

import java.io.*;
import java.net.*;
import java.util.*;

class WinEPTS extends EftIo {
	private DatagramSocket sock;
	private int port;
	private String codepage, n5p5g3;
	private int packet = 0;
	private InetAddress inet;
	static final int hdr_size = 21;
	static final int polynomial = 0xA001;

	Message sndMsg, rcvMsg;

	String ec_pan;
	int ec_cur = 'E', ec_copies;
	long ec_sls_amt, ec_csh_bck;

	static int crc_16(byte[] data, int offs, int size) {
		int crc = 0;

		while (size-- > 0) {
			crc ^= data[offs++] & 0xff;
			for (int bits = 8; bits-- > 0; crc >>= 1) {
				if ((crc & 1) > 0)
					crc ^= polynomial << 1;
			}
		}
		return (crc & 0xff) << 8 | crc >> 8; /* little endian */
	}

	class Message {
		byte data[];
		int index;
		DatagramPacket dp;

		Message(int size) {
			data = new byte[size];
		}

		Message pushHex(int val, int len) {
			for (int ind = len; ind-- > 0; val >>= 8) {
				data[index + ind] = (byte) val;
			}
			return skip(len);
		}

		Message pushNum(long val, int len) {
			return push(leftFill(Long.toString(val), len, '0'));
		}

		Message push(String s) {
			byte b[] = null;
			if (codepage != null)
				try {
					b = s.getBytes(codepage);
				} catch (IOException e) {
				}
			if (b == null)
				b = s.getBytes();
			for (int ind = 0; ind < s.length(); data[index++] = b[ind++])
				;
			return this;
		}

		Message init() {
			index = hdr_size;
			return skip(2);
		}

		Message skip(int len) {
			index += len;
			return this;
		}

		int scanHex(int len) {
			int val = 0;
			while (len-- > 0) {
				val <<= 8;
				val += data[index++] & 0xff;
			}
			return val;
		}

		long scanNum(int len) {
			try {
				return Long.parseLong(scan(len));
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		String scan(int len) {
			index += len;
			if (codepage != null)
				try {
					return new String(data, index - len, len, codepage);
				} catch (IOException e) {
				}
			return new String(data, index - len, len);
		}

		private int recv(int secs) {
			String cmd;
			dp.setLength(data.length);
			do {
				try {
					sock.setSoTimeout(secs * 1000);
					sock.receive(dp);
					eft_log(1, data, dp.getLength());
					index = 0;
					cmd = scan(3);
					if (!n5p5g3.equals(scan(13)))
						return ERROR;
				} catch (IOException e) {
					return ERROR;
				}
			} while (packet != scanNum(5));
			int len = dp.getLength() - index;
			if (len < 0 || len > 0 && len < 5)
				return -2;
			if (len > 0) {
				if (crc_16(data, index, len) != 0)
					return -3;
				len -= 2;
				if (len != scanHex(2))
					return -4;
				if (!cmd.equals("200"))
					return -5;
			} else if (!cmd.equals("210"))
				return -5;
			return len;
		}

		private int send() {
			int size = index - hdr_size;
			String hdr = "201" + n5p5g3 + editNum(++packet, 5);
			index = 0;
			push(hdr);
			if (size > 2) {
				pushHex(size, 2).skip(size - 2);
				pushHex(crc_16(data, hdr_size, size), 2);
			} else
				data[2] += 4; /* ping message */
			dp.setLength(index);
			try {
				sock.send(dp);
				eft_log(2, data, index);
				return 0;
			} catch (IOException e) {
				return ERROR;
			}
		}
	}

	public static void main(String[] args) {
		byte[] data = new byte[1];
		do {
			if ((data[0] & 15) == 0)
				System.out.println("");
			System.out.print(editHex(crc_16(data, 0, 1), 4) + " ");
		} while (++data[0] != 0);
	}

	/***************************************************************************
	 * open communications and log
	 ***************************************************************************/
	void start() {
		super.start();
		try {
			inet = InetAddress.getByName(dev.name);
			sock = new DatagramSocket();
			port = dev.version;
			rcvMsg = new Message(5220);
			rcvMsg.dp = new DatagramPacket(rcvMsg.data, 0);
			sndMsg = new Message(2048);
			sndMsg.dp = new DatagramPacket(sndMsg.data, 0, inet, port);
			Properties p = new Properties();
			p.load(new FileInputStream(dev.protocol + ".env"));
			n5p5g3 = rightFill(p.getProperty("N5P5G3", "00001"), 13, '0');
			codepage = p.getProperty("CodePage");
		} catch (IOException e) {
			dev.error(e);
		}
	}

	/***************************************************************************
	 * open operator at EFT device
	 ***************************************************************************/
	void ckrOpen() {
		if (port == 0)
			return;
	}

	/***************************************************************************
	 * terminate session with EFT device
	 ***************************************************************************/
	void ckrClose() {
		if (port == 0)
			return;
	}

	/***************************************************************************
	 * debit / credit / void a monetary amount
	 *
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int authorize() {
		int sts, type = 0xB0; /* payment */

		if (port == 0)
			return 7;
		ec_pan = "";
		ec_sls_amt = Math.abs(itm.pos);
		ec_csh_bck = Math.abs(itm.pov);
		lBOF.open(null, "FORM_T" + editNum(itm.tnd, 2) + ".TMP", 1);
		if (itm.mark != ' ') {
			type = 0xB2; /* void */
			ec_sls_amt = ec_csh_bck = 0; /* error correct */
		} else if (itm.cnt < 0)
			type = 0xB4; /* refund */
		sts = eft_autho(type);
		lBOF.close();
		if (sts == 0)
			if (ec_pan.length() > 0) {
				if (ec_pan.length() > 20) /* omit 59 or 67 */
				{
					ecn.bank = ec_pan.substring(2, 10);
					ecn.acct = ec_pan.substring(11, 21);
					itm.stat = ecn.seqno;
				} else
					itm.serial = ec_pan;
				itm.flag |= T_BNKREF;
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
		if (port == 0)
			return 7;
		lBOF.open(null, "FORM_T" + editNum(itm.tnd, 2) + ".TMP", 1);
		ec_sls_amt = ec_csh_bck = 0;
		int sts = eft_autho(0xBE);
		lBOF.close();
		if (sts == FAIL_EFT && ec_err == 240)
			sts = 0; /* not supported */
		return endTrans(sts);
	}

	/***************************************************************************
	 * close communications and log
	 ***************************************************************************/
	void stop() {
		if (port != 0)
			sock.close();
		super.stop();
	}

	private void eft_ticket(String text) {
		int ind = -1, len;
		String cpy = "$copies=" + (ec_copies == 1 ? 2 : 1);

		lBOF.init(cpy).index = lBOF.dataLen();
		lBOF.write();
		while ((len = text.indexOf('\n', ++ind)) > 0) {
			String data = text.substring(ind, len - 1);
			if (text.charAt(ind) > '\0')
				ind = len;
			else
				data = cpy;
			lBOF.init(data).index = lBOF.dataLen();
			lBOF.write();
		}
	}

	private boolean eft_string(int type) {
		int max = rcvMsg.scanHex(1);
		int min = rcvMsg.scanHex(1) > 0 ? max : 1;
		int txt = rcvMsg.scanHex(1) == 6 ? 36 : 15;
		int len = rcvMsg.scanHex(1);
		int dec = txt == 36 ? tnd[itm.tnd].dec : 0;

		return acceptNbr(rcvMsg.scan(len), txt, min, max, max, dec);
	}

	private int eft_dialog(int base) {
		int nbr, sts;

		for (;; panel.clearLink(Mnemo.getInfo(sts), 0x81)) {
			input.prompt = Mnemo.getText(15);
			input.init(0x00, 1, 1, sts = 0);
			SelDlg dlg = new SelDlg(Mnemo.getText(22));
			rcvMsg.index = base;
			for (nbr = rcvMsg.scanHex(1); sts < nbr;) {
				int type = rcvMsg.scanHex(1);
				if (type == 0) {
					panel.display(1, rcvMsg.scan(20));
					nbr--;
				} else
					dlg.add(9, editNum(++sts, 1), " " + rcvMsg.scan(20));
			}
			dlg.show("LBS");
			if ((sts = dlg.code) < 1) {
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.num < 1 || input.key != input.ENTER)
					sts = 5;
				else
					sts = input.adjust(input.pnt);
			}
			if (input.key == input.CLEAR)
				return 0;
			if (sts > 0)
				continue;
			sts = input.scanNum(input.num);
			if (sts > 0 && sts <= nbr)
				return sts;
			sts = 46;
		}
	}

	private void eft_status(int type) {
		String txt = rcvMsg.scan(rcvMsg.scanHex(1));
		int ind = txt.lastIndexOf("\r\n");

		if (ind >= 0) {
			stsLine.init(' ').upto(20, txt.substring(ind + 2)).show(2);
			txt = txt.substring(0, ind);
		}
		if (txt.length() > 0)
			stsLine.init(txt).show(1);
	}

	private int pan_checking(int type) {
		int sts = rcvMsg.scanHex(1);

		input.prompt = Mnemo.getText(31);
		input.init(0x80, sts, 0, 0);
		input.reset(rcvMsg.scan(sts));
		if (GdCusto.chk_cusspc(12) > 0)
			return 1;
		if (GdCusto.src_clu(input.pb, 1) > 0)
			return 1;
		if (type != cus.spec) {
			if (GdSigns.chk_autho(Mnemo.getInfo(38)) > 0)
				return 1;
		}
		return 0;
	}

	private int eft_rcvAck() {
		int sts = rcvMsg.recv(2); /* ack within 2 seconds */
		if (sts == 0)
			return sts;
		eft_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
		if (sts > 0)
			return -6; /* not an ACK */
		return sts;
	}

	private int eft_rcvMsg() {
		int code, sts, type;

		do {
			sts = rcvMsg.recv(120);
			if (sts < 0) {
				eft_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
				return sts;
			} else
				ec_prv = -1; /* enforce record header in log */
			if (sts < 3) {
				ec_err = 1111;
				return FAIL_EFT;
			}
			code = rcvMsg.scanHex(1);
			if (code == 0xA4 || code == 0xA5) {
				ec_err = (int) rcvMsg.scanNum(4);
				if (code == 0xA5) {
					panel.display(1, rcvMsg.scan(20));
					panel.display(2, rcvMsg.scan(20));
					gui.clearLink(Mnemo.getInfo(32), 0xA1);
					panel.display(2, Mnemo.getInfo(23));
				} else
					panel.display(2, rcvMsg.scan(20));
				return FAIL_EFT;
			}
			if (code == 0xA6) /* Get String */
			{
				type = rcvMsg.scanHex(1);
				if (!eft_string(type)) {
					sndMsg.init().pushHex(0xAF, 1);
					if ((sts = sndMsg.send()) != 0)
						return sts;
					return FAIL_DLG;
				}
				sndMsg.init().pushHex(0xA7, 1).push(input.pb);
				if ((sts = sndMsg.send()) != 0)
					return sts;
			}
			if (code == 0xA8) /* status notification */
			{
				eft_status(rcvMsg.scanHex(1));
			}
			if (code == 0xAA) /* choice between options */
			{
				type = eft_dialog(rcvMsg.index);
				sndMsg.init().pushHex(0xAB, 1).pushHex(type, 1);
				if ((sts = sndMsg.send()) != 0)
					return sts;
				if (type == 0)
					return FAIL_DLG;
			}
			if (code == 0xAD) /* PAN checking */
			{
				type = pan_checking(rcvMsg.scanHex(1));
				sndMsg.init().pushHex(0xAE, 1).pushHex(type, 1).pushHex(ec_cur, 1).pushNum(ec_sls_amt, 9).pushHex(0, 1);
				if ((sts = sndMsg.send()) != 0)
					return sts;
				if (type > 0)
					return FAIL_DLG;
			}
			if (code >= 0xB0 && code <= 0xBF)
				break;
			if (code == 0xFF)
				break;
		} while (code != 0xA3);
		type = rcvMsg.scanHex(1);
		ec_sls_amt = rcvMsg.skip(1).scanNum(9);
		ec_copies = (int) rcvMsg.scanNum(1);
		itm.number = rcvMsg.skip(23).scan(8);
		sts = rcvMsg.skip(26).scanHex(1);
		ec_pan = rcvMsg.scan(sts).trim();
		sts = rcvMsg.scanHex(2);
		if (sts > 0)
			eft_ticket(rcvMsg.scan(sts));
		if (code == 0xA3)
			return 0;
		if (code == 0xFF) {
			panel.display(2, Mnemo.getInfo(23));
			ec_err = 2222;
			return FAIL_EFT;
		}
		sts = sndMsg.init().pushHex(0xAC, 1).send();
		eft_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
		return sts;
	}

	private int eft_autho(int type) {
		int sts = sndMsg.init().send(); /* ping */
		if (sts < 0)
			return sts;
		if ((sts = eft_rcvAck()) < 0)
			return sts;

		do {
			sndMsg.init();
			sndMsg.pushHex(0xA1, 1).pushHex(type, 1) /* merge cashback into sales */
					.pushHex(ec_cur, 1).pushNum(ec_sls_amt + ec_csh_bck, 9).pushHex(0, 1);
			sndMsg.pushNum(ctl.sto_nbr, 4).pushNum(ctl.ckr_nbr, 4).push(editKey(ctl.reg_nbr, 4)).pushNum(ctl.tran, 4);
			sndMsg.pushHex(0, 2).pushNum(ec_sls_amt + ec_csh_bck, 9);
			sndMsg.pushHex(0, 3); /* 3 tracks */

			// if (ec_csh_bck > 0) /* redundant cashback in TLV format */
			// { sndMsg.pushHex (13, 2);
			// sndMsg.pushHex (0x1f800b09, 4);
			// sndMsg.pushNum (ec_csh_bck, 9);
			// }

			// sndMsg.pushHex (12, 2);
			// sndMsg.pushHex (0x1f800d08, 4);
			// sndMsg.push ("20070101");

			if ((sts = sndMsg.send()) < 0)
				return sts;
			sts = eft_rcvMsg();
		} while (sts == FAIL_EFT && ec_err == 252);
		return sts;
	}
}
