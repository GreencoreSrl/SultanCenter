package com.ncr;

import java.awt.*;

public class BarCode extends Canvas {
	int bars[];
	String nmbr;

	static String ean13_mask[] = { "AAAAAA", "AABABB", "AABBAB", "AABBBA", "ABAABB", /* 1st digit = 0 - 4 */
			"ABBAAB", "ABBBAA", "ABABAB", "ABABBA", "ABBABA", /* 1st digit = 5 - 9 */
	};
	static String upc_E_mask[] = { "BBBAAA", "BBABAA", "BBAABA", "BBAAAB", "BABBAA", /* chk digit = 0 - 4 */
			"BAABBA", "BAAABB", "BABABA", "BABAAB", "BAABAB", /* chk digit = 5 - 9 */
	};
	static byte bar_code[][] = { { 0x0D, 0x19, 0x13, 0x3D, 0x23, 0x31, 0x2F, 0x3B, 0x37, 0x0B }, /* A */
			{ 0x27, 0x33, 0x1B, 0x21, 0x1D, 0x39, 0x05, 0x11, 0x09, 0x17 }, /* B */
			{ 0x72, 0x66, 0x6C, 0x42, 0x5C, 0x4E, 0x50, 0x44, 0x48, 0x74 }, /* C */
	};
	static String code39 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
	static int bar_code39[] = { 0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064,
			0x109, /* 0 1 2 3 4 5 6 7 8 9 A */
			0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C, 0x103, 0x043, /* B C D E F G H I J K L */
			0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016, 0x181, 0x0C1, 0x1C0, /* M N O P Q R S T U V W */
			0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, 0x0A8, 0x0A2, 0x08A, 0x02A, /* X Y Z - . * $ / + % */
	};

	static int bar_ctrl[][] = { /* ean13 */ { 8 + 42 + 5 + 42 + 8, 7 }, /* upc A */ { 8 + 42 + 5 + 42 + 8, 1, 6, 11 },
			/* 11 */ { 0 }, /* 10 */ { 0 }, /* PARAF */ { 4 + 13 + 6 * 13 + 12 + 4 },
			/* ean 8 */ { 8 + 28 + 5 + 28 + 8, 0, 4 }, /* upc E */ { 8 + 42 + 3 + 0 + 9, 7 }, };

	private void setBars39(String data) {
		int bar = 0, pos = 4;

		data = '*' + data + '*';
		for (int ind = 0; ind < data.length(); ind++) {
			int code = code39.indexOf(data.charAt(ind));
			if (code >= 0)
				code = bar_code39[code] << 1;
			for (int msk = 0x200; msk > 0; msk >>= 1) {
				bars[pos++] = bar ^= 52;
				if ((code & msk) > 0)
					bars[pos++] = bar;
			}
		}
	}

	private Dimension getCharSize() {
		Font f = getFont();
		FontMetrics fm = getFontMetrics(f);
		return new Dimension(fm.charWidth(' ') >> 2, f.getSize() + 2 >> 2);
	}

	public Dimension getPreferredSize() {
		Dimension d = getCharSize();
		d.width *= bars.length;
		d.height *= 18;
		return d;
	}

	public String getText() {
		return nmbr;
	}

	public void paint(Graphics g) {
		Dimension chr = getCharSize(), d = getSize();
		int x = d.width - chr.width * bars.length >> 1;
		int y = d.height - chr.height * 18 >> 1;
		g.setColor(Color.white);
		g.fillRoundRect(x, y, chr.width * bars.length, chr.height * 18, chr.width << 2, chr.height << 1);
		g.setColor(getForeground());
		for (int ind = 0; ind < bars.length; ind++)
			g.fillRect(x + ind * chr.width, y + chr.height, chr.width, chr.height * bars[ind] >> 2);
		x += chr.width;
		y += chr.height * 17;
		for (int ind = 0; ind < nmbr.length(); ind++) {
			g.drawString(nmbr.substring(ind, ind + 1), x + chr.width * ind * 7, y);
		}
	}

	void setBars(String ean) {
		int size = ean.length();

		nmbr = ean;
		bars = new int[size * 7];
		if (size < 8 || size == 10 || size == 11 || size > 13)
			return;
		if (size == 8) {
			if (ean.charAt(0) == '0')
				size--;
		}
		int ctrl[] = bar_ctrl[13 - size];
		bars = new int[ctrl[0]];
		if (size == 9) {
			nmbr = "   A" + ean;
			setBars39(FmtIo.ipcBase32(ean));
			return;
		}
		char mask = 'A';
		StringBuffer sb = new StringBuffer(ean);
		int black = 56, dig, ind = 5;
		for (dig = ctrl.length; --dig > 0; sb.insert(ctrl[dig], ' '))
			;
		nmbr = sb.toString();
		bars[ind] = bars[ind + 2] = black;
		ind += 3;
		for (dig = size & 1; dig < size; dig++) {
			if (dig < 7)
				if (size == 13)
					mask = ean13_mask[ean.charAt(0) - '0'].charAt(dig - 1);
			if (size == 7)
				mask = upc_E_mask[ean.charAt(7) - '0'].charAt(dig - 1);
			int code = bar_code[mask - 'A'][ean.charAt(dig) - '0'];
			int high = size == 12 && (dig == 0 || dig == 11) ? 0 : 4;
			for (int bit = 0x80; (bit >>= 1) > 0; ind++)
				if ((code & bit) > 0)
					bars[ind] = black - high;
			if (dig == (size - 1) >> 1) {
				mask = 'C';
				if (size > 7) {
					bars[ind + 1] = bars[ind + 3] = black;
					ind += 5;
				}
			}
		}
		if (size == 7) {
			bars[ind + 1] = black;
			ind += 3;
		}
		bars[ind] = bars[ind + 2] = black;
	}
}
