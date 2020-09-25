package com.ncr;

abstract class Delta extends Table {
	static String hdr;
	static Total block = new Total();

	/***************************************************************************
	 * write header and trailer record to sequential file DTL
	 *
	 * @param type
	 *            H = header, F = trailer
	 * @param info
	 *            date/time for header, record counter for trailer
	 ***************************************************************************/
	static void control(char type, long info) {
		if (type == 'H') {
			lDTL.recno = 0;
			hdr = editNum(ctl.sto_nbr, 4) + "+" + editNum(ctl.ckr_nbr, 3) + editKey(ctl.grp_nbr, 3) + "+"
					+ editKey(ctl.reg_nbr, 3) + editNum(ctl.tran, 4);
		}
		lDTL.init(':').push(type).skip().push(hdr).pushDec(info, 11);
		lDTL.write();
		if (type == 'F')
			lDTL.sync();
	}

	/***************************************************************************
	 * write sales totals (deltas) to sequential file DTL
	 *
	 * @param io
	 *            sales data file id and key
	 * @param blk
	 *            number of the total block within sales data record
	 * @param trans
	 *            transaction count
	 * @param items
	 *            item count
	 * @param total
	 *            sales total
	 ***************************************************************************/
	static void write(SlsIo io, int blk, int trans, int items, long total) {
		lDTL.init(' ').push(io.id.substring(0, 1)).push(editHex(blk, 1)).push(editKey(io.key, 4));
		block.trans = trans;
		block.items = items;
		block.total = total;
		block.edit(lDTL);
		lDTL.recno++;
		lDTL.write();
	}
}
