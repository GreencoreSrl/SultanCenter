package com.ncr;

import java.io.*;
import java.util.*;

/*******************************************************************
 *
 * Access to local data files (record data is based on 8-bit oem codepages) (record size is fixed) (record separator is
 * hex 0d0A)
 *
 *******************************************************************/
class DatIo extends LinIo {
	/**
	 * size of access key or record header (depending on file type)
	 **/
	int fixSize;
	/**
	 * byte array holding oem data after read
	 **/
	byte record[];
	/**
	 * abstract path/file name
	 **/
	File pathfile;
	/**
	 * reference to underlying io service object
	 **/
	RandomAccessFile file;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param fixSize
	 *            size of access key or record header
	 * @param recSize
	 *            record size in bytes including separators CR/LF
	 ***************************************************************************/
	DatIo(String id, int fixSize, int recSize) {
		super(id, 0, recSize - 2);
		this.fixSize = fixSize;
		record = new byte[recSize];
	}

	/***************************************************************************
	 * read data record and prepare subsequent parse functions (unicode string in parse buffer pb, index to zero)
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec) {
		if (file == null)
			return 0;
		while (true) {
			try {
				int len = record.length;
				file.seek((rec - 1l) * len);
				file.readFully(record);
				if (record[--len] == 0x0a)
					if (record[--len] == 0x0d) {
						pb = new String(record, index = 0, len, oem);
						return len;
					}
				error(new IOException("record " + rec + ": size"), true);
			} catch (EOFException e) {
				return 0;
			} catch (IOException e) {
				error(e, false);
			}
		}
	}

	/***************************************************************************
	 * convert unicode buffer (0 to index) to oem data and overwrite in file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param off
	 *            target offset within record on file
	 ***************************************************************************/
	void rewrite(int rec, int off) {
		while (true) {
			try {
				file.seek((rec - 1l) * record.length + off);
				file.write(toString(0, index).getBytes(oem));
				break;
			} catch (IOException e) {
				error(e, false);
			}
		}
	}

	/***************************************************************************
	 * convert unicode buffer (0 to index) to oem data and append complete record (data size = index) plus CR/LF to file
	 ***************************************************************************/
	void write() {
		if (index != dataLen()) {
			error(new IOException("data size = " + index), true);
		}
		rewrite(getSize() + 1, 0);
		try {
			file.write(new byte[] { 0x0d, 0x0a });
		} catch (IOException e) {
			error(e, true);
		}
	}

	/***************************************************************************
	 * synchronize file size and data buffers with underlying device
	 ***************************************************************************/
	void sync() {
		while (true) {
			try {
				file.getFD().sync();
				break;
			} catch (IOException e) {
				error(e, false);
			}
		}
	}

	/***************************************************************************
	 * open local data file
	 *
	 * @param path
	 *            relative or absolute path
	 * @param name
	 *            file name
	 * @param mode
	 *            open mode (0=read only, 1=read/write, 2=new)
	 ***************************************************************************/
	void open(String path, String name, int mode) {
		pathfile = localFile(path, name);
		if (mode > 1) {
			pathfile.delete();
			if (pathfile.length() > 0)
				error(new IOException("deletion failed"), true);
		}
		try {
			file = new RandomAccessFile(pathfile, mode > 0 ? "rw" : "r");
			if (file.length() % record.length > 0)
				error(new IOException("file size"), true);
		} catch (IOException e) {
			if (mode < 1)
				file = null;
			else
				error(e, true);
		}
	}

	/***************************************************************************
	 * close local data file
	 ***************************************************************************/
	void close() {
		if (file == null)
			return;
		try {
			file.close();
			file = null;
		} catch (IOException e) {
			error(e, false);
		}
	}

	/***************************************************************************
	 * get file size
	 *
	 * @return number of records in file
	 ***************************************************************************/
	int getSize() {
		int rec = 0;

		if (file != null)
			try {
				rec = (int) (file.length() / record.length);
			} catch (IOException e) {
				error(e, true);
			}
		return rec;
	}
}

/*******************************************************************
 *
 * Access to local non resettable totals (assuming a CMOS-type block device/path in system property CMOS)
 *
 *******************************************************************/
class LocalPOS extends DatIo {
	/**
	 * transaction/customer counter
	 **/
	int trans;
	/**
	 * total amount
	 **/
	long total;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	LocalPOS(String id) {
		super(id, 0, 18);
		open(System.getProperty("CMOS"), "GdCmos." + id, 1);
		if (getSize() < 2)
			error(new IOException(pathfile.getPath() + " missing"), true);
	}

	/***************************************************************************
	 * read data record and scan data fields (transaction count and total amount)
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec) {
		int len = super.read(rec);

		if (len > 0)
			try {
				trans = (int) scanDec(5);
				total = scanDec(11);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}

	/***************************************************************************
	 * edit data fields and overwrite complete record in file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 ***************************************************************************/
	void rewrite(int rec) {
		index = 0;
		pushDec(trans, 5);
		pushDec(total, 11);
		super.rewrite(rec, 0);
		sync();
	}
}

/*******************************************************************
 *
 * Access to local data files with sales totals (assuming a directory named data under the current directory) (assuming
 * all files to exist with the name S_???%REG%.DAT) (assuming remote mirror image data available at the server)
 *
 *******************************************************************/
class SlsIo extends DatIo {
	/**
	 * 4-digit record access key (numeric or asterisk)
	 **/
	int key;
	/**
	 * 4-digit major layer access key (numeric or asterisk)
	 **/
	int grp;
	/**
	 * array of sales total blocks (transaction count, item count, amount)
	 **/
	Total block[];

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param fixSize
	 *            size of the record header (descriptive part)
	 * @param blocks
	 *            number of total blocks (accumulating data part)
	 ***************************************************************************/
	SlsIo(String id, int fixSize, int blocks) {
		super(id, fixSize, fixSize + Total.length * blocks + 2);
		block = new Total[blocks];
		while (blocks > 0)
			block[--blocks] = new Total();
		open("data", "S_" + id + REG + ".DAT", 1);
	}

	/***************************************************************************
	 * read data record from local or remote file and prepare subsequent parse functions
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal/group, 0=all terminals, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		if (sel == LOCAL)
			return super.read(rec);
		return net.readSls(rec, sel, this);
	}

	/***************************************************************************
	 * read data record from local file, parse record key and selected sales total block
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param blk
	 *            block number
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int readSls(int rec, int blk) {
		int len = super.read(rec);
		if (len > 0)
			try {
				key = scanKey(4);
				index = fixSize + blk * Total.length;
				block[blk].scan(this);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}

	/***************************************************************************
	 * edit selected sales total block, overwrite the total block in local data file, send the total block to remote
	 * mirror image
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param blk
	 *            block number
	 ***************************************************************************/
	void writeSls(int rec, int blk) {
		index = 0;
		block[blk].edit(this);
		rewrite(rec, fixSize + blk * Total.length);
		net.writeSls(rec, blk, this);
	}
}

/*******************************************************************
 *
 * Access to hourly activity data (sales totals per time slice)
 *
 *******************************************************************/
class LocalACT extends SlsIo {
	/**
	 * flag byte (x00-xFF) not used by core
	 **/
	int flag;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param blocks
	 *            number of total blocks (accumulating data part)
	 ***************************************************************************/
	LocalACT(String id, int blocks) {
		super(id, 12, blocks);
	}

	/***************************************************************************
	 * read data record from local or remote file and parse descriptive part along with sales data part(s)
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal/group, 0=all terminals, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		int len = super.read(rec, sel);

		if (len > 0)
			try {
				key = scanKey(4);
				scan(':');
				grp = scanKey(4);
				scan(':');
				flag = scanHex(2);
				for (int ind = 0; ind < block.length; block[ind++].scan(this))
					;
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}
}

/*******************************************************************
 *
 * Access to department data (sales totals per department)
 *
 *******************************************************************/
class LocalDPT extends SlsIo {
	/**
	 * flag byte (x00-xFF) defining department properties
	 **/
	int flag;
	/**
	 * selective itemizer 0-9 used to limit empl/cust discount
	 **/
	int sit;
	/**
	 * vat code 0-7
	 **/
	int vat;
	/**
	 * sales category (51-58, 61-68, 71-78, 81-88, 91-98)
	 **/
	int cat;
	/**
	 * amount entry limitation (halo 00 - 99, lalo 00 - 99)
	 **/
	int halo;
	/**
	 * secondary flag byte (x00-xFF) defining department properties
	 **/
	int flg2;
	/**
	 * index of age control info (0 = no control)
	 **/
	int ages;
	/**
	 * item type
	 **/
	int type;
	/**
	 * department description (20 chars)
	 **/
	String text;
	/**
	 * reserve
	 **/
	String xtra;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param blocks
	 *            number of total blocks (accumulating data part)
	 ***************************************************************************/
	LocalDPT(String id, int blocks) {
		super(id, 58, blocks);
	}

	/***************************************************************************
	 * read data record from local or remote file and parse descriptive part along with sales data part(s)
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal/group, 0=all terminals, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		int len = super.read(rec, sel);

		if (len > 0)
			try {
				key = scanKey(4);
				scan(':');
				grp = scanKey(4);
				scan(':');
				flag = scanHex(2);
				scan(':');
				sit = scanNum(1);
				vat = scanNum(1);
				cat = scanNum(2);
				scan(':');
				halo = scanHex(4);
				scan(':');
				flg2 = scanHex(2);
				ages = scanNum(1);
				type = scanNum(1);
				scan(':');
				text = scan(20);
				xtra = scan(10);
				for (int ind = 0; ind < block.length; block[ind++].scan(this))
					;
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}
}

/*******************************************************************
 *
 * Access to cashier data (sales totals for up to 16 cashiers)
 *
 *******************************************************************/
class LocalPOT extends SlsIo {
	/**
	 * index into table of cashiers active at this terminal
	 **/
	int blk;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param blocks
	 *            number of total blocks (accumulating data part)
	 * @param tb
	 *            single total block for all read/writeSls operations
	 ***************************************************************************/
	LocalPOT(String id, int blocks, Total tb) {
		super(id, 4, blocks);
		while (blocks > 0)
			block[--blocks] = tb;
	}

	/***************************************************************************
	 * append new record with key and 10 equal total blocks to file, where key (inherited from SlsIo) has been set to
	 * ic/sc
	 ***************************************************************************/
	void write() {
		onto(0, editNum(key, 4));
		for (int ind = 0; ind < block.length; block[ind++].edit(this))
			;
		super.write();
	}
}

/*******************************************************************
 *
 * Access to register financial data (sales totals per terminal)
 *
 *******************************************************************/
class LocalREG extends SlsIo {
	/**
	 * flag byte (x00-xFF) defining properties of report total
	 **/
	int flag;
	/**
	 * percentage with one assumed decimal place
	 **/
	int rate;
	/**
	 * transaction flag (x00-xFF) defining operational properties
	 **/
	int tflg;
	/**
	 * description of financial total (12 chars)
	 **/
	String text;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param blocks
	 *            number of total blocks (accumulating data part)
	 ***************************************************************************/
	LocalREG(String id, int blocks) {
		super(id, 28, blocks);
	}

	/***************************************************************************
	 * read data record from local or remote file and parse descriptive part along with sales data part(s)
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal/group, 0=all terminals, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		int len = super.read(rec, sel);

		if (len > 0)
			try {
				key = scanNum(4);
				scan(':');
				text = scan(12);
				scan(':');
				flag = scanHex(2);
				scan(':');
				rate = scanNum(4);
				scan(':');
				tflg = scanHex(2);
				for (int ind = 0; ind < block.length; block[ind++].scan(this))
					;
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}
}

/*******************************************************************
 *
 * Access to salesperson data (sales totals per salesperson)
 *
 *******************************************************************/
class LocalSLM extends SlsIo {
	/**
	 * flag byte (x00-xFF) not used by core
	 **/
	int flag;
	/**
	 * employee number (8 digits)
	 **/
	int pers;
	/**
	 * name (20 chars)
	 **/
	String text;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param blocks
	 *            number of total blocks (accumulating data part)
	 ***************************************************************************/
	LocalSLM(String id, int blocks) {
		super(id, 42, blocks);
	}

	/***************************************************************************
	 * read data record from local or remote file and parse descriptive part along with sales data part(s)
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal/group, 0=all terminals, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		int len = super.read(rec, sel);

		if (len > 0)
			try {
				key = scanKey(4);
				scan(':');
				grp = scanKey(4);
				scan(':');
				flag = scanHex(2);
				scan(':');
				text = scan(20);
				scan(':');
				pers = scanNum(8);
				for (int ind = 0; ind < block.length; block[ind++].scan(this))
					;
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}
}

/*******************************************************************
 *
 * Access to cashier control file (list of cashiers and supervisors)
 *
 *******************************************************************/
class LocalCTL extends SlsIo {
	/**
	 * name (20 chars)
	 **/
	String text, xtra;
	/**
	 * employee number (8 digits)
	 **/
	int pers;
	/**
	 * age of operator (yymmdd)
	 **/
	int age;
	/**
	 * functional specs (flom, roll, location)
	 **/
	int flag, lvl, pin;
	/**
	 * lock indicator (1=locked, 2=forced close)
	 **/
	int lck;
	/**
	 * terminal number (last open)
	 **/
	int reg;
	/**
	 * drawer number
	 **/
	int drw;
	/**
	 * secret number
	 **/
	int sec;
	/**
	 * status (00=inactive, 01=opened, 02=closed, 19=settled)
	 **/
	int sts;
	/**
	 * last password change yymmdd, hhmm
	 **/
	int datePwd, timePwd;
	/**
	 * last settlement yymmdd, hhmm
	 **/
	int dateBal, timeBal;
	/**
	 * last sign in yymmdd, hhmm
	 **/
	int dateOpn, timeOpn;
	/**
	 * last close yymmdd, hhmm
	 **/
	int dateCls, timeCls;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	LocalCTL(String id) {
		super(id, 128, 0);
	}

	/***************************************************************************
	 * read data record from local or remote file and parse all operator control data
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=inquiry (rec=0), 0=remote, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		int len = super.read(rec, sel);

		if (len > 0)
			try {
				key = scanNum(4);
				scan(':');
				pers = scanNum(8);
				scan(':');
				flag = scanHex(2);
				scan(':');
				text = scan(20);
				xtra = scan(10);
				scan(':');
				age = scanNum(6);
				scan(':');
				lvl = scanNum(3);
				scan(':');
				pin = scanKey(3);
				scan(':');
				lck = scanNum(1);
				scan(':');
				reg = scanKey(3);
				scan(':');
				drw = scanNum(3);
				scan(':');
				sec = scanNum(4);
				scan(':');
				sts = scanNum(2);
				scan(':');
				datePwd = scanNum(6);
				scan(':');
				timePwd = scanNum(4);
				scan(':');
				dateBal = scanNum(6);
				scan(':');
				timeBal = scanNum(4);
				scan(':');
				dateOpn = scanNum(6);
				scan(':');
				timeOpn = scanNum(4);
				scan(':');
				dateCls = scanNum(6);
				scan(':');
				timeCls = scanNum(4);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}

	/***************************************************************************
	 * edit data fields and overwrite complete record in file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 ***************************************************************************/
	void rewrite(int rec) {
		index = 0;
		push(editNum(key, 4));
		push(':');
		push(editNum(pers, 8));
		push(':');
		push(editHex(flag, 2));
		push(':');
		push(text);
		push(xtra);
		push(':');
		push(editNum(age, 6));
		push(':');
		push(editNum(lvl, 3));
		push(':');
		push(editKey(pin, 3));
		push(':');
		push(editNum(lck, 1));
		push(':');
		push(editKey(reg, 3));
		push(':');
		push(editNum(drw, 3));
		push(':');
		push(editNum(sec, 4));
		push(':');
		push(editNum(sts, 2));
		push(':');
		push(editNum(datePwd, 6));
		push(':');
		push(editNum(timePwd, 4));
		push(':');
		push(editNum(dateBal, 6));
		push(':');
		push(editNum(timeBal, 4));
		push(':');
		push(editNum(dateOpn, 6));
		push(':');
		push(editNum(timeOpn, 4));
		push(':');
		push(editNum(dateCls, 6));
		push(':');
		push(editNum(timeCls, 4));
		super.rewrite(rec, 0);
	}

	/***************************************************************************
	 * check duration of password
	 *
	 * @param days
	 *            lifetime of secret number
	 * @return true = expired
	 ***************************************************************************/
	boolean pwdOlder(int days) {
		Calendar c = Calendar.getInstance();
		Date date = c.getTime();

		if (days < 1)
			return false;
		int yyyy = c.get(c.YEAR) / 100 * 100 + datePwd / 10000;
		if (yyyy > c.get(c.YEAR))
			yyyy -= 100;
		c.set(yyyy, datePwd / 100 % 100 - 1, datePwd % 100, timePwd / 100, timePwd % 100, 0);
		c.add(c.DATE, days);
		return c.getTime().before(date);
	}

	/***************************************************************************
	 * synchronize spare checkers with remote file
	 ***************************************************************************/
	void update() {
		while (read(++recno, LOCAL) > 0) {
			if (key < 1000)
				continue;
			if (read(recno, 0) < 1 || key > 999)
				break;
			reg = sts = 0;
			dateOpn = timeOpn = dateCls = timeCls = 0;
			rewrite(recno);
			logConsole(2, "update " + id + " key=" + editNum(key, 3), null);
		}
		--recno;
	}

	/***************************************************************************
	 * search for checker locally
	 *
	 * @param ckr
	 *            checker number (cashier 001-799, supervisors 801-999)
	 * @return relative record number (0 = not on file)
	 ***************************************************************************/
	int find(int ckr) {
		for (int rec = 0; read(++rec, LOCAL) > 0;) {
			if (key == ckr)
				return rec;
		}
		return 0;
	}
}

/*******************************************************************
 *
 * Access to terminal control file (list of all terminals in cluster) Local file consists of one record only (this
 * terminal)
 *
 *******************************************************************/
class LocalLAN extends SlsIo {
	/**
	 * terminal type ('R'=register (PoS), 'C'=crt (B/O), 'D'=daemon, 'S'=server, ' '=spare)
	 **/
	char type;
	/**
	 * terminal characteristics (service mask, exit no)
	 **/
	int flag, exit;
	/**
	 * id mnemonics (terminal, reserve, invoice printer)
	 **/
	String text, xtra, prin;
	/**
	 * last transaction (operator, actioncode)
	 **/
	int ckr, sts;
	/**
	 * record counters of sequential files
	 **/
	int idc, jrn, mnt, dtl, gpo, imp;
	/**
	 * time stamp yymmdd hhmmss
	 **/
	int date, time;
	/**
	 * last reset yymmdd hhmmss
	 **/
	int zdat, ztim;
	/**
	 * org version (parameters) and dat version (reset counter)
	 **/
	int org, dat;
	/**
	 * LAN status x00-xFF
	 **/
	int lan;
	/**
	 * array of cashier numbers (pot allocation)
	 **/
	int tbl[];

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param blocks
	 *            max number of cashiers active at this terminal
	 ***************************************************************************/
	LocalLAN(String id, int blocks) {
		super(id, 160 + blocks * 3, 0);
		tbl = new int[blocks];
	}

	/***************************************************************************
	 * read data record from local or remote file and parse all terminal control data
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=inquiry (rec=0), 0=remote, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec, int sel) {
		int len = super.read(rec, sel);

		if (len > 0)
			try {
				type = scan();
				scan(':');
				key = scanKey(3);
				scan(':');
				grp = scanKey(2);
				scan(':');
				flag = scanHex(2);
				scan(':');
				text = scan(20);
				xtra = scan(10);
				scan(':');
				exit = scanNum(3);
				scan(':');
				prin = scan(10);
				scan(':');
				ckr = scanNum(3);
				scan(':');
				sts = scanNum(2);
				scan(':');
				idc = scanNum(8);
				scan(':');
				jrn = scanNum(8);
				scan(':');
				mnt = scanNum(8);
				scan(':');
				dtl = scanNum(8);
				scan(':');
				gpo = scanNum(8);
				scan(':');
				imp = scanNum(8);
				scan(':');
				date = scanNum(6);
				scan(':');
				time = scanNum(6);
				scan(':');
				zdat = scanNum(6);
				scan(':');
				ztim = scanNum(6);
				scan(':');
				org = scanNum(4);
				scan(':');
				dat = scanNum(4);
				scan(':');
				lan = scanHex(2);
				scan(':');
				for (int ind = 0; ind < tbl.length; tbl[ind++] = scanNum(3))
					;
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}

	/***************************************************************************
	 * edit descriptive fields and append complete record to file
	 ***************************************************************************/
	void write() {
		init(' ');
		push("R:" + REG + ":" + GRP + ":00:");
		push(editTxt("xxxxxxxxxx", 30) + ":000:          :");
		skip(dataLen() - index);
		super.write();
	}

	/***************************************************************************
	 * edit data fields and overwrite status data in file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 ***************************************************************************/
	void rewrite(int rec) {
		index = 0;
		push(editNum(ckr, 3));
		push(':');
		push(editNum(sts, 2));
		push(':');
		push(editNum(idc, 8));
		push(':');
		push(editNum(jrn, 8));
		push(':');
		push(editNum(mnt, 8));
		push(':');
		push(editNum(dtl, 8));
		push(':');
		push(editNum(gpo, 8));
		push(':');
		push(editNum(imp, 8));
		push(':');
		push(editNum(date, 6));
		push(':');
		push(editNum(time, 6));
		push(':');
		push(editNum(zdat, 6));
		push(':');
		push(editNum(ztim, 6));
		push(':');
		push(editNum(org, 4));
		push(':');
		push(editNum(dat, 4));
		push(':');
		push(editHex(lan, 2));
		push(':');
		for (int ind = 0; ind < tbl.length; push(editNum(tbl[ind++], 3)))
			;
		super.rewrite(rec, 58);
	}
}

/*******************************************************************
 *
 * Access to High-order CustomerTransaction Accounts (srv\data\S_HCA%SRV%.DAT = command/status/log file)
 *
 *******************************************************************/
class RmoteHCA extends LinIo {
	/**
	 * requesting terminal, operator, date, time
	 **/
	int reg1, ckr, dat1, tim1;
	/**
	 * responding terminal, date, time
	 **/
	int reg2, dat2, tim2;
	/**
	 * processing status (0=active, 1=ready, 2=cancelled)
	 **/
	int sts;
	/**
	 * message key = customer number (13 digits)
	 **/
	String key;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	RmoteHCA(String id) {
		super(id, 0, 56);
	}

	/***************************************************************************
	 * read data record from remote file and parse all message data
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec) {
		int len = net.readSeq(rec, 0, this);

		if (len > 0)
			try {
				reg1 = scanKey(3);
				scan(':');
				ckr = scanNum(3);
				scan(':');
				dat1 = scanNum(6);
				scan(':');
				tim1 = scanNum(6);
				scan(':');
				reg2 = scanKey(3);
				scan(':');
				dat2 = scanNum(6);
				scan(':');
				tim2 = scanNum(6);
				scan(':');
				sts = scanHex(2);
				scan(':');
				key = scan(13);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}

	/***************************************************************************
	 * edit all message data fields and update record on remote file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return record size - 2 (<1 = access error)
	 ***************************************************************************/
	int write(int rec) {
		index = 0;
		push(editKey(reg1, 3));
		push(':');
		push(editNum(ckr, 3));
		push(':');
		push(editNum(dat1, 6));
		push(':');
		push(editNum(tim1, 6));
		push(':');
		push(editKey(reg2, 3));
		push(':');
		push(editNum(dat2, 6));
		push(':');
		push(editNum(tim2, 6));
		push(':');
		push(editHex(sts, 2));
		push(':');
		push(editTxt(key, 13));
		return net.updNews(rec, this);
	}
}

/*******************************************************************
 *
 * Access to news/messages data file (srv\data\S_NEW%SRV%.DAT = command/status/log file)
 *
 *******************************************************************/
class RmoteNEW extends LinIo {
	/**
	 * sending terminal, operator, date, time
	 **/
	int reg1, ckr, dat1, tim1;
	/**
	 * receiving terminal, date, time
	 **/
	int reg2, dat2, tim2;
	/**
	 * message status
	 **/
	int sts;
	/**
	 * message text
	 **/
	String text;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	RmoteNEW(String id) {
		super(id, 0, 78);
	}

	/***************************************************************************
	 * read data record from remote file and parse all message data
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	int read(int rec) {
		int len = net.readSeq(rec, 0, this);

		if (len > 0)
			try {
				reg1 = scanKey(3);
				scan(':');
				ckr = scanNum(3);
				scan(':');
				dat1 = scanNum(6);
				scan(':');
				tim1 = scanNum(4);
				scan(':');
				reg2 = scanKey(3);
				scan(':');
				dat2 = scanNum(6);
				scan(':');
				tim2 = scanNum(4);
				scan(':');
				sts = scanHex(1);
				scan(':');
				text = scan(40);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}

	/***************************************************************************
	 * edit all message data fields and update record on remote file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return record size - 2 (<1 = access error)
	 ***************************************************************************/
	int write(int rec) {
		index = 0;
		push(editKey(reg1, 3));
		push(':');
		push(editNum(ckr, 3));
		push(':');
		push(editNum(dat1, 6));
		push(':');
		push(editNum(tim1, 4));
		push(':');
		push(editKey(reg2, 3));
		push(':');
		push(editNum(dat2, 6));
		push(':');
		push(editNum(tim2, 4));
		push(':');
		push(editHex(sts, 1));
		push(':');
		push(text);
		return net.updNews(rec, this);
	}
}

/*******************************************************************
 *
 * Access to suspend/resume control file (srv\inq\S_HSHSAR.DAT = command/status/log file)
 *
 *******************************************************************/
class RmoteSAR extends LinIo {
	/**
	 * transaction id (terminal number, transaction number)
	 **/
	int reg, tran;
	/**
	 * last procedure (operator, date, time)
	 **/
	int ckr, date, time;
	/**
	 * transaction status
	 **/
	char stat;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	RmoteSAR(String id) {
		super(id, 0, 36);
	}

	/***************************************************************************
	 * edit all control data fields and update record on remote file
	 *
	 * @param key
	 *            String "Sxxxyyyy" (xxx=terminal yyyy=transaction)
	 * @return record size - 2 (<1 = access error)
	 ***************************************************************************/
	int find(String key) {
		pb = key + ':' + stat + ':' + editKey(reg, 3) + editNum(tran, 4) + ':' + editNum(ckr, 3) + ':'
				+ editNum(date, 6) + ':' + editNum(time, 6);

		int len = net.readHsh('R', pb, this);
		if (len > 0)
			try {
				stat = skip(9).scan();
				reg = scan(':').scanKey(3);
				tran = scanNum(4);
				ckr = scan(':').scanNum(3);
				date = scan(':').scanNum(6);
				time = scan(':').scanNum(6);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}
}

/*******************************************************************
 *
 * Access to customer sales data file (srv\inq\S_HSHCLS.DAT = hash data file)
 *
 *******************************************************************/
class RmoteCLS extends LinIo {
	/**
	 * timestamp
	 **/
	int date, time;
	/**
	 * total block (transaction counter, item counter, total amount)
	 **/
	Total block = new Total();

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	RmoteCLS(String id) {
		super(id, 0, 60);
	}

	/***************************************************************************
	 * search for sales data record remotely and parse data fields
	 *
	 * @param type
	 *            lastSale=C00, rewards=CP0, points=CP1-8, variables=CV0-9
	 * @param key
	 *            customer number
	 * @return record size - 2 (0 = not on file)
	 ***************************************************************************/
	int find(String type, String key) {
		int len = net.readHsh('I', type + editTxt(key, 13), this);
		if (len > 0)
			try {
				date = skip(17).scanNum(6);
				time = scan(':').scanNum(6);
				block.trans = (int) scanDec(7);
				block.items = (int) scanDec(8);
				block.total = scanDec(15);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return len;
	}
}

/*******************************************************************
 *
 * Access to VariousNumberLookup file (at server only)
 *
 *******************************************************************/
class RmoteVNU extends LinIo {
	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	RmoteVNU(String id) {
		super(id, 0, 26);
	}

	/***************************************************************************
	 * read record and increment its counter
	 *
	 * @param key
	 *            file access key
	 * @return <0=offline, 0=not found, >0=unique counter
	 ***************************************************************************/
	int find(String key) {
		init(' ').push(id.charAt(0)).upto(16, key);
		int sts = net.readHsh('I', toString(), this);
		if (sts > 0)
			try {
				sts = skip(16).scanNum(10);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		return sts;
	}
}

/*******************************************************************
 *
 * Access to Invoice Printing Order file (at server only)
 *
 *******************************************************************/
class RmoteBIL extends LinIo {
	/**
	 * original terminal
	 **/
	int reg;
	/**
	 * transaction
	 **/
	int tran;

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 ***************************************************************************/
	RmoteBIL(String id) {
		super(id, 0, 56);
	}

	/***************************************************************************
	 * write new order record
	 *
	 * @param type
	 *            currency (0=home, >0=foreign currency tender)
	 * @return <0=offline, 0=unavailable, >0=ok
	 ***************************************************************************/
	int write(int type) {
		index = 0;
		push(editKey(0, 3));
		push(':');
		push(editKey(reg, 3));
		push(':');
		push(editNum(0, 6));
		push(':');
		push(editNum(0, 6));
		push(':');
		push(editNum(tran, 4));
		push(':');
		push(editNum(0, 8));
		push(':');
		push(editKey(0, 3));
		push(':');
		push(editNum(0, 6));
		push(':');
		push(editNum(0, 6));
		push(':');
		push(editNum(type, 2));
		return net.updNews(0, this);
	}
}

/*******************************************************************
 *
 * Access to local parameter files (binary search) (assuming all files to be sorted in ascending order) (assuming all
 * files to be named S_PLU???.DAT) (files are read-only and may or may not exist)
 *
 *******************************************************************/
class BinIo extends DatIo {
	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param keySize
	 *            size of the record key in bytes
	 * @param recSize
	 *            record size in bytes including separators CR/LF
	 ***************************************************************************/
	BinIo(String id, int keySize, int recSize) {
		super(id, keySize, recSize);
		open(null, "S_PLU" + id + ".DAT", 0);
	}

	//ECOMMERCE-SSAM#C BEG
	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param keySize
	 *            size of the record key in bytes
	 * @param recSize
	 *            record size in bytes including separators CR/LF
	 * @param open
	 * 	 *        if open or not the file
	 ***************************************************************************/
	BinIo(String id, int keySize, int recSize, boolean open) {
		super(id, keySize, recSize);
		if (open) {
			open(null, "S_PLU" + id + ".DAT", 0);
		}
	}
	//ECOMMERCE-SSAM#C END

	/***************************************************************************
	 * search by n iterative bisections (2 in the nth power = file size in records)
	 *
	 * @param key
	 *            search argument = record key
	 * @return record size - 2 (0 = not on file)
	 ***************************************************************************/
	int find(String key) {
		int sts, top = 0, end = getSize();

		while ((recno = (top + end) >> 1) < end) {
			if (super.read(++recno) <= 0)
				return ERROR;
			sts = key.compareTo(pb.substring(0, fixSize));
			if (sts == 0)
				return pb.length();
			if (sts < 0)
				end = recno - 1;
			else
				top = recno;
		}
		return 0;
	}

	/***************************************************************************
	 * search for first occurrence of non unique key
	 *
	 * @param key
	 *            search argument = record key
	 * @return record size - 2 (0 = not on file)
	 ***************************************************************************/
	int start(String key) {
		int sts = find(key);

		if (sts <= 0)
			return sts;
		do {
			if (recno == 1)
				return sts;
			if (super.read(--recno) < sts)
				return ERROR;
		} while (pb.startsWith(key));
		return super.read(++recno);
	}

	/***************************************************************************
	 * search for next occurrence of non-unique key
	 *
	 * @param key
	 *            search argument = record key
	 * @return record size - 2 (0 = not on file)
	 ***************************************************************************/
	int next(String key) {
		int sts = super.read(++recno);

		if (sts <= 0)
			return sts;
		return pb.startsWith(key) ? sts : 0;
	}
}

/*******************************************************************
 *
 * Access to hash files (random search) (assuming all files to have unique record keys) (assuming all files to be named
 * S_HSH???.DAT) (files are locally read/write and created if non-existent)
 *
 *******************************************************************/
class HshIo extends DatIo {
	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param keySize
	 *            size of the record key in bytes
	 * @param recSize
	 *            record size in bytes including separators CR/LF
	 ***************************************************************************/
	HshIo(String id, int keySize, int recSize) {
		super(id, keySize, recSize);
		open("inq", "S_HSH" + id + ".DAT", 1);
	}

	/***************************************************************************
	 * search sequentially starting at record as computed by hash formula
	 *
	 * @param key
	 *            search argument = record key
	 * @return record size - 2 (0 = not on file, -1 = file full)
	 ***************************************************************************/
	int find(String key) {
		int top = 0, del = 0, end = getSize();
		if (end < 8)
			return ERROR;
		int ind = fixSize + 5 & ~7, val = key.charAt(1) & 0x0f;
		String s = editTxt(key.substring(2), ind);
		while (val-- > 0)
			s = s.substring(1) + s.charAt(0);
		for (char tmp[] = new char[8]; ind-- > 0;) {
			val = s.charAt(ind) & 0x0f;
			if (val > 9)
				val -= 6;
			tmp[ind & 7] = (char) (val + '0');
			if ((ind & 7) > 0)
				continue;
			top += Integer.parseInt(new String(tmp));
		}
		top %= end >> 3;
		recno = top <<= 3;
		while (super.read(++recno) > 0) {
			char c = pb.charAt(fixSize - 1);
			if (c < '0') {
				if (del == 0)
					del = recno;
				if (c == ' ')
					break;
			} else if (pb.startsWith(key))
				return pb.length();
			if (recno == end)
				recno = 0;
			if (recno == top)
				break;
		}
		if ((recno = del) > 0)
			return 0;
		error(new IOException("no space"), false);
		return ERROR;
	}

	/***************************************************************************
	 * search locally first, then remotely, or viceversa
	 *
	 * @param key
	 *            search argument = record key
	 * @return record size - 2 (0 = not on file, -1 = LAN offline)
	 ***************************************************************************/
	int find(String key, boolean remoteFirst) {
		int sts = remoteFirst ? 0 : find(key);

		if (sts < 1)
			sts = net.readHsh('R', key, this);
		if (remoteFirst && sts < 1)
			if (find(key) > 0)
				return pb.length();
		return sts;
	}

	/***************************************************************************
	 * erase record with all '-' characters
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 ***************************************************************************/
	void delete(int rec) {
		for (index = 0; index < dataLen(); push('-'))
			;
		rewrite(rec, 0);
	}
}

/*******************************************************************
 *
 * Access to sequential data files (both local and remote) (assuming all files to be named data\S_???%REG%.DAT) (files
 * are locally read/write and created if non-existent)
 *
 *******************************************************************/
class SeqIo extends DatIo {
	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param fixSize
	 *            size of the record header (descriptive part)
	 * @param recSize
	 *            record size in bytes including separators CR/LF
	 ***************************************************************************/
	SeqIo(String id, int fixSize, int recSize) {
		super(id, fixSize, recSize);
		open("data", "S_" + id + REG + ".DAT", 1);
	}

	/***************************************************************************
	 * read record from local or remote file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal, -1=local)
	 * @return record size - 2 (0 = end of file, -1 = LAN offline)
	 ***************************************************************************/
	int read(int rec, int sel) {
		if (sel == LOCAL)
			return super.read(rec);
		return net.readSeq(rec, sel, this);
	}
}
