package com.ncr;

abstract class FiscalId extends Basis {
	static byte values[] =
	/* 0, 1, 2, 3, 4, 5, 6 ,7, 8, 9 */
	{ 1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23 };
	/* A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z */

	static boolean isValid(String fid) {
		int ind = 0, val;

		for (int cdg = 0;; cdg += val) {
			char type = "AAAAAA00A00A000A".charAt(ind);
			val = fid.charAt(ind++) - type;
			if (val < 0 || val > (type == 'A' ? 25 : 9))
				return false;
			if ((ind & 1) > 0)
				val = values[val];
			if (ind == fid.length())
				return val == cdg % values.length;
		}
	}

	static int accept(int len) {
		stsLine.init(Mnemo.getText(74)).show(1);
		input.prompt = "";
		input.init(0x10, len, len, 0);
		input.alpha = kbd_alpha[3]; /* DABC3 = keyboard for Fiscal Id */
		for (int sts;; panel.clearLink(Mnemo.getInfo(sts), 0x81)) {
			AbcDlg dlg = new AbcDlg("Alphanumeric Keyboard");
			input.reset(input.pb);
			dlg.show("ABC");
			if ((sts = dlg.code) < 1) {
				if (input.key == 0)
					input.key = input.CLEAR;
				if (input.num < 1 || input.key != input.ENTER)
					sts = 5;
			}
			if (input.key == input.CLEAR)
				return sts;
			if (input.num < input.max)
				sts = 3;
			if (sts > 0)
				continue;
			if (isValid(input.pb))
				return 0;
			sts = 9;
		}
	}
}
