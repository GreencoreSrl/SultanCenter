package com.ncr;

import com.ncr.eft.*;
import com.ncr.eft.EftPlugin;
import com.ncr.eft.MarshallEftPlugin;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

public class GdRegis extends Action {
	/**
	 * slip preselect by actioncode
	 */

	private static final Logger logger = Logger.getLogger(GdRegis.class);

	static void set_ac_ctl(int code) {
		int ind = slpFind(tra.code = code);

		if (ind < 0) {
			if (tra.slip > 0)
				slpStatus(0, tra.slip);
			return;
		}
		if (ctl.mode != M_RENTRY) {
			if (DevIo.station(4))
				if (slp[ind].flag > ' ')
					slpStatus(1, 1);
			if (slp[ind].flag == '-')
				tra.slip = 0x20;
		}
		if (tra.slip == 0)
			tra.slip |= options[O_copy2] & 0x10;
	}

	/**
	 * setup begin of transaction
	 */

	public static void set_tra_top() {
		set_tra_top(false);
	}

	static void set_tra_top(boolean voucher) {
		GdSigns.setEod(false);  // FIX-20170413-CGA#A
        GdPsh.setUTID(); // AMZ-2017-003#ADD
		ECommerce.resetAutomaticAmazonItem();  //AMAZON-COMM-CGA#A
		ECommerce.resetAlreadyAmzCommCalc();
		//AMAZON-COMM-CGA#A END
		ECommerce.setInstashopChoice("0");  //INSTASHOP-SELL-CGA#A

		//INSTASHOP-SELL-CGA#A BEG
		//GdTrans.setIsInstanshopResume(false);
		//GdTrans.setNumberTraResume(0);
		//GdTrans.resetInstashop();
		ECommerce.setInstashopChoiceType("");
		//INSTASHOP-SELL-CGA#A END


			tra.tim = sec_time();
		lCTL.read(ctl.ckr, lCTL.LOCAL);
		tra.slm_nbr = keyValue(editNum(lCTL.key, 4));
		tra.slm_prs = lCTL.pers;
		if (tra.code < 8) {
			if (tra.code == 0)
				set_ac_ctl(tra.code);
			for (int ind = 10; ind < 16; ind++) {
				if (head_txt[ind] != null)
					prtLine.init(head_txt[ind]).type(2);
				if (options[O_CKRon] == ind)
					prtLine.init(lCTL.text).type(2);
			}
			if ((options[O_copy2] & 0x20) > 0) {
				set_trailer();
				if (trl_line.charAt(34) != 'p')
					prtLine.onto(34, "  ");
				prtLine.onto(-1, ' ').onto(36, "    ").book(3);
			}
			EftIo.eftTrans(true); /* start preswipe mode */
			if (tra.code > 0) {
				prtDwide(ELJRN + 3, Mnemo.getMenu(tra.head));
				TView.append('>', 0x00, Mnemo.getMenu(tra.head), "", "", "", "");
			}
		}
		if (ctl.mode > 0)
			prtDwide(ELJRN + 2, Mnemo.getInfo(ctl.mode + 17));

		if (!voucher && tra.code == 0 && SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().startTransaction(ctl.tran);
			SscoPosManager.getInstance().itemResponse();
		}

		if (tra.code == 0 && tra.spf1 == 0) {
			Promo.startTransaction();
			if ((options[O_copy2] & 0x40) > 0)
				tra.slip |= 0x40;
		}
		if (ECommerce.isResumeInstashop()) tra.slip &= ~0x40;
	}

	/**
	 * setup trailer line
	 */
	public static void set_trailer() {
		set_trailer(0);
	}

	static void set_trailer(int delta) {
		logger.info("setting trailer with tra: " + ctl.tran + " delta: " + delta);
		prtLine.init(trl_line).poke(-1, (tra.slip & 0x21) > 0 ? '.' : '*');
		prtLine.push(editNum(ctl.tran + delta, 4)).skip().push(editNum(ctl.sto_nbr, 4)).skip().push(editKey(ctl.reg_nbr, 3))
				.skip().push(editNum(ctl.ckr_nbr, 3));
		prtLine.onto(20, editDate(ctl.date)).onto(29, editTime(ctl.time / 100)).onto(38, editNum(tra.code, 2));
		if (prtLine.peek(34) == 'p') {
			int hh = ctl.time / 10000;
			if (hh < 12)
				prtLine.onto(34, 'a');
			if (hh == 0)
				hh = 24;
			if (hh > 12)
				prtLine.onto(29, editNum(hh - 12, 2));
		}
	}

	/**
	 * print receipt header
	 */
	public static void hdr_print() {
		if (!DevIo.station(0x40))
			DevIo.cutPaper();
		else
			DevIo.tpmPrint(2, 2, "");
		prtBlock(2, head_txt, 0, 10);
		if (DevIo.station(0x40))
			DevIo.cutPaper();
	}

	static void vat_print() {
		int rate, rec;
		long limit = limitBy(options[O_Vaton]);

		prtLine.init(' ').book(2);
		for (int ind = 0; ind < vat.length; ind++) {
			if ((rec = reg.find(7, 11 + ind)) < 1)
				continue;
			Sales sls = reg.sales[rec - 1][0];
			if (sls.isZero())
				continue;
			itm = new Itemdata();
			itm.number = editRate(rate = vat[ind].rate);
			itm.dsc = roundBy(sls.total * rate * 10 / (1000 + rate), 10);
			itm.cnt = sls.items;
			itm.amt = sls.total - itm.dsc;
			accumTax(0, ind, itm.cnt, itm.dsc);
			Itmdc.IDC_write('V', ind, 1, itm.number, itm.cnt, itm.amt);
			if (rate > 0)
				Itmdc.IDC_write('V', ind, 0, itm.number, itm.cnt, itm.dsc);
			if (vat[ind].flat > 0) {
				prtLine.init(vat[ind].text).upto(17, editInt(itm.cnt)).upto(40, editMoney(0, itm.amt)).book(3);
				continue;
			}
			if (limit > Math.abs(tra.amt))
				continue;
			if ((options[O_DWide] & 0x04) > 0) {
				prtLine.init(vat[ind].text).upto(17, editMoney(0, itm.dsc)).onto(20, Mnemo.getText(77))
						.upto(25, itm.number).upto(40, editMoney(0, itm.amt + itm.dsc)).book(3);
				continue;
			}
			prtLine.init(Mnemo.getText(55)).onto(20, vat[ind].text).upto(40, editMoney(0, itm.amt)).book(3);
			if (itm.dsc == 0)
				continue;
			prtLine.init(Mnemo.getText(59)).upto(25, itm.number).upto(40, editMoney(0, itm.dsc)).book(3);
		}
	}

	static void dsc_print() {
		int ind = 0, rec, ic = 3, sc = 0;
		int cnt;
		long amt;

		while (ic < 5) {
			if (++sc > 8) {
				ic++;
				sc = 0;
				continue;
			}
			if ((rec = reg.find(ic, sc)) == 0)
				continue;
			if ((amt = reg.sales[rec - 1][0].total) == 0)
				continue;
			lREG.read(rec, lREG.LOCAL);
			if ((lREG.tflg & 1) == 0)
				continue;
			if (ind < 1) {
				panel.jrnPicture("JRN_SAVE");
				prtBlock(2, save_txt, 0, ind = 10);
			}
			stsLine.init(lREG.text).upto(20, editMoney(0, -amt));
			prtDwide(2, stsLine.toString());
		}
		while (sc++ < 4) {
			if ((rec = reg.find(8, 30 + sc)) == 0)
				continue;
			if ((cnt = reg.sales[rec - 1][0].items) == 0)
				continue;
			lREG.read(rec, lREG.LOCAL);
			if ((lREG.tflg & 1) == 0)
				continue;
			if (ind < 11) {
				if (!Promo.isNoPrintPoints())    //NOPRINTPOINTS-CGA#A
					prtBlock(2, save_txt, 10, ind = save_txt.length);
			}

			if (!Promo.isNoPrintPoints()) {   //NOPRINTPOINTS-CGA#A
				stsLine.init(lREG.text).upto(20, editPoints(cnt, false));
				prtDwide(2, stsLine.toString());
			}
		}
		prtBlock(2, mess_txt, 0, 10);
	}

	/**
	 * print receipt trailer
	 */
	public static int prt_trailer(int lfs) {
		int ind, rec = 0;

		dspLine.show(1);
		panel.dspPicture(dspBmap);
		if (tra.mode == M_GROSS) {
			prtBlock(ELJRN + 2, euro_txt, 0, 10);
			if ((ind = tnd_tbl[K_AltCur]) > 0)
				if ((options[O_DWide] & 0x20) > 0)
					prtLine.init(tnd[ind].editXrate(true)).book(2);
			if ((options[O_DWide] & 0x10) > 0) {
				if ((options[O_DWide] & 2) > 0) {
					stsLine.init(tnd[ind].symbol).upto(20, editMoney(ind, tnd[ind].hc2fc(tra.amt)));
					prtDwide(ELJRN + 2, stsLine.toString());
				} else
					prtLine.init(Mnemo.getText(24)).onto(20, tnd[ind].symbol)
							.upto(40, editMoney(ind, tnd[ind].hc2fc(tra.amt))).book(2);
			}
			prtBlock(ELJRN + 2, euro_txt, 10, euro_txt.length);
		}
		while (--lfs > 0)
			prtLine.init(' ').book(2);
		if (ctl.mode > 0)
			prtDwide(ELJRN + 2, Mnemo.getInfo(ctl.mode + 17));

		eftPluginManager.printVouchers(EftPlugin.SAME_RECEIPT_TYPE);

		set_trailer();
		prtLine.book(3);

		logger.info("prt_trailer print Philoshopic uniqueTransactionId barcode");
		if (tra.print) GdPsh.printBarcode();

		if (tra.slip == 0x10 && tra.print) {
			ElJrn.second_cpy(2, ctl.tran, tra.slip = 0);
		}
		while (rec < mat.length) {
			MsgLines ptr = mat[rec++];
			if (tra.mode != ptr.mode)
				continue;
			if (tra.code != ptr.code)
				continue;
			prtBlock(2, mdac_txt, ptr.line, ptr.last);
		}
		if (tra.mode == M_GROSS) {
			if (tra.spf1 == 0)
				dsc_print();
			if ((options[O_copy2] & 0x02) > 0) {
				String nbr = editNum(ctl.date, 4) + editKey(ctl.reg_nbr, 3) + editNum(ctl.tran, 4);
				DevIo.tpmLabel(2, cdgSetup(1 + nbr + 0, ean_weights, 10));
			}
		}
		ElJrn.tender_cpy();

		if (tra.slip > 0) {
			if ((tra.slip & 1) > 0)
				ElJrn.second_cpy(4, ctl.tran, 0);
			slpStatus(0, 0x21);
		} else
			hdr_print();
		//WINEPTS-CGA#A BEG
		logger.info("PosGPE.isCreditCardVoucherPrintEnabled: " + PosGPE.isCreditCardVoucherPrintEnabled());
		logger.info("PosGPE.getMinAmountToPrint(): " + PosGPE.getMinAmountToPrint());
		logger.info("tra.amt: " + tra.amt);
		if (!PosGPE.isCreditCardVoucherPrintEnabled() || tra.amt >= PosGPE.getMinAmountToPrint()) {
			DevIo.haveToPrintCreditCardVoucher();
		} else {
			DevIo.removeCreditCardVoucher();
		}
		//WINEPTS-CGA#A END
		if (ctl.mode > 0)
			tra.mode = ctl.mode;
		ElJrn.tender_bof();
		if (tra.mode == M_GROSS) {
			if (DevIo.station(4))
				for (ind = 0; ind < 8;) {
					if ((rec = reg.find(2, ++ind)) == 0)
						continue;
					if ((tra.spf1 & 256 >> ind) == 0)
						continue;
					Sales sls = reg.sales[rec - 1][0];
					if (sls.isZero())
						continue;
					lREG.read(rec, lREG.LOCAL);
					if ((lREG.tflg & 0x20) == 0)
						continue;
					oplLine.init(center(lREG.text, 20, '*')).show(2);
					DevIo.slpInsert(0);
					prtLine.init(Mnemo.getText(21)).upto(17, editInt(sls.items)).onto(20, lREG.text)
							.upto(40, editMoney(0, sls.total)).type(4);
					set_trailer();
					prtLine.type(4);
					DevIo.slpRemove();
				}
			for (ind = 0; ++ind < tnd.length;) {
				if (tnd[ind].type != 'D')
					continue;
				if ((rec = reg.findTnd(ind, 1)) < 1)
					continue;
				if (reg.sales[rec - 1][0].total != 0)
					tra.spf3 |= 2;
			}
			if ((tra.spf3 & 1) > 0) /* tax exemption */ {
				ElJrn.print_tnd(2, 2);
				prtBlock(2, xtax_txt, 0, xtax_txt.length);
				hdr_print();
			}
			// UPB-EMEA-DMA BEG
			for (int i = 0; i < tra.itemsVsUPB.size(); i++) {
				WinUpb.getInstance().upb_confirm(tra.itemsVsUPB.get(i), i);
				if (!tra.itemsVsUPB.get(i).isVoid())
					hdr_print();
			}
			// UPB-EMEA-DMA END

            //PSH-ENH-20151120-CGA#A BEG
			GdPsh.printUtilities();
			GdPsh.printGiftCardPayments();
            //PSH-ENH-20151120-CGA#A END

            if ((tra.spf3 & 6) > 0    /* surcharge or charge */
					|| !ECommerce.getInstashopChoice().equals("0")) {   //INSTASHOP-SELL-CGA#A
				if (tra.spf2 > 0) {
					prtLine.init(Mnemo.getText(sc_value(tra.spf2))).upto(20, tra.number).type(2);
				}
				ElJrn.printAdditionalHeader(tra.special);
				ElJrn.print_tnd(2, 2);
				prtBlock(2, spec_txt, 0, spec_txt.length);
				hdr_print();
			}

			eftPluginManager.printVouchers(EftPlugin.NEW_RECEIPT_TYPE);
		}
		if (tra.eCommerce) {
			ECommerceManager.getInstance().endOfTransaction();
		}

		//INSTASHOP-SELL-CGA#A BEG
		if (ECommerce.getInstashopChoiceType().trim().startsWith("S")
				|| ECommerce.getInstashopChoiceType().trim().equals("F")) {
			if (ECommerce.getSecondCopyDeliveryEnable().get(ECommerce.getTndInstashop()).trim().equals("true")) {    //qui second copy
				ElJrn.second_cpy(2, ctl.tran, 1);
			}
			ECommerce.setInstashopChoiceType("");
		}
		//INSTASHOP-SELL-CGA#A END

		if (tra.mode < 9 && ctl.ckr_nbr < 800) {
			if (tra.code != 2)
				for (ind = 0; ind < tnd.length; ind++) {
					if (ctl.lan > 0)
						break;
					if (tnd[ind].limit[L_MaxDrw] == 0)
						continue;
					tnd[ind].alert = netio.mnyget(ctl.ckr_nbr, ind, tnd[ind].alert);
				}
			accumReg(9, 1, 1, sec_diff(ctl.work));
			ctl.work = sec_time();
			for (int sc = 4; sc < 7; sc++) {
				if ((ind = reg.find(9, sc)) > 0)
					tra.tim += reg.sales[ind - 1][0].total;
			}
		}
		if (tra.code == 99) {
			if ((options[O_xTill] & 4) == 0)
				DevIo.drwPulse(0);
		}
		//INSTASHOP-SELL-CGA#A BEG
		if (!ECommerce.getInstashopChoice().equals("0") && ECommerce.getInstashopChoiceType().trim().startsWith("S")) {
			Itmdc.IDC_write('t', 0, 0, ECommerce.getAccount(), 0, 0);
			Itmdc.IDC_write('d', 0, 0, ECommerce.getInstashopChoice(), 0, 0);

			//INSTASHOP-RECORD-CGA#A BEG
			lDDQ.recno = 1;

			String ddqCode = ECommerce.getDdqCodeMap().get(ECommerce.getTndInstashop());

			try {
				if (Match.dd_query(ddqCode.charAt(0), Integer.parseInt(ddqCode.charAt(1) + "")) >= 0) {
					Itmdc.IDC_write('Q', itm.spf1, 0, itm.number, itm.cnt, 1);
				}
			} catch(Exception e) {

			}
			//INSTASHOP-RECORD-CGA#A END
		} else { //INSTASHOP-RECORD-CGA#A BEG
			if (!ECommerce.getAccount().equals("") && tra.amt == 0) {

				lDDQ.recno = 1;

				String ddqCode = ECommerce.getDdqCodeMap().get(ECommerce.getTndInstashop());

				try {
					if (Match.dd_query(ddqCode.charAt(0), Integer.parseInt(ddqCode.charAt(1) + "")) >= 0) {
						Itmdc.IDC_write('Q', itm.spf1, 0, itm.number, itm.cnt, 1);
					}
				} catch(Exception e) {

				}
				//INSTASHOP-RECORD-CGA#A END
			}
		}
		//INSTASHOP-SELL-CGA#A END


		//SSCO-RECORD-CGA#A BEG
		if (SscoPosManager.getInstance().isUsed() && itm != null){
			lDDQ.recno = 1;
			if (Match.dd_query('S', 3) >= 0) {
				Itmdc.IDC_write('Q', itm.spf1, 0, itm.number, itm.cnt, 1);
			}
		}
		//SSCO-RECORD-CGA#A END

		Itmdc.IDC_write('F', trx_pres(), tra.spf3, tra.number, tra.cnt, tra.amt);
		if (tra.spf2 > 0)
			if (lTRA.getSize() > 1) /* late customer to H-record */ {
				Itmdc.IDC_update(1, trx_pres(), tra.spf3, cus.getNumber(), tra.rate);
			}
		tblWrite();
		if (tra.mode < 9 && ctl.ckr_nbr < 800) {
				//&& !GdTrans.isInstanshopResume()) {  //INSTASHOP-RESUME-CGA#A
			DevIo.drwCheck(options[O_Alert]);
			TView.clear();
			showTotal(0);
			panel.jrnPicture("JRN_" + editNum(ctl.date, 4));
			if (panel.journal.image == null)
				panel.jrnPicture("JRN_0000");
			if (tra.isActive()) {
				if (ctl.lan == 0)
					if (netio.eodPoll(ctl.reg_nbr) > 0) {
						panel.clearLink(Mnemo.getMenu(18), 1);
					}
				if (tra.code < 8) {
					EftIo.eftTrans(false); /* cancel preswipe mode */
				}
				accumReg(9, 7, 1, sec_diff(tra.tim)); // trailing time
			}
		}

		return GdTrans.tra_clear();
	}

	/**
	 * pers/cust preselect
	 */
	public int action0(int spec) {
		int sts;

		if (tra.stat > 0)
			return 5;
		if (spec == 0) /* anonymous sales */ {
			dspLine.init(Mnemo.getMenu(37));
			tra.stat = 1;
			return 0;
		}

		if (GdSarawat.getInstance().isCapillaryEnabled()) // PSH-ENH-005-AMZ#BEG -- customer id
			// SARAWAT-ENH-20150507-CGA#A BEG
			if (!GdSarawat.getInstance().isCustomerMenuEnabled()) {
				logger.info("customer menu is disabled - return error");
				return 7;
			}
		// SARAWAT-ENH-20150507-CGA#A END

		if ((sts = sc_checks(3, spec)) > 0)
			return sts;
		dspLine.init(Mnemo.getMenu(15 + spec));
		event.onto(0, editNum(spec, 3));
		event.rewrite(event.nxt + 1, 19);
		return 0;
	}

	/**
	 * ac 03/05/06/07 preselect
	 */
	public int action1(int spec) {
		int sc, sts;

		if (spec == 3) /* return */ {
			int flg = M_TRRTRN;
			if ((sts = sc_checks(2, sc = sc_value(flg))) > 0)
				return sts;
			if ((sts = Match.chk_reason(sc)) > 0)
				return sts;
			tra.spf1 |= flg;
			set_ac_ctl(spec);
			dspLine.init(Mnemo.getMenu(tra.head = 45));
			if (!tra.isActive())
				set_tra_top();
			if (rcd_tbl[sc - 1] > 0)
				prtLine.init(Mnemo.getText(12)).upto(17, editReason(sc)).book(3);
			return 0;
		}
		if (spec == 7)
			if ((sts = sc_checks(8, 4)) > 0)
				return sts;
		tra.stat = 1;
		set_ac_ctl(spec);
		dspLine.init(Mnemo.getMenu(tra.head = spec + 19));
		return 0;
	}

	/**
	 * extra till number
	 */
	public int action2(int spec) {
		int nbr = input.scanKey(input.num);

		if (nbr == 0 || nbr == ctl.reg_nbr)
			return 8;
		if (!tra.isActive())
			set_tra_top();
		tra.number = editKey(nbr, 4);
		dspLine.init(Mnemo.getText(42)).onto(13, tra.number);
		prtLine.init(dspLine.toString()).book(3);
		return 0;
	}

	/**
	 * training/reentry start/stop
	 */
	public int action3(int spec) {
		int mode = spec - 13, sts;

		if (ctl.mode > 0 && ctl.mode != mode)
			return 5;
		if ((sts = sc_checks(1, mode)) > 0)
			return sts;
		set_ac_ctl(spec);
		if (!tra.isActive())
			set_tra_top();
		prtTitle(event.dec);
		if (SscoPosManager.getInstance().isUsed()) {
			SscoPosManager.getInstance().enterExitTrainingModeResponse();
		}
		if ((ctl.mode ^= mode) != M_RENTRY)
			return prt_trailer(2);
		event.nxt = event.alt;
		return 0;
	}

	/**
	 * reentry date
	 */
	public int action4(int spec) {
		int date = input.scanDate(input.num);
		if (!dat_valid(date))
			return 8;
		tra.number = editNum(date, 6);
		dspLine.init(Mnemo.getText(37)).onto(12, editDate(date));
		prtLine.init(dspLine.toString()).book(3);

		return prt_trailer(2);
	}

	/**
	 * inventory/xfer/layaway
	 */
	public int action5(int spec) {
		int mode = spec % 10, sts;

		if ((sts = sc_checks(1, mode)) > 0)
			return sts;
		set_ac_ctl(spec);
		tra.mode = mode;
		tra.stat = 1;
		if (tra.code > 50)
			tra.spf1 = M_TRRTRN;
		tra.head = event.dec;
		if (!tra.isActive())
			set_tra_top();
		prtTitle(tra.head);
		return 0;
	}

	/**
	 * shelf/page number
	 */
	public int action6(int spec) {
		int nbr = input.scanNum(input.num);

		if (nbr == 0)
			return 8;
		tra.number = Integer.toString(nbr);
		dspLine.init(Mnemo.getText(40)).onto(12, tra.number);
		prtLine.init(dspLine.toString()).book(3);
		return 0;
	}

	/**
	 * other store number
	 */
	public int action7(int spec) {
		int nbr = input.scanNum(input.num);

		if (nbr == 0 || nbr == ctl.sto_nbr)
			return 8;
		tra.number = editNum(nbr, 4);
		dspLine.init(Mnemo.getText(11)).onto(13, tra.number);
		prtLine.init(dspLine.toString()).book(3);
		return 0;
	}

	/**
	 * layaway number
	 */
	public int action8(int spec) {
		tra.number = input.scan(input.num);
		dspLine.init(Mnemo.getText(spec)).onto(12, tra.number);
		prtLine.init(dspLine.toString()).book(3);
		return 0;
	}

	/**
	 * ac 30 tender media exchange
	 */
	public int action9(int spec) {
		int sts;

		if ((sts = sc_checks(8, 5)) > 0)
			return sts;
		set_ac_ctl(spec);
		return GdTndrs.tnd_clear(0);
	}
}
