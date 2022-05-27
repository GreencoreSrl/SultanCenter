package com.ncr;

import com.ncr.eft.EftPlugin;
import com.ncr.eft.EftPluginManager;
import com.ncr.eft.GeideaEftPlugin;
import com.ncr.eft.MarshallEftPlugin;
import com.ncr.struc.Customer;

import java.util.*;

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
	// TSC-MOD2014-AMZ#BEG
	/** credit card number --unused ? **/
	String credit = "";
	// TSC-MOD2014-AMZ#END
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
	public static LinIo dspLine = new LinIo("DSP", 0, 20);
	static LinIo oplLine = new LinIo("OPL", 0, 20);
	static LinIo hdrLine = new LinIo("HDR", 0, 20);
	static LinIo cusLine = new LinIo("CUS", 0, 20);
	static LinIo stsLine = new LinIo("STS", 0, 20);
	static LinIo cntLine = new LinIo("CNT", 0, 20);
	static LinIo idsLine = new LinIo("IDS", 0, 17);
	public static LinIo prtLine = new LinIo("PRN", 1, 42);

	public static Terminal ctl = new Terminal();
	public static Transact tra = new Transact(), dct;
	public static Itemdata itm;
	static Itemdata pit;
	static Itemdata plu;
	static Itemdata ref;
	static Itemdata dci;
	static Itemdata dlu = new Itemdata();
	static Customer cus;
	static PayCards ecn = new PayCards();
	static Monitors mon = new Monitors();

	public static EftPluginManager eftPluginManager = EftPluginManager.getInstance();

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

	public static TaxRates vat[] = new TaxRates[8];
	public static TndMedia tnd[] = new TndMedia[40];
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
	static String specialHeader[][] = new String[10][10];

	static {
		TndMedia.tbl = tnd; /* for inner base reference */
	}
}
