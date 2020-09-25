package com.ncr;

abstract class GdScale extends Basis {
	static boolean isKg() {
		return (options[O_Scale] & 1) < 1;
	}

	static String getUnit() {
		return isKg() ? "kg" : "lb";
	}

	static String unitPrice(long price) {
		return editPrice(price) + '/' + getUnit();
	}

	static String editWeight(int weight) {
		StringBuffer sb = new StringBuffer(editDec(weight, 3));
		if (!isKg())
			sb.setCharAt(sb.length() - 1, ' ');
		return sb.append(getUnit()).toString();
	}

	static void version() {
		int crc = version[3], sts;

		if (crc == 0)
			return;
		if ((version[2] = DevIo.scale.version) > 0) {
			prtLine.init(vrs_tbl[2]).upto(17, Integer.toString(version[2])).onto(20, vrs_tbl[3])
					.upto(37, editNum(crc, 5)).book(3);
			panel.display(1, prtLine.toString(20, 20));
			WghIo.setItemData(0, Mnemo.getMenu(20));
			for (;; panel.clearLink(Mnemo.getInfo(sts), 0x81)) {
				sts = get_weight(prtLine.toString(0, 20), true);
				if (input.key == input.CLEAR)
					sts = 0;
				if (sts > 0)
					continue;
				if (input.scanNum(input.num) == 0)
					break;
				sts = 33;
			}
		}
	}

	static boolean weight(String msg) {
		if (DevIo.scale.state == 0)
			return false;
		if (msg.charAt(3) == '1') /* weigh */
		{
			input.reset(msg.substring(4));
			return true;
		}
		if (msg.charAt(3) == '4') /* monitor */
		{
			String data = msg.charAt(4) == '4' ? msg.substring(5) : "";
			input.reset(input.leftFill(data, input.msk, '0'));
		}
		return false;
	}

	static private int get_weight(String info, boolean live) {
		int max = isKg() ? 5 : 4;

		input.prompt = Mnemo.getText(61);
		input.init(0x00, max, max, max - 2);
		if (DevIo.scale.version > 0) {
			input.reset(editNum(input.max = 0, max));
			WghIo.control(DevIo.scale.state = live ? 4 : 1);
		}
		WghDlg dlg = new WghDlg(info);
		dlg.show("WGH");
		//if (input.key == input.ENTER) { input.pb = "123"; input.num = 3; }	//TAU-20160816-SBE#A
		if (dlg.code < 0)
			input.key = input.ENTER;
		else
			WghIo.control(2);
		DevIo.scale.state = 0;
		if (dlg.code > 0)
			return dlg.code;
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.num < 1 || input.key != input.ENTER)
			return 5;

		return input.adjust(input.pnt);
	}

	static int weigh(Itemdata ptr) {
		int price = ptr.prpos, tare = tare_tbl[ptr.tare];
		boolean live = (options[O_Scale] & 0x10) > 0;

		panel.display(1, ptr.text);
		panel.dspPicture("DPT_" + editKey(ptr.dpt_nbr, 4));
		if (price < 1)
			price = ptr.prpov > 0 ? ptr.prpov : ptr.price;
		WghIo.setItemData(price, ptr.text);
		int sts = get_weight(unitPrice(price), live);
		if (sts > 0)
			return sts;
		ptr.dec = input.scanNum(input.num);
		if (!isKg()) {
			ptr.dec *= 10;
			tare *= 10;
		}
		if ((ptr.dec -= tare) < 1)
			return 8;
		if (set_price(ptr, price) < 1)
			return 8;
		return 0;
	}

	static void book(int station) {
		int tare = tare_tbl[itm.tare];

		if (tare > 0)
			if ((options[O_Scale] & 0x20) > 0) {
				prtLine.init(' ').upto(17, editWeight(itm.dec + tare) + " -").upto(27, editWeight(tare))
						.onto(28, Mnemo.getText(62)).book(station);
			}
		//TAU-20160816-SBE#A BEG
		if (itm.prlbl > 0) {
			prtLine.init(DevIo.scale.version > 0 ? "*/*" : "").upto(15, editWeight(itm.dec))
					.onto(16, Mnemo.getText(60).charAt(0)).onto(20, unitPrice(itm.prlbl)).book(station);
		} else {
			prtLine.init(DevIo.scale.version > 0 ? "*/*" : "").upto(15, editWeight(itm.dec))
					.onto(16, Mnemo.getText(60).charAt(0)).onto(20, unitPrice(itm.prpos)).book(station);
		}
		//TAU-20160816-SBE#A END
	}
}
