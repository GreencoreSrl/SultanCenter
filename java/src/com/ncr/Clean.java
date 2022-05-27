package com.ncr;

abstract class Clean extends Basis {
	static boolean isSuited(int spf1) {
		int msk = options[O_Clean];
		return ((spf1 ^ itm.spf1) & ~msk) == 0;
	}

	static boolean combiItm() {
		Itemdata ptr = itm;

		if (itm.spf2 > 0 || itm.prm > 0)
			return false;
		while ((ptr = tra.vItems.getElement('S', ptr.index, 1)) != null) {
			if (ptr.prm > 0)
				continue;
			if (!isSuited(ptr.spf1))
				continue;
			if (ptr.spf2 != itm.spf2)
				continue;
			if (ptr.rcd != itm.rcd)
				continue;
			if (ptr.dpt_nbr != itm.dpt_nbr)
				continue;
			if (!ptr.number.equals(itm.number))
				continue;
			if (ptr.price != itm.price)
				continue;
			if (ptr.prpov != itm.prpov)
				continue;
			if (ptr.prpos != itm.prpos)
				continue;
			ptr.spf1 &= ~options[O_Clean];
			ptr.qty = Math.abs(ptr.cnt += itm.cnt);
			ptr.amt += itm.amt;
			return true;
		}
		return false;
	}

	static boolean combiCrd() {
		Itemdata ptr = itm;

		if (itm.spf2 > M_REBATE || itm.prm > 0)
			return false;
		while ((ptr = tra.vItems.getElement('C', ptr.index, 1)) != null) {
			if (ptr.prm > 0)
				continue;
			if (!isSuited(ptr.spf1))
				continue;
			if (ptr.spf2 != itm.spf2)
				continue;
			if (ptr.dpt_nbr != itm.dpt_nbr)
				continue;
			if (!ptr.number.equals(itm.number))
				continue;
			if (!ptr.text.equals(itm.text))
				continue;
			ptr.spf1 &= ~options[O_Clean];
			ptr.qty = Math.abs(ptr.cnt += itm.cnt);
			ptr.amt += itm.amt;
			return true;
		}
		return false;
	}

	static boolean combiPnt() {
		Itemdata ptr = itm;

		while ((ptr = tra.vItems.getElement('G', ptr.index, 1)) != null) {
			if (!isSuited(ptr.spf1))
				continue;
			if (ptr.spf3 != itm.spf3)
				continue;
			if (ptr.cmp_nbr != itm.cmp_nbr)
				continue;
			if (!ptr.number.equals(itm.number))
				continue;
			if (!ptr.text.equals(itm.text))
				continue;
			ptr.pnt += itm.pnt;
			return true;
		}
		return false;
	}

	static void print() {
		tra.slip &= ~0x40;

		if ((tra.spf3 & 4) > 0)
			prtDwide(ELJRN + 2, Mnemo.getMenu(46));
		if (tra.res > 0)
			prtLine.init(Mnemo.getMenu(47)).book(2);
		if (tra.spf2 > 0)
			GdCusto.cus_print(2);

		for (int ind = 0; ind < tra.vItems.size(); ind++) {
			itm = tra.vItems.getElement(ind);
			if (itm.id == 'S') {
				String[] promos = Promo.getItemRewardLines(true, itm.index, true);
				if (promos == null) {
					if (combiItm())
						continue;
				}
				if (itm.cnt == 0)
					continue;
				GdSales.itm_print(2);
				while (promos != null) {
					for (int line = 0; line < promos.length; line++)
						prtLine.init(promos[line]).book(2);
					promos = Promo.getItemRewardLines(false, itm.index, true);
				}
			}
			if (itm.id == 'C') {
				if (combiCrd())
					continue;
				if (itm.amt == 0)
					continue;
				if ((itm.spf2 & 0x70) > 0)
					GdSales.crd_print(2);
				else
					GdSales.itm_edit().book(2);
			}
			if (itm.id == 'G') {
				if (combiPnt())
					continue;
				if (itm.pnt == 0)
					continue;
				if (!Promo.isNoPrintPoints())    //NOPRINTPOINTS-CGA#A
					prtLine.init(itm.text).onto(20, editPoints(itm.pnt, false)).book(2);
			}
			if (itm.id == 'Q') {
				prtLine.init(itm.text).book(2);
			}
		}
	}
}