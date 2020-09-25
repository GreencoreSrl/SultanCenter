package com.ncr; /*******************************************************************/
/*                                                                 */
/*     mnemonic table                                              */
/*                                                                 */
/*******************************************************************/

class Mnemo {
	private static String mnemo_tbl[] = { /**************************************/
			/* MNEMO[00] */ "            ", /* MNEMO[01] */ "CASHIER     ", /* MNEMO[02] */ "SUPERVISOR  ",
			/* MNEMO[03] */ "TERMINAL    ", /* MNEMO[04] */ "TIME SLICE  ", /* MNEMO[05] */ "SALESPERSON ",
			/* MNEMO[06] */ "DEPARTMENT  ", /* MNEMO[07] */ "EMPLOYEE    ", /* MNEMO[08] */ "CUSTOMER    ",
			/* MNEMO[09] */ "ARTICLE     ", /* MNEMO[10] */ "PRESET-KEY  ", /* MNEMO[11] */ "STORE       ",
			/* MNEMO[12] */ "REASON CODE ", /* MNEMO[13] */ "TYPE        ", /* MNEMO[14] */ "ACTION CODE ",
			/* MNEMO[15] */ "NUMBER      ", /* MNEMO[16] */ "SALES / AC  ", /* MNEMO[17] */ "ITEM / TL   ",
			/* MNEMO[18] */ "DPT / ITEM  ", /* MNEMO[19] */ "COMMISSION  ", /* MNEMO[20] */ "DISCOUNT    ",
			/* MNEMO[21] */ "ITEMS       ", /* MNEMO[22] */ "SELECTION   ", /* MNEMO[23] */ "SUBTOTAL    ",
			/* MNEMO[24] */ "T O T A L   ", /* MNEMO[25] */ "TENDERED    ", /* MNEMO[26] */ "BALANCE     ",
			/* MNEMO[27] */ "CHANGE      ", /* MNEMO[28] */ "NO SALE     ", /* MNEMO[29] */ "ACCOUNT     ",
			/* MNEMO[30] */ "RECEIPT#    ", /* MNEMO[31] */ "CARD-NO     ", /* MNEMO[32] */ "CHECK#      ",
			/* MNEMO[33] */ "ACCOUNT#    ", /* MNEMO[34] */ "BANK-NO     ", /* MNEMO[35] */ "COUNT       ",
			/* MNEMO[36] */ "AMOUNT      ", /* MNEMO[37] */ "DATE        ", /* MNEMO[38] */ "TIME        ",
			/* MNEMO[39] */ "BONUS SCORE ", /* MNEMO[40] */ "SHELF/PAGE# ", /* MNEMO[41] */ "NEW PRICE   ",
			/* MNEMO[42] */ "EXTRA TILL  ", /* MNEMO[43] */ "IDENT#      ", /* MNEMO[44] */ "IDENT2      ",
			/* MNEMO[45] */ "PAGE NUMBER ", /* MNEMO[46] */ "SLIP-TOTAL  ", /* MNEMO[47] */ "REPORT / END",
			/* MNEMO[48] */ "GROUP       ", /* MNEMO[49] */ "A/Conly     ", /* MNEMO[50] */ "DISCNT/CASH ",
			/* MNEMO[51] */ "SURCHARGE   ", /* MNEMO[52] */ "CHECK LIMIT ", /* MNEMO[53] */ "CHARGE LIMIT",
			/* MNEMO[54] */ "BRANCH      ", /* MNEMO[55] */ "NET TOTAL   ", /* MNEMO[56] */ "PAID OUT    ",
			/* MNEMO[57] */ "CHANGE RATE ", /* MNEMO[58] */ "Sx FOR      ", /* MNEMO[59] */ "SALES TAX   ",
			/* MNEMO[60] */ "@VRM SCXOPSP", /* MNEMO[61] */ "WEIGHT      ", /* MNEMO[62] */ "TARE        ",
			/* MNEMO[63] */ "BONUS POINTS", /* MNEMO[64] */ "FOOD STAMPS ", /* MNEMO[65] */ "FS CHANGE   ",
			/* MNEMO[66] */ "CASH BACK   ", /* MNEMO[67] */ "TAX IDENT#  ", /* MNEMO[68] */ "PHONE#      ",
			/* MNEMO[69] */ "NON FS CASH ", /* MNEMO[70] */ "VALID THRU  ", /* MNEMO[71] */ "SERVER      ",
			/* MNEMO[72] */ "BIRTH DATE  ", /* MNEMO[73] */ "PASSWORD    ", /* MNEMO[74] */ "FISCAL ID   ",
			/* MNEMO[75] */ "EOD COUNTER ", /* MNEMO[76] */ "SERIAL-NO   ", /* MNEMO[77] */ "xx.x% IN    ",
			/* MNEMO[78] */ "            ",
			/* MNEMO[79] */ "            ",
			// TAMI-ENH-20140526-SBE#A BEG
			/* MNEMO[80] */ "AUTH CODE   ",
			/* MNEMO[81] */ "PLEASE WAIT ",
			// TAMI-ENH-20140526-SBE#A END
			// EMEA-UPB-DMA#A BEG
			/* MNEMO[82] */ "UPB NUMBER  ",
			// EMEA-UPB-DMA#A END
			// SARAWAT-ENH-20150507-CGA#A BEG
			/* MNEMO[83] */ "CUST. CARD? ", /* MNEMO[84] */ "REGIS. CUST?", /* MNEMO[85] */ "MOBILE?     ",
			/* MNEMO[86] */ "COUPON      ", /* MNEMO[87] */ "NUM. POINTS ", /* MNEMO[88] */ "POINTS      ",
			// SARAWAT-ENH-20150507-CGA#A END
			// PSH-ENH-001-AMZ#BEG
			/* MNEMO[89] */ "G.Card num :", /* MNEMO[90] */ "Info :      ", /* MNEMO[91] */ "NEW BALANCE ",
			// PSH-ENH-001-AMZ#END
            // AMZ-2017-005#BEG
            /* MNEMO[92] */ "PHONE       ",
            // AMZ-2017-005#END
			//SPINNEYS-2017-033-CGA#A BEG
			/* MNEMO[93] */ "OLD C.      ", /* MNEMO[94] */ "NEW CARD    ",
			/* MNEMO[95] */ "OLD PH.     ", /* MNEMO[96] */ "NEW PHONE   ",
			//SPINNEYS-2017-033-CGA#A END
			/* MNEMO[97] */ "CURRENCY    ", //UMNIA-20180109-CGA#A
			/* MNEMO[98] */ "PIN:        ", //UMNIA-20180109-CGA#A
			//ECR-CGA#A BEG
			/* MNEMO[99] */ "ECR AMOUNT  ",
			/* MNEMO[100]*/ "ECR RECEIPT",
			/* MNEMO[101]*/ "ECR RCPVOID",
			//ECR-CGA#A END
			/* MNEMO[102]*/ "NUM. TRANS.", //INSTASHOP-FINALIZE-CGA#A
			/* MNEMO[103]*/ "CARD TYPE  ", //INSTASHOP-FINALIZE-CGA#A


			/**************************************/
	};

	private static String super_tbl[] = { /**************************************/
			/* SUPER[00] */ "SALES       ", /* SUPER[01] */ "DISCOUNT    ", /* SUPER[02] */ "CREDITS     ",
			/* SUPER[03] */ "            ",
			/**************************************/
	};

	private static String menus_tbl[] = { /**************************************/
			/* MENUS[00] */ "CLUSTER   END-OF-DAY", /* MENUS[01] */ "OFFL.TRM. EXCLUSION ",
			/* MENUS[02] */ "DECLARE   DEFECTIVE ",
			/**************************************/
			/* MENUS[03] */ "----ADMIN-FUNCTIONS-", /* MENUS[04] */ "LOAN                ",
			/* MENUS[05] */ "PICKUP              ", /* MENUS[06] */ "FLOAT               ",
			/* MENUS[07] */ "CASHIER   SETTLEMENT", /* MENUS[08] */ "CASHIER   LIST      ",
			/* MENUS[09] */ "CASHIER   REPORT    ", /* MENUS[10] */ "                    ",
			/* MENUS[11] */ "FINANCIAL REPORT    ", /* MENUS[12] */ "ACTIVITY  REPORT    ",
			/* MENUS[13] */ "CLERK     REPORT    ", /* MENUS[14] */ "DEPARTMENTREPORT    ",
			/* MENUS[15] */ "CASHIER   BALANCE   ", /* MENUS[16] */ "CLUSTER   STATUS    ",
			/* MENUS[17] */ "PLU PRICE CHANGE    ", /* MENUS[18] */ "TERMINAL  END-OF-DAY",
			/**************************************/
			/* MENUS[19] */ "-REGISTER-FUNCTIONS-", /* MENUS[20] */ "OPERATOR  OPEN      ",
			/* MENUS[21] */ "OPERATOR  CLOSE     ", /* MENUS[22] */ "EMPLOYEE  SALES     ",
			/* MENUS[23] */ "CUSTOMER  CARD      ", /* MENUS[24] */ "RECEIVED  ON ACCOUNT",
			/* MENUS[25] */ "PAID OUT            ", /* MENUS[26] */ "EXTERNAL  POS TOTALS",
			/* MENUS[27] */ "CASHIER   PAUSE/LOCK", /* MENUS[28] */ "MEDIA     DECLARE   ",
			/* MENUS[29] */ "TRAINING  START/STOP", /* MENUS[30] */ "RE-ENTRY  START/STOP",
			/* MENUS[31] */ "STOCK     COUNT     ", /* MENUS[32] */ "TRANSFER  OUT       ",
			/* MENUS[33] */ "TRANSFER  IN        ", /* MENUS[34] */ "APPROVAL  TAKE      ",
			/* MENUS[35] */ "APPROVAL  RETURN    ", /* MENUS[36] */ "DPT KEYS  CHANGE    ",
			/* MENUS[37] */ "ANONYMOUS SALES     ", /* MENUS[38] */ "PASSWORD  CHANGE    ",
			/* MENUS[39] */ "                    ", /* MENUS[40] */ "DEVICE    SERVICES  ",
			/* MENUS[41] */ "                    ", /* MENUS[42] */ "                    ",
			/**************************************/
			/* MENUS[43] */ "----TRANS-PRESELECT-", /* MENUS[44] */ "TRANS     VOID      ",
			/* MENUS[45] */ "TRANS     RETURN    ", /* MENUS[46] */ "DELIVERY  FEES      ",
			/* MENUS[47] */ "TRANS     RESUME    ", /* MENUS[48] */ "REMOTE    INVOICE   ",
			/* MENUS[49] */ "TAX EXEMPT          ", /* MENUS[50] */ "MEDIA     EXCHANGE  ",
			/**************************************/
			/* MENUS[51] */ "-----ITEM-PRESELECT-", /* MENUS[52] */ "ITEM      VOID      ",
			/* MENUS[53] */ "ITEM      RETURN    ", /* MENUS[54] */ "MARK      DOWN      ",
			/* MENUS[55] */ "PRICE     OVERRIDE  ", /* MENUS[56] */ "COMMISSIONCODE      ",
			/* MENUS[57] */ "PRICE     INQUIRY   ", /* MENUS[58] */ "SET/BATCH SALE      ",
			/**************************************/
			/* MENUS[59] */ "----TENDER-MEDIA----", /* MENUS[60] */ "----CHARGE-TYPES----",
			/* MENUS[61] */ "----CREDIT-CARDS----", /* MENUS[62] */ "---FOREIGN-TENDERS--",
			/* MENUS[63] */ "---OFFICE-MESSAGES--",
			/**************************************/
			/* MENUS[64] */ "----REPORT-TYPE-----", /* MENUS[65] */ "ALL LEVEL 1  *XXX   ",
			/* MENUS[66] */ "ALL LEVEL 2  **XX   ", /* MENUS[67] */ "ALL LEVEL 3  ***X   ",
			/* MENUS[68] */ "SELECTIVE    XXXX   ", /* MENUS[69] */ "ONE UNDER    XXXX   ",
			/* MENUS[70] */ "ALL UNDER    XXXX   ", /* MENUS[71] */ "ALL LEVELS          ",
			/**************************************/
			/* MENUS[72] */ "--SPECIAL-FUNCTIONS-", /* MENUS[73] */ "DISCOUNT  RATE      ",
			/* MENUS[74] */ "DISCOUNT  AMOUNT    ", /* MENUS[75] */ "ERROR     CORRECT   ",
			/* MENUS[76] */ "VALIDATIONPRINT     ", /* MENUS[77] */ "SUSPEND / RESUME    ",
			/* MENUS[78] */ "PREVIOUS  DENOM     ", /* MENUS[79] */ "NEXT      DENOM     ",
			// SARAWAT-ENH-20150507-CGA#A BEG
			/* MENUS[80] */ "CUSTOMER MENU       ", /* MENUS[81] */ "POINTS REDEMPTION   ",
			/* MENUS[82] */ "POINTS INQUIRY      ", /* MENUS[83] */ "CUSTOMER CARD       ",
			/* MENUS[84] */ "CUSTOMER REGIST.    ", /* MENUS[85] */ "INSERT COUPON       ",
			/* MENUS[86] */ "COUPON NOT ACCEPTED ",
			// SARAWAT-ENH-20150507-CGA#A END
            // AMZ-2017-005#BEG
            /* MENUS[87] */ "CUSTOMER PHONE NBR. ",
            // AMZ-2017-005#END
            /* MENUS[88] */ "CUSTOMER REGISTRATIO",
            /* MENUS[89] */ "CARD REPLACEMENT    ",
            /* MENUS[90] */ "UPDATE PHONE NUMBER ",
            /* MENUS[91] */ "INTERNATIONAL CODE  ",
            /* MENUS[92] */ "PHONE LOCAL CODE    ",
			//SPINNEYS-2017-033-CGA#A BEG
            /* MENUS[93] */ "IDENTIF. BY CARD	 ",
            /* MENUS[94] */ "IDENTIF. BY PHONE   ",
            /* MENUS[95] */ "OPERATION COMPLETED ",
			//SPINNEYS-2017-033-CGA#A END
            /* MENUS[96] */ "RECHARGE TYPE       ",
			/* MENUS[97] */ "RECHARGE DETAILS    ",
			/* MENUS[98] */ "PRINT INSTASHOP     ",     //INSTASHOP-SELL-CGA#A
			/* MENUS[99] */ "INSTASHOP RESUME    ",     //INSTASHOP-FINALIZE-CGA#A
			/* MENUS[100]*/ "INSTASHOP DELIVERY  ",    //INSTASHOP-FINALIZE-CGA#A
			/* MENUS[101]*/ "HARDWARE INIT OK    ",    //WINEPTS-CGA#A
			/* MENUS[102]*/ "ECR VOID            ",    //ECR-CGA#A
			/* MENUS[103]*/ "       TYPES        ",    //INSTASHOP-SELL-CGA#A
			/* MENUS[104]*/ "                    ",    //WINEPTS-CGA#A
			/* MENUS[105]*/ "   VDI SETTLEMENT   ",    //ECR-CGA#A
			/* MENUS[106]*/ "VDI SETTLE COMPLETED",    //ECR-CGA#A
			/* MENUS[107]*/ "PENDING TRANSACTION ",    //INSTASHOP-SELL-CGA#A


			/**************************************/
	};

	private static String error_tbl[] = { /**************************************/
			/* ERROR[00] */ "--- C L O S E D --- ", /* ERROR[01] */ "**KEYLOCK*POSITION**",
			/* ERROR[02] */ "**TOO*MANY*DIGITS!**", /* ERROR[03] */ "***TOO*FEW*DIGITS***",
			/* ERROR[04] */ "***DECIMAL*PLACES***", /* ERROR[05] */ "****INPUT**ERROR****",
			/* ERROR[06] */ "** POWER RECOVERY **", /* ERROR[07] */ "*** UNAVAILABLE! ***",
			/* ERROR[08] */ "*** INVALID DATA ***", /* ERROR[09] */ "**** READ ERROR ****",
			/* ERROR[10] */ "<<< CLOSE DRAWER >>>", /* ERROR[11] */ "<<<<< JOURNAL? >>>>>",
			/* ERROR[12] */ "<<<<< RECEIPT? >>>>>", /* ERROR[13] */ "<<<<<<< SLIP >>>>>>>",
			/* ERROR[14] */ "<<<< CMOS ERROR >>>>", /* ERROR[15] */ "< ACCESS ERROR *** >",
			/* ERROR[16] */ "<<< LAN OFF-LINE >>>", /* ERROR[17] */ "< PRINTER  PROBLEM >",
			/* ERROR[18] */ "<<< INSERT SLIP! >>>", /* ERROR[19] */ "<<< REMOVE SLIP! >>>",
			/* ERROR[20] */ "***** TRAINING *****", /* ERROR[21] */ "**** RE-ENTRIES ****",
			/* ERROR[22] */ "*** SECOND  COPY ***", /* ERROR[23] */ "**** CANCELLED *****",
			/* ERROR[24] */ "***** SETTLED ******", /* ERROR[25] */ "***** NO SALE ******",
			/* ERROR[26] */ "LAN*XFER*IN*PROGRESS", /* ERROR[27] */ "**ITEM*NOT*ON*FILE**",
			/* ERROR[28] */ "*DEPARTMENT*INVALID*", /* ERROR[29] */ "LINK*ITEM*INCORRECT!",
			/* ERROR[30] */ "** VISUAL  VERIFY **", /* ERROR[31] */ "*** INVALID CARD ***",
			/* ERROR[32] */ "**** EFT ACTIVE ****", /* ERROR[33] */ "*** SCALE ACTIVE ***",
			/* ERROR[34] */ "* STORAGE OVERFLOW *", /* ERROR[35] */ "<< MIXMATCH ERROR >>",
			/* ERROR[36] */ "** SALESPERSON ID **", /* ERROR[37] */ "ACTIVE TERMINAL: ***",
			/* ERROR[38] */ "** AUTHORIZATION ***", /* ERROR[39] */ "** SELF-EXCLUSION **",
			/* ERROR[40] */ "*** CONFIRMATION ***", /* ERROR[41] */ "<< COIN DISPENSER >>",
			/* ERROR[42] */ "*OPERATOR*ATTENTION*", /* ERROR[43] */ "*QUANTITY**REQUIRED*",
			/* ERROR[44] */ "** LOYALTY_CARD CARD ? **", /* ERROR[45] */ "< PROMOTION FAILED >",
			/* ERROR[46] */ "*** OUT OF RANGE ***", /* ERROR[47] */ "*** OUT OF DATE ****",
			/* ERROR[48] */ "*ENTER*NEW*PASSWORD*", /* ERROR[49] */ "off-line / no server",
			/* ERROR[50] */ "mismatch / no mirror", /* ERROR[51] */ "warning / do pickup ",
			/* ERROR[52] */ "message is available", /* ERROR[53] */ "check under cart ...",
			/* ERROR[54] */ "<<< back-up mode >>>", /* ERROR[55] */ "<<< Forced Close >>>",
			/* ERROR[56] */ "** turn key to S ***", /* ERROR[57] */ "* AGE RESTRICTION **",
			/* ERROR[58] */ "*** MEMBERS ONLY ***", /* ERROR[59] */ "*QUANTITY*INHIBITED*",
			/* ERROR[60] */ "** Read MICR Data **", /* ERROR[61] */ "** Journal Watch ***",
			/* ERROR[62] */ "ACTIVE CASHIER:  ***", /* ERROR[63] */ "< Read FingerPrint >",
			/* ERROR[64] */ "***IDENTIFICATION***", /* ERROR[65] */ "CHECK DATE OF EXPIRY",
			/* ERROR[66] */ "                    ", /* ERROR[67] */ "                    ",
			/* ERROR[68] */ "                    ", /* ERROR[69] */ "                    ",
			// TAMI-ENH-20140526-SBE#A BEG
			/* ERROR[70] */ "  ERR_NOTCONNECED   ", /* ERROR[71] */ "ERR_TIMEOUTTRANSACTI",
			/* ERROR[72] */ "  ERR_NOTCONFIGURED ", /* ERROR[73] */ "    ERR_RESPONSE    ",
			/* ERROR[74] */ "SWIPE CARD + ENTER  ",
			// TAMI-ENH-20140526-SBE#A END
			// EMEA-UPB-DMA#A BEG
			/* ERROR[75] */ "UPB SYSTEM ERROR    ", /* ERROR[76] */ "UPB AUTHORIZE ERROR ",
			/* ERROR[77] */ "UPB CONFIRM ERROR   ", /* ERROR[78] */ "UPB TIMEOUT ERROR   ",
			// EMEA-UPB-DMA#A END
			// SARAWAT-ENH-20150507-CGA#A BEG
			/* ERROR[79] */ "REDEEM POINTS FAILED", /* ERROR[80] */ "   ALREADY REDEEM   ",
			/* ERROR[81] */ " RED. COUP. FAILED  ", /* ERROR[82] */ "  CAPILLARY FAILED  ",
			/* ERROR[83] */ "  ONLY BY SCANNER   ", /* ERROR[84] */ "   RETURN COUPON    ",
			/* ERROR[85] */ "CUST ALREADY REGIST.",
			// SARAWAT-ENH-20150507-CGA#A END
			// PSH-ENH-001-AMZ#BEG
			/* ERROR[86] */ "G.Card server error ", /* ERROR[87] */ "G.Card NO Multiplier",
			/* ERROR[88] */ "No G.Card to void   ", /* ERROR[89] */ "G.Card pay overtotal",
			/* ERROR[90] */ "No keyb for G.Card  ", /* ERROR[91] */ "Loyalty not found   ",
			// PSH-ENH-001-AMZ#END
			// ALSH-ENH-20140526-SBE#A BEG
			/* ERROR[92] */ "    INVALID CARD    ", /* ERROR[93] */ "    CARD EXPIRED    ",
			/* ERROR[94] */ "COMMUNICAT. FAILURE ", /* ERROR[95] */ "      DECLINED      ",
			/* ERROR[96] */ "   INCORRECT PIN    ",
			// ALSH-ENH-20140526-SBE#A END
            //PSH-ENH-20151120-CGA#A BEG
            /* ERROR[97] */ "UTILITY FAILURE     ", /* ERROR[98] */ "Utility server error",
            /* ERROR[99] */ "No keyb for Utility ", /* ERROR[100]*/ "companies not found ",
            /* ERROR[101]*/ "ut. items not found ", /* ERROR[102]*/ "G.Card pay undertotal",
            /* ERROR[103]*/ "   G.Card empty     ",
            //PSH-ENH-20151120-CGA#A END
            /* ERROR[104]*/ "   ERR_TRANSBASE    ", //VERIFONE-20160201-CGA#A
            /* ERROR[105]*/ " AMOUNT TOO HIGH    ", //TAU-20160816-SBE#A
			// EYEPAY-20161116-CGA#A BEG
			/* ERROR[106]*/ "ERROR PARAMETER EYEP",
			/* ERROR[107]*/ "PORT EYEP NOT OPENED",
			// EYEPAY-20161116-CGA#A END
            /* ERROR[108]*/ "BROKEN TRANSACTION  ", // AMZ-2017#ADD
            /* ERROR[109]*/ "BAD CARD ERROR      ",
			/* ERROR[110]*/ "RETRY?              ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[111]*/ "   INACTIVE CARD    ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[112]*/ " CUSTOMER NOT FOUND ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[113]*/ "    ACTIVE CARD     ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[114]*/ "   REMOTE OFFLINE   ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[115]*/ "    REMOTE ERROR    ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[116]*/ " OFFLINE OPERATION  ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[117]*/ "CUSTOMER DATA EXISTS", //SPINNEYS-2017-033-CGA#A
			/* ERROR[118]*/ "   COUPON OFFLINE   ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[119]*/ "    COUPON ERROR    ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[120]*/ "MAX RETRIES REACHED ", //SPINNEYS-2017-033-CGA#A
			/* ERROR[121]*/ "ECR ERROR           ", //ECR-CGA#A
			/* ERROR[122]*/ "CHECK INSTASHOP DEL ", //INSTASHOP-EOD-CGA#A
			//WINEPTS-CGA#A BEG
			/* ERROR[123] */ "PINPAD  DISCONNECTED",
			/* ERROR[124] */ "   REMOVE THE CARD  ",
			/* ERROR[125] */ "**WINEPTS DISABLED**",
			/* ERROR[126] */ "  PAYMENT CANCELED  ",
			/* ERROR[127] */ "EPTS COMMUNIC. ERROR",
			/* ERROR[128] */ " CARD READING ERROR ",
			/* ERROR[129] */ "   TIMEOUT ERROR    ",
			/* ERROR[130] */ "    PINPAD ERROR    ",
			/* ERROR[131] */ "   KEY SEND ERROR   ",
			/* ERROR[132] */ "    VOID PAYMENT    ",
			/* ERROR[133] */ " PAYMENT WINEPTS OK ",
			//WINEPTS-CGA#A END
			/* ERROR[134] */ "VDI SETTLEMENT ERROR", //ECR-CGA#A
			/**************************************/
	};

	// TAMI-ENH-20140526-SBE#A BEG
	private static String eftmsg_tbl[] = { /* EFTMSG[00] */ "No POS Device Connected", };

	static String getEftMsg(int ind) {
		return eftmsg_tbl[ind];
	}
	// TAMI-ENH-20140526-SBE#A END

	private static String order_tbl[] = { /**************************************/
			/* ORDER[00] */ "repeat finger-print ", /* ORDER[01] */ "image data available",
			/* ORDER[02] */ "too far to the right", /* ORDER[03] */ "too far to the left ",
			/* ORDER[04] */ "position too high   ", /* ORDER[05] */ "position was too low",
			/* ORDER[06] */ "position too far off", /* ORDER[07] */ "position too close  ",
			/* ORDER[08] */ "too far forward     ", /* ORDER[09] */ "too far backward    ",
			/* ORDER[10] */ "motion was too fast ", /* ORDER[11] */ "motion was too slow ",
			/* ORDER[12] */ "sensor is not clean ", /* ORDER[13] */ "no data from sensor ",
			/* ORDER[14] */ "                    ", /* ORDER[15] */ "                    ",
			/* ORDER[16] */ "                    ", /* ORDER[17] */ "                    ",
			/* ORDER[18] */ "                    ", /* ORDER[19] */ "                    ",
			/**************************************/
	};

	private static String diags_tbl[] = { /**************************************/
			/* DIAGS[00] */ "EFT DEVICE SERVICES ", /* DIAGS[01] */ "                    ",
			/* DIAGS[02] */ "                    ", /* DIAGS[03] */ "                    ",
			/* DIAGS[04] */ "                    ", /* DIAGS[05] */ "                    ",
			/* DIAGS[06] */ "                    ", /* DIAGS[07] */ "                    ",
			/* DIAGS[08] */ "                    ", /* DIAGS[09] */ "                    ",
			/* DIAGS[10] */ "BCR DEVICE SERVICES ", /* DIAGS[11] */ "Select Cassette (A) ",
			/* DIAGS[12] */ "Select Cassette (B) ", /* DIAGS[13] */ "Empty NoteRecycler  ",
			/* DIAGS[14] */ "Dump to Base Level  ", /* DIAGS[15] */ "Dump to Work Level  ",
			/* DIAGS[16] */ "Dump to Threshold TL", /* DIAGS[17] */ "Release Cassette    ",
			/* DIAGS[18] */ "Latch Cassette      ", /* DIAGS[19] */ "Release Top Cover   ",
			// PSH-ENH-001-AMZ#BEG
			/* DIAGS[20] */ "Gift Card Serial    ", /* DIAGS[21] */ "Gift Card Topup     ",
			/* DIAGS[22] */ "Gift Card Total =   ", /* DIAGS[23] */ "Points Total =      ",
			/* DIAGS[24] */ "Extended info       ",
			// PSH-ENH-001-AMZ#END
			/**************************************/
	};

	static String getText(int ind) {
		return mnemo_tbl[ind];
	}

	static String getHead(int ind) {
		return super_tbl[ind];
	}

	static String getMenu(int ind) {
		return menus_tbl[ind];
	}

	static String getInfo(int ind) {
		return error_tbl[ind];
	}

	static String getHint(int ind) {
		return order_tbl[ind];
	}

	static String getDiag(int ind) {
		return diags_tbl[ind];
	}

	static void setText(int ind, String txt) {
		mnemo_tbl[ind] = txt;
	}

	static void setHead(int ind, String txt) {
		super_tbl[ind] = txt;
	}

	static void setMenu(int ind, String txt) {
		menus_tbl[ind] = txt;
	}

	static void setInfo(int ind, String txt) {
		error_tbl[ind] = txt;
	}

	static void setHint(int ind, String txt) {
		order_tbl[ind] = txt;
	}

	static void setDiag(int ind, String txt) {
		diags_tbl[ind] = txt;
	}
}
