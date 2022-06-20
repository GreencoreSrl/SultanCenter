package com.ncr;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*******************************************************************
 *
 * definitions of constants in use on access to data structures
 *
 *******************************************************************/
interface Constant {
	/* size table */
	/** sales categories by ckr **/
	static final int S_CAT = 40;
	/** checkers per terminal **/
	static final int S_CKR = 16;
	/** mix/match table entries **/
	static final int S_MMT = 100;
	/** totals by dpt / slm **/
	static final int S_MOD = 3;
	/** max. lan message size **/
	static final int S_MSG = 400;
	/** rebate table entries **/
	static final int S_RBT = 10;

	/* ids of financial totals */

	/* mode: sales and others */
	/** gross sales **/
	static final int M_GROSS = 1;
	/** sales abort **/
	static final int M_CANCEL = 2;
	/** training **/
	static final int M_SCHOOL = 3;
	/** re-entry **/
	static final int M_RENTRY = 4;
	/** inventory **/
	static final int M_INVTRY = 5;
	/** goods xfer **/
	static final int M_LEDGER = 6;
	/** layaway **/
	static final int M_LAYWAY = 7;
	/** suspend sales **/
	static final int M_SUSPND = 8;

	/* spf1: negative selling */
	/** expenses **/
	static final int M_EXPNSE = 0x80;
	/** neg. plu's / dept's **/
	static final int M_REFUND = 0x40;
	/** credits **/
	static final int M_CREDIT = 0x20;
	/** item return **/
	static final int M_RETURN = 0x10;
	/** transaction return **/
	static final int M_TRRTRN = 0x08;
	/** transaction void **/
	static final int M_TRVOID = 0x04;
	/** item void **/
	static final int M_VOID = 0x02;
	/** error corrects **/
	static final int M_ERRCOR = 0x01;

	/* spf2: sales reductions */
	/** price overrides **/
	static final int M_PRCRED = 0x80;
	/** item credits **/
	static final int M_ITMCRD = 0x40;
	/** item discount **/
	static final int M_ITMDSC = 0x20;
	/** automatic rebate **/
	static final int M_REBATE = 0x10;
	/** special mixmatch **/
	static final int M_SPCMXM = 0x08;
	/** limited quantity **/
	static final int M_SPCLQD = 0x04;
	/** employee discount **/
	static final int M_EMPDSC = 0x02;
	/** customer discount **/
	static final int M_CUSDSC = 0x01;

	/* spf3: money reductions */
	/** rounding after tax add **/
	static final int M_RNDTAX = 0x80;
	/** p&l by currency rounding **/
	static final int M_RNDEXC = 0x40;
	/** auto rebate on SI totals **/
	static final int M_TOTRBT = 0x20;
	/** surcharge on delivery **/
	static final int M_CHARGE = 0x10;
	/** discount on total amount **/
	static final int M_TOTDSC = 0x02;
	/** discount for cash **/
	static final int M_CSHDSC = 0x01;

	/* sales item properties */

	// TSC-MOD2014-AMZ#BEG
	static final int F_QTYPRH = 0x100;
	// TSC-MOD2014-AMZ#END
	/* plu/dpt flag */
	/** negative sales plu / dpt **/
	static final int F_NEGSLS = 0x80;
	/** bottle deposit **/
	static final int F_DPOSIT = 0x40;
	/** weight/quantity required **/
	static final int F_WEIGHT = 0x20;
	/** no promotion points **/
	static final int F_XPROMO = 0x10;
	/** special sales flag **/
	static final int F_SPCSLS = 0x08;
	/** print on slip station **/
	static final int F_ONSLIP = 0x04;
	/** skip SKU after coded dpt **/
	static final int F_SKPSKU = 0x02;
	/** decimal quantity allowed **/
	static final int F_DECQTY = 0x01;

	/* plu/dpt flg2 */
	/** sales temporarily locked **/
	static final int F_LOCKED = 0x80;
	/** quantity prohibited **/
	static final int F_NONQTY = 0x40;
	/** sales to FS members only **/
	static final int F_MEMBER = 0x20;
	/** serial no entry required **/
	static final int F_SERIAL = 0x10;
	/** check expiration date **/
	static final int F_EXPIRY = 0x08;
	/** charitable tax exempt **/
	static final int F_CHARIT = 0x04;
	/** free item if price zero **/
	static final int F_GRATIS = 0x02;
	/** food stampable **/
	static final int F_FSABLE = 0x01;

	/* tender properties */
	/** negative balance tender **/
	static final int T_NEGTND = 0x80;
	/** enter receipt number **/
	static final int T_NUMBER = 0x40;
	/** receipt no in trans void **/
	static final int T_VOIDNO = 0x20;
	/** print ec formatted slip **/
	static final int T_ONSLIP = 0x10;
	/** bank reference info **/
	static final int T_BNKREF = 0x08;
	/** force skip on tender amt **/
	static final int T_NOAMNT = 0x04;
	/** no skip on tender amt **/
	static final int T_NOSKIP = 0x02;
	/** no automatic change **/
	static final int T_NOAUTO = 0x01;

	//TAU-20160816-SBE#A BEG
	/* tender properties flg2*/
	static final int T_NEGTRA = 0x01;
	//TAU-20160816-SBE#A END


	/* options in COPT0 */
	/** cash and carry (add vat) **/
	static final int O_CandC = 0;
	/** card reading enforcement **/
	static final int O_xCaRd = 1;
	/** print device for 2nd cpy **/
	static final int O_copy2 = 2;
	/** 42 columns ec (top lf's) **/
	static final int O_chk42 = 3;
	/** show vat if above halo **/
	static final int O_Vaton = 4;
	/** enforce 10=cust 01=slmNo **/
	static final int O_SLMon = 5;
	/** control 10=void 01=ec1st **/
	static final int O_VdCtl = 6;
	/** 01=21x8 char 10=sort ref **/
	static final int O_ElJrn = 7;
	/** till alert after xx secs **/
	static final int O_Alert = 8;
	/** auto pause after xx mins **/
	static final int O_Pause = 9;
	/** ckr secret nbr at random **/
	static final int O_SecNo = 10;
	/** sec nbr lifetime in days **/
	static final int O_SecEx = 11;
	/** item print suppression **/
	static final int O_ItmPr = 12;
	/** allow authorization mode **/
	static final int O_Autho = 13;
	/** 1=tsep, 2=' 'before dsep **/
	static final int O_EdAmt = 14;
	/** 1 = wait until enter key **/
	static final int O_Scale = 15;
	/** double wide total print **/
	static final int O_DWide = 16;
	/** 10=none, 01=ok by Clear **/
	static final int O_xTill = 17;
	/** ckr name after this line **/
	static final int O_CKRon = 18;
	/** IDC/JRN visible digits **/
	static final int O_CardX = 19;

	/* options in COPT1 */
	/** E-record 1=maybe, 2=must **/
	static final int O_PluEx = 20;
	/** Unicode pg graphic print **/
	static final int O_Graph = 21;
	/** 1=info 2=fiscal 10=hocus **/
	static final int O_Custo = 22;
	/** combi 1=ec 2=void 3=both **/
	static final int O_Clean = 23;
	/** Sync 1=JRN 2=GPO 3=both **/
	static final int O_Sync = 24;

	/* pseudo tenders in CMNY */
	/** alternate home currency **/
	static final int K_AltCur = 00;
	/** RoA subcode of donation **/
	static final int K_DonaSc = 16;
	/** tender number of change **/
	static final int K_Change = 17;
	/** EFT tender (EoD totals) **/
	static final int K_TndEft = 19;

	/* limits in tender media */
	/** maximum amount in drawer **/
	static final int L_MaxDrw = 0;
	/** minimum amount tendered **/
	static final int L_MinTnd = 1;
	/** maximum amount tendered **/
	static final int L_MaxTnd = 2;
	/** minimum amount offline **/
	static final int L_MinOfl = 3;
	/** maximum amount offline **/
	static final int L_MaxOfl = 4;
	/** minimum amount cashback **/
	static final int L_MinCsh = 5;
	/** maximum amount cashback **/
	static final int L_MaxCsh = 6;
	/** min. sales for cashback **/
	static final int L_MinSls = 7;
	/** maximum amount of change **/
	static final int L_MaxChg = 8;
	Set<Integer> SALESTRN_MODES = new HashSet<Integer>(Arrays.asList(M_GROSS, M_CANCEL, M_SUSPND));
	// EMEA-UPB-DMA#A BEG
	public static final int UPB_TIMEOUT_ERROR = 251;
	public static final int UPB_TRA_VOID = 1001;
	// EMEA-UPB-DMA#A END
}
