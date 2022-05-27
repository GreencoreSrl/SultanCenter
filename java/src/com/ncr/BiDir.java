package com.ncr;

import java.io.*;

class BiDir extends FmtIo {
	private static int mapping[][] = { /* at first, midst, last position */
			{ 0xA0, 0xA0, 0xA0 }, { 0xA1, 0xA1, 0xA1 }, { 0xC2, 0xA2, 0xA2 }, { 0xA3, 0xA3, 0xA3 },
			{ 0xA4, 0xA4, 0xA4 }, { 0xC3, 0xA5, 0xA5 }, { 0xA6, 0xA6, 0xA6 }, { 0xA7, 0xA7, 0xA7 },
			{ 0xC7, 0xA8, 0xA8 }, { 0xC8, 0xC8, 0xA9 }, { 0xCA, 0xCA, 0xAA }, { 0xCB, 0xCB, 0xAB },
			{ 0xAC, 0xAC, 0xAC }, { 0xCC, 0xCC, 0xAD }, { 0xCD, 0xCD, 0xAE }, { 0xCE, 0xCE, 0xAF },
			{ 0xB0, 0xB0, 0xB0 }, { 0xB1, 0xB1, 0xB1 }, { 0xB2, 0xB2, 0xB2 }, { 0xB3, 0xB3, 0xB3 },
			{ 0xB4, 0xB4, 0xB4 }, { 0xB5, 0xB5, 0xB5 }, { 0xB6, 0xB6, 0xB6 }, { 0xB7, 0xB7, 0xB7 },
			{ 0xB8, 0xB8, 0xB8 }, { 0xB9, 0xB9, 0xB9 }, { 0xE1, 0xE1, 0xBA }, { 0xBB, 0xBB, 0xBB },
			{ 0xD3, 0xD3, 0xBC }, { 0xD4, 0xD4, 0xBD }, { 0xD5, 0xD5, 0xBE }, { 0xBF, 0xBF, 0xBF },
			{ 0xC0, 0xC0, 0xC0 }, { 0xC1, 0xC1, 0xC1 }, { 0xC2, 0xA2, 0xA2 }, { 0xC3, 0xA5, 0xA5 },
			{ 0xC4, 0xC4, 0xC4 }, { 0xC5, 0xC3, 0xC3 }, { 0xC6, 0xC6, 0xC6 }, { 0xC7, 0xA8, 0xA8 },
			{ 0xC8, 0xC8, 0xA9 }, { 0xC9, 0xC9, 0xC9 }, { 0xCA, 0xCA, 0xAA }, { 0xCB, 0xCB, 0xAB },
			{ 0xCC, 0xCC, 0xAD }, { 0xCD, 0xCD, 0xAE }, { 0xCE, 0xCE, 0xAF }, { 0xCF, 0xCF, 0xCF },
			{ 0xD0, 0xD0, 0xD0 }, { 0xD1, 0xD1, 0xD1 }, { 0xD2, 0xD2, 0xD2 }, { 0xD3, 0xD3, 0xBC },
			{ 0xD4, 0xD4, 0xBD }, { 0xD5, 0xD5, 0xBE }, { 0xD6, 0xD6, 0xEB }, { 0xD7, 0xD7, 0xD7 },
			{ 0xD8, 0xD8, 0xD8 }, { 0xD9, 0xEC, 0xDF }, { 0xDA, 0xF7, 0xEE }, { 0xDB, 0xDB, 0xDB },
			{ 0xDC, 0xDC, 0xDC }, { 0xDD, 0xDD, 0xDD }, { 0xDE, 0xDE, 0xDE }, { 0xD9, 0xEC, 0xDF },
			{ 0xE0, 0xE0, 0xE0 }, { 0xE1, 0xE1, 0xBA }, { 0xE2, 0xE2, 0xF8 }, { 0xE3, 0xE3, 0xFC },
			{ 0xE4, 0xE4, 0xFB }, { 0xE5, 0xE5, 0xEF }, { 0xE6, 0xE6, 0xF2 }, { 0xE7, 0xF4, 0xF3 },
			{ 0xE8, 0xE8, 0xE8 }, { 0xE9, 0xE9, 0xE9 }, { 0xEA, 0xEA, 0xF5 }, { 0xD6, 0xD6, 0xEB },
			{ 0xD9, 0xEC, 0xDF }, { 0xED, 0xED, 0xED }, { 0xDA, 0xF7, 0xEE }, { 0xE5, 0xE5, 0xEF },
			{ 0xF0, 0xF0, 0xF0 }, { 0xF1, 0xF1, 0xF1 }, { 0xE6, 0xE6, 0xF2 }, { 0xE7, 0xF4, 0xF3 },
			{ 0xE7, 0xF4, 0xF3 }, { 0xEA, 0xEA, 0xF5 }, { 0xF6, 0xF6, 0xF6 }, { 0xDA, 0xF7, 0xEE },
			{ 0xE2, 0xE2, 0xF8 }, { 0xF9, 0xF9, 0xF9 }, { 0xFA, 0xFA, 0xFA }, { 0xE4, 0xE4, 0xFB },
			{ 0xE3, 0xE3, 0xFC }, { 0xFD, 0xFD, 0xFD }, { 0xFE, 0xFE, 0xFE }, { 0xFF, 0xFF, 0xFF } };

	static boolean isReverse() {
		return oem.equals("Cp864");
	}

	static String localize(String text) {
		if (isReverse())
			try {
				text = arabize(text);
			} catch (UnsupportedEncodingException e) {
			}
		return text;
	}

	private static boolean isTopChar(int prev) {
		int breakers[] = { 0x20, 0xA0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC7, 0xCF, 0xD0, 0xD1, 0xD2, 0xE8, };
		for (int ind = breakers.length; ind-- > 0;) {
			if (breakers[ind] == prev)
				return true;
		}
		return false;
	}

	private static String arabize(String text) throws UnsupportedEncodingException {
		int mode = 1; /* -1 = Arabic, 0 = space(s) after Arabic, 1 = Latin */
		int ind, top = 0, end = 0;
		byte[] data = oemBytes(text);
		String result = "";

		for (ind = 0; ind < text.length(); ind++) {
			if (data[ind] == ' ') {
				if (mode == 1)
					result += " ";
				if (mode == -1) {
					mode = 0;
					end = ind;
				}
			} else if (data[ind] > 0) /* Latin */
			{
				if (mode == -1) {
					result += reverse(data, top, ind);
					mode = 1;
				}
				if (mode == 0) {
					result += reverse(data, top, end);
					result += text.substring(end, ind);
					mode = 1;
				}
				if (mode == 1) {
					result += new String(data, ind, 1, oem);
				}
			} else /* Arabic */
			{
				if (mode == 1)
					top = ind;
				mode = -1;
			}
		}
		if (mode < 1) {
			if (mode == -1)
				end = ind;
			result += reverse(data, top, end);
			result += text.substring(end, ind);
		}
		return result;
	}

	private static String reverse(byte[] data, int ind, int end) throws UnsupportedEncodingException {
		int len = end - ind, type = 0;
		byte[] part = new byte[len];

		for (int code = ' '; ind < end; part[--len] = (byte) code) {
			int prev = code, rank = type;
			type = 1;
			code = data[ind++] & 0xff;
			int next = ind < end ? data[ind] & 0xff : ' ';
			if (next == ' ' || next == 0xA0)
				type = 2;
			else if (isTopChar(prev))
				type = 0;
			if (code >= 0xA0)
				code = mapping[code - 0xA0][type];
			if (prev == 0xE4)
				if (code == 0xA8 || code == 0xC3) {
					code = rank == 0 ? 0x9D : 0x9E;
					len++;
				}
		}
		String s = new String(part, len, part.length - len, oem);
		return rightFill(s, part.length, ' ');
	}

	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(args[0], "r");
		byte[] data = new byte[(int) raf.length()];

		if (raf.read(data) < data.length)
			throw new IOException("format error");
		String s = new String(data, oem);
		System.out.println("UniCode data with size=" + s.length());
		for (int i = 0; i < s.length(); i++)
			System.out.print(editHex(s.charAt(i), 4) + " ");
		System.out.println();
		data = oemBytes(BiDir.localize(s));
		System.out.println("contextualized OEM/DOS " + oem);
		for (int i = 0; i < s.length(); i++) {
			System.out.print(editHex(data[i] & 0xff, 2) + " ");
		}
		System.out.println();
	}
}