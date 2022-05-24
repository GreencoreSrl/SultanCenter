package com.ncr;

import org.apache.log4j.Logger;

import java.awt.event.*;

public class ConIo extends LinIo {
	private static final Logger logger = Logger.getLogger(ConIo.class);

	static int dky = -1, sel = 0, lck = 8;
	int key, num, dec, tic;
	int flg, max, msk, pnt;

	String alpha, label, prompt;
	String qrcode = "";  //QRCODE-SELL-CGA#A
	String aux1 = ""; // TSC-ENH2014-1-AMZ#ADD -- extended EAN 128 info
	static final char QR_CODE_SEPARATOR = ';';

	static final int ABORT = 0x1B;
	static final int AUTHO = 0x0A;
	static final int BACK1 = 0x08;
	static final int CLEAR = 0x0B;
	static final int ENTER = 0x0D;
	static final int JRNAL = 0x0C;
	static final int NORTH = 0x1E;
	static final int NOTES = 0xF0;
	static final int PAUSE = 0xA8;
	static final int POINT = 0x2E;
	static final int SOUTH = 0x1F;
	static final int TOTAL = 0x19;

	static int optAuth = 0, posLock = 0;

	ConIo(int size) {
		super("CON", 0, size);
	}

	void init(int flg, int max, int msk, int pnt) {
		this.flg = flg;
		this.max = max;
		this.msk = msk;
		this.pnt = pnt;
		reset("");
	}

	void reset(String s) {
		key = dec = 0;
		num = (pb = s).length();
		if ((flg & 0x80) == 0 || num > 0)
			echo();
		else
			tic = index = 0;
	}

	void echo() {
		char chr;
		int ind, pos = dec, pre = msk - num;

		super.init(prompt);
		tic = index;
		if (pnt > 0 && pos == 0)
			pos = pnt + 1;
		for (ind = dataLen(); ind > 0; poke(--ind, chr)) {
			if (pos > 0)
				if (--pos == 0) {
					chr = POINT;
					continue;
				}
			if (num > 0) {
				chr = pb.charAt(--num);
				if ((flg & (pos > 0 ? 0x20 : 0x40)) > 0)
					chr = 'X';
				continue;
			}
			chr = '*';
			if (--pre < 0) {
				if (ind == dataLen())
					break;
				if ((chr = ' ') == peek(ind))
					break;
			}
		}
		num = pb.length();
		show(2);
	}

	int numeric(int code) {
		if (num >= max)
			return 2;
		pb += (char) code;
		if (dec > 0)
			dec++;
		num++;
		echo();
		return -1;
	}

	int accept(int code) {
		int sts = -1;
		label = null;
		if (code >= '0' && code <= '9') {
			return numeric(code);
		}
		if (code >= 0x3a && code <= 0x3b) {
			while (code-- >= '9')
				sts = numeric('0');
			return sts;
		}
		if (code == POINT) {
			if (max == 0)
				return 2;
			if (dec++ > 0)
				return 5;
			echo();
			return sts;
		}
		if (code == BACK1) {
			if (max == 0)
				return sts;
			if (num > 0 && dec != 1)
				pb = pb.substring(0, --num);
			if (dec > 0)
				dec--;
			echo();
			return sts;
		}
		key = code;
		logConsole(16, "input.flg=0x" + editHex(flg, 2) + " key=0x" + editHex(key, 2) + " lck=0x" + editHex(lck, 2)
				+ " dec=" + dec + " pb=" + pb, null);
		if (code == CLEAR)
			if (num > 0 || dec > 0)
				return 5;
		return 0;
	}

	int adjust(int len) {
		if (dec == 0)
			return 0;
		if (len < 1 || dec > ++len)
			return 4;
		while (dec < len)
			if (numeric('0') > 0)
				return 2;
		dec--;
		return 0;
	}

	boolean isEmpty() {
		return num + dec == 0;
	}

	int label(String cmd) {
		if (max < 1 || !isEmpty())
			return 5;
		int ind = 4;
		char c = cmd.charAt(ind);
		if (cmd.length() <= ind)
			return 9;
		label = cmd;
		if (c == ']') /* RSS14 */
		{
			if (cmd.indexOf("01") != 7)
				return 9;
			cmd = cmd.substring(0, 5) + cmd.substring(9, 23);
		}
		if (cmd.length() == 12 && cmd.indexOf("B1") == ind) {
			cmd = cmd.substring(0, 6) + ipcDecode(cmd, 6);
		}
		if (c == 'B' || cmd.indexOf("FF") == ind)
			ind++;
		if (!Character.isDigit(c))
			ind++;
		for (; ind < cmd.length(); num++) {
			// TSC-ENH2014-1-AMZ#BEG
			if (cmd.length() == 30 && num == 13) {
				aux1 = cmd.substring(18, 30);
				break;
			}
			// TSC-ENH2014-1-AMZ#END
			if (num >= dataLen())
				return 2;
			pb += cmd.charAt(ind++);
		}
		key = 0x4f4f;
		return 0;
	}

	int track(String cmd) {

		int len, track = cmd.charAt(3) & 15, yymm = 0;
		PayCards ecn = Struc.ecn;

		if (max < 1 || !isEmpty())
			return 5;
		label = cmd;
		if (track == 1) {
			logger.info("MSR Card , Track1 = [" + cmd + "]");

			//SAFP-20170224-CGA#A BEG
			if (GdSaf.isEnabled()) {
				System.out.println("Track1 [" + cmd + "] Len [" + cmd.length() + "] ");
				if (cmd.length() >= 34) {
					ecn.custom = cmd.substring(22, 34);
				}
			}
			//SAFP-20170224-CGA#A END

			return -1;
		}
		if (track == 2) {
			// TAMI-ENH-NEWTRACK-CUST-CGA#A BEG
			logger.info("Param.getNewMSR_Track(): " + Param.getNewMSR_Track());
			logger.info("MSR Card , Track2 = [" + cmd + "]");
			if (Param.getNewMSR_Track() > 0) {

				if (Param.getNewMSR_Track() == 3) {
					logger.info("MSR Card , using new specifications for customer mapping");
					String costumerCode = "";

					/*
					 * Spec says : 1. The changes are required in ARS system to read the customer card number from
					 * position 1 to length 2 and from position 10 to length 9. The reason to 2 parts from track2 data
					 * is the customer number in ARS system file structure is limited to 12 digits.
					 * 
					 * MSR2220398111020680000=1409
					 * 
					 * 12 3456789 012345678 (spec. index) 012 3 45 6789012 345678901 23456 (java index) MSR 2 22 0398111
					 * 020680000 =1409 (java MSR string) XX XXXXXXXXX
					 * 
					 */
					costumerCode = cmd.substring(4, 6) + cmd.substring(13, 22);

					if ((len = cmd.indexOf('=')) < 0)
						len = cmd.length();
					else
						yymm = Integer.parseInt(cmd.substring(len + 1, len + 5));

					logger.info(" len: " + len);
					logger.info(" date read: " + yymm);
					pb = costumerCode;
					logger.info("customer code - pb: " + pb);
					key = 0x4d4d;
				} else {
					if (cmd.substring(4, 7).equals("672")) {
						logger.info("EXIT return -1  - because substr(4,7) == 672 ");
						return -1;
					}

					if ((len = cmd.indexOf('=')) < 0)
						len = cmd.length();
					else
						yymm = Integer.parseInt(cmd.substring(len + 1, len + 5));

					logger.info(" len: " + len);
					logger.info(" date read: " + yymm);

					// if this param is enabled, it doesn't check date.
					if (Param.getNewMSR_Track() == 2) {
						yymm += 4900;
						logger.info(" new date: " + yymm);
					}
					// PSH-ENH-001-AMZ#BEG -- len MSR on giftcard
					if (GdPsh.readingGCSerial) {
						if (len > 32 + 4) {
							logger.debug("EXIT - len > 32 + 4 - return 31");
							return 31;
						}
					} else {
						if (len > 24) {
							logger.debug("EXIT - len > 24 - return 31");
							return 31;
						}
					}
					// PSH-ENH-001-AMZ#END -- len MSR on giftcard
					// PSH-ENH-001-AMZ#DEL -- len MSR on giftcard
					/*
					 * if (len > 24) { logger.exiting(CLASSNAME, "track", 31); return 31; }
					 */
					// PSH-ENH-001-AMZ#DEL -- len MSR on giftcard
					pb = cmd.substring(13, len - 3);
					logger.info("customer code - pb: " + pb);
					key = 0x4d4d;
				}
			} else {
				if (cmd.substring(4, 7).equals("672"))
					return -1;
				if ((len = cmd.indexOf('=')) < 0)
					len = cmd.length();
				else
					yymm = Integer.parseInt(cmd.substring(len + 1, len + 5));

				// 0123456789012345678901234
				// ;220398111013476000=1208?
				pb = cmd.substring(4, len);
				key = 0x4d4d;
			}

			/*
			 * if (cmd.substring (4, 7).equals ("672")) return -1; if ((len = cmd.indexOf ('=')) < 0) len = cmd.length
			 * (); else yymm = Integer.parseInt (cmd.substring (len + 1, len + 5));
			 * 
			 * if (len > 24) return 31;
			 * 
			 * pb = cmd.substring (4, len); key = 0x4d4d;
			 */
			// TAMI-ENH-NEWTRACK-CUST-CGA#A END
		}
		if (track == 3) {
			logger.info("MSR Card , Track3 = [" + cmd + "]");
			try {
				if (Integer.parseInt(cmd.substring(4, 6)) > 1)
					return -1;
				if (Integer.parseInt(cmd.substring(6, 8)) != 59)
					return -1;
			} catch (NumberFormatException e) {
				return -1;
			} catch (StringIndexOutOfBoundsException e) {
				return -1;
			}
			ecn.bank = cmd.substring(8, 16);
			ecn.nation = Integer.parseInt(cmd.substring(29, 32));
			ecn.currency = Integer.parseInt(cmd.substring(32, 35));
			ecn.seqno = Integer.parseInt(cmd.substring(68, 69));
			yymm = Integer.parseInt(cmd.substring(64, 68));
			pb = cmd.substring(17, 27);
			key = 0x4dec;
		}
		num = pb.length();
		if ((ecn.yymm = yymm) > 0) {
			if (cmpDates(yymm * 100 + 31, Struc.ctl.date) < 0) {
				logger.info("Dates: [" + ecn.yymm + "] [" + Struc.ctl.date + "]");
				// TSC-MOD2014-AMZ#ADD
				if (!GdTsc.customerCardDateChk(pb)) {
					return 31;
				}
				// TSC-MOD2014-AMZ#ADD
			}
		}
		return 0;
	}

	void keyLock(int pos) {
		int lck_bit[] = { 8, 1, 4, 2 };
		String lck_txt[] = { "[ L ]", "[N/R]", "[ S ]", "[ X ]" };

		if ((optAuth & 2) > 0)
			return;
		if (pos < 1 || pos > lck_txt.length)
			return;
		posLock = pos--;
		tic = 0;
		gui.dspStatus(2, lck_txt[pos], true, pos > 1);
		lck &= 0xF0;
		lck |= lck_bit[pos];
		if (pos == 2) {
			if (optAuth < 2)
				lck |= 0x10;
		} else if (optAuth != 1 || pos != 1)
			lck &= ~0x10;
	}

	boolean isLockPos(int pos) {
		return (optAuth & 2) > 0 || (lck & pos) > 0;
	}

	int keyTrans(int vkey) {
		for (int ind = vkeys.length; ind-- > 0;)
			if (vkeys[ind][0] == vkey)
				return vkeys[ind][1];
		return 0;
	}

	int keyBoard(KeyEvent e) {
		if (DevIo.wdge.filter(e)) {
			e.consume();
			return ERROR;
		}
		if (e.isAltDown())
			return ERROR;
		int code = e.getKeyChar();
		int vkey = e.getKeyCode();
		gui.feedBack(e);
		if (vkey == e.VK_PAUSE)
			return accept(PAUSE);
		logger.debug("Code: [" + code + "] Vkey: " + vkey);
		if (vkey >= e.VK_F1 && vkey <= e.VK_F10) {
			code = vkey - e.VK_F1 + 0xbb;
			if (e.isControlDown())
				code += 0x23;
			else if (e.isShiftDown())
				code += 0x19;
		} else if ((vkey = keyTrans(vkey)) > 0)
			code = vkey;
		else if (code < 0x20 || code > 0xff)
			return ERROR;
		else if ((flg & 0x10) > 0)
			return numeric(code);
		else if (code > 0x9f)
			return ERROR;
		code = table[code];
		if (code < 1 || code >= 255)
			return ERROR;
		e.consume();
		if (code >= 0x80 && code < 0x88) {
			if (dky < 0)
				return ERROR;
			code -= 0x80;
			if (sel > 0) {
				gui.select(code);
				return ERROR;
			}
			if ((code = dynas[dky][code]) == 0xFF)
				return ERROR;
		}
		return accept(code);
	}

	String dkyRule(int ind) {
		return rules[dky][ind];
	}

	static boolean hasDyna() {
		return table[0xBB] == 0x80;
	}

	static int vkeys[][] = { /* virtual key code, MS DOS code */
			{ KeyEvent.VK_ENTER, 0x0d }, { KeyEvent.VK_BACK_SPACE, 0x08 }, { KeyEvent.VK_TAB, 0x09 },
			{ KeyEvent.VK_ESCAPE, 0x1b }, { KeyEvent.VK_HOME, 0x47 + 0x80 }, { KeyEvent.VK_UP, 0x48 + 0x80 },
			{ KeyEvent.VK_PAGE_UP, 0x49 + 0x80 }, { KeyEvent.VK_LEFT, 0x4b + 0x80 }, { KeyEvent.VK_RIGHT, 0x4d + 0x80 },
			{ KeyEvent.VK_END, 0x4f + 0x80 }, { KeyEvent.VK_DOWN, 0x50 + 0x80 }, { KeyEvent.VK_PAGE_DOWN, 0x51 + 0x80 },
			{ KeyEvent.VK_INSERT, 0x52 + 0x80 }, { KeyEvent.VK_DELETE, 0x53 + 0x80 }, { KeyEvent.VK_F11, 0x54 + 0x80 },
			{ KeyEvent.VK_F12, 0x55 + 0x80 }, };

	static int table[] = { // x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 xA xB xC xD xE xF
			/* 00 - 0F */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xC2, 0x00, 0x0D, 0x00, 0x00, 0x0D, 0x00,
			0x00, /* 10 - 1F */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00,
			0x00, 0x00, /* 20 - 2F */ 0xC1, 0x00, 0xC4, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x06, 0xDD,
			0x11, 0x2E, 0xDB, /* 30 - 3F */ 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0xC3, 0x00,
			0x00, 0x00, 0x00, 0x00, /* 40 - 4F */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 50 - 5F */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0xDC, 0x00, 0x00, 0xDE, /* 60 - 6F */ 0x00, 0xA5, 0xA6, 0xB9, 0xBA, 0xD1, 0xD2, 0xD3, 0xB1,
			0xB2, 0xBB, 0xBC, 0xD4, 0xD5, 0xD6, 0xA3, /* 70 - 7F */ 0xA4, 0xBD, 0xBE, 0xD7, 0xD8, 0xD9, 0xF9, 0xFA,
			0xFB, 0xE1, 0xE2, 0x00, 0x00, 0x00, 0x00, 0x00, /* 80 - 8F */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 90 - 9F */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x00, /* A0 - AF */ 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* B0 - BF */ 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF0, 0xF1, 0x1E, 0xF2, 0x1F, /* C0 - CF */ 0xF3, 0x0F, 0xF4,
			0xF8, 0x04, 0x00, 0x00, 0x00, 0x1E, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* D0 - DF */ 0x1F, 0x00,
			0xB6, 0x08, 0x0B, 0x05, 0xFF, 0x1B, 0x01, 0x02, 0x18, 0x19, 0x3A, 0x00, 0x00, 0x00, /* E0 - EF */ 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			/* F0 - FF */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, };

	static int dynas[][] = { // F1 F2 F3 F4 F5 F6 F7 F8
			/* state 0 */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xBE },
			/* state 1 */ { 0xA2, 0xA8, 0x04, 0xF1, 0xF2, 0xF3, 0xF4, 0xF8 },
			/* state 2 */ { 0x01, 0x02, 0xB9, 0x11, 0x18, 0x1D, 0xB6, 0x07 },
			/* state 3 */ { 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8 },
			/* state 4 */ { 0xC1, 0xC2, 0xC3, 0xC4, 0xF8, 0xF9, 0xFA, 0xFB },
			/* state 5 */ { 0xFF, 0xFF, 0xB9, 0xFF, 0x06, 0xFF, 0x11, 0x12 },
			/* state 6 */ { 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88 },
			/* state 7 */ { 0xB0, 0xB4, 0xB5, 0xFF, 0xA3, 0xFF, 0xA5, 0xA6 },
			/* state 8 */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state 9 */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state A */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state B */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state C */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state D */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state E */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
			/* state F */ { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, };

	static String rules[][] = { { " 00", " 00", " 00", " 00", " 00", " 00", " 00", "M57" },
			{ "M21", "M27", "K05", "M03", "M19", "M43", "M51", "M50" },
			{ "M73", "M74", "M52", "M75", "K23", "M77", "M49", "M48" },
			{ "D01", "D02", "D03", "D04", "D05", "D06", "D07", "D08" },
			{ "c01", "c02", "c03", "c04", "M59", "M61", "M60", "M62" },
			{ " 00", " 00", "M52", " 00", "K35", " 00", "M78", "M79" },
			{ "M00", "M00", "M00", "M00", "M00", "M00", "M00", "M00" },
			{ "M37", "M22", "M23", " 00", "M45", " 00", "M24", "M25" }, };

	static String GS1AIdent[] =
	// * AINx=Application Identifier (x=length of identifier)
	// ....Nxx = field 1 (N=numeric/X=alpha, xx = fixed length)
	// .......Xxx = field 2 (N=numeric/X=alpha, xx = max length)
	{ "01N2N14 00", /* Global Trade Item Number */
			"15N2N06 00", /* best before date */
			"17N2N06 00", /* expiration date */
			"21N2 00 20", /* serial number */
			"31N4N06 00", /* trade measures */
	};

	int parseGS1(String[] result, int ind) {
		int size = -1, hdr = 4 + 3; /* RDRn]e0 */
		String rule = "??N2 00 99"; /* default = not in table */

		if (label.charAt(4) != ']')
			return 0;
		if ((ind += hdr) + 2 >= label.length())
			return 0;
		String id = label.substring(ind, ind + 2);
		while (++size < GS1AIdent.length) {
			if (GS1AIdent[size].startsWith(id)) {
				rule = GS1AIdent[size];
			}
		}
		ind += size = rule.charAt(3) & 15;
		result[0] = label.substring(ind - size, ind); /* application identifier */
		ind += size = Integer.parseInt(rule.substring(5, 7));
		result[1] = label.substring(ind - size, ind); /* data part 1 (fixed size) */
		size = Integer.parseInt(rule.substring(8, 10));
		if (size > 0) /* data part 2 (terminated by GS) */
		{
			int last = label.indexOf(0x1d, ind);
			if (last < 0)
				last = label.length();
			result[2] = label.substring(ind, last);
			if (rule.charAt(7) == 'N')
				result[2] = leftFill(result[2], size, '0');
			if (rule.charAt(7) == 'X')
				result[2] = rightFill(result[2], size, ' ');
			if (last < label.length())
				last++;
			ind = last;
		} else
			result[2] = "";
		return ind - hdr;
	}
}
