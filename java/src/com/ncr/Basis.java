package com.ncr;

/*******************************************************************
 * basic set of useful references and subroutines
 *******************************************************************/
abstract class Basis extends Table {
	/** gui provider **/
	static GdPos panel;
	/** console input/echo control **/
	static final ConIo input = new ConIo(20);
	/** event table access **/
	static final Motor event = new Motor("EVENT");
	/** connection to server / a2a services **/
	static final NetIo netio = new NetIo("NET", S_CKR);

	/***************************************************************************
	 * print message block
	 *
	 * @param station
	 *            selection of print device
	 * @param blk
	 *            array of message strings
	 * @param ind
	 *            index of first message to print
	 * @param end
	 *            index of last message plus 1
	 ***************************************************************************/
	static void prtBlock(int station, String blk[], int ind, int end) {
		for (; ind < end; ind++) {
			if (blk[ind] == null)
				continue;
			prtLine.init(blk[ind]).type(station);
		}
	}

	/***************************************************************************
	 * print doublewide
	 *
	 * @param station
	 *            selection of print device
	 * @param data
	 *            message string
	 ***************************************************************************/
	static void prtDwide(int station, String data) {
		prtLine.init('>' + data).type(station);
	}

	/***************************************************************************
	 * display and print abort message
	 *
	 * @param memo
	 *            index of message within ERROR table
	 ***************************************************************************/
	static void prtAbort(int memo) {
		dspLine.init(Mnemo.getInfo(memo));
		prtDwide(ELJRN + 3, dspLine.toString());
		tra.mode = M_CANCEL;
	}

	/***************************************************************************
	 * display and print receipt title
	 *
	 * @param menu
	 *            index of message within MENUS table
	 ***************************************************************************/
	static void prtTitle(int menu) {
		String txt = Mnemo.getMenu(menu);
		dspLine.init(txt);
		prtDwide(ELJRN + 3, txt);
		TView.append('>', 0x00, txt, "", "", "", "");
	}

	/***************************************************************************
	 * print stub receipts from local file
	 *
	 * @param sel
	 *            selection of print device
	 * @param name
	 *            file name (extension TMP --> BAK)
	 ***************************************************************************/
	static void prtForm(int sel, String name) {
		int ind = 1, rec = 1;
		String cpy = "$copies=";

		lBOF.open(null, name + ".TMP", 0);
		if (lBOF.file == null)
			return;
		while (lBOF.read(lBOF.recno = rec) > 0) {
			if (!lBOF.pb.startsWith(cpy))
				lBOF.recno--;
			else
				ind = lBOF.skip(cpy.length()).scanNum(1);
			if (ind < 1)
				break;
			for (int dev = sel; ind-- > 0; dev = 2) {
				panel.display(2, center(name, 20, '*'));
				if (dev == 4)
					DevIo.slpInsert(0);
				else if (dev == 2)
					prtLine.init(' ').type(dev);
				for (rec = lBOF.recno + 1; lBOF.read(rec) > 0; rec++) {
					if (lBOF.pb.startsWith(cpy))
						break;
					prtLine.init(lBOF.pb).type(dev);
				}
				if (dev == 4)
					DevIo.slpRemove();
				else if (dev == 2)
					GdRegis.hdr_print();
			}
		}
		lBOF.close();
		localMove(lBOF.pathfile, localFile(null, name + ".BAK"));
	}

	static void prtDline(String key) {
		if (lDBL.find(key) < 1)
			return;
		String txt = lDBL.pb.substring(8);
		prtLine.init(txt).book(3);
		if (key.charAt(0) > 'P')
			TView.append(' ', 0x02, txt, "", "", "", "");
	}

	/***************************************************************************
	 * update and show slip preselection status
	 *
	 * @param fnc
	 *            0=reset trx, 1=toggle trx, 2=reset tnd, 3=toggle tnd
	 * @param value
	 *            bit mask to be reset or toggled
	 ***************************************************************************/
	static void slpStatus(int fnc, int value) {
		switch (fnc) {
		case 0:
			tra.slip |= value;
		case 1:
			tra.slip ^= value;
			break;
		case 2:
			tra.tslp |= value;
		case 3:
			tra.tslp ^= value;
			break;
		}
		panel.dspStatus(4, null, (tra.slip & 1) > 0, tra.tslp > 0);
		DevIo.oplSignal(2, tra.tslp > 0 ? 2 : tra.slip & 1);
	}

	/***************************************************************************
	 * compute package price
	 *
	 * @param ptr
	 *            reference to all item properties
	 * @param price
	 *            price related to one unit
	 * @return package price of the item (rounded if weight item)
	 ***************************************************************************/
	static long set_price(Itemdata ptr, long price) {
		if (ptr.ext > 0)
			return price;
		price = roundBy(ptr.unit * price, 10);
		return roundBy(ptr.dec * price, 1000);
	}

	/***************************************************************************
	 * compute employee/customer discount
	 *
	 * @return discount of current item
	 ***************************************************************************/
	static long getDiscount() {
		int rate = (tra.spf2 & M_EMPDSC) > 0 ? rbt[itm.sit].rate_empl : rbt[itm.sit].rate_cust;

		if (tra.rate < 1 || rate < 1)
			return 0;
		if (rate > tra.rate)
			rate = tra.rate;

		return -roundBy(itm.amt * rate, 1000);
	}

	/***************************************************************************
	 * convert current time to seconds
	 *
	 * @return hh * 3600 + mm * 60 + ss
	 ***************************************************************************/
	static int sec_time() {
		int mm = ctl.time / 100;
		return (mm / 100 * 60 + mm % 100) * 60 + ctl.time % 100;
	}

	/***************************************************************************
	 * time elapsed in seconds
	 *
	 * @param start
	 *            start time in seconds
	 * @return current time minus start time in seconds
	 ***************************************************************************/
	static long sec_diff(int start) {
		long value = sec_time() - start;
		if (value < 0)
			value += 24 * 60 * 60;
		return value;
	}

	/***************************************************************************
	 * transaction preselect code for IDC
	 *
	 * @return preselect code (empl/cust/void)
	 ***************************************************************************/
	static int trx_pres() {
		int code = (tra.spf1 & M_TRVOID) > 0 ? 4 : 0;
		return code | tra.spf2;
	}

	/***************************************************************************
	 * edit plu/sku number for receipt print
	 *
	 * @param nbr
	 *            plu/sku number string
	 * @param special
	 *            true=on special sale
	 * @return plu/sku edited according to options
	 ***************************************************************************/
	static String editIdent(String nbr, boolean special) {
		StringBuffer sb = new StringBuffer(17);
		if ((options[O_ItmPr] & 0x40) == 0) {
			sb.append(editTxt(nbr, 16)).append(' ');
			for (int ind = 0; ind < sb.length(); ind++)
				if (sb.charAt(ind) == ' ')
					sb.setCharAt(ind, '#');
		} else
			sb.append(nbr.trim()).append(' ');
		if (special)
			sb.setCharAt(sb.length() - 1, '*');
		return sb.toString();
	}

	/***************************************************************************
	 * edit bonus-points
	 *
	 * @param value
	 *            number of bonus-points
	 * @param balance
	 *            true=edit as balance
	 * @return bonus-points edited
	 ***************************************************************************/
	static String editPoints(int value, boolean balance) {
		String s = Integer.toString(value);
		if (balance)
			return '*' + s + '*';
		return value < 0 ? s : '+' + s;
	}

	/***************************************************************************
	 * edit sales price
	 *
	 * @param price
	 *            sales price
	 * @return international currency symbol combined with price
	 ***************************************************************************/
	static String editPrice(long price) {
		return tnd[0].symbol + editDec(price, tnd[0].dec);
	}

	/***************************************************************************
	 * edit percent rate
	 *
	 * @param rate
	 *            percent rate (one decimal assumed)
	 * @return percent rate edited with percent symbol
	 ***************************************************************************/
	static String editRate(int rate) {
		StringBuffer sb = new StringBuffer(editDec(rate, 1));
		while (sb.length() < 5)
			sb.insert(1, '0');
		return sb.append(dfs.getPercent()).toString();
	}

	/***************************************************************************
	 * edit monitary amount
	 *
	 * @param ind
	 *            tender number = index into array of tenders
	 * @param total
	 *            amount to be edited
	 * @return amount edited according to params and regional settings
	 ***************************************************************************/
	static String editMoney(int ind, long total) {
		int dec = tnd[ind].dec;
		StringBuffer sb = new StringBuffer(editDec(total, 0));
		if (dec > 0) {
			int len = dec + 2 - (options[O_EdAmt] >> 1 & 1);
			char c = ctl.mode > 0 ? '#' : dfs.getDecimalSeparator();
			while (sb.length() < len)
				sb.insert(1, dfs.getZeroDigit());
			sb.insert(sb.length() - dec, c);
		}
		if (tnd[ind].xsym > ' ')
			sb.insert(1, tnd[ind].xsym);
		return sb.toString();
	}

	/***************************************************************************
	 * edit reason code
	 *
	 * @param code
	 *            left part of access key to S_PLURCD.DAT (00 - 09)
	 * @return actual reason code edited in brackets
	 ***************************************************************************/
	static String editReason(int code) {
		int ind = rcd_tbl[code - 1];
		return ind < 1 ? "" : '[' + editNum(ind, 2) + ']';
	}

	/***************************************************************************
	 * edit version
	 *
	 * @param nbr
	 *            version number
	 * @param dot1
	 *            true = exe/org version, false = any number
	 * @return version edited (5 chars)
	 ***************************************************************************/
	static String editVersion(int nbr, boolean dot1) {
		return dot1 ? editNum(nbr / 10, 3) + "." + nbr % 10 : editTxt(nbr, 5);
	}

	/***************************************************************************
	 * deduce number from label being read (according to EANX rules)
	 *
	 * @param type
	 *            rule identifier (B=box, C=customer)
	 * @param max
	 *            maximum number of digits remaining
	 * @return 0=ok, >0=error index
	 ***************************************************************************/
	static int ean_reduce(char type, int max) {
		String ean = editTxt(input.pb, 16);
		String rule = ean_special(type, ean);
		if (rule != null) {
			if (rule.charAt(19) == '%')
				if (cdgCheck(ean, ean_weights, 10) > 0)
					return 9;
			for (int ind = 0; ind < 16; ind++) {
				char c = rule.charAt(4 + ind);
				if (c < ':') {
					if (CapillaryService.getInstance().isEnabled() && type == 'C') {
						int prepend = Integer.parseInt(rule.substring(2, 3));
						if (c < '0' || c > '9' || ind + prepend >= 16 || rule.charAt(4 + ind + prepend) != 'N') {
							continue;
						}
					} else {
						continue;
					}
				}
				if (c == 'Z')
					if (input.index == 0)
						if (ean.charAt(ind) == '0')
							continue;
				input.push(ean.charAt(ind));
			}
			if (input.index < 1)
				return 3;
			input.reset(input.toString(0, input.index));
		}
		return input.num > max ? 2 : 0;
	}

	/***************************************************************************
	 * find EANX rule for special ean/upc handling
	 *
	 * @param id
	 *            rule identifier (P=price/qty/weight, U=update cdg, S=set zero, Z=add leading zeroes, B=box,
	 *            C=customer)
	 * @param ean
	 *            ean/upc label being read or entered manually
	 * @return rule to be applied or null
	 ***************************************************************************/
	static String ean_special(char id, String ean) {
		int ind, nbr = ean_16spec.length;

		while (nbr > 0) {
			String rule = ean_16spec[--nbr];
			if (rule == null)
				continue;
			if (rule.charAt(0) != id)
				continue;
			for (ind = 0; ind < 16; ind++) {
				char c = rule.charAt(4 + ind);
				if (ean.charAt(ind) != c) {
					if (c == ' ' || ean.charAt(ind) == ' ')
						break;
					if (Character.isDigit(c))
						break;
				}
			}
			if (ind == 16)
				return rule;
		}
		return null;
	}

	/***************************************************************************
	 * validate date entered
	 *
	 * @param date
	 *            (yy)mmdd
	 * @return true=ok
	 ***************************************************************************/
	static boolean dat_valid(int date) {
		int dd = date % 100, mm = date / 100 % 100;
		if (dd > 31 || mm > 12)
			return false;
		return (dd > 0 && mm > 0);
	}

	/***************************************************************************
	 * deduce transaction id from label being read to reg/tran
	 *
	 * @return 0=ok, >0=error index
	 ***************************************************************************/
	static int tid_reduce() {
		int sel = ctl.reg_nbr;

		if (input.num == 13) {
			if (input.scan() != '1')
				return 5;
			if (cdgCheck(input.pb, ean_weights, 10) > 0)
				return 9;
			if (!dat_valid(input.scanNum(4)))
				return 47;
			input.reset(input.scan(7));
		} else if (input.num > 7)
			return 2;
		if (input.num > 4) {
			if ((sel = input.scanHex(input.num - 4)) < 1)
				return 8;
		}
		input.reset(editKey(sel, 3) + input.scan(4));
		return 0;
	}

	/***************************************************************************
	 * show data on customer linedisplay and cid
	 *
	 * @param line
	 *            line number (0 - 3)
	 * @param data
	 *            message (20 characters)
	 ***************************************************************************/
	static void cusDisplay(int line, String data) {
		if (ctl.mode > 0)
			return;
		if (line < 1)
			mon.adv_rec = -1;
		DevIo.cusDisplay(line, data);
		panel.cid.display(line, data);
	}

	/***************************************************************************
	 * show toggling data on operator linedisplay
	 *
	 * @param line
	 *            bit0=line, bit1=start toggle if set
	 * @param msg
	 *            message (20 characters) or null to stop
	 ***************************************************************************/
	static void oplToggle(int line, String msg) {
		if (line > 1)
			mon.odisp = line &= 1;
		if ((mon.opd_alt = msg) == null) {
			if (mon.odisp < 2)
				DevIo.oplDisplay(line, panel.dspArea[line + 1].getText());
			mon.odisp = ERROR;
		} else
			DevIo.oplDisplay(line, msg);
	}

	/***************************************************************************
	 * accept number from operator
	 *
	 * @param hdr
	 *            title
	 * @param txt
	 *            index of prompt in mnemo table
	 * @param min
	 *            min number of digits
	 * @param max
	 *            max number of digits
	 * @param msk
	 *            asterisks in prompt
	 * @param dec
	 *            digits after decimal point
	 * @return true=Enter, false=Clear
	 ***************************************************************************/
	static boolean acceptNbr(String hdr, int txt, int min, int max, int msk, int dec) {
		for (int sts;; panel.clearLink(Mnemo.getInfo(sts), 1)) {
			input.prompt = Mnemo.getText(txt);
			input.init(0x00, max, msk, dec);
			ModDlg dlg = new ModDlg(hdr);
			oplToggle(2, hdr);
			dlg.show("NBR");
			oplToggle(0, null);
			if ((sts = dlg.code) > 0)
				continue;
			if (input.key == 0)
				input.key = input.CLEAR;
			if (input.key == input.CLEAR) {
				dspLine.init("").show(1);

				return false;
			}
			if (input.num < 1 || input.key != input.ENTER)
				sts = 5;
			else
				sts = input.adjust(input.pnt);
			if (sts != 0)
				continue;
			if (input.num >= min)
				return true;
			sts = 3;
		}
	}
}
