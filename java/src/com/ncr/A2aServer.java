package com.ncr;

/*******************************************************************
 *
 * The file access methods to remote files need a2a services. Before such methods can be used, the reference to the
 * provider (normally NetIo) has to be set in FmtIo.net.
 *
 *******************************************************************/
interface A2aServer {
	/***************************************************************************
	 * read record from remote hash files (plu, clu, ...)
	 *
	 * @param type
	 *            R = read (plu/clu/sar), I = sales inquiry (pls/cls)
	 * @param key
	 *            record access key (correct size)
	 * @param io
	 *            LinIo object for parsing
	 * @return status (-1=offl 0=not found >0=length of parse buffer)
	 ***************************************************************************/
	int readHsh(char type, String key, LinIo io);

	/***************************************************************************
	 * read remote sequential files (idc, jrn)
	 *
	 * @param rec
	 *            record number (1, 2, ...)
	 * @param sel
	 *            terminal number selected
	 * @param io
	 *            LinIo object for parsing
	 * @return status (-1=offl 0=EoF >0=length of parse buffer)
	 ***************************************************************************/
	int readSeq(int rec, int sel, LinIo io);

	/***************************************************************************
	 * read remote sales data files (act, dpt, pot, reg, slm, lan, ctl, new)
	 *
	 * @param rec
	 *            record number (1, 2, ...)
	 * @param sel
	 *            selection of terminal/group/all
	 * @param io
	 *            LinIo object for parsing
	 * @return status (-1=offl 0=EoF >0=length of parse buffer)
	 ***************************************************************************/
	int readSls(int rec, int sel, LinIo io);

	/***************************************************************************
	 * write sales total block to mirror image file (act, dpt, pot, reg, slm)
	 *
	 * @param rec
	 *            record number (1, 2, ...), 0 = flush / send incomplete msg buffer
	 * @param blk
	 *            number of total block within data record (0, 1, ...)
	 * @param io
	 *            SlsIo object holding key and total block
	 ***************************************************************************/
	void writeSls(int rec, int blk, SlsIo io);

	/***************************************************************************
	 * write / update data record in news file (new)
	 *
	 * @param rec
	 *            record number (1, 2, ...)
	 * @param io
	 *            LinIo object holding data record
	 * @return status (-1=offl 0=error >0=length of parse buffer)
	 ***************************************************************************/
	int updNews(int rec, LinIo io);
}
