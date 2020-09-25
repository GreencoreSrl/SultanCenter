package com.ncr;

import java.text.*;

/*******************************************************************
 *
 * Single sales total structure (storage for item count and sales amount)
 *
 *******************************************************************/
class Sales {
	/**
	 * item count
	 **/
	int items;

	/**
	 * sales amount
	 **/
	long total;

	/***************************************************************************
	 * set sales total
	 *
	 * @param items
	 *            item count
	 * @param total
	 *            sales amount
	 ***************************************************************************/
	void set(int items, long total) {
		this.items = items;
		this.total = total;
	}

	/***************************************************************************
	 * add to sales total
	 *
	 * @param items
	 *            item count
	 * @param total
	 *            sales amount
	 ***************************************************************************/
	void add(int items, long total) {
		this.items += items;
		this.total += total;
	}

	/***************************************************************************
	 * zero-check sales total
	 *
	 * @return true if sales total all zero
	 ***************************************************************************/
	boolean isZero() {
		return items == 0 && total == 0;
	}

	/***************************************************************************
	 * write sales total to data file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param blk
	 *            number of the total block within sales data record
	 * @param io
	 *            provider of access to sales data file
	 ***************************************************************************/
	void write(int rec, int blk, SlsIo io) {
		if (io.readSls(rec, blk) > 0) {
			io.block[blk].update(items, total);
			io.writeSls(rec, blk);
			Delta.write(io, blk, 1, items, total);
		}
	}
}

/*******************************************************************
 *
 * Table of sales data records in memory (act / reg / dpt / slm) (for transaction-based accumulation of sales totals)
 *
 *******************************************************************/
class TableSls {
	/**
	 * provider of access to sales data file
	 **/
	SlsIo io;

	/**
	 * number of total blocks within sales data record
	 **/
	int blocks;

	/**
	 * array of access keys (search argument)
	 **/
	int key[];

	/**
	 * array of relative record numbers (reference to major)
	 **/
	int grp[];

	/**
	 * array of sets of sales totals
	 **/
	Sales sales[][];



	/***************************************************************************
	 * Constructor
	 *
	 * @param io
	 *            provider of access to sales data file
	 ***************************************************************************/
	TableSls(SlsIo io) {
		int ind, rec = (this.io = io).getSize();
		key = new int[rec];
		grp = new int[rec];
		sales = new Sales[rec][blocks = io.block.length];
		while (rec-- > 0) {
			for (ind = blocks; ind-- > 0; sales[rec][ind] = new Sales())
				;
		}
	}

	/***************************************************************************
	 * initialize complete table reading the sales data file and prepare subsequent search functions
	 ***************************************************************************/
	void init() {
		int rec = 0;
		while (io.read(rec + 1, io.LOCAL) > 0) {
			key[rec] = io.key;
			grp[rec++] = io.grp;
		}
		while (rec-- > 0) {
			if (grp[rec] > 0)
				grp[rec] = find(grp[rec]);
		}
	}

	/***************************************************************************
	 * search for the given access key
	 *
	 * @param code
	 *            the access key
	 * @return the relative record number (0 = not found)
	 ***************************************************************************/
	int find(int code) {
		for (int rec = 0; rec < key.length;) {
			if (key[rec++] == code)
				return rec;
		}
		return 0;
	}

	/***************************************************************************
	 * reset the transaction-based sales totals
	 ***************************************************************************/
	void reset() {
		int ind, rec = key.length;
		while (rec-- > 0) {
			for (ind = blocks; ind-- > 0; sales[rec][ind].set(0, 0))
				;
		}
	}

	/***************************************************************************
	 * accumulate one sales total throughout all major levels
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param blk
	 *            number of the total block within sales data record
	 * @param items
	 *            item count
	 * @param total
	 *            sales amount
	 ***************************************************************************/
	void addSales(int rec, int blk, int items, long total) {
		for (; rec-- > 0; rec = grp[rec]) {
			sales[rec][blk].add(items, total);
		}
	}

	/***************************************************************************
	 * get the sum of all total blocks within one sales data record
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @return new sales total object
	 ***************************************************************************/
	Sales netSales(int rec) {
		Sales ptr[] = sales[rec - 1], sls = new Sales();
		for (int ind = blocks; ind-- > 0; sls.total += ptr[ind].total) {
			if (ind == 0)
				sls.items += ptr[ind].items;
		}
		return sls;
	}

	/***************************************************************************
	 * update all active sales totals of one sales data record to the data file
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 ***************************************************************************/
	void write(int rec) {
		for (int ind = 0; ind < blocks; ind++) {
			Sales sls = sales[rec - 1][ind];
			if (sls.isZero())
				continue;
			sls.write(rec, ind, io);
		}
	}
}

/*******************************************************************
 *
 * Table of sales data records for departments (dpt) (for transaction-based accumulation of sales totals)
 *
 *******************************************************************/
class TableDpt extends TableSls {
	/**
	 * array of discountable sales totals
	 **/
	long dsc_sls[] = new long[key.length];
	/**
	 * array of counters of discountable sales
	 **/
	int dsc_cnt[] = new int[key.length];

	/**
	 * array of sales totals with possible points
	 **/
	long pnt_sls[] = new long[key.length];

	/***************************************************************************
	 * Constructor
	 *
	 * @param io
	 *            provider of access to department data file
	 ***************************************************************************/
	TableDpt(SlsIo io) {
		super(io);
	}

	/***************************************************************************
	 * reset the transaction-based sales totals
	 ***************************************************************************/
	void reset() {
		for (int rec = key.length; rec-- > 0; dsc_sls[rec] = pnt_sls[rec] = 0)
			;
		super.reset();
	}
}

/*******************************************************************
 *
 * Table of sales data records for financial reports (ckr / reg) (for transaction-based accumulation of sales totals)
 *
 *******************************************************************/
class TableReg extends TableSls {
	/***************************************************************************
	 * Constructor
	 *
	 * @param io
	 *            provider of access to sales data file
	 ***************************************************************************/
	TableReg(SlsIo io) {
		super(io);
	}

	/***************************************************************************
	 * search for the access key given as ic / sc
	 *
	 * @param ic
	 *            the itemcode
	 * @param sc
	 *            the subcode
	 * @return the relative record number (0 = not found)
	 ***************************************************************************/
	int find(int ic, int sc) {
		return super.find(ic * 100 + sc);
	}

	/***************************************************************************
	 * search for the access key given as tender / sc
	 *
	 * @param ind
	 *            the tender number (0 = all tender total)
	 * @param sc
	 *            the subcode
	 * @return the relative record number (0 = not found)
	 ***************************************************************************/
	int findTnd(int ind, int sc) {
		return find(10 + ind, sc);
	}
}

/*******************************************************************
 * mix match table entry
 *******************************************************************/
class TableMmt {
	Sales sls = new Sales();
}

/*******************************************************************
 * rebate table entry
 *******************************************************************/
class TableRbt {
	/** selective itemizer **/
	String text;
	/** max employee discount rate (1 dec assumed) **/
	int rate_empl;
	/** max customer discount rate (1 dec assumed) **/
	int rate_cust;
	/** max item % discount (1 dec assumed) **/
	int rate_item;
	/** max item $ discount (1 dec assumed) **/
	int rate_ival;
	/** discount granted **/
	long amt;
	/** bonuspoints granted **/
	int pnt;
	/** sales with possible discount **/
	Sales dsc_sls = new Sales();
	/** sales with possible bonuspoints **/
	Sales pnt_sls = new Sales();

	/** reset all accumulators for new transaction **/
	void reset() {
		dsc_sls.set(0, amt = 0);
		pnt_sls.set(pnt = 0, 0);
	}
}

/*******************************************************************
 * vat/tax table entry
 *******************************************************************/
class TaxRates extends FmtIo {
	/** flat (environment) tax if > 0 **/
	int flat;
	/** default constant rate (1 dec assumed) **/
	int rate; /* default constant rate */
	/** applied discounts on totals (SD, SI, ST) **/
	long tld_amt;
	/** tax description **/
	String text;

	/** reset all accumulators for new transaction **/
	void reset() {
		tld_amt = 0;
	}

	/***************************************************************************
	 * calculate tax amount
	 *
	 * @param total
	 *            taxable sales amount
	 * @return rounded tax amount
	 ***************************************************************************/
	long collect(long total) {
		return roundBy(total * rate, 1000);
	}

	/***************************************************************************
	 * calculate tax amount included
	 *
	 * @param total
	 *            gros sales amount
	 * @return rounded tax amount
	 ***************************************************************************/
	long exempt(long total) {
		return roundBy(total * rate * 10 / (1000 + rate), 10);
	}
}

/*******************************************************************
 *
 * tender table entry
 *
 *******************************************************************/
class TndMedia extends FmtIo {

	/** smallest coin **/
	int coin;

	// TSC-ENH2014-3-AMZ#BEG
	boolean customerFavour;
	// TSC-ENH2014-3-AMZ#END
	/** 0=controlled, 4=uncontrolled **/
	int ctrl = 4;
	/** decimal places in currency **/
	int dec;
	/** tender flags **/
	int flag;
	/** function lockout mask **/
	int flom;
	/** second tender flag byte **/
	int flg2;
	/** item count available **/
	int icnt;
	/** cashdrawer number **/
	int till = 1;
	/** lead currency **/
	int club;
	/** foreign currency base for exchange value **/
	int unit;
	/** decimal places in exchange rate **/
	int xflg;
	/** auto discount rate **/
	int rate;
	/** auto surcharge rate **/
	int xtra;
	/** amount of money in cashdrawer **/
	long alert;
	/** foreign currency exchange value **/
	long value;
	/** tender limitations **/
	long limit[] = new long[9];
	/** tender type (input sequence) **/
	char type = 'A';
	/** national currency symbol **/
	char xsym = ' ';
	/** international currency symbol (3 chars) **/
	String symbol = "";
	/** tender description (reports) **/
	String text = "";
	/** tender description (receipt) **/
	String tx20 = "";
	/** foreign currency rate description (8 chars) **/
	String xtext;
	/** denomination table **/
	CshDenom dnom[] = new CshDenom[32];
	// TAMI-ENH-20140526-SBE#A BEG
	// States if tender must start eftTerminal communication

    /** identify FDP for using verifon pinpad **/
    boolean verifone = false; //VERIFONE-20160201-CGA#A

	// boolean eftTerminal = false; //TAMI-ENH-20140526-CGA#D
	String eftTerminal = ""; // TAMI-ENH-20140526-CGA#A
	// TAMI-ENH-20140526-SBE#A END

	/** table base ref for lead currency access **/
	static TndMedia tbl[] = null;

	/**************************************************************************
	 * find tender of given type
	 *
	 * @param type
	 *            tender type
	 * @return tender number (0 = not found)
	 ***************************************************************************/
	static int find(char type) {
		int ind = tbl.length;
		while (--ind > 0 && tbl[ind].type != type)
			;
		return ind;
	}

	/***************************************************************************
	 * Constructor
	 ***************************************************************************/
	TndMedia() {
		int ind = dnom.length;
		while (ind > 0)
			dnom[--ind] = new CshDenom();
	}

	/**************************************************************************
	 * initialize tender with params
	 *
	 * @param sc
	 *            subcode
	 * @param ptr
	 *            S_REG data record
	 ***************************************************************************/
	void init(int sc, LocalREG ptr) {
		if (sc == 1) {
			text = ptr.text;
			icnt = ptr.flag & 3;
			dec = tbl[0].dec;
			xsym = tbl[0].xsym;
			symbol = tbl[0].symbol;
		} else if (sc < 6)
			ctrl = 0;
		if (sc == 8) {
			dec = ptr.rate % 10;
			club = ptr.rate / 10;
			xflg = ptr.tflg;
			xsym = ptr.text.charAt(11);
			xtext = ptr.text.substring(0, 8);
			symbol = ptr.text.substring(8, 11).trim();
			unit = ptr.block[0].items;
			value = unit > 0 ? ptr.block[0].total : 0;
		}
		if (sc == 15)
			rate = ptr.rate; /* auto discount */
		if (sc == 16)
			xtra = ptr.rate; /* auto surcharge */
	}

	/***************************************************************************
	 * round tender to smallest coin
	 *
	 * @param total
	 *            the monitary amount in tender currency
	 * @return the rounded result (0 no, 1-4 down, 5-9 up)
	 ***************************************************************************/
	long round(long total) {
		if (coin < 2)
			return total;
		// TSC-ENH2014-3-AMZ#BEG -- customer favour
		if (customerFavour) {
            // AMZ-2017-004-001#BEG
            if(GdSarawat.getRoundReturnsCustomerFavour()){
                if(Struc.tra.bal < 0){
                    return ((total / coin) + ((total%coin)>0?1:0)) * coin;
                }
            }
            // if total > 0 fall thru to default behaviour
            // AMZ-2017-004-001#END
            return (total / coin) * coin;
		}
		// TSC-ENH2014-3-AMZ#END -- customer favour
		return roundBy(total, coin) * coin;
	}

	/***************************************************************************
	 * round change to smallest coin
	 *
	 * @param total
	 *            the monitary amount in tender currency
	 * @return the rounded result (1-5 down, 6-9 up, 0 no)
	 ***************************************************************************/
	long change(long total) {
		return change(total, false);
	}

	long change(long total, boolean negativeTransaction) {
		// TSC-ENH2014-3-AMZ#BEG -- customer favour
		if (customerFavour) {
            // AMZ-2017-004-001#BEG
            if(GdSarawat.getRoundReturnsCustomerFavour()){
                return (total - coin + 1) / coin * coin;
            }
            // AMZ-2017-004-001#END
			return negativeTransaction ? (total + coin - 1) / coin * coin : (total - coin + 1) / coin * coin;

		}
		// TSC-ENH2014-3-AMZ#END -- customer favour
		int mod = coin - 1 >> 1;

		if (mod < 1)
			return total;
		return (total < 0 ? total - mod : total + mod) / coin * coin;
	}

	/***************************************************************************
	 * foreign currency exchange to home currency
	 *
	 * @param total
	 *            the monitary amount in tender currency
	 * @return the rounded result in home currency
	 ***************************************************************************/
	long fc2hc(long total) {
		if (unit < 1)
			return total;
		total *= 10;
		if (club > 0) {
			TndMedia lead = tbl[club];
			if (lead.unit > 0)
				total = total * lead.unit / lead.value;
		}
		return roundBy(total * unit / value, 10);
	}

	/***************************************************************************
	 * home currency exchange to foreign currency
	 *
	 * @param total
	 *            the monitary amount in home currency
	 * @return the rounded result in tender currency
	 ***************************************************************************/
	long hc2fc(long total) {
		if (unit < 1)
			return total;
		total *= 10;
		if (club > 0) {
			TndMedia lead = tbl[club];
			if (lead.unit > 0)
				total = total * lead.value / lead.unit;
		}
		return roundBy(total * value / unit, 10);
	}

	/***************************************************************************
	 * edit foreign currency exchange rate
	 *
	 * @param full
	 *            including descriptive text and currency symbol if true
	 * @return new String as defined by REG sc08
	 ***************************************************************************/
	String editXrate(boolean full) {
		String s = editDec(club > 0 ? value : unit, xflg & 0x0f);
		return full ? xtext + symbol + s : s;
	}
}

/*******************************************************************
 * local/remote file definitions and memory table access
 *******************************************************************/
abstract class Table extends Struc {
	static LocalACT lACT = new LocalACT("ACT", 1);
	static LocalDPT lDPT = new LocalDPT("DPT", S_MOD);
	static LocalREG lREG = new LocalREG("REG", 1);
	static LocalSLM lSLM = new LocalSLM("SLM", S_MOD);

	static LocalCTL lCTL = new LocalCTL("CTL");
	static LocalLAN lLAN = new LocalLAN("LAN", S_CKR);
	static LocalPOT lPOT = new LocalPOT("POT", S_CKR, lREG.block[0]);
	static LocalPOS lPOS = new LocalPOS("POS");
	static RmoteBIL rBIL = new RmoteBIL("BIL");
	static RmoteCLS rCLS = new RmoteCLS("CLS");
	static RmoteHCA rHCA = new RmoteHCA("HCA");
	static RmoteNEW rNEW = new RmoteNEW("NEW");
	static RmoteSAR rSAR = new RmoteSAR("SAR");
	static RmoteVNU rVNU = new RmoteVNU("VNU");

	static HshIo lPLU = new HshIo("PLU", 16, 80);
	static HshIo lCLU = new HshIo("CLU", 16, 80);
	static HshIo lGLU = new HshIo("GLU", 16, 80);
	static LinIo rMNT = new LinIo("MNT", 0, 88);

	static BinIo lALU = new BinIo("ACC", 4, 50);
	static BinIo lDLU = new BinIo("DPT", 4, 54);
	static BinIo lSLU = new BinIo("SLM", 4, 40);
	static BinIo lRLU = new BinIo("RBT", 16, 80);
	static BinIo lRCD = new BinIo("RCD", 4, 30);
	static BinIo lREF = new BinIo("REF", 9, 54);
	static BinIo lCIN = new BinIo("CIN", 16, 80);
	static BinIo lCGR = new BinIo("CGR", 16, 80);
	static BinIo lPWD = new BinIo("PWD", 4, 6);
	static BinIo lQLU = new BinIo("QUA", 16, 80);
	static BinIo lSIN = new BinIo("SIN", 7, 90);
	static BinIo lSET = new BinIo("SET", 9, 54);
	static BinIo lDBL = new BinIo("DBL", 7, 50);

	static DatIo lBOX = new DatIo("BOX", 4, 38);
	static DatIo lCRD = new DatIo("CRD", 19, 44);
	static DatIo lDDQ = new DatIo("DDQ", 7, 40);

	static DatIo lBOF = new DatIo("BOF", 0, 42);
	static DatIo lTRA = new DatIo("TRA", 0, 80);
	static SeqIo lIDC = new SeqIo("IDC", 0, 80);
	static SeqIo lJRN = new SeqIo("JRN", 0, 44);
	static SeqIo lGPO = new SeqIo("GPO", 0, 44);
	static SeqIo lDTL = new SeqIo("DTL", 0, 34);

	static DatIo lFile[] = { lIDC, lJRN, lDTL, lGPO, lSLM, lDPT, lACT, lPOT, lREG };

	// EMEA-UPB-DMA#A BEG
	static BinIo lUPB = new BinIo("UPB", 16, 80);
	// EMEA-UPB-DMA#A END
	static TableSls act = new TableSls(lACT);
	static TableDpt dpt = new TableDpt(lDPT);
	static TableReg reg = new TableReg(lREG);
	static TableSls slm = new TableSls(lSLM);
	static TableRbt[] rbt = new TableRbt[S_RBT];
	static TableMmt[] mmt = new TableMmt[S_MMT];

	/***************************************************************************
	 * initialize parameter and sales tables in memory
	 ***************************************************************************/
	static void tblInit() {
		int rec;

		for (rec = tnd.length; rec > 0; tnd[--rec] = new TndMedia())
			;
		for (rec = mat.length; rec > 0; mat[--rec] = new MsgLines())
			;
		for (rec = slp.length; rec > 0; slp[--rec] = new SlpLines())
			;
		for (rec = eod.length; rec > 0; eod[--rec] = new EodTypes())
			;
		for (rec = rbt.length; rec > 0; rbt[--rec] = new TableRbt())
			;
		for (rec = vat.length; rec > 0; vat[--rec] = new TaxRates())
			;

		act.init();
		dpt.init();
		slm.init();
		tnd[0].dec = NumberFormat.getCurrencyInstance().getMaximumFractionDigits();
		while (lREG.read(++rec, lREG.LOCAL) > 0) {
			reg.key[rec - 1] = lREG.key;
			int ic = lREG.key / 100, sc = lREG.key % 100;
			if (ic == 1 && sc == 1) {
				version[sc] = lREG.rate;
				ctl.gross = lREG.block[0].total;
			}
			if (rec > lPOT.getSize()) {
				if (ic < 10 || sc < 8)
					lREG.block[0].reset();
				lPOT.key = lREG.key;
				lPOT.write();
			}
			if (ic > 9 && sc > 0)
				tnd[ic - 10].init(sc, lREG);
			if (ic != 7)
				continue;
			if (sc > 0 && sc < 9) {
				vat[sc - 1].rate = lREG.rate;
				vat[sc - 1].text = lREG.text;
				if (lREG.text.charAt(5) == '*')
					vat[sc - 1].flat = 1;
			}
			if (sc > 10 && sc < 19) {
				if (vat[sc - 11].flat > 0)
					vat[sc - 11].text = lREG.text;
			}
		}
		for (rec = 0; lPOS.read(++rec) > 0; posWrite(rec, 0, 0L))
			;
		if (lLAN.getSize() < 1) {
			lLAN.write();
			lLAN.key = ctl.reg_nbr;
			lLAN.org = version[1];
			lLAN.dat = ctl.zero;
			lLAN.rewrite(1);
			lLAN.sync();
			lPOT.sync();
		}
        ExtResume.consolidateTra(); // AMZ-2017#ADD
		lTRA.open("data", "S_TRA" + REG + ".DAT", 2);
		lDDQ.open(null, "P_REG" + lDDQ.id + ".DAT", 0);
		UpSet.init();
	}

	/***************************************************************************
	 * search for active checker in POT using local LAN
	 *
	 * @param nbr
	 *            checker number (000-799)
	 * @return index of checker or first free slot, -1=full
	 ***************************************************************************/
	static int ckrBlock(int nbr) {
		lLAN.read(1, lLAN.LOCAL);
		for (int ind = 0; ind < lLAN.tbl.length; ind++) {
			if (lLAN.tbl[ind] == nbr)
				return ind;
			if (lLAN.tbl[ind] < 1)
				return ind;
		}
		return lLAN.ERROR;
	}

	/***************************************************************************
	 * access combinations of REG header and POT data
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sel
	 *            selection (>0=terminal/group, 0=all terminals, -1=local)
	 * @return record size - 2 (0 = end of file)
	 ***************************************************************************/
	static int ckrRead(int rec, int sel) {
		String id = lREG.id;
		lREG.id = lPOT.id;
		int sts = lREG.read(rec, sel);
		if (sel == lREG.LOCAL)
			if (sts > 0)
				lPOT.readSls(rec, lPOT.blk);
		lREG.id = id;
		return sts;
	}

	/***************************************************************************
	 * update financial report data in REG and POT
	 *
	 * @param rec
	 *            relative record number (1 <= rec <= records in file)
	 * @param sls
	 *            single sales total structure
	 ***************************************************************************/
	static void ckrWrite(int rec, Sales sls) {
		sls.write(rec, 0, lREG);
		if (ctl.ckr_nbr < 800) {
			sls.write(rec, lPOT.blk, lPOT);
		}
	}

	/***************************************************************************
	 * update financial report data in CMOS and REG ic00
	 *
	 * @param sc
	 *            subcode 00 - 08
	 * @param trans
	 *            transaction count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void posWrite(int sc, int trans, long total) {
		lPOS.read(sc);
		lPOS.trans = (lPOS.trans + trans) % 10000;
		lPOS.total = (lPOS.total + total) % 10000000000L;
		if (sc == 1) {
			if (lPOS.trans < 1)
				lPOS.trans++;
			ctl.tran = lPOS.trans;
		}
		if (sc == 2)
			ctl.zero = lPOS.trans;
		lPOS.rewrite(sc);
		int blk = 0, rec = reg.find(0, sc);
		if (rec > 0) {
			lREG.readSls(rec, blk);
			lREG.block[blk].trans = lPOS.trans;
			lREG.block[blk].items = 0;
			lREG.block[blk].total = lPOS.total;
			lREG.writeSls(rec, blk);
		}
	}

	/***************************************************************************
	 * read control data of department not in DLU from DPT into department lookup structure (dlu), if dpt
	 ***************************************************************************/
	static void getMemdpt(int rec) {
		lDPT.read(rec, lDPT.LOCAL);
		dlu.flag = lDPT.flag;
		dlu.sit = lDPT.sit;
		dlu.vat = lDPT.vat;
		dlu.cat = lDPT.cat;
		dlu.halo = lDPT.halo;
		dlu.flg2 = lDPT.flg2;
		dlu.ages = lDPT.ages;
		dlu.type = lDPT.type;
		dlu.text = lDPT.text;
		dlu.xtra = lDPT.xtra;
	}

	/***************************************************************************
	 * search for slip control data
	 *
	 * @param code
	 *            action code
	 * @return index into form definition table (slp), -1=not found
	 ***************************************************************************/
	static int slpFind(int code) {
		int ind = slp.length;
		while (ind-- > 0)
			if (slp[ind].top > 0)
				if (slp[ind].code == code)
					break;
		return ind;
	}

	/***************************************************************************
	 * clear local totals at end of transaction
	 ***************************************************************************/
	static void tblClear() {
		int nbr = 0, rec;
		act.reset();
		dpt.reset();
		reg.reset();
		slm.reset();
		for (rec = vat.length; rec > 0; vat[--rec].reset())
			;
		for (rec = rbt.length; rec > 0; rbt[--rec].reset())
			;
		for (rec = mmt.length; rec > 0; mmt[--rec] = new TableMmt())
			;

		for (ctl.alert = false; rec < tnd.length; rec++) {
			if (tnd[rec].limit[L_MaxDrw] > 0)
				ctl.alert |= tnd[rec].alert >= tnd[rec].limit[L_MaxDrw];
		}
		for (rec = 0; lTRA.read(++rec) > 0;) {
			char type = lTRA.pb.charAt(32);
			if (type == 'C') /* skip empl/cust % template */
				if (lTRA.pb.charAt(35) == '9')
					continue;
			// EMEA-UPB-DMA#BEG
			if (type == 'u') {
				String ean = lTRA.pb.substring(43, 59);
				// logger.info("yyyy: " + lTRA.pb);
				// logger.info("xxxx: >" + ean+"<");
				int i = WinUpb.getInstance().findUpbTra(ean, false);
				if (i >= 0 && tra.itemsVsUPB.get(i).isConfirmed()) {
					lTRA.pb = lTRA.pb.substring(0, lTRA.pb.length() - 1) + "0";
				}
			}
			// EMEA-UPB-DMA#END
			lIDC.onto(0, lTRA.scan(28)).push(editNum(++nbr, 3));
			lIDC.push(lTRA.skip(3).scan(3)).push(editNum(tra.mode, 1));
			lIDC.push(lTRA.pb.substring(++lTRA.index));
			if (type == 'F')
				if (ctl.alert)
					lIDC.poke(38, '*');
			lIDC.write();
		}
		lLAN.read(1, lLAN.LOCAL);
		lLAN.ckr = ctl.ckr_nbr;
		lLAN.sts = tra.code;
		lLAN.idc = lIDC.getSize();
		lLAN.jrn = lJRN.getSize();
		if (cntLine.recno > 0)
			lLAN.mnt = cntLine.recno;
		lLAN.dtl = lDTL.getSize();
		lLAN.gpo = lGPO.getSize();
		lLAN.date = ctl.date;
		lLAN.time = ctl.time;
		lLAN.lan = ctl.lan;
		if (lLAN.ckr < 800)
			lLAN.tbl[lPOT.blk] = ctl.ckr_nbr;
		lLAN.rewrite(1);
		lLAN.sync();
		lCTL.sync();
		lIDC.sync();
		lGPO.sync();
		lJRN.sync();
		lTRA.close();
		lCIN.close();
		lCGR.close();
		lTRA.open("data", "S_TRA" + REG + ".DAT", 2);
		lDDQ.recno = 1;
		net.writeSls(0, 0, lLAN);
	}

	/***************************************************************************
	 * write local totals at end of transaction
	 ***************************************************************************/
	static void tblWrite() {
		int rec = 0;
		Delta.control('H', ctl.time / 100 + ctl.date * 10000);
		while (rec < reg.key.length) {
			int ic = reg.key[rec] / 100, sc = reg.key[rec] % 100;
			Sales sls = reg.sales[rec++][0];
			if (ic == 1) {
				if (sc == 1) {
					ctl.gross += sls.total;
					posWrite(sc, 1, sls.total);
					ckrWrite(rec, sls);
					if (tra.mode > 1 && tra.mode < 9) {
						sls.set(-sls.items, -sls.total);
						ckrWrite(reg.find(ic, tra.mode), sls);
					}
					continue;
				}
			} else if (tra.mode > 1 && ic != 9)
				continue;
			if (sls.isZero())
				continue;
			ckrWrite(rec, sls);
			if (ic > 9) {
				if (sc > 3)
					continue;
				tnd[ic - 10].alert += sc > 2 ? -sls.total : sls.total;
			}
		}
		lREG.sync();
		lPOT.sync();

		if (tra.mode < 2 && ctl.ckr_nbr < 800) {
			for (rec = 0; rec < act.key.length; act.write(++rec))
				;
			for (rec = 0; rec < dpt.key.length; dpt.write(++rec))
				;
			for (rec = 0; rec < slm.key.length; slm.write(++rec))
				;
			lACT.sync();
			lDPT.sync();
			lSLM.sync();
		}
		Delta.control('F', lDTL.recno);
		tblClear();
	}

	/***************************************************************************
	 * add to hourly activity totals
	 *
	 * @param key
	 *            integer time (hhmm)
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumAct(int key, int items, long total) {
		int ind = 0, rec = 0;
		key = keyValue(String.valueOf(key));
		while (ind < act.key.length) {
			if (act.key[ind++] < 0x2400) {
				if (key < act.key[ind - 1])
					break;
				rec = ind;
			}
		}
		act.addSales(rec, 0, items, total);
	}

	/***************************************************************************
	 * add to checker / register totals
	 *
	 * @param ic
	 *            item code
	 * @param sc
	 *            subcode
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumReg(int ic, int sc, int items, long total) {
		int rec = reg.find(ic, sc);
		reg.addSales(rec, 0, items, total);
	}

	/***************************************************************************
	 * add to deparmtment totals using itm.dpt and itm.cat
	 *
	 * @param blk
	 *            total block index (0 - 2)
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumDpt(int blk, int items, long total) {
		dpt.addSales(itm.dpt, blk, items, total);
		if (itm.cat > 50)
			accumReg(8, itm.cat, blk > 0 ? 0 : items, total);
	}

	/***************************************************************************
	 * add to salesmen totals using tra.slm
	 *
	 * @param blk
	 *            total block index (0 - 2)
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumSlm(int blk, int items, long total) {
		slm.addSales(itm.slm, blk, items, total);
	}

	/***************************************************************************
	 * add to tax info block
	 *
	 * @param sc
	 *            basic subcode (00=tax, 10=sales, 20=deposit, 30=refund)
	 * @param ind
	 *            index into vat table (0-7)
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumTax(int sc, int ind, int items, long total) {
		accumReg(7, sc + ind + 1, items, total);
	}

	/***************************************************************************
	 * accumulate to individual (itm.tnd) and total tender
	 *
	 * @param sc
	 *            subcode (1=sales, 2=loan, 3=pickup, 4=onhand, 5=float etc)
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumTnd(int sc, int items, long total) {
		accumReg(10 + itm.tnd, sc, items, total);
		accumReg(10, sc, items, tnd[itm.tnd].fc2hc(total));
	}

	/***************************************************************************
	 * accumulate bonus points
	 *
	 * @param sc
	 *            subcode (1-8) of ic30
	 * @param items
	 *            item count
	 * @param total
	 *            monitary amount
	 ***************************************************************************/
	static void accumPts(int sc, int items, long total) {
		accumReg(8, 30 + sc, items, total);
	}

	/***************************************************************************
	 * determination of subcodes
	 *
	 * @param mask
	 *            bit mask of preselections (1 byte)
	 * @return number of highest bit set in mask (8, 7, ... 1)
	 ***************************************************************************/
	static int sc_value(int mask) {
		for (int ind = 1; mask > 0; ind++)
			if ((mask <<= 1) > 255)
				return ind;
		return 0;
	}

	/***************************************************************************
	 * determination of subcode for storage of bonuspoints
	 *
	 * @param ind
	 *            type of points (0=on item, 1=on total, 2=free, 3=redeemed)
	 * @return subcode for customers (1-4) or anonymous (5-8)
	 ***************************************************************************/
	static int sc_points(int ind) {
		return (tra.spf2 > 0 ? 1 : 5) + ind;
	}
}
