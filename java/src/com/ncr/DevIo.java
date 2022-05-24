package com.ncr;

import java.io.*;
import java.util.Vector;

import com.ncr.ssco.communication.manager.SscoPosManager;
import com.sun.jna.StringArray;
import jpos.*; // JavaPOS generics
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

abstract class DevIo extends Struc {
	private static final Logger logger = Logger.getLogger(DevIo.class);
	static int prin_id = 0, till_id = 0x10;
	static int drw_state, drw_timer = -1;

	static CusIo cdsp, odsp;
	static RdrIo rdr1, rdr2;
	static Wedge wdge;
	static BioIo biom;
	static PrnIo prin = null;
	static Device mfptr = new Device("MFPTR");
	static Device scale = new Device("SCALE");
	static LineMap lMap = new LineMap("PrtLine");

	//WINEPTS-CGA#A BEG
	private static Vector creditCardVoucher = new Vector();
	private static Vector voucherCopyNumber = new Vector();
	private static boolean voucherFiscalReceipt = false;
	private static final int PRINTNORMAL = 1;
	private static final int PRINTCOMMENTAFTERLOGO = 16;
	private static final int BEGINNORMAL = 0;
	private static final int ENDNORMAL = 2;
	private static final int PRINTFIXEDOUTPUT = 32;
	private static final int PRINTTRAILERLINE = 33;
	//WINEPTS-CGA#A END
	static boolean needGraphic(String data) {
		int opt = options[O_Graph] << 8;

		if (opt > 0)
			for (int ind = data.length(); ind-- > 0;) {
				if ((data.charAt(ind) & 0xff00) == opt)
					return true;
			}
		return false;
	}

	static void tpmImage(int dev, String name) {
		if (dev != 2)
			return;
		prin.bitmap(localFile("bmp", name).getPath());
	}

	static void tpmPrint(int dev, int lfs, String data) {
		if (tra.slip > 0)
			dev &= ~2;
		if (!station(dev))
			return;
		if (dev == 4)
			if (mfptr.state < 0)
				return;
		prin.lfeed(dev, lfs);
		if (needGraphic(data)) {
			if (dev != 2)
				return;
			String name = lMap.update(data, "." + editNum(++tra.bmpSequence, 4));
			if (name != null) {
				prin.bitmap(name);
				return;
			}
		}
		StringBuffer sb = new StringBuffer(66);
		if (data.length() > 0) {
			if (data.charAt(1) == '@') {
				if (SscoPosManager.getInstance().isEnabled()) {
					if (data.length() >= 5 && data.charAt(5) == '@') {
						logger.info("Printing data: " +  data);
						String position = data.substring(2, 5);
						prin.logo(dev, "top".equals(position));
						return;
					}
				}
				tpmImage(dev, data.substring(2).trim());
				return;
			}
			prin.ldata(dev, data, sb);
		}
		prin.write(dev, sb.append('\n').toString());
	}

	static void tpmLabel(int dev, String nbr) {
		//int len = nbr.length(), type = prin.PTR_BCS_Code128;  //PRINTBARCODE-CGA#D
		int len = nbr.length(), type = prin.PTR_BCS_Code128_Parsed;  //PRINTBARCODE-CGA#A
		logger.info("ENTER tpmLabel()");
		if (!station(dev)) {
			logger.info("EXIT tpmLabel() !station(dev)");
			return;
		}
		if (len == 13)
			type = prin.PTR_BCS_EAN13;
		if (len == 12)
			type = prin.PTR_BCS_UPCA;
		if (len == 9)
			type = prin.PTR_BCS_Code39;
		if (len == 8) {
			if (nbr.charAt(0) == '0') {
				type = prin.PTR_BCS_UPCE;
				nbr = upcSpreadE(nbr);
			} else
				type = prin.PTR_BCS_EAN8;
		}
		if (type == prin.PTR_BCS_Code128)
			nbr = "{B" + nbr;

		//PRINTBARCODE-CGA#A BEG
		if (type == prin.PTR_BCS_Code128_Parsed)
			nbr = "{C" + nbr;
		//PRINTBARCODE-CGA#A END

		logger.info("CAll prin.label("+dev+", "+ type+", "+ nbr);
		prin.label(dev, type, nbr);
		logger.info("EXIT tpmLabel() ok");
	}

	static void tpmCheque(int ind, String nbr, long value) {
		int dec = tnd[ind].dec, base = 1;

		if (!station(4))
			return;
		if (value < 0)
			value = -value;
		String dig[] = new String[6];
		String amt = editTxt(editDec(value, dec), 10).replace(' ', '*');

		while (dec-- > 0)
			base *= 10;
		value /= base;
		for (int x = (int) value; ++dec < dig.length; x /= 10)
			dig[dec] = chk_nbr[x % 10].substring(3);
		prin.setQuality(4, true);
		slpInsert(options[O_chk42]);
		if (prin.slpColumns > 60) {
			LinIo slpLine = new LinIo("SLP", 1, prin.recColumns == 44 ? 57 : 54);
			if (value > 999999)
				gui.clearLink(Mnemo.getInfo(2), 1);
			slpLine.init(" *" + dig[5] + dig[4] + dig[3] + dig[2] + dig[1] + dig[0]).onto(35, tnd[ind].symbol)
					.upto(51, amt).type(4);
			slpLine.init(' ').onto(12, tra.number).type(4);
			slpLine.init(' ').onto(12, chk_line).type(4);
			slpLine.init(' ').onto(12, editNum(ctl.tran, 4)).skip().push(editNum(ctl.sto_nbr, 4)).push('/')
					.push(editKey(ctl.reg_nbr, 3)).push('/').push(editNum(ctl.ckr_nbr, 3)).type(4);
			slpLine.init(' ').upto(32, nbr).onto(35, editDate(ctl.date)).upto(52, editTime(ctl.time / 100)).type(4);
		} else {
			LinIo slpLine = new LinIo("SLP", 1, 44);
			if (value > 9999)
				gui.clearLink(Mnemo.getInfo(2), 1);
			slpLine.init(dig[3] + dig[2] + dig[1] + dig[0]).onto(23, tnd[ind].symbol).upto(40, amt).type(4);
			slpLine.init(tra.number).type(4);
			slpLine.init(chk_line).type(4);
			slpLine.init(' ').push(editNum(ctl.tran, 4)).skip().push(editNum(ctl.sto_nbr, 4)).push('/')
					.push(editKey(ctl.reg_nbr, 3)).push('/').push(editNum(ctl.ckr_nbr, 3)).type(4);
			slpLine.init(' ').upto(20, nbr).onto(23, editDate(ctl.date)).upto(40, editTime(ctl.time / 100)).type(4);
		}
		slpRemove();
		prin.setQuality(4, false);
		createVirtualVoucher(ind, nbr, amt, dig);  //WINEPTS-CGA#A
	}

	static boolean tpmMICRead() {
		return false;
	}

	static void cutPaper() {
		if ((prin_id & 2) == 0)
			return;
		prin.knife(prin_id);
		if (prin.paperState(2))
			//ECOMMERCE-SSAM#A BEG
			if (!ECommerceManager.getInstance().abortTransaction())
			//ECOMMERCE-SSAM#A END
			gui.clearLink(Mnemo.getInfo(12), 1);
		if (prin.logo.exists())
			prin.write(2, "\u001b|1B");
	}

	static void slpInsert(int lfs) {
		prin.waitIdle();
		while (true) {
			try {
				prin.prn1.beginInsertion(2000);
				prin.prn1.endInsertion();
				break;
			} catch (JposException je) {
				if (gui.clearLink(Mnemo.getInfo(18), 5) > 1) {
					mfptr.state = ERROR;
					return;
				}
			}
		}
		prin.lfeed(mfptr.state = 4, lfs);
	}

	static void slpRemove() {
		if (mfptr.state > 0)
			prin.waitIdle();
		else
			gui.display(2, Mnemo.getInfo(23));
		while (true) {
			try {
				prin.prn1.beginRemoval(2000);
				prin.prn1.endRemoval();
				break;
			} catch (JposException je) {
				if (je.getErrorCodeExtended() == prin.JPOS_EPTR_SLP_EMPTY)
					break;
				gui.clearLink(Mnemo.getInfo(19), 1);
			}
		}
		gui.display(2, editTxt("", 20));
		mfptr.state = 0;
	}

	static boolean station(int dev) {
		return (prin_id & dev) > 0;
	}

	/***************************************************************************
	 * sound tone using JavaPos ToneIndicator (wedge or speaker), if not configured by Toolkit (sound device or speaker)
	 *
	 * @param type
	 *            0 = error, 1 = alert
	 ***************************************************************************/
	static void alert(int type) {
		if (!wdge.kbdTone(type)) {
			if (File.separatorChar == '/')
				java.awt.Toolkit.getDefaultToolkit().beep();
			else {
				try {
					Runtime.getRuntime().exec("BEEP");
				} catch (Exception e) {
				}
			}
		}
		// System.err.print ('\7'); /* by Java Console */
	}

	static boolean drwOpened() {
		if ((till_id & 0x10) > 0)
			return true;
		return prin.tillState();
	}

	/***************************************************************************
	 * open cashdrawer
	 *
	 * @param nbr
	 *            cashdrawer id 1 or 2 (0=both)
	 ***************************************************************************/
	static void drwPulse(int nbr) {
		if (ctl.mode > 0) {
			if (ctl.mode == M_RENTRY)
				return;
			else if ((till_id & 2) == 0)
				return;
		}
		drw_state = nbr > 0 ? nbr : 3;
		if ((till_id & 0x10) > 0)
			return;
		prin.waitIdle();
		if (nbr < 1) {
			prin.pulse(nbr = 1);
		}
		prin.pulse(--nbr);
	}

	static void drwCheck(int ticks) {
		if (SscoPosManager.getInstance().isEnabled()) return;

		if (drw_state > 0)
			if (drwOpened()) {
				drw_timer = ticks;
				gui.clearLink(Mnemo.getInfo(10), (till_id & 0x11) > 0 ? 0x11 : 0x10);
				drw_state = 0;
				drw_timer = ERROR;
			}
		if (mon.adv_rec < 0) {
			cdsp.clear();
			mon.adv_rec = 0;
		}
		// power_check ();
		if (station(1)) {
			if (prin.paperState(1))
				gui.clearLink(Mnemo.getInfo(11), 1);
		}
	}

	static boolean drwWatch(int ticks) {
		if (drw_timer >= 0) {
			if (!drwOpened())
				return true;
			if (drw_timer > 0)
				if (--drw_timer < 1) {
					drw_timer = ticks;
					alert(1);
				}
		}
		return false;
	}

	static void cusDisplay(int line, String data) {
		cdsp.write(line, data);
	}

	static void oplDisplay(int line, String data) {
		if (data.length() != 20)
			data = rightFill(data, 20, ' ');
		if (odsp != null)
			odsp.write(line, data);
	}

	static void oplSignal(int lamp, int mode) {
		if (odsp != null)
			odsp.blink(lamp, mode);
	}

	static boolean hasKeylock() {
		return wdge.keyLock();
	}

	static void start() {
		RdrIo.scale = scale;
		wdge = new Wedge(); /* Jpos devices keylock and tone */
		biom = new BioIo(); /* Jpos device biometrics */
		odsp = new CusIo(0); /* Jpos device operator display */
		cdsp = new CusIo(1); /* Jpos device customer display */
		rdr1 = new RdrIo(1); /* Jpos device scanner/scale */
		rdr2 = new RdrIo(2); /* Jpos device scanner/scale */
		wdge.init(); /* Jpos devices msr and scanner */
		biom.init(); /* Jpos device biometrics */
		prin = new PrnIo(mfptr);
		if (prin.jposActive(prin.prn1))
			prin_id = prin.init(0x50);
		till_id = options[O_xTill];
		if (!prin.jposActive(prin.drw1))
			if (!prin.jposActive(prin.drw2))
				till_id |= 0x10;
		prin_id &= ~Integer.parseInt(System.getProperty("NOP", "0"), 16);
		if ((prin_id & 0x10) == 0)
			prin.setPitch(56);
	}

	static void stop() {
		if (prin != null)
			prin.stop();
		biom.stop();
		rdr1.stop();
		rdr2.stop();
		cdsp.stop();
		odsp.stop();
		wdge.stop();
	}

	static void setAlerted(int nbr) {
		int msk = Integer.getInteger("RDR_BEEP", 0).intValue();
		RdrIo.alert |= 1 << nbr & msk;
	}

	static void setEnabled(boolean state) {
		if (rdr1 != null)
			rdr1.setEnabled(state);
		if (rdr2 != null)
			rdr2.setEnabled(state);
		wdge.setEnabled(state);
	}
	//WINEPTS-CGA#A BEG
	static void createVirtualVoucher(int ind, String nbr, String amt, String dig[]) {
		logger.info("ENTER createVirtualVoucher");
		logger.info("ind: " + ind);
		logger.info("amt: " + amt);
		logger.info("nbr: " + nbr);

		LinIo slpLine = new LinIo("SLP", 1, prin.recColumns == 44 ? 57 : 54);
		CreditCardVoucher LineToAdd = new CreditCardVoucher();

		LineToAdd.setTypeOfLine('B');
		LineToAdd.setPrintedLineDescription("");
		logger.info("linedescr 1 [" + LineToAdd.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd);
		CreditCardVoucher LineToAdd1 = new CreditCardVoucher();

		LineToAdd1.setTypeOfLine('D');
		slpLine.init(" *").onto(2, dig[5]).push(dig[4]).push(dig[3]).push(dig[2]).push(dig[1]).push(dig[0])
				.onto(35, tnd[ind].symbol).upto(51, amt);
		LineToAdd1.setPrintedLineDescription(slpLine.toString());
		logger.info("linedescr 2 [" + LineToAdd1.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd1);
		CreditCardVoucher LineToAdd2 = new CreditCardVoucher();

		LineToAdd2.setTypeOfLine('D');

		slpLine.init(' ').onto(12, tra.number);
		LineToAdd2.setPrintedLineDescription(slpLine.toString());
		logger.info("linedescr 3 [" + LineToAdd2.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd2);
		CreditCardVoucher LineToAdd3 = new CreditCardVoucher();

		LineToAdd3.setTypeOfLine('D');
		slpLine.init(' ').onto(12, chk_line);
		LineToAdd3.setPrintedLineDescription(slpLine.toString());
		logger.info("linedescr 4 [" + LineToAdd3.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd3);
		CreditCardVoucher LineToAdd4 = new CreditCardVoucher();

		LineToAdd4.setTypeOfLine('D');
		slpLine.init(' ').onto(12, editNum(ctl.tran, 4)).skip().push(editNum(ctl.sto_nbr, 4)).push('/')
				.push(editKey(ctl.reg_nbr, 3)).push('/').push(editNum(ctl.ckr_nbr, 3));
		LineToAdd4.setPrintedLineDescription(slpLine.toString());
		logger.info("linedescr 5 [" + LineToAdd4.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd4);
		CreditCardVoucher LineToAdd5 = new CreditCardVoucher();

		LineToAdd5.setTypeOfLine('D');
		slpLine.init(' ').upto(32, nbr).onto(35, editDate(ctl.date)).skip(3).push(editTime(ctl.time / 100));
		LineToAdd5.setPrintedLineDescription(slpLine.toString());
		logger.info("linedescr 6 [" + LineToAdd5.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd5);
		CreditCardVoucher LineToAdd6 = new CreditCardVoucher();

		LineToAdd6.setTypeOfLine('E');
		LineToAdd6.setPrintedLineDescription("");
		logger.info("linedescr 7 [" + LineToAdd6.getPrintedLineDescription() + "]");
		pushVirtualVoucherElements(LineToAdd6);

		logger.info("EXIT createVirtualVoucher");
	}

	static void removeCreditCardVoucher() {
		logger.info("ENTER removeCreditCardVoucher");

		if (creditCardVoucher.isEmpty()) {
			logger.info("EXIT removeCreditCardVoucher - isEmpty");
			return;
		}

		creditCardVoucher.removeAllElements();
		voucherCopyNumber.removeAllElements();

		logger.info("EXIT removeCreditCardVoucher");
	}

	static public boolean ThereIsVoucher() {
		return !creditCardVoucher.isEmpty();
	}

	static int getVoucherCopyNumber(boolean firstcopyonreceipt) {
		logger.info("ENTER getVoucherCopyNumber");
		logger.info("firstcopyonreceipt: " + firstcopyonreceipt);

		int num = 0;

		if (!voucherCopyNumber.isEmpty()) {
			if (firstcopyonreceipt) {
				num = ((Integer) voucherCopyNumber.elementAt(0)).intValue();
				for (int i = 0; i < voucherCopyNumber.size(); i++) {
					voucherCopyNumber
							.setElementAt(new Integer(((Integer) voucherCopyNumber.elementAt(i)).intValue() - 1), i);
				}
			} else {
				num = ((Integer) voucherCopyNumber.remove(0)).intValue();
			}
		}

		logger.info("ENTER getVoucherCopyNumber - return " + num);
		return num;
	}

	static void haveToPrintCreditCardVoucher() {
		voucherFiscalReceipt = false;
	}

	static void hateToPrintCreditCardVoucher(boolean firstcopyonreceipt) {
		while (PrintCCV(firstcopyonreceipt)) {
		}
	}

	static boolean PrintCCV(boolean firstcopyonreceipt) {
		logger.info("ENTER PrintCCV");
		logger.info("firstcopyonreceipt: " + firstcopyonreceipt);
		// First see if there's anything in the vector. Quit if so.
		if (((tra.mode & M_CANCEL) > 0) || ((tra.mode & M_SUSPND) > 0)) {
			firstcopyonreceipt = false;
		}
		if (creditCardVoucher.isEmpty()) {
			logger.info("EXIT PrintCCV 1");

			return false;
		}
		if (tra.mode != 2) {
			PosGPE.deleteEptsVoidFlag();
		}

		// Number of voucher copy to print
		int vouchersNumber, printtype = 0;
		int maxVouchersNumber = 0;

		vouchersNumber = getVoucherCopyNumber(firstcopyonreceipt);
		if (!firstcopyonreceipt) {
			logger.info("vouchersNumber = " + vouchersNumber);
			if (((tra.mode & M_CANCEL) > 0) || ((tra.mode & M_SUSPND) > 0)) {
				vouchersNumber = 2;
			}
			maxVouchersNumber = vouchersNumber;
			logger.info("vouchersNumber = " + vouchersNumber);
			printtype = PRINTNORMAL;
		} else {
			if (tra.mode != M_VOID && tra.mode != M_SUSPND) {
				vouchersNumber = 1;
				maxVouchersNumber = vouchersNumber;
				printtype = PRINTCOMMENTAFTERLOGO;

				DevIo.tpmPrint(2, 0, "");
			}
		}
		Vector tmp = new Vector();
		int nov = 0;

		logger.info("creditCardVoucher.size(): " + creditCardVoucher.size());
		while (nov < creditCardVoucher.size()) {
			CreditCardVoucher ccv = (CreditCardVoucher) creditCardVoucher.elementAt(nov);

			tmp.add(ccv);
			if (!firstcopyonreceipt) {
				creditCardVoucher.remove(ccv);
				nov--;
				if (ccv.getTypeOfLine() == 'E') {
					break;
				}
			}
			nov++;
		}
		if (!firstcopyonreceipt) {
			if (vouchersNumber == 0) {
				logger.info("EXIT PrintCCV 2");

				return (creditCardVoucher.size() > 0);
			}
		}
		while ((vouchersNumber--) > 0) {
			for (int counter = 0; counter < tmp.size(); counter++) {
				CreditCardVoucher ccv = (CreditCardVoucher) tmp.elementAt(counter);

				logger.info("ccv.getTypeOfLine () = " + ccv.getTypeOfLine());
				if (ccv.getPrintedLineDescription().equals("SKIP VOUCHER")) {
					if ((!firstcopyonreceipt) && ((vouchersNumber + 1) != maxVouchersNumber)) {
						break;
					}
				}

				logger.info("ccv.getTypeOfLine(): " + ccv.getTypeOfLine());
				switch (ccv.getTypeOfLine()) {
					case 'B':
						/*if (printerObject.GetCapSlpPresent()) {
							slpInsert(options[O_chk42]);
						} else {*/
						if (!firstcopyonreceipt) {
							DevIo.tpmPrint(2, 0, "");
						}
						//}
						break;

					case 'E':
						GdRegis.set_trailer(-1);

						if (!firstcopyonreceipt) {
							DevIo.tpmPrint(2, 0, rightFill(prtLine.toString(), 41, ' '));
							DevIo.tpmPrint(2, 0, rightFill(ccv.getPrintedLineDescription(), 41, ' '));
						}
						//}
						break;

					case 'D':
					default:
						/*if (printerObject.GetCapSlpPresent()) {
							DevIo.tpmPrint(4, 0, ccv.getPrintedLineDescription());
						} else {*/
						DevIo.tpmPrint(2, 0, rightFill(ccv.getPrintedLineDescription(), 41, ' '));

						//}
						break;
				}
			}
			GdRegis.hdr_print();
		}

		logger.info("EXIT PrintCCV");
		return (creditCardVoucher.size() > 0 && (!firstcopyonreceipt));
	}

	static void pushVirtualVoucherElements(CreditCardVoucher element) {
		creditCardVoucher.addElement(element);
	}

	static void addVoucherCopyNumber(int copyNumber) {
		voucherCopyNumber.add(new Integer(copyNumber));
	}

	static void printCreditCardVoucher() {
		logger.info("ENTER printCreditCardVoucher 1");

		while (PrintCCV(voucherFiscalReceipt)) {
		}
	}

	static void printCreditCardVoucher(int inFiscalReceipt) {
		logger.info("ENTER printCreditCardVoucher 2");
		logger.info("inFiscalReceipt: " + inFiscalReceipt);

		if ((inFiscalReceipt == 0 && voucherFiscalReceipt) || (inFiscalReceipt == 1)) {
			while (PrintCCV((inFiscalReceipt == 0 && voucherFiscalReceipt))) {
			}
		}
	}

	/*public static void setScannersEnabled(boolean state) {
		if (wdge != null) {
			wdge.setScannersEnabled(state);
		}
	}*/
	//WINEPTS-CGA#A END
}

class PrnIo extends PosIo implements POSPrinterConst {
	int recColumns = 42, jrnColumns = 42, slpColumns = 66;
	int recCompressed = 0;
	private static final Logger logger = Logger.getLogger(PrnIo.class);
	POSPrinter prn1;
	CashDrawer drw1, drw2;
	File logo = localFile("bmp", "P_REGELO.BMP");

	PrnIo(Device dev) {
		try {
			prn1 = new POSPrinter();
			if (dev.version > 0) {
				jposOpen("PosPrinter.1", prn1, true);
				if (codePage > 0)
					prn1.setCharacterSet(codePage);
			}
		} catch (JposException je) {
			logConsole(0, prn1.getClass().getName(), je.getMessage());
			if (je.getErrorCode() != JPOS_E_ILLEGAL)
				gui.eventStop(255);
		}
		try {
			drw1 = new CashDrawer();
			jposOpen("CashDrawer.1", drw1, false);
		} catch (JposException je) {
			jposError(je, drw1);
		}
		try {
			drw2 = new CashDrawer();
			jposOpen("CashDrawer.2", drw2, false);
		} catch (JposException je) {
			jposError(je, drw2);
		}
	}

	void stop() {
		jposClose(prn1);
		jposClose(drw1);
		jposClose(drw2);
	}

	int init(int id) {
		try {
			prn1.setMapMode(PTR_MM_METRIC);
			if (prn1.getCapJrnPresent()) {
				id |= 0x01;
				if ((jrnColumns = prn1.getJrnLineChars()) < 40)
					prn1.setJrnLineChars(jrnColumns = 40);
				prn1.setJrnLineSpacing(425);
			}
			if (prn1.getCapRecPresent()) {
				id |= 0x02;
				if ((recColumns = prn1.getRecLineChars()) < 40)
					prn1.setRecLineChars(recColumns = 40);
				prn1.setRecLineSpacing(425);
			}
			if (prn1.getCapSlpPresent()) {
				id |= 0x04;
				if ((slpColumns = prn1.getSlpLineChars()) > 66)
					prn1.setSlpLineChars(slpColumns = 66);
				prn1.setSlpLineSpacing(425);
			}
			if (prn1.getCapRecPapercut())
				id |= 0x80;
		} catch (JposException je) {
			error(je, true);
		}
		try {
			if (logo.exists()) {
				setQuality(PTR_S_RECEIPT, true);
				prn1.setBitmap(1, PTR_S_RECEIPT, logo.getPath(), PTR_BM_ASIS, PTR_BM_CENTER);
				setQuality(PTR_S_RECEIPT, false);
			}
		} catch (JposException je) {
			logConsole(0, prn1.getClass().getName(), je.getMessage());
			if (je.getErrorCodeExtended() == JPOS_EPTR_BADFORMAT)
				logConsole(0, prn1.getClass().getName(), "WRONG LOGO FORMAT: " + logo.getPath());
			if (je.getErrorCodeExtended() == JPOS_EPTR_TOOBIG)
				logConsole(0, prn1.getClass().getName(), "WRONG LOGO SIZE: " + logo.getPath());
		}
		return id;
	}

	void error(JposException je, boolean abort) {
		jposError(je, prn1);
		gui.clearLink(Mnemo.getInfo(17), abort ? 4 : 1);
		if (abort)
			gui.eventStop(255);
	}

	void write(int dev, String data) {
		while (true) {
			try {
				logger.debug("Printing in dev: "  + dev + " data [" + data + "]");
				prn1.printNormal(dev, data);

				break;
			} catch (JposException je) {
				//ECOMMERCE-SSAM#A BEG
				if ( ECommerceManager.getInstance().abortTransaction()) {
					break;
				}
				//ECOMMERCE-SSAM#A END
				error(je, false);
				if (je.getErrorCodeExtended() == JPOS_EPTR_SLP_EMPTY) {
					DevIo.slpInsert(0);
					if (DevIo.mfptr.state < 0)
						break;
				}
			}
		}
	}

	void bitmap(String name) {
		while (true) {
			try {
				setQuality(PTR_S_RECEIPT, true);
				prn1.printBitmap(PTR_S_RECEIPT, name, PTR_BM_ASIS, PTR_BM_CENTER);
				setQuality(PTR_S_RECEIPT, false);
				break;
			} catch (JposException je) {
				//ECOMMERCE-SSAM#A BEG
				if ( ECommerceManager.getInstance().abortTransaction()) {
					break;
				}
				//ECOMMERCE-SSAM#A END
				error(je, false);
			}
		}
	}

	boolean paperState(int dev) {
		boolean state = false;

		try {
			if (dev == PTR_S_JOURNAL)
				state = prn1.getJrnNearEnd();
			if (dev == PTR_S_RECEIPT)
				state = prn1.getRecNearEnd();
		} catch (JposException je) {
		}
		return state;
	}

	boolean tillState() {
		boolean state = false;

		try {
			state |= drw1.getDrawerOpened();
		} catch (JposException je) {
		}
		try {
			state |= drw2.getDrawerOpened();
		} catch (JposException je) {
		}
		return state;
	}

	void waitIdle() {
		return;
	}

	void label(int dev, int type, String nbr) {
		logger.info("ENTER label type: " + type + " - nbr: " + nbr);
		lfeed(dev, 1);
		while (true) {
			try {
				if (dev == PTR_S_RECEIPT)
					if (!prn1.getCapRecBarCode()) {
						logger.info("EXIT label 1");
						return;
					}
				if (dev == PTR_S_SLIP)
					if (!prn1.getCapSlpBarCode()) {
						logger.info("EXIT label 2");
						return;
					}
				if (type == PTR_BCS_Code39) {
					prn1.printBarCode(dev, ipcBase32(nbr), type, 1275, 5200, PTR_BC_CENTER, PTR_BC_TEXT_NONE);
					write(dev, dwide("\u001b|cAA" + nbr + '\n'));
				} else {
					logger.info("PRINT BARCODE type: " +type);
					prn1.printBarCode(dev, nbr, type, 1275, 5200, PTR_BC_CENTER, PTR_BC_TEXT_BELOW);
				}
				break;
			} catch (JposException je) {
				logger.info("label exception. : " + je.getMessage());
				error(je, false);
			}
		}
	}

	void ldata(int dev, String data, StringBuffer sb) {
		int cols = dev > 1 ? recColumns : jrnColumns;

		data = jposOemCode(data);
		if (dev == PTR_S_SLIP) {
			for (cols = slpColumns - data.length(); cols-- > 0; sb.append(' '))
				;
			if (data.charAt(1) != '>')
				sb.append(data);
			else
				sb.append(' ').append(dwide(data.substring(2, 22)));
			return;
		}
		if (cols == 40) {
			if (data.charAt(1) != '>')
				sb.append(data.substring(1, 41));
			else
				sb.append(dwide(data.substring(2, 22)));
			return;
		}
		if (recCompressed == 0)
			for (cols = cols - 42 >> 1; cols-- > 0; sb.append(' '))
				;
		if (data.charAt(1) != '>')
			sb.append(data);
		else
			sb.append(' ').append(dwide(data.substring(2, 22)));
	}

	void lfeed(int dev, int lfs) {
		if (lfs > 0)
			write(dev, "\u001b|" + lfs + "lF");
	}

	void pulse(int nbr) {
		CashDrawer co = nbr == 0 ? drw1 : drw2;
		if (!jposActive(co))
			return;
		try {
			co.openDrawer();
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	void knife(int msk) {
		if ((msk & 0x40) == 0) {
			try {
				lfeed(PTR_S_RECEIPT, prn1.getRecLinesToPaperCut());
			} catch (JposException je) {
			}
		}
		if ((msk & 0x80) != 0) {
			write(2, "\u001b|75P");
		}
	}

	void setPitch(int chars) {
		if (!jposActive(prn1))
			return;
		try {
			prn1.setRecLineChars(recCompressed = chars);
		} catch (JposException je) {
			error(je, true);
		}
	}

	void setQuality(int dev, boolean high) {
		try {
			if (dev == PTR_S_RECEIPT)
				prn1.setRecLetterQuality(high);
			if (dev == PTR_S_JOURNAL)
				prn1.setJrnLetterQuality(high);
			if (dev == PTR_S_SLIP)
				prn1.setSlpLetterQuality(high);
		} catch (JposException je) {
			error(je, true);
		}
	}

	String dwide(String data) {
		String dwide = "\u001b|2C" + data;
		if (SscoPosManager.getInstance().isEnabled()) dwide += "\n";
		return dwide;
	}

	void logo(int dev, boolean top) {
		String position = top ? "1" : "2";
		write(dev, "\u001b|" + position + "B");
	}
}
