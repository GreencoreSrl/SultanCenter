package com.ncr;

import com.ncr.gpe.GpeResult_ReceiptDataInterface;
import org.apache.log4j.Logger;

import java.awt.*;
import java.text.SimpleDateFormat;

abstract class Itmdc extends Basis {
	private static final Logger logger = Logger.getLogger(Itmdc.class);
	static void chk_local(Itemdata ptr) {
		int rec = tra.vItems.size();

		if (tra.code > 3 || tra.res > 0)
			return;
		while (rec-- > 0) {
			Itemdata dci = tra.vItems.getElement(rec);
			if (dci.id != 'S')
				continue;
			if (dci.ext > 0)
				continue;
			if (((dci.spf1 ^ ptr.spf1) & M_RETURN) > 0)
				continue;
			if (!dci.number.equals(ptr.number))
				continue;
			ptr.price = dci.price;
			break;
		}
	}

	static int chk_match(boolean bundled) {
		int cnt = 0, rec = tra.vItems.size();

		while (rec-- > 0) {
			Itemdata dci = tra.vItems.getElement(rec);
			if (dci.id != 'S')
				continue;
			if (dci.mmt != itm.mmt)
				continue;
			if (dci.unit != itm.unit)
				continue;
			if (dci.prpos != itm.prpos)
				continue;
			if ((dci.spf1 & M_RETURN) > 0)
				continue;
			if (dci.spf2 > 0)
				continue;
			if (!bundled)
				if (!dci.number.equals(itm.number))
					continue;
			cnt += dci.cnt * dci.dec;
		}
		return cnt;
	}

	static boolean chk_tender(Itemdata ptr) {
		int rec = tra.vItems.size();

		while (rec-- > 0) {
			Itemdata dci = tra.vItems.getElement(rec);
			if (dci.id != 'T')
				continue;
			if (dci.tnd == ptr.tnd)
				return true;
		}
		return false;
	}

	static boolean chk_void(Itemdata ptr) {
		int spf1 = ptr.spf1 ^ tra.spf1 ^ M_VOID;
		int bal = 0, cnt = ptr.qty, rec = tra.vItems.size();

		if (options[O_VdCtl] < 10)
			return false;
		if (tra.code == 7 || tra.res > 0)
			return false;
		for (int ind = 128; ind > 0; ind >>= 1)
			if ((spf1 & ind) > 0)
				cnt = 0 - cnt;
		while (rec-- > 0) {
			Itemdata dci = tra.vItems.getElement(rec);
			if (dci.id != 'S')
				continue;
			if (((dci.spf1 ^ ptr.spf1) & M_RETURN) > 0)
				continue;
			if (dci.dec != ptr.dec)
				continue;
			if (dci.dpt_nbr != ptr.dpt_nbr)
				continue;
			if (!dci.number.equals(ptr.number))
				continue;
			if (!dci.serial.equals(ptr.serial))
				continue;
			if (!dci.gCardSerial.equals(ptr.gCardSerial))
				continue; // PSH-ENH-001-AMZ#ADD -- void check serial GC
			if (dci.gCardTopup != ptr.gCardTopup)
				continue; // PSH-ENH-001-AMZ#ADD -- void check topup GC
			if (ptr.prpos > 0)
				if (dci.prpos != ptr.prpos)
					continue;
			if (ptr.prpov > 0)
				if (dci.prpov != ptr.prpov)
					continue;
			if (dci.price == ptr.price)
				bal += dci.cnt;
		}
		return cnt > 0 ? bal < cnt : bal > cnt;
	}

	private static String mask(String serial) {
		return serial.substring(0, serial.length() - 2) + "**";
	}

	static void IDC_write(char type, int code, int subc, String nbr, int cnt, long amt) {
		if (lTRA.getSize() < 1)
			if (type != 'H' && type != 'F')
				IDC_write('H', trx_pres(), tra.spf3, tra.number, tra.cnt, tra.rate);
		if (ctl.sup > 0)
			if (type != 'H') {
				lCTL.read(ctl.sup, lCTL.LOCAL);
				IDC_write('A', trx_pres(), ctl.sup = 0, editNum(lCTL.pers, 8), lCTL.sec, 0l);
			}
		lTRA.init(':');
		lTRA.push(editNum(ctl.sto_nbr, 4)).skip();
		lTRA.push(editKey(ctl.reg_nbr, 3)).skip();
		lTRA.push(editNum(ctl.date, 6)).skip();
		lTRA.push(editNum(ctl.time, 6)).skip();
		lTRA.push(editNum(ctl.tran, 4)).skip(8);
		//WINEPTS-CGA#A BEG
		if ((type == 'i') && ((code == 0x65) || (code == 0x66))) {
			logger.info("push ef");
			lTRA.push("ef".charAt(code - 0x65)).push("PSVTE".charAt(PosGPE.getLastTransactionType())).skip(); // BAS-ENH-2130248-HRL#A
		} else { //WINEPTS-CGA#A END
			lTRA.push(editNum(code, 1)).push(editNum(subc, 1)).skip();
		}
		lTRA.poke(32, type);
		if (type == 'S' || type == 's') {
			if (tra.mode > M_GROSS)
				lTRA.poke(32, "IXLR".charAt(tra.mode - M_INVTRY));
			lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(nbr, 16));
			if (itm.prm < 1)
				lTRA.pushDec(itm.cnt, 5).push(editNum(itm.unit, 4));
			else {
				lTRA.pushDec(itm.dec * itm.cnt, 8).skip(-3).push('.').push(editNum(itm.dec, 3));
			}
			if (itm.ext == 0)
				lTRA.push('*').push(editNum(itm.price, 9));
			else
				lTRA.pushDec(amt, 10);
		}
		if (type == 'C') {
			lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(nbr, 16));
			if (itm.prm < 1)
				lTRA.pushDec(cnt, 5).push(editNum(itm.unit, 4));
			else {
				lTRA.pushDec(itm.dec * cnt, 8).skip(-3).push('.').push(editNum(itm.dec, 3));
			}
			if (itm.ext == 0 && code < 2) {
				int price = code > 0 ? itm.prpov - itm.prpos : itm.price - itm.prpov;
				if (price < 0) {
					lTRA.push('>');
					price = 0 - price;
				} else
					lTRA.push('<');
				lTRA.push(editNum(price, 9));
			} else
				lTRA.pushDec(amt, 10);
		}

		//INSTASHOP-SELL-CGA#A BEG
		if (type == 'd') {
			lTRA.push("0000:" + editTxt("", 16)).skip().push(editNum(Integer.parseInt(nbr), 18));
		}
		//INSTASHOP-SELL-CGA#A END
		//WINEPTS-CGA#A BEG
		//0112:001:200427:115854:3436:007:i:110:eP:0360:                      :200427125
		if (type == 'i') {
			if ((code == 0x65) || (code == 0x66)) {
				String spaces = null;
				GpeResult_ReceiptDataInterface lastReceiptData = PosGPE.getLastEptsReceiptData();

				switch (code) {
					case 'e':
						int tcode = 0;
						String tdate = "";
						String ttime = "";
						int trxResult = 0;

						spaces = "                      ";
						if (PosGPE.isChangeStatusEpts) {
							trxResult = 1;
							nbr = PosGPE.getVariazAddress();
							logger.info("nbr = " + nbr);
							tcode = 0;
							tdate = dateToStr(PosGPE.getVariazDataora(), "yyMMdd");
							ttime = dateToStr(PosGPE.getVariazDataora(), "HHmmss");
						} else {
							if (tra.mode == M_CANCEL) {
								trxResult = 2;
							} else {
								if ((PosGPE.getDataCollectEptsError() == 0) && lastReceiptData != null) {
									trxResult = 0;
									nbr = PosGPE.getPartialCardNumber(lastReceiptData.getCardNumber(), 22);
									logger.info("nbr = " + nbr);
									try {
										tcode = Integer.parseInt(lastReceiptData.getTerminalCode());
									} catch (Exception e) {

									}
									tdate = PosGPE.getFormattedTransactionDate(lastReceiptData.getTransactionDate());
									ttime = PosGPE.getFormattedTransactionTime(lastReceiptData.getTransactionTime());
								} else {
									if (PosGPE.getDataCollectEptsError() == 4) {
										nbr = PosGPE.getErrorDescription();
									}
									trxResult = 1;
								}
							}
						}
						int nbrLegth = nbr == null ? 0 : nbr.length();

						spaces = spaces.substring(nbrLegth);
						lTRA.push(editNum(tcode, 3) + trxResult).skip().push(nbr + spaces).skip()
								.push(editTxt(tdate, 6) + editTxt(ttime, 6));
						break;

					case 'f':
						spaces = "          ";
						int retry = 0;
						String retryEnd = "0";
						String authorizationCode = "";
						int posTenderId = 0;
						int wineptsErrorType = 0;

						if (PosGPE.isChangeStatusEpts) {
							wineptsErrorType = 25;
						} else {
							if ((PosGPE.getDataCollectEptsError() == 0) && (tra.mode != M_CANCEL)) {
								authorizationCode = lastReceiptData.getAuthorizationCode();
								spaces = spaces.substring(authorizationCode.length());
								posTenderId = Integer.parseInt(lastReceiptData.getPosTenderId().toString());
							}
							wineptsErrorType = PosGPE.getDataCollectEptsError();
							retry = PosGPE.getRetry();
							if (PosGPE.getLastTransactionType() == 0) {
								if (retry == PosGPE.getMaxNumberOfRetries()) {
									retryEnd = "1";
								} else {
									if (retry > PosGPE.getMaxNumberOfRetries()) {
										retryEnd = "2";
										retry = PosGPE.getMaxNumberOfRetries();
									}
								}
							} else {
								if (PosGPE.getLastTransactionType() == 4) {
									if (retry == PosGPE.getMaxNumberOfRetries()) {
										retryEnd = "1";
									}
								}
							}
							retry = retry > 9 ? 9 : retry;
						}
						lTRA.push(editNum(posTenderId, 2) + editNum(wineptsErrorType, 2)).skip()
								.push(authorizationCode + spaces).skip()
								.push(String.valueOf(retry) + retryEnd + "                      ");
						break;
				}
			} else if (code == 3) {
				lTRA.push(editNum(ctl.ckr_nbr, 4)).skip().push(editTxt(" ", 16)).skip().push(editNum(tra.code, 2))
						.pushDec(1, 6).pushDec(amt, 10);
			}
		}
		//WINEPTS-CGA#A END
		if (type == 'W') {
			if (code == 0) {
				lTRA.push(editNum(itm.sit, 1) + editNum(itm.vat, 1) + editNum(itm.cat, 2)).skip().push(editTxt(nbr, 16))
						.skip().push(editNum(itm.mmt, 2)).skip().push(editHex(itm.flag, 2)).push(editHex(itm.flg2, 2))
						.push(editNum(itm.ages, 1)).pushDec(cnt, 10);
			} else if (code == 1) {
                lTRA.push("0021:" + editTxt(nbr, 35));
            } else {
                lTRA.push("0000:" + editTxt(nbr, 16)).skip().push(editNum(0, 18));
            }
		}
		// PSH-ENH-001-AMZ#BEG -- idc record g GiftCard
		if (type == 'g') {
			lTRA.push(editKey(itm.dpt_nbr, 4) + ":" + editTxt(mask(nbr), 35));
		}
		// PSH-ENH-001-AMZ#END -- idc record g
		// PSH-ENH-008-AMZ#BEG -- idc record Z - PhiloShopic Sync Errors
		if (type == 'Z') {
			lTRA.push(' ').push(editTxt(mask(nbr), 20)).skip().push(editNum(cnt, 2)).pushDec(0, 6).pushDec(amt, 10);
		}
		// PSH-ENH-008-AMZ#END -- idc record Z - PhiloShopic Sync Errors
		if (type == 'A') {
			lTRA.push(editNum(lCTL.key, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'B' || type == 'Q' || type == 'J' || type == 'V') {
			lTRA.push(editNum(ctl.ckr_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}  //0112:001:200505:152541:3549:010:Q:120:0027:            8002:00+00001+000000001
		if (type == 'G') {
			lTRA.push(editKey(itm.cmp_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'P') {
			lTRA.push(editKey(tra.slm_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'M') {
			lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(itm.stat, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'N') {
			lTRA.push(editNum(ctl.ckr_nbr, 4)).skip();
			if (subc < 3) {
				lTRA.push(editNum(itm.stat, 2)).skip().push(editTxt(nbr, 32));
			} else {
				lTRA.push(editTxt(nbr, 16)).skip().push(editNum(itm.tnd, 2)).pushDec(cnt, 6).pushDec(amt, 10);
			}
		}

		//SAFP-20170224-CGA#A BEG
		if (type == 't') {
			lTRA.push(editNum(ctl.ckr_nbr, 4)).skip();
			lTRA.push(editNum(itm.stat, 2)).skip();
			lTRA.push(editNum(subc, 4)).skip();
			lTRA.push(editTxt(nbr, 27));
		}
		//SAFP-20170224-CGA#A END

		// TAMI-ENH-20140526-SBE#A BEG
		if (type == 'O' || type == 'T' || type == 'z')
		// TAMI-ENH-20140526-SBE#A END
		{
			lTRA.push(editNum(itm.dpt, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(itm.tnd, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'D' || type == 'K') {
			//SPINNEYS-2017-033-CGA#A BEG
			logger.info("codePromoTrim: " + WsLoyaltyService.getInstance().getCodePromoTrim());
			if (type == 'K') {
				logger.info("nbr: >" + nbr + "<");
				nbr = nbr.substring(4);
				leftFill(String.valueOf(nbr), 16, ' ');
				if (WsLoyaltyService.getInstance().getCodePromoTrim()) {
					if (nbr.trim().length() > 6) {
						nbr = nbr.trim().substring(0, 6);
					}
				}
			}
			//SPINNEYS-2017-033-CGA#A END

			lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'H' || type == 'F' || type == 'Y') {
			lTRA.push(editNum(ctl.ckr_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		if (type == 'k') {
			int index = Integer.valueOf(nbr);
			Itemdata dci = tra.vItems.getElement(index - 1);
			tra.vItems_k.addElement('S', dci);
			lTRA.push(editNum(index, 4)).skip().push(editTxt(dci.number, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}
		// EMEA-UPB-DMA
		if (type == 'u') {
			if (nbr.length() > 16) {
				lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(nbr, 32)).push(":00");
			} else {
				lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(nbr, 16)).push(':')
						.push(String.format("%016d", itm.operationID)).push(":1");
			}
		}
		// EMEA-UPB-DMA
		// SARAWAT-ENH-20150507-CGA#A BEG
		if (type == 'c') {
			int state = GdSarawat.getInstance().isRegistration() ? 1 : 0;

			//SPINNEYS-2017-033-CGA#A BEG
			if (GdSpinneys.getInstance().getFunctionLoyalty() >= 0) {
				state = GdSpinneys.getInstance().getFunctionLoyalty();
				String cusId = cus.cusId == null ? "" : cus.cusId;
				lTRA.push(editTxt(cusId, 10)).skip().push(editTxt(cus.number, 10)).push(':')
						.push(editTxt(cus.mobile, 16)).push(":" + state);
			} else {  //SPINNEYS-2017-033-CGA#A END
				lTRA.push(editNum(ctl.ckr_nbr, 4)).skip().push(editTxt(cus.number, 16)).push(':')
						.push(editTxt(cus.mobile, 16)).push(":" + state);
			}

		}
		// SARAWAT-ENH-20150507-CGA#A END


		if (type == 'a') {
			lTRA.push(editKey(itm.dpt_nbr, 4)).skip().push(editTxt(GdUmniah.getInstance().getRechargePin(), 16)).push(':')
					.push(editTxt(GdUmniah.getInstance().getTransactionId(), 18));
			//0112:001:180110:173901:1049:::::a::00:          :   1234567:            976543
		}

		if (type == 'b') {
			lTRA.push(editKey(itm.cmp_nbr, 4)).skip().push(editTxt(nbr, 16)).skip().push(editNum(tra.code, 2))
					.pushDec(cnt, 6).pushDec(amt, 10);
		}

		lTRA.write();
	}

	static void IDC_update(int rec, int code, int subc, String nbr, long amt) {
		lTRA.read(rec);
		lTRA.init(lTRA.pb).onto(35, editNum(code, 1)).push(editNum(subc, 1)).skip(6).push(editTxt(nbr, 16)).skip(9)
				.pushDec(amt, 10);
		lTRA.rewrite(rec, 0);
	}
	//WINEPTS-CGA#A BEG
	public static String dateToStr(java.util.Date data, String format) {
		String ret;
		SimpleDateFormat sdf = new SimpleDateFormat(format);

		if (data != null) {
			ret = sdf.format(data);
		} else {
			ret = "";
		}
		return ret;
	}
	//WINEPTS-CGA#A END
}