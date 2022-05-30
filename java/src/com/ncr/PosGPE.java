package com.ncr;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ncr.gpe.*;
import com.ncr.gpe.epts.MessageToPos_ListOfOptions;
import com.ncr.gpe.std.VariazioneStatoPing;
import org.apache.log4j.Logger;

public class PosGPE extends Action {

	private static final int groupindex = 17;
	private static final Logger logger = Logger.getLogger(PosGPE.class);

	// P_REGPAR Parameters row GPE00
	private static boolean isPingEnabled = false;
	private static byte dataCollectType = 0;
	private static boolean isEptsPartialPaymentAllowed = false;
	private static boolean isPinPadPingEnabled = false;
	private static boolean isGpeDisabled = true;
	private static boolean isSmartCardStatusDisabled = false;

	private static boolean isActiveDisplayGPE = false;
	private static boolean isActiveAnnulo = false;

	private static boolean isGPElite = false;

	// P_REGPAR Parameters row KEPT0
	public static int preselect[] = new int[40];

	public static final String EPTSVOIDFLAG = "EptsVoid";
	private static int errorCode = 0;
	private static int lastTransactionType = 0;
	private static int idcEptsErrorCode = 0;
	private static String errorDescription;
	private static int preset = -1;
	private static GpeInterface gpe;
	public static boolean crdSts;
	public static boolean isChangeStatusEpts = false;
	private static int retry = 0;
	private static GpeResult_ReceiptDataInterface lastEptsReceiptData;
	private static EptsCardInfo lastEptsCardInfo = new EptsCardInfo();
	public static VariazioneStatoPing vsp;
	static int sts = 0;
	static long tmout;
	static boolean flagEptsVoid = false;
	static long amountEptsVoid = 0;

	static int panCheckingAcquirerId = 0;
	static int panCheckingPosId = 0;
	static String panCheckingPan = null;
	private static PosGPE instance = null;

	private static boolean isCreditCardVoucherPrintEnabled = false;
	private static int minAmountToPrint = 0;
	private static char currencyId = '0';  //CURRENCY-CGA#A


	public static final byte[] EXTRADATA_GS1_GIFT = new byte[] {
			(byte) 0x1F, (byte) 0x80, (byte) 0x26 };


	public static void doLog(String msg) {
		logger.info(msg);
	}

	public PosGPE() {
	}

	static public PosGPE getInstance() {
		if (instance == null) {
			instance = new PosGPE();
		}

		return instance;
	}

	static void printMap(Map map) {
		doLog("Visualizzazione Mappa:");
		doLog(" PosCommand:" + map.get("PosCommand"));
		doLog(" amount:" + map.get("amount"));
		doLog(" extradata:" + map.get("extradata"));
		doLog(" cashier:" + map.get("cashier"));
		doLog(" posid:" + map.get("posid"));
		doLog(" receiptno:" + map.get("receiptno"));
		doLog(" totamount:" + map.get("totamount"));
	}

	public static void setPreset(int value) {

		preset = value;
	}

	static int payGpe(long amt) {
		logger.info("ENTER payGpe 1");
		return payGpe(amt, -1, null, null);
	}

	public static int payGpe(long amt, int preselection, byte[][] addextradataheader, byte[][] addextradatavalue) {
		logger.info("ENTER payGpe 2");
		logger.info("amt: " + amt);
		logger.info("preselection: " + preselection);

		lastEptsReceiptData = null;
		lastTransactionType = 0;
		Integer Amt = new Integer((int) amt);
		com.ncr.gpe.CommandFromPos_Payment cmd;

		cmd = new CommandFromPos_Payment();
		cmd.setAmount(Amt.intValue());
		cmd.setCashier(ctl.ckr_nbr);
		cmd.setPosId(ctl.reg_nbr);
		cmd.setReceiptNo(ctl.tran);
		cmd.setTotalAmount(Amt.intValue());
		cmd.setCurrencyId(currencyId); //CURRENCY-CGA#A

		/*if (GiftCardBarcode.getInstance().isPagamentoGiftCardBarcode()) {
			cmd.appendExtradataTag(GiftCardBarcode.EXTRADATA_GIFTCARD_CODELINE,
					GiftCardBarcode.getInstance().getCodeline());
			cmd.appendExtradataTag(GiftCardBarcode.EXTRADATA_GIFTCARD_POS_ID,
					new byte[] { (byte) GiftCardBarcode.getInstance().getPosID() });
		}*/

		if (preset != -1) {
			cmd.setPreselection(preselect[preset]);
		}

		if (preselection > -1) {
			cmd.setPreselection(preselection);
		}

		if (addextradataheader != null && addextradatavalue != null) {
			for (int i = 0; i < addextradataheader.length; i++) {
				cmd.appendExtradataTag(addextradataheader[i], addextradatavalue[i]);
			}
		}

		/*if (preset != -1) {
			byte[] byteAmountCard = String.valueOf(Amt.intValue()).getBytes();  //momentaneo
			cmd.appendExtradataTag(EXTRADATA_GS1_GIFT, byteAmountCard);  //momentaneo
		}*/

		logger.info("EXIT payGpe 2");
		return PosGPE.ppp(cmd);
	}

	//WINEPTS-CGA#A BEG
	static int refund(long amt) {
		logger.info("ENTER refund 1");
		return refund(-amt, -1, null, null);
	}

	public static int refund(long amt, int preselection, byte[][] addextradataheader, byte[][] addextradatavalue) {
		logger.info("ENTER refund 2");
		logger.info("amt: " + amt);
		logger.info("preselection: " + preselection);

		lastEptsReceiptData = null;
		lastTransactionType = 0;
		Integer Amt = new Integer((int) amt);
		com.ncr.gpe.CommandFromPos_Return cmd;

		cmd = new CommandFromPos_Return();
		cmd.setAmount(Amt.intValue());
		cmd.setCashier(ctl.ckr_nbr);
		cmd.setPosId(ctl.reg_nbr);
		cmd.setReceiptNo(ctl.tran);

		/*if (preset != -1) {
			cmd.setPreselection(preselect[preset]);
		}Fcomp

		if (preselection > -1) {
			cmd.setPreselection(preselection);
		}

		if (addextradataheader != null && addextradatavalue != null) {
			for (int i = 0; i < addextradataheader.length; i++) {
				cmd.appendExtradataTag(addextradataheader[i], addextradatavalue[i]);
			}
		}*/

		/*if (preset != -1) {
			byte[] byteAmountCard = String.valueOf(Amt.intValue()).getBytes();  //momentaneo
			cmd.appendExtradataTag(EXTRADATA_GS1_GIFT, byteAmountCard);  //momentaneo
		}*/

		logger.info("EXIT refund 2");
		return PosGPE.ppp(cmd);
	}
	//WINEPTS-CGA#A END

	static int PinpadInit() {
		logger.info("ENTER PinpadInit");

		com.ncr.gpe.CommandFromPos_InitializePinpadWithoutEpts cmd;
		cmd = new CommandFromPos_InitializePinpadWithoutEpts();

		logger.info("EXIT PinpadInit");
		return PosGPE.ppp(cmd);
	}

	static int EMVPinpadInit() {
		logger.info("ENTER EMVPinpadInit");

		CommandFromPos_InitializePinpadWithEpts cmd;

		cmd = new CommandFromPos_InitializePinpadWithEpts();
		cmd.setCashier(ctl.ckr_nbr);
		cmd.setPosId(ctl.reg_nbr);
		cmd.setReceiptNo(ctl.tran);

		logger.info("EXIT EMVPinpadInit");
		return PosGPE.ppp(cmd);
	}

	public static int EptsVoid() {
		logger.info("ENTER EptsVoid");

		CommandFromPos_Void cmd;

		cmd = new CommandFromPos_Void();

		cmd.setCashier(ctl.ckr_nbr);
		cmd.setPosId(ctl.reg_nbr);
		cmd.setReceiptNo(ctl.tran);

		logger.info("EXIT EptsVoid");
		return PosGPE.ppp(cmd);
	}

	public static int EptsReadCard() {
		CommandFromPos_ReadCardOnly cmd;
		cmd = new CommandFromPos_ReadCardOnly();

		return PosGPE.ppp(cmd);
	}

	public static GpeResult_ReceiptDataInterface getLastEptsReceiptData() {
		return lastEptsReceiptData;
	}

	public static void setLastEptsReceiptData(GpeResult_ReceiptDataInterface receiptData) {
		PosGPE.lastEptsReceiptData = receiptData;
	}

	public static void deleteLastEptsReceiptData() {
		PosGPE.lastEptsReceiptData = null;
	}

	public static EptsCardInfo getLastEptsCardInfo() {
		return lastEptsCardInfo;
	}

	public static Map createMapWithChosenOptionReceivedFromPos(int value) {
		Map map = new HashMap();

		map.put("chosenOption", new Integer(value));

		return map;
	}

	public static CommandFromPos createCmdWithOption(int value) {
		logger.info("ENTER createCmdWithOption");
		logger.info("value: " + value);

		CommandFromPos_ResponseToChoice cmd;

		cmd = new CommandFromPos_ResponseToChoice();
		cmd.setChosenOption(value);

		logger.info("EXIT createCmdWithOption");
		return cmd;
	}

	public static void smartCardStatus() {
		logger.info("ENTER smartCardStatus");

		crdSts = false;

		if (!isSmartCardStatusDisabled) {
			CommandFromPos_CardStatus cmd;
			cmd = new CommandFromPos_CardStatus();
			do {
				PosGPE.ppp(cmd);

				logger.info("crdSts: " + crdSts);
				if (crdSts) {
					panel.clearLink(Mnemo.getInfo(124), 1);
				}
			} while (crdSts);

			logger.info("errorCode: " + errorCode);
			if (errorCode > 0)
				panel.clearLink(Mnemo.getInfo(errorCode), 1);
		}

		logger.info("EXIT smartCardStatus");
	}

	public static void Init() {
		doLog("INIT");
		int reg_nbr = Integer.valueOf(editKey(ctl.reg_nbr, 3)).intValue();
		logger.info("reg_nbr: " + reg_nbr);

		gpe = DefaultGpe.createInstance(reg_nbr);
	}

	public static int ppp(CommandFromPos cmd) {
		logger.info("ENTER ppp");

		MessageToPosInterface toPOS;
		PosGPEHandler gpeHandler = new PosGPEHandler(cmd);
		String oldPrompt;

		isActiveAnnulo = false;
		boolean completed = false;

		logger.info("isGpeDisabled: " + isGpeDisabled);
		if (isGpeDisabled) {
			completed = true;
			errorCode = 125;
		}

		executing = true;
		errorDescription = "";
		idcEptsErrorCode = 0;

		while (!completed) {
			completed = true;
			sts = 0;
			input.reset("");
			oldPrompt = input.prompt;

			PosGPE.doLog("Call EXECUTE");
			GpeMessageHandler.setPosDisplay(new PosGPEShowMessage());
			toPOS = gpe.executeCommand(cmd);

			PosGPE.doLog("EXECUTE ended. Call process");
			toPOS.process(gpeHandler);
			if (toPOS instanceof MessageToPos_ListOfOptions) {
				isActiveDisplayGPE = true;
			}

			PosGPE.doLog("Event processed. sts value: " + sts);
			switch (sts) {
				case 1: // opt
					int optS = 0;

					optS = input.scanNum(input.num);
					if (optS == 0) {
						isActiveAnnulo = true;
					}
					completed = false;

					PosGPE.doLog("make map with option: " + optS);
					cmd = PosGPE.createCmdWithOption(optS);
					input.prompt = oldPrompt;
					input.init(event.lck, event.max, event.min, event.dec);

					break;

				case 2: // OK
					errorCode = 0;
					PosGPE.doLog("Authorization OK.");
					break;

				case 3: // KO
					PosGPE.doLog("Authorization denied.");
					break;

				case 4: // PanChecking
					PosGPE.doLog("Fidelity card Reconized.");
					errorCode = -2;
					break;

				case 5: // ack ricevuto
					doLog("ACK received");
					errorCode = 0;
					break;

				default:
					errorCode = 126;
					PosGPE.doLog("strange situation, not handled.");
			}
		}

		logger.info("errorCode: " + errorCode);
		logger.info("errorDescription: " + errorDescription);
		if (errorCode > 0) {
			lastEptsReceiptData = null;
			if (errorDescription.length() > 0) {
				String displayError = errorDescription.length() > 20 ? errorDescription.substring(0, 20)
						: errorDescription;

				panel.display(2, displayError);
			} else {
				panel.display(2, Mnemo.getInfo(errorCode));
			}
			/*if (ItalianOption.getFastIsFastLane()) {
				errorCode = 132;
			}*/
		} else {
			panel.display(2, "");
		}

		executing = false;

		logger.info("EXIT ppp - return " + errorCode);
		return errorCode;
	}

	public static void writeEptsVoidFlag() {
		logger.info("ENTER writeEptsVoidFlag");

		FileOutputStream fout;

		try {
			fout = new FileOutputStream(EPTSVOIDFLAG);
			fout.write(12);
			fout.close();
		} catch (Exception e) {
			fout = null;
		}

		logger.info("EXIT writeEptsVoidFlag");
	}

	public static void deleteEptsVoidFlag() {
		logger.info("ENTER deleteEptsVoidFlag");

		File f = new File(EPTSVOIDFLAG);

		if (f.exists()) {
			f.delete();
		}

		logger.info("EXIT deleteEptsVoidFlag");
	}

	public static boolean isEptsVoidFlagPresent() {
		logger.info("ENTER isEptsVoidFlagPresent");

		boolean isPresent = false;
		File voucher = new File(EPTSVOIDFLAG);

		if (voucher.exists()) {
			isPresent = true;
		}

		logger.info("EXIT isEptsVoidFlagPresent - return " + isPresent);
		return isPresent;
	}

	public static int finalizeTransaction() {
		logger.info("ENTER finalizeTransaction");

		CommandFromPos_ResponseAck cmd;

		cmd = new CommandFromPos_ResponseAck();

		logger.info("EXIT finalizeTransaction");
		return ppp(cmd);
	}

	public static void fail(int eCode, String eDescription) {
		logger.info("ENTER fail");

		idcEptsErrorCode = 0;

		logger.info("eCode: " + eCode);
		switch (eCode) {
			case MessageToPos_Error.ERRORCODE_EPTS_FEEDBACK_ERROR:
				setRetry(getMaxNumberOfRetries());
				idcEptsErrorCode = 26;
				errorCode = 127;
				lastTransactionType = 0;
				break;

			case MessageToPos_Error.ERRORCODE_EPTS_IOERROR:
			case MessageToPos_Error.ERRORCODE_EPTS_TIMEOUTERROR:
				if (isActiveDisplayGPE) {
					idcEptsErrorCode = 13;
				} else {
					idcEptsErrorCode = 3;
					lastTransactionType = 4;
				}
				errorCode = 127;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_DISCONNECTED:
				lastTransactionType = 4;
				idcEptsErrorCode = 1;
				errorCode = 123;
				break;

			case MessageToPos_Error.ERRORCODE_CARD_READING:
				errorCode = 128;
				break;

			case MessageToPos_Error.ERRORCODE_MAGNETIC_CARD_READING:
				idcEptsErrorCode = 21;
				errorCode = 128;
				break;

			case MessageToPos_Error.ERRORCODE_SMART_CARD_READING:
				lastTransactionType = 4;
				idcEptsErrorCode = 22;
				errorCode = 128;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_TIMEOUTEXPIRED:
				idcEptsErrorCode = 10;
				errorCode = 129;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_READPIN_TIMEOUTEXPIRED:
				idcEptsErrorCode = 10;
				errorCode = 129;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_READCARD_TIMEOUTEXPIRED:
				lastTransactionType = 4;
				idcEptsErrorCode = 12;
				errorCode = 129;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_DIRECTCMD_TIMEOUTEXPIRED:
				idcEptsErrorCode = 15;
				errorCode = 129;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_WRONGPIN:
				errorCode = 130;
				break;

			case MessageToPos_Error.ERRORCODE_PINPAD_GENERIC:
				idcEptsErrorCode = 6;
				errorCode = 130;
				crdSts = false;
				break;

			case MessageToPos_Error.ERRORCODE_USER_CANCELED:
				if (isActiveAnnulo) {
					idcEptsErrorCode = 14;
				} else {
					idcEptsErrorCode = 11;
				}
				errorCode = 126;
				break;

			case MessageToPos_Error.ERRORCODE_INTERNALERROR:
				errorDescription = eDescription;
				errorCode = 126;
				break;

			case MessageToPos_Error.ERRORCODE_EPTS_GENERIC:
			default:
				idcEptsErrorCode = 4;
				errorDescription = eDescription;
				PosGPE.errorCode = 126;
				break;
		}
		PosGPE.doLog("Errore:" + eDescription);
		isActiveDisplayGPE = false;

		logger.info("EXIT fail");
	}

	public static int continueDiscountedPayment(long amt) {
		logger.info("ENTER continueDiscountedPayment");
		logger.info("amt: " + amt);

		Integer Amt = new Integer((int) amt);
		CommandFromPos_ResponseToPanChecking cmd;

		cmd = new CommandFromPos_ResponseToPanChecking();
		if (amt > 0) {

			logger.info("preset: " + preset);
			if (preset != -1) {
				cmd.setExtraData(preselect[preset]);
			}

			cmd.setPurchaseAmount(Amt.intValue());
			cmd.setSuccess(true);
		} else {
			cmd.setSuccess(false);
		}

		logger.info("EXIT continueDiscountedPayment");
		return ppp(cmd);
	}

	public static boolean pingPinPad() {
		logger.info("ENTER pingPinPad");

		CommandFromPos_PingPinpad cmd;

		cmd = new CommandFromPos_PingPinpad();

		logger.info("EXIT pingPinPad");
		return PosGPE.ppp(cmd) == 0;
	}

	public static void checkPinPad() {
		logger.info("ENTER checkPinPad");

		boolean active;

		if (isPinPadPingEnabled && !isGpeDisabled) {
			active = PosGPE.pingPinPad();

			logger.info("active: " + active);
			if (!active) {
				panel.clearLink(Mnemo.getInfo(130), 1);
			}
			panel.dspStatus(5, "PINPAD", true, !active);
		}

		logger.info("EXIT checkPinPad");
	}

	public static void checkEptsUPB(boolean withModal) {
		logger.info("ENTER checkEptsUPB");

		boolean active = false;
		boolean upbServerActive = false;
		String msg = "";

		logger.info("isPingEnabled: " + isPingEnabled);
		logger.info("isGpeDisabled: " + isGpeDisabled);
		if (isPingEnabled && !isGpeDisabled) {
			active = gpe.getServerIsActive();
			logger.info("active: " + active);
			logger.info("withModal: " + withModal);

			if (!active && withModal) {
				panel.clearLink(Mnemo.getInfo(125), 1);
			}
			panel.updateEpts(active);
		}

		logger.info("EXIT checkEptsUPB");
	}
	public static void checkEcommerce(){
		logger.info("ENTER checkEcommerce");

		if(ECommerceManager.getInstance().isEnabled()){
			ECommerceManager.getInstance().sendHeartBeatMessage();
		}

		logger.info("EXIT checkEcommerce");

	}

	public static String getErrorDescription() {
		return errorDescription;
	}

	public static int getErrorCode() {
		return errorCode;
	}

	public void GPEDataCollect(int tndnum) {
		logger.info("ENTER GPEDataCollect");

		int lTRASize = lTRA.getSize();
		int intStep = 0;

		try {
			logger.info("tnd[tndnum].type: " + tnd[tndnum].type);
			logger.info("dataCollectType: " + dataCollectType);
			if (tnd[tndnum].type == 'P') {
				switch (dataCollectType) {
					case 1:
						intStep = 1;
						Itmdc.IDC_write('i', 'e', 0, "", 0,0);
						break;

					case 2:
						intStep = 2;
						Itmdc.IDC_write('i', 'e', 0x50, "", 0, 0);
						intStep = 3;
						Itmdc.IDC_write('i', 'f', 0x50, "", 0,0);
						break;
				}

				if ((lTRASize == 0) && (lTRA.getSize() > 0)) {
					intStep = 4;
					logger.info("write F line");
					Itmdc.IDC_write('F', sc_value(tra.spf1), tra.spf3, tra.number, 0, 0);
					intStep = 5;
					tblWrite();
				}
			} else {
				logger.error("Tender not allowed: type: " + tnd[tndnum].type + " num: " + tndnum);
			}
		} catch (Exception e) {
			logger.error("Eccezione in GPEDataCollect (" + tndnum + ") step " + intStep + "! \n" + e);
		}

		logger.info("EXIT GPEDataCollect");
	}

	public static int getDataCollectEptsError() {
		return idcEptsErrorCode;
	}

	public static void loadGPEParameters(String line, int nrOfLine) {
		logger.info("ENTER loadGPEParameters");
		logger.info("line: " + line);
		logger.info("nrOfLine: " + nrOfLine);

		switch (nrOfLine) {
			case 0:
				isPingEnabled = Integer.parseInt(line.substring(0, 2)) != 0;
				dataCollectType = (byte) Integer.parseInt(line.substring(2, 4));
				isEptsPartialPaymentAllowed = Integer.parseInt(line.substring(4, 6)) != 0;
				isPinPadPingEnabled = Integer.parseInt(line.substring(6, 8)) != 0;
				isGpeDisabled = Integer.parseInt(line.substring(8, 10)) != 0;
				isSmartCardStatusDisabled = Integer.parseInt(line.substring(10, 12)) != 0;
				isGPElite = Integer.parseInt(line.substring(12, 14)) != 0;
				isCreditCardVoucherPrintEnabled = Integer.parseInt(line.substring(14, 16)) != 0;
				minAmountToPrint = Integer.parseInt(line.substring(16, 22));
				currencyId = line.charAt(23);  //CURRENCY-CGA#A

				break;
		}

		logger.info("EXIT loadGPEParameters");
	}

	public static boolean getIsEptsPartialPaymentAllowed() {
		return isEptsPartialPaymentAllowed;
	}

	public static boolean getIsGPElite() {
		return isGPElite;
	}

	public static int getLastTransactionType() {
		return lastTransactionType;
	}

	public static void setLastTransactionType(int trans) {
		lastTransactionType = trans;
	}

	public static String getVariazAddress() {
		return vsp.getEptsAddress();
	}

	public static boolean getVariazStatus() {
		return vsp.getStatusPing();
	}

	public static Date getVariazDataora() {
		return vsp.getDataOra();
	}

	public static void setRetry(int retryVal) {
		retry = retryVal;
	}

	public static int getRetry() {
		return retry;
	}

	public static int getMaxNumberOfRetries() {
		return gpe.getMaxNumberOfRetries();
	}

	public static boolean getFlagEptsVoid() {
		return flagEptsVoid;
	}

	public static void setFlagEptsVoid(boolean value) {
		flagEptsVoid = value;
	}

	public static long getAmountEptsVoid() {
		return amountEptsVoid;
	}

	public static void setAmountEptsVoid(long value) {
		amountEptsVoid = value;
	}

	public static boolean isCreditCardVoucherPrintEnabled() {
		return isCreditCardVoucherPrintEnabled;
	}

	public static int getMinAmountToPrint() {
		return minAmountToPrint;
	}

	/*
	 ** Hardware init
	 */

	public int action0(int spec) {
		logger.info("ENTER action0");
		logger.info("spec: " + spec);

		int retv;

		retv = 0;
		if (PosGPE.pingPinPad()) {
			logger.info("ping pinpad");

			if (spec == 1) {
				retv = PosGPE.PinpadInit();
			} else {
				retv = PosGPE.EMVPinpadInit();
				logger.info("EMVPinpadInit retv: " + retv);

				if (retv == 0) {
					PosGPE.finalizeTransaction();
					PosGPE.deleteEptsVoidFlag();
					DevIo.printCreditCardVoucher();
				}
			}

			if (retv == 0) {
				panel.clearLink(Mnemo.getMenu(101), 1);
			} else {
				panel.clearLink(Mnemo.getInfo(131), 1);
				retv = 0;
			}
		} else {
			panel.clearLink(Mnemo.getInfo(130), 1);
		}

		logger.info("retv: " + retv);
		logger.info("EXIT action0");
		return retv;
	}

	/*
	 ** Void
	 */
	public int action1(int spec) {
		logger.info("ENTER action1");
		logger.info("spec: " + spec);

		int retv;

		if (tra.mode == Struc.M_GROSS) {
			logger.info("EXIT action1 - return 7");
			return 7;
		}

		lastTransactionType = 1;
		setFlagEptsVoid(true);
		setAmountEptsVoid(0);
		retv = PosGPE.EptsVoid();
		setFlagEptsVoid(false);

		logger.info("retv: " + retv);
		if (retv == 0) {
			accumReg(8, 14, 1, getAmountEptsVoid());
			int oldTraCode = tra.code;

			tra.mode = 0;
			tra.code = 30;

			Itmdc.IDC_write('i', 3, 0, "", 0, getAmountEptsVoid());
			Itmdc.IDC_write('F', sc_value(tra.spf1), tra.spf3, tra.number, 0, getAmountEptsVoid());

			tra.code = oldTraCode;

			tblWrite();

			PosGPE.finalizeTransaction();
			PosGPE.deleteEptsVoidFlag();
			DevIo.printCreditCardVoucher();
			//DevIo.setScannersEnabled(true);
			/*if (ItalianOption.getFastIsFastLane()) {
				panel.clearLink(161, 1);
			}*/
			//FastlaneInternational.gotoPosPrinter();
		}

		GPEDataCollect(7);

		logger.info("EXIT action1 - return " + retv);
		return retv;
	}

	public static int abortTransaction() {
		CommandFromPos_ResponseNack cmd;

		cmd = new CommandFromPos_ResponseNack();

		return ppp(cmd);
	}

	public static String getPartialCardNumber(String cardNumber, int length) {
		logger.info("ENTER getPartialCardNumber");
		logger.info("cardNumber: " + cardNumber);
		logger.info("lenght: " + length);

		if (cardNumber.length() == 0) {
			logger.info("EXIT getPartialCardNumber - return empty");
			return "";
		}
		if (cardNumber.length() > length) {
			String retValue = cardNumber.substring((cardNumber.length() - length));
			logger.info("EXIT getPartialCardNumber - return: " + retValue);
			return retValue;
		}

		logger.info("EXIT getPartialCardNumber - return " + cardNumber);
		return cardNumber;
	}

	public static String getFormattedTransactionDate(String transactionDate) {
		logger.info("ENTER getFormattedTransactionDate");
		logger.info("transactionDate: " + transactionDate);

		if (transactionDate.length() == 0) {
			logger.info("EXIT return empty");
			return "      ";
		}

		String retValue = transactionDate.substring(8) + transactionDate.substring(3, 5) + transactionDate.substring(0, 2);
		logger.info("EXIT getFormattedTransactionDate - return " + retValue);
		return retValue;
	}

	public static String getFormattedTransactionTime(String transactionTime) {
		logger.info("ENTER getFormattedTransactionTime");
		logger.info("transactionTime: " + transactionTime);

		if (transactionTime.length() == 0) {
			return "      ";
		}

		String seconds = (transactionTime.length() == 8) ? transactionTime.substring(6, 8) : "00";
		String retValue = transactionTime.substring(0, 2) + transactionTime.substring(3, 5) + seconds;
		logger.info("seconds: " + seconds);
		logger.info("retValue: " + retValue);

		logger.info("EXIT getFormattedTransactionTime");
		return transactionTime.substring(0, 2) + transactionTime.substring(3, 5) + seconds;
	}

	private static boolean executing = false;

	public static boolean isExecuting() {
		return executing;
	}
}
