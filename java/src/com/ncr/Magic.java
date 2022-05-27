package com.ncr;

abstract class Magic extends Basis {

	/*******************************************************************/
	/*                                                                 */
	/* resume a transaction from todays item data capture */
	/* with receipt number = terminal / transaction number */
	/*                                                                 */
	/*******************************************************************/

	static int src_frec(int tran, int sel) {
		int top = 0, mid, end = lIDC.getSize();

		if (sel > 0) {
			if (ctl.lan > 0)
				return ERROR;
			if ((mid = netio.findTra(tran, sel)) < 1)
				return mid;
			if (lIDC.read(mid, sel) < 1)
				return ERROR;
			return mid;
		}
		if (tran < ctl.tran)
			tran += 10000;
		while ((mid = (top + end) >> 1) < end) {
			if (lIDC.read(++mid) < 1)
				break;
			int nbr = lIDC.skip(23).scanNum(4);
			if (nbr < ctl.tran)
				nbr += 10000;
			if (nbr == tran && lIDC.pb.charAt(32) == 'F') {
				lIDC.index = 0;
				return mid;
			}
			if (tran < nbr)
				end = mid - 1;
			else
				top = mid;
		}
		return 0;
	}

	static int src_hrec(int tran, int sel) {
		int nbr, rec;

		if ((rec = src_frec(tran, sel)) < 1)
			return rec;
		if ((nbr = lIDC.skip(28).scanNum(3)) < 1)
			nbr = 1000;
		for (--nbr; (rec -= nbr) > 0; nbr = 1000) {
			if (lIDC.read(rec, sel) < 1)
				return ERROR;
			if (tran != lIDC.skip(23).scanNum(4))
				break;
			if (lIDC.pb.charAt(32) != 'H')
				continue;
			lIDC.index = 0;
			return rec;
		}
		return 0;
	}

	static void IDC_scan() {
		char spec;
		int spf2, spf3;

		switch (lIDC.pb.charAt(32)) {
		case 'H':
		case 'F':
			dct.mode = lIDC.skip(34).scanNum(1);
			dct.spf2 = lIDC.scanNum(1);
			dct.spf3 = lIDC.scanNum(1);
			dct.number = lIDC.skip(6).scan(16);
			dct.code = lIDC.scan(':').scanNum(2);
			dct.cnt = (int) lIDC.scanDec(6);
			dct.amt = lIDC.scanDec(10);
			dct.spf1 = (dct.spf2 & 4) > 0 ? M_TRVOID : 0;
			if (dct.code == 3)
				dct.spf1 |= M_TRRTRN;
			dct.spf2 &= 3;
			break;
		case 'B':
			dci.stat = lIDC.skip(36).scanNum(1);
			dci.number = lIDC.skip(6).scan(16);
			dci.cnt = (int) lIDC.skip(3).scanDec(6);
			dci.amt = lIDC.scanDec(10);
			break;
		case 'P':
			dci.stat = lIDC.skip(36).scanNum(1);
			dct.slm_nbr = lIDC.scan(':').scanKey(4);
			dci.number = lIDC.scan(':').scan(16);
			dci.prcom = lIDC.skip(10).scanNum(9);
			break;
		case 'Q':
			spf2 = lIDC.skip(36).scanNum(1);
			if (spf2 == 1) {
				spf3 = lIDC.skip(19).scanNum(1);
				if (spf3 > 0)
					rcd_tbl[spf3 - 1] = lIDC.scanNum(2);
			}
			if (spf2 == 2)
				cus.setAge(lIDC.skip(33).scanNum(8));
			break;
		case 'S':
		case 'I':
		case 'X':
		case 'R':
		case 'L':
			dct.mode = lIDC.skip(34).scanNum(1);
			dci.spf1 = lIDC.scanNum(1);
			dci.stat = lIDC.scanNum(1);
			dci.dpt_nbr = lIDC.scan(':').scanKey(4);
			dci.number = lIDC.scan(':').scan(16);
			dci.spec = lIDC.scan();
			dci.qty = lIDC.scanNum(4);
			if (lIDC.pb.charAt(lIDC.index) == '.') {
				dci.dec = lIDC.skip().scanNum(3) + dci.qty * 1000;
				dci.prm++;
				dci.qty = 1;
			} else
				dci.unit = lIDC.scanNum(4);
			spec = lIDC.scan();
			if (spec != '*' && spec != '/')
				dci.ext = 1;
			dci.price = lIDC.scanNum(9);
			if (dci.spf1 > 0) {
				dci.spf1 = 256 >> dci.spf1 & ~dct.spf1;
				if (dct.spf1 > 0)
					if (dci.spf1 > 0)
						dci.spf1 ^= M_ERRCOR;
				if (dci.spec != '-')
					dci.spf1 ^= M_ERRCOR;
			}
			break;
		case 'W':
			spf2 = lIDC.skip(35).scanNum(1);
			if (spf2 == 0) {
				dci.sit = lIDC.skip(2).scanNum(1);
				dci.vat = lIDC.scanNum(1);
				dci.cat = lIDC.scanNum(2);
				dci.eanupc = lIDC.scan(':').scan(16);
				dci.mmt = lIDC.scan(':').scanNum(2);
				dci.flag = lIDC.scan(':').scanHex(2);
				dci.flg2 = lIDC.scanHex(2);
				dci.ages = lIDC.scanNum(1);
			} else
				dci.serial = lIDC.skip(7).scan(35).trim();
			break;
		case 'D':
			dci.spf3 = lIDC.skip(35).scanNum(1);
			dci.sit = lIDC.scanNum(1);
			dci.dpt_nbr = lIDC.scan(':').scanKey(4);
			dci.rate = lIDC.scan(':').scanRate(16);
			dci.cnt = (int) lIDC.skip(3).scanDec(6);
			dci.crd = lIDC.scanDec(10);
			break;
		case 'G':
			dci.spf3 = lIDC.skip(35).scanNum(1);
			dci.spf2 = lIDC.scanNum(1);
			dci.cmp_nbr = lIDC.scan(':').scanKey(4);
			dci.number = lIDC.scan(':').scan(16);
			dci.pnt = (int) lIDC.skip(3).scanDec(6);
			dci.amt = lIDC.scanDec(10);
			break;
		case 'K':
			dci.flag = lIDC.skip(36).scanNum(1);
			dci.dpt_nbr = lIDC.scan(':').scanKey(4);
			dci.promo = lIDC.scan(':').scan(16);
			dci.rew_qty = (int) lIDC.skip(3).scanDec(6);
			dci.rew_amt = lIDC.scanDec(10);
			break;
		case 'C':
			spf2 = lIDC.skip(35).scanNum(1);
			spf3 = lIDC.scanNum(1);
			if (spf2 > 4)
				break;
			dci.dpt_nbr = lIDC.scan(':').scanKey(4);
			dci.number = lIDC.scan(':').scan(16);
			dci.spec = lIDC.scan();
			dci.qty = lIDC.scanNum(4);
			if (lIDC.pb.charAt(lIDC.index) == '.') {
				dci.dec = lIDC.skip().scanNum(3) + dci.qty * 1000;
				dci.prm++;
				dci.qty = 1;
			} else
				dci.unit = lIDC.scanNum(4);
			spec = lIDC.scan();
			dci.prcrd = lIDC.scanNum(9);
			if (spec == '>' ^ spec != dci.spec)
				dci.prcrd = 0 - dci.prcrd;
			if (spf2 == 0) {
				dci.spf3 = spf3 - 1;
				dci.prpov = dci.price + dci.prcrd;
				break;
			}
			dci.spf2 = 256 >> spf2;
			if (dci.spf2 == M_PRCRED) {
				dci.prpos = (dci.prpov != 0 ? dci.prpov : dci.price) + dci.prcrd;
				break;
			}
			dci.crd = dci.prcrd;
		}
	}

	static void nxt_item(int sts) {
		if (sts > 0) {
			dspLine.init(' ').show(1);
			panel.clearLink(Mnemo.getInfo(sts), 0x81);
			pit = null;
			return;
		}
		dspLine.show(1);
		oplLine.show(2);
		// idle_loop ();
	}
}
