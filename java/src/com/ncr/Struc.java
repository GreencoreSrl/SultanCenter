package com.ncr;

import java.util.*;

/*******************************************************************
 * terminal control data
 *******************************************************************/
class Terminal extends FmtIo {
	/** terminal number 3hex **/
	int reg_nbr = Integer.parseInt(REG, 16);
	/** server number 3hex **/
	int srv_nbr = Integer.parseInt(SRV, 16);
	/** terminal group number 2hex + 0xF00 **/
	int grp_nbr = Integer.parseInt(GRP, 16) + 0x0F00;
	/** store number 4dec **/
	int sto_nbr = Integer.parseInt(STO, 10);
	/** accountability option 0=terminal, 1=cashier, (2=cashdrawer) **/
	int ability = Integer.getInteger("ACCOUNTABILITY", 1).intValue();
	/** checker number 001-799=cashier, 800-999=supervisor **/
	int ckr_nbr;
	/** secret number of operating checker **/
	int ckr_sec;
	/** date of birth yymmdd **/
	int ckr_age;
	/** lan (local area network) status 0=online, 1=offline, 2=mismatch, 3=standalone **/
	int lan = 3;
	/** record number of operating checker in CTL file **/
	int ckr;
	/** record number of authorizing supervisor in CTL file **/
	int sup;
	/** mode of operation 0=normal, 3=training, 4=re-entry **/
	int mode;
	/** current transaction number 4dec **/
	int tran;
	/** number of generations since CMOS reset (day count) **/
	int zero;
	/** current journal view 0=all transactions, 1=active transaction **/
	int view;
	/** checkers working time in seconds until start of transaction **/
	int work;
	/** current century 2dec cc **/
	int cent;
	/** current date 6dec yymmdd **/
	int date;
	/** current time 6dec hhmmss **/
	int time;
	/** current time milliseconds 3dec **/
	int msec;
	/** current day of week (1-7, 1=sunday) **/
	int wday;
	/** current day of year (1-365/366) **/
	int yday;
	/** current daily gross total **/
	long gross;
	/** cash drawer limit exceeded **/
	boolean alert;
	/** eod-blocking event **/
	boolean block;

	/** set current date, time, msec, wday, yday **/
	void setDatim() {
		Calendar c = sdf.getCalendar();
		c.setTime(new Date());
		cent = (c.get(c.YEAR) / 100);
		date = (c.get(c.YEAR) % 100 * 100 + c.get(c.MONTH) + 1) * 100 + c.get(c.DATE);
		time = (c.get(c.HOUR_OF_DAY) * 100 + c.get(c.MINUTE)) * 100 + c.get(c.SECOND);
		msec = (c.get(c.MILLISECOND));
		wday = (c.get(c.DAY_OF_WEEK));
		yday = (c.get(c.DAY_OF_YEAR));
	}

	boolean tooYoung(int years, int birth) {
		if (birth > date)
			years -= 100;
		return date < birth + years * 10000;
	}
}

/*******************************************************************
 * transaction control data
 *******************************************************************/
class Transact {
	/** registry mode 0=money, 1=sales, 2=cancel, 5=inventory, 6=ledger, 7=layaway, 8=suspend **/
	int mode;
	/** action code 00=sales, 01=open, 02=close, etc **/
	int code;
	/** subcode (report type all, single, etc) **/
	int subc;
	/** index of title in text table (menu section) **/
	int head;
	/** negative transaction preselections (void, return) **/
	int spf1;
	/** discount preselections (employee, customer) **/
	int spf2;
	/** other preselections (tax exempt, surcharge, etc) **/
	int spf3;
	/** print preselections (no receipt, deferred receipt, slip instead **/
	int slip;
	/** prerequest copy of tenderization on slip **/
	int tslp;
	/** personalized sales (1=anonymous, 2=customer#, 3=in file) **/
	int stat;
	/** record number of salesperson in SLM data file **/
	int slm;
	/** record number of sales total line in GPO data file **/
	int gpo;
	/** transaction resumption mode **/
	int res;
	/** time of transaction start in seconds **/
	int tim;
	/** tenderization phase (1 after 1st tender) **/
	int tnd;
	/** customer's date of birth yymmdd **/
	int age;
	/** supervisor number closing a cashier **/
	int who;
	/** terminal number selected in reports **/
	int comm;
	/** rate of employee/customer discount (1 decimal place assumed) **/
	int rate;
	/** rate of surcharge on delivery (1 decimal place assumed) **/
	int xtra;
	/** salesperson short number **/
	int slm_nbr;
	/** salesperson employee number **/
	int slm_prs;
	/** signed count of items **/
	int cnt;
	/** signed amount of sales total **/
	long amt;
	/** signed count of items with surcharge **/
	int chg_cnt;
	/** signed amount of surcharge **/
	long chg_amt;
	/** signed amount of sales with possible surcharge **/
	long chg_sls;
	/** signed count of items with employee/customer discount **/
	int dsc_cnt;
	/** signed amount of employee/customer discount **/
	long dsc_amt;
	/** signed amount of sales with possible discount **/
	long dsc_sls;
	/** signed count of items with possible bonuspoints **/
	int pnt_cnt;
	/** signed amount of bonuspoints **/
	int pnt;
	/** signed amount of sales with possible bonuspoints **/
	long pnt_sls;
	/** signed count of items before latest subtotal **/
	int sub_cnt;
	/** signed amount of items before latest subtotal **/
	long sub_amt;
	/** signed amount of discount on totals (dept, si, tl) **/
	long rbt_amt;
	/** signed amount of all auto discounts on totals (rbt + dp) **/
	long tld_amt;
	/** signed balance due **/
	long bal;
	/** signed amount of total cashback **/
	long csh_bck;
	/** employee/customer or other id number **/
	String number = "";
	/** tax exemption permit number **/
	String taxidn = "";

	public int prpnt;	// PSH-ENH-001-SBE
	public long gctnd;	// PSH-ENH-001-SBE

	ItemVector vItems = new ItemVector();
	ItemVector vItems_k = new ItemVector(); //DMA_VAT-DISTRIBUTION#A
	ItemVector vTrans = new ItemVector();
	// EMEA-UPB-DMA#A BEG
	ArrayList<UPBTrans> itemsVsUPB = new ArrayList<UPBTrans>();
	// EMEA-UPB-DMA#A END
	boolean successRedeemPoint = true;
	boolean successTransaction = true;
	int bmpSequence = 0;

	/***************************************************************************
	 * check the transacton state
	 * 
	 * @return true=in progress, false=not begun
	 ***************************************************************************/
	boolean isActive() {
		return slm_nbr > 0;
	}
}

/*******************************************************************
 * electronic payment control data
 *******************************************************************/
class PayCards {
	/** nation code valid with bank orders **/
	int home = 280;
	/** currency code (ec track3) **/
	int currency;
	/** nation code (ec track3) **/
	int nation;
	/** valid thru (track2) **/
	int yymm;
	/** card sequence number (track3) **/
	int seqno;

	/** ec account number (track3) **/
	String acct = "";
	/** ec bank number (track3) **/
	String bank = "";
	/** ec card number (manual input) **/
	String card = "";
	/** ec check number (manual input) **/
	String cheque = "";
	/** ec customer number **/
	String custom = "";
}

/*******************************************************************
 * vector of sales and discount items
 *******************************************************************/
class ItemVector extends Vector {
	/***************************************************************************
	 * add to vector of items
	 * 
	 * @param id
	 *            S=sales P=salesperson M=money, C=manual discount
	 ***************************************************************************/
	void addElement(char id, Itemdata ptr) {
		ptr.id = id;
		ptr.index = size();
		addElement(ptr);
	}

	/***************************************************************************
	 * get reference to item
	 * 
	 * @param ind
	 *            index of item in vector
	 * @return reference to com.ncr.Itemdata object
	 ***************************************************************************/
	Itemdata getElement(int ind) {
		return (Itemdata) elementAt(ind);
	}

	/***************************************************************************
	 * get element with matching id from vector of items
	 * 
	 * @param id
	 *            S=sales L=link M=money C=manual discount *=any
	 * @param ind
	 *            start of search (0, 1, ..., n)
	 * @param step
	 *            direction (-1=previous, 0=this, +1=next)
	 * @return reference to com.ncr.Itemdata object (null=not in vector)
	 ***************************************************************************/
	Itemdata getElement(char id, int ind, int step) {
		for (ind += step; ind >= 0 && ind < elementCount; ind += step) {
			Itemdata ptr = getElement(ind);
			if (id == '*' || ptr.id == id)
				return ptr;
			if (step == 0)
				break;
		}
		return null;
	}
}

/*******************************************************************
 * control data for various monitors
 *******************************************************************/
class Monitors {
	/** minute (00-59) of latest time display **/
	int clock = -1;
	/** operator display toggle (bit0=line0/1, bit1=normal/alternative text) **/
	int odisp = -1;
	/** total display in home/alternative currency **/
	int total = -1;
	/** deposit value in cash-recycler **/
	int money = -1;
	/** h/o customer account inquiry (-1=inactive, 0=started, 1=monitoring **/
	int hocus = -1;
	/** watch for keylock change in authorization dialogue **/
	int autho;
	/** ring bell for important news 0=no, 1=yes, -1=end **/
	int alert;
	/** watch for completion of image data transfer to server **/
	int image;
	/** watch for terminal status change in cluster EoD **/
	int lan99;
	/** record number of remote journal watch **/
	int watch;
	/** used to trigger automatic procedures (four ticks per second) **/
	int tick;

	/** advertizing on customer display (character position) **/
	int adv_dsp;
	/** advertizing on customer display (line of text) **/
	int adv_rec;
	/** status information message by message on operator display **/
	int opd_sts;
	/** notes/messages (originator of message received and displayed) **/
	int rcv_ckr;
	/** notes/messages (record number of message received and displayed) **/
	int rcv_dsp;
	/** notes/messages (record number of message sent and displayed) **/
	int snd_dsp;
	/** notes/messages (text received and displayed) **/
	String rcv_msg;
	/** notes/messages (header info of message received) **/
	String rcv_mon;
	/** notes/messages (header info of message sent) **/
	String snd_mon;
	/** advertizing on customer display (text) **/
	String adv_txt;
	/** total display in home/alternative currency (text) **/
	String tot_txt;
	/** operator display toggle (alternative text) **/
	String opd_alt;
}

/*******************************************************************
 * cash denominations for loan, pickup, cashcount, float
 *******************************************************************/
class CshDenom {
	/** monitary amount of denomination **/
	long value;
	/** description of denomination (operator prompt) **/
	String text;
}

/*******************************************************************
 * customer information data
 *******************************************************************/
class Customer {
	/** valid selection from menu of charge types (0=all) **/
	int spec;
	/** 01-09 = blocking reason, 10 - 99 = category **/
	int branch;
	/** employee/customer discount rate (1 decimal place assumed) **/
	int rate;
	/** rate of discount for cash (1 decimal place assumed) **/
	int dscnt;
	/** rate of surcharge on delivery (1 decimal place assumed) **/
	int extra;
	/** date-of-birth yymmdd **/
	int age;
	/** initial bonuspoints **/
	int pnt;
	/** check limit regarding CLS file data **/
	long limchk;
	/** charge limit regarding CLS file data **/
	long limcha;
	/** customer number **/
	String number;
	/** customer name **/
	String name;
	/** customer address (street and house number) **/
	String adrs;
	/** customer address (ZIP code and town) **/
	String city;
	/** customer company name **/
	String nam2;
	/** 30 characters reserved for customization issues **/
	String dtbl;
	/** 20 characters fiscal identification **/
	String fiscalId;
	/** customer telephone number **/
	String mobile; // SARAWAT-ENH-20150507-CGA#A

	String cusId;  //SPINNEYS-2017-033-CGA#A

    String selfSellEANList; // AMZ-2017-003-006#BEG - articoli da vendere automaticamente  -- e' un elenco di EAN separati da virgola ','
}

/*******************************************************************
 * messages on receipt selected by mode/actioncode
 *******************************************************************/
class MsgLines {
	/** triggering registry mode **/
	int mode;
	/** triggering actioncode **/
	int code;
	/** first line to print from mdac_txt **/
	int line;
	/** last line to print from mdac_txt **/
	int last;
	/** parameter not used ":" **/
	char logo;
	/** parameter not used " " **/
	char flag;
}

/*******************************************************************
 * slip print control info
 *******************************************************************/
class SlpLines {
	/** actioncode **/
	int code;
	/** lines to skip on top of form **/
	int top;
	/** last line available on form **/
	int end;
	/** "L" = print logo on top of form **/
	char logo;
	/** "-" = no receipt, "*" = slip instead **/
	char flag;
}

/*******************************************************************
 * end-of-day report request params
 *******************************************************************/
class EodTypes {
	/** report actioncode **/
	int ac;
	/** report subcode (0=XXXX, 1=*XXX, 2=**XX, 3=***X) **/
	int type;
	/** terminal selection (A=All consolidated, S=Single) **/
	char sel;
}

/*******************************************************************
 * static initialization of commonly used basic data structures
 *******************************************************************/
public abstract class Struc extends FmtIo implements Constant {
	static String dspBmap, dspSins;
	static LinIo dspLine = new LinIo("DSP", 0, 20);
	static LinIo oplLine = new LinIo("OPL", 0, 20);
	static LinIo hdrLine = new LinIo("HDR", 0, 20);
	static LinIo cusLine = new LinIo("CUS", 0, 20);
	static LinIo stsLine = new LinIo("STS", 0, 20);
	static LinIo cntLine = new LinIo("CNT", 0, 20);
	static LinIo idsLine = new LinIo("IDS", 0, 17);
	public static LinIo prtLine = new LinIo("PRN", 1, 42);

	static Terminal ctl = new Terminal();
	static Transact tra = new Transact(), dct;
	static Itemdata itm, pit, plu, ref, dci, dlu = new Itemdata();
	static Customer cus;
	static PayCards ecn = new PayCards();
	static Monitors mon = new Monitors();

    static VeriFoneTerminal verifone = new VeriFoneTerminal(); //VERIFONE-20160201-CGA#A

	// TAMI-ENH-20140526-SBE#A BEG
	static EftTerminal eftTerminalGeidea = new EftTerminalGeidea();
	static EftTerminal eftTerminal = eftTerminalGeidea;
	// TAMI-ENH-20140526-SBE#A END
	//static EftTerminal eftTerminalToneTag = new EftTerminalToneTag();  //TONETAG-CGA#A   //1610 del
	//static EftTerminal eftTerminalAlshaya = new EftTerminalAlshaya();  //1610 del
	//static EftTerminal eftTerminalEyePay = new EftTerminalEyePay();  // EYEPAY-20161116-CGA#A    //1610 del

	/** CandC, xCaRd, copy2, etc **/
	static int options[] = new int[40];
	/** actual reason codes **/
	static int rcd_tbl[] = new int[10];
	/** denomination counters **/
	static int dnom_tbl[] = new int[32];
	/** tare table **/
	static int tare_tbl[] = new int[100];
	/** age restriction table **/
	static int ckr_age[] = new int[10];
	/** age restriction table **/
	static int cus_age[] = new int[10];

	/** cash preset values **/
	static int csh_tbl[] = new int[10];
	/** department preset table **/
	static int dir_tbl[] = new int[15];
	/** department descriptions **/
	static String dir_txt[] = new String[15];
	/** plu preset table **/
	static String plu_tbl[] = new String[15];
	/** plu descriptions **/
	static String plu_txt[] = new String[15];
	/** list selector table **/
	static String sel_tbl[][] = new String[15][8];
	/** list descriptions **/
	static String sel_txt[][] = new String[15][8];

	/** alt tl / tender keys 1-F **/
	static int tnd_tbl[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, };

	static String key_txt[] = /* touch key inscriptions */
	{ " Confirm  ", "  Clear   ", "  Cancel  ", "  Select  ", };

	static String vrs_tbl[] = { "EXE Version ", "ORG Version ", "VRS Scale   ", "CRC Scale   ", };
	static int version[] = { 90, 0, 0, 23718 };

	static String chk_nbr[] = { "Zero*", "*One*", "*Two*", "Three", "Four*", "Five*", "*Six*", "Seven", "Eight",
			"Nine*", };

	static String ean_weights = "31313131313131313131";
	static String lbl_weights = "31731731731731731731";

	static TaxRates vat[] = new TaxRates[8];
	static TndMedia tnd[] = new TndMedia[40];
	static MsgLines mat[] = new MsgLines[5];
	static SlpLines slp[] = new SlpLines[50];
	static EodTypes eod[] = new EodTypes[10];

	static String ean_16spec[] = new String[32];
	static String msr_20spec[] = new String[32];

	static String mnt_line = "LAN PLU MAINTENANCE A00000 C00000 D00000";
	static String stl_line = "                     ------- -----------";
	static String chk_line = "NCR GREAT DEALER 90    86156 AUGSBURG   ";
	static String cpy_line = "valid with the original receipt only !!!";
	static String iht_line = "document for internal use / no receipt !";
	static String inq_line = "Ctl Dsc Vat Mm/T  Unit/Pkg   Link   Dept";
	static String trl_line = "**** ****/***/***   dd.mm.yy hh:mm AC-**";
	static String fso_line = "01 collection of frequentShopper options";
	static String ecu_line = "B954B000E954E000-000-000-000-000-000-000";

	/** alphanumeric keyboards **/
	static String kbd_alpha[] = { "  Alpha-   Keyboard  Missing! ParamDABC0",
			"  Alpha-   Keyboard  Missing! ParamDABC1", "  Alpha-   Keyboard  Missing! ParamDABC2",
			"ABCDEFGHIJKLMNOPQRST  UVWXYZ  0123456789", /* Italian Fiscal Id */
	};

	/** target terminal presets **/
	static int note_tbl[] = new int[10];
	/** contents of notices **/
	static String note_txt[] = new String[10];
	/** bank order form template **/
	static String bank_txt[] = new String[16];
	/** EURO club advertizing **/
	static String euro_txt[] = new String[16];
	/** messages by mode/ac **/
	static String mdac_txt[] = new String[16];
	/** LAN offline apology **/
	static String offl_txt[] = new String[16];
	/** total discount statement **/
	static String save_txt[] = new String[16];
	/** receipt header lines **/
	static String head_txt[] = new String[16];
	/** merchandize messages **/
	static String mess_txt[] = new String[16];
	/** charge/delivery specials **/
	static String spec_txt[] = new String[16];
	/** tax exempt certificate **/
	static String xtax_txt[] = new String[16];
	/** trans view column header **/
	static String view_txt[] = { "Item.Description....   Quantity     Pric",
			"e     Amount TagXXXXXXXXXXXXXXXXXXXXXXXX", };

	static {
		TndMedia.tbl = tnd; /* for inner base reference */
	}
}
