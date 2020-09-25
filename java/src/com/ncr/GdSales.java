package com.ncr;

import com.ncr.ssco.communication.entities.AdditionalProcessType;
import com.ncr.ssco.communication.entities.pos.SscoItem;
import com.ncr.ssco.communication.entities.pos.SscoItemPromotion;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.util.Vector;

class GdSales extends Action {
	private static final Logger logger = Logger.getLogger(GdSales.class);

    static int itm_displ() {
        String marks = Mnemo.getText(60);
        int qty = itm.qty > 0 ? itm.qty : 1;

        dspLine.init(' ').upto(9, editDec(qty * itm.dec, 3)).push(marks.charAt(0));
        if (itm.prm < 1) {
            if (itm.qty < 1)
                dspLine.init(' ');
            else
                dspLine.upto(9, editTxt("", 4));
        }
        if (itm.prcom > 0)
            dspLine.onto(0, marks.charAt(5));
        if ((itm.spf1 & M_VOID) > 0)
            dspLine.onto(10, marks.charAt(1));
        if ((itm.spf1 & M_RETURN) > 0)
            dspLine.onto(11, marks.charAt(2));
        if (itm.prpos > 0)
            dspLine.upto(20, editMoney(0, itm.prpos));
        else if (itm.prpov > 0)
            dspLine.upto(20, editMoney(0, itm.prpov));
        if (itm.dpt_nbr > 0) {
            if (itm.number.length() > 0 && itm.number.charAt(0) > ' ')
                dspLine.init("#" + itm.number);
            else
                dspLine.init(itm.text);
        }
        dspBmap = "DPT_" + editKey(itm.dpt_nbr, 4);
        return 0;
    }

    static void itm_charge(int items, long total) {
        int sc = sc_value(M_CHARGE);

        total = roundBy(total * tra.xtra, 1000);
        accumReg(4, sc, items, total);
        accumTax(10, itm.vat, 0, total);
        tra.chg_cnt += items;
        tra.chg_amt += total;
    }

    static void itm_discount() {
        int items = 0, sc = sc_value(tra.spf2);

        if (itm.id == 'S')
            items = itm.cnt;
        rbt[itm.sit].amt += itm.dsc;
        tra.dsc_amt += itm.dsc;
        accumReg(3, sc, items, itm.dsc);
        accumDpt(1, items, itm.dsc);
        accumSlm(1, items, itm.dsc);
        Itmdc.IDC_update(itm.idc, sc, itm.sit, itm.number, itm.dsc);
        itm_tally(0, itm.dsc);
    }

    static void itm_collect(int code) {
        Itmdc.IDC_write('S', code, itm.stat, itm.number, itm.cnt, itm.amt - itm.pov - itm.pos);
        Itmdc.IDC_write('W', 0, itm.type, itm.eanupc, itm.index + 1, 0);
        if (GdPsh.isGiftCard(itm)) // PSH-ENH-001-AMZ#ADD -- idc record g
            Itmdc.IDC_write('g', 0, itm.gCardTopup ? 1 : 0, itm.gCardSerial, 0, 0); // PSH-ENH-001-AMZ#ADD -- idc record g
        if (itm.prm >= 1)
            Itmdc.IDC_write('s', code, itm.stat, itm.number, itm.cnt, itm.originalPrice);

        //PSH-ENH-20151120-CGA#A BEG
        if (GdPsh.isUtility(itm)) {
            Itmdc.IDC_write('u', 0, 0, itm.utilityTransaction, 0, 0);
            logger.info("write record u into idc file");
        }
        //PSH-ENH-20151120-CGA#A END

        if (itm.serial.length() > 0)
            Itmdc.IDC_write('W', 1, itm.type, itm.serial, 0, 0);
        // EMEA-UPB-DMA
        if (itm.operationID > 0) {
            Itmdc.IDC_write('u', code, itm.stat, itm.number, 0, itm.operationID);
        }
        // EMEA-UPB-DMA
        if (code > 3)
            itm_reason(code, itm.amt - itm.pos);
        if (tra.mode <= M_GROSS)
            if (cus_age[itm.ages] > 0) {
                tra.age = cus.age;
                Itmdc.IDC_write('Q', trx_pres(), 2, editNum(ckr_age[itm.ages], 2) + editNum(cus_age[itm.ages], 2), 0,
                        tra.age);
            }
    }

    static void itm_reason(int code, long total) {
        int ind = rcd_tbl[code - 1];

        if (ind < 1)
            return;
        Itmdc.IDC_write('Q', trx_pres(), 1, editNum(code * 100 + ind, 4), itm.cnt, total);
    }

    static void itm_tally(int items, long total) {
        accumTax(10, itm.vat, items, total);
        if ((itm.flag & F_DPOSIT) > 0) {
            int sc = (itm.flag & F_NEGSLS) > 0 ? 30 : 20;
            accumTax(sc, itm.vat, items, total);
        }
        if (items != 0) {
            if (itm.number.length() == 0)
                accumReg(8, 8, items, total);
            else
                accumReg(8, 7 - (itm.stat & 1), items, total);
            if (tra.xtra > 0)
                itm_charge(items, total);
        }
        if ((itm.spf1 & 0xF8) == 0) {
            rbt[itm.sit].dsc_sls.add(items, total);
            if (itm.sit > 0) {
                tra.dsc_cnt += items;
                tra.dsc_sls += total;
            }
            if ((itm.flag & F_XPROMO) == 0) {
                rbt[itm.sit].pnt_sls.add(items, total);
                tra.pnt_cnt += items;
                tra.pnt_sls += total;
            }
            for (int rec = itm.dpt; rec-- > 0; rec = dpt.grp[rec]) {
                if (itm.sit > 0) {
                    dpt.dsc_cnt[rec] += items;
                    dpt.dsc_sls[rec] += total;
                }
                if ((itm.flag & F_XPROMO) == 0)
                    dpt.pnt_sls[rec] += total;
            }
        }
    }

    static void crd_print(int station) {
        int option = options[O_ItmPr];
        int sc = sc_value(itm.spf2 & 0x70);

        if (tra.spf2 > 0)
            option &= ~1;
        prtLine.init(itm.text);
        if (sc < 4)
            prtLine.onto(13, editReason(sc));
        if (itm.rate > 0)
            prtLine.upto(25, editRate(itm.rate));
        prtLine.upto(36, editMoney(0, itm.amt));
        if ((option & 2) == 0)
            prtLine.push(vat[itm.vat].text.substring(4, 6));
        if ((option & 1) == 0)
            prtLine.onto(38, Mnemo.getText(58).substring(0, 1) + itm.sit);
        prtLine.book(station);
    }

    static void crd_line() {
        int sc = sc_value(itm.spf2);

        tra.vItems.addElement('C', itm);

        //SPINNEYS-20180215-CGA#A BEG
        logger.info("itm.meal: " + itm.meal);
        if (itm.meal == 1) {
            //itm.meal = 0;
            Itmdc.IDC_write('D', sc, 0, "", itm.cnt, itm.amt);
        } else { //SPINNEYS-20180215-CGA#A END
            Itmdc.IDC_write('C', sc, 0, itm.number, itm.cnt, itm.amt);
        }

        if (itm.promo.length() > 0) {
            Itmdc.IDC_write('K', trx_pres(), itm.flag, itm.promo, itm.rew_qty, itm.rew_amt);
            Promo.writePromoDetails(itm.promo);
        }


        if (sc < 4)
            itm_reason(sc, itm.amt);
        accumReg(3, sc, itm.cnt, itm.amt);
        accumDpt(2, itm.cnt, itm.amt);
        accumSlm(2, itm.cnt, itm.amt);
        itm.dsc = getDiscount();
        Itmdc.IDC_write('C', 9, itm.sit, itm.number, 0, itm.dsc);
        itm.idc = lTRA.getSize();
        if (itm.dsc != 0)
            itm_discount();
        //DMA_VAT-DISTRIBUTION#A BEG
        if (itm.meal == 1) {
            itm.spf3 = sc_value(M_TOTRBT);
            itm.crd = itm.amt;
            itm.meal = 0;
            GdTrans.rbt_distrib_k(itm.crd);
        } else
            //DMA_VAT-DISTRIBUTION#A END
            itm_tally(0, itm.amt);
        tra.amt += itm.amt;
        TView.append('C', 0x01, itm.text, "", itm.rate > 0 ? editRate(itm.rate) : "", editMoney(0, itm.amt), "");
        crd_print(3);
        GdTrans.tra_balance();
    }

    static int crd_register(long total) {
        int sc = sc_value(itm.spf2);

        itm.rcd = rcd_tbl[sc - 1];
        lREG.read(reg.find(3, sc), lREG.LOCAL);
        itm.text = lREG.text;
        itm.cnt = itm.qty;
        itm.amt = -total;
        itm.sign();
        crd_line();
        dspLine.init(itm.text).upto(20, editMoney(0, itm.amt));
        cusLine.init(itm.text).show(10);
        cusLine.init(' ').upto(20, editMoney(0, itm.amt)).show(11);
        Promo.updateItems(itm.amt);
        return GdTrans.itm_clear();
    }

    static LinIo itm_edit() {
        int option = options[O_ItmPr];

        if (tra.spf2 > 0)
            option &= ~1;
        prtLine.init(itm.text);
        if ((option & 4) == 0)
            prtLine.onto(20, editKey(itm.dpt_nbr, 4));
        prtLine.upto(36, editMoney(0, itm.amt));
        if ((option & 2) == 0)
            prtLine.push(vat[itm.vat].text.substring(4, 6));
        if ((option & 1) == 0)
            prtLine.onto(38, Mnemo.getText(58).substring(0, 1) + itm.sit);
        else
            prtLine.index = 40;
        return prtLine;
    }

    static void itm_print(int station) {
        int option = options[O_ItmPr];
        String marks = Mnemo.getText(60);

        if (tra.code > 6)
            option &= ~0x30; /* with number and text */
        for (int sc = 3; sc < 9; sc++) {
            if ((itm.spf1 & ~tra.spf1 & 256 >> sc) == 0)
                continue;
            lREG.read(reg.find(2, sc), lREG.LOCAL);
            prtLine.init(lREG.text);
            if (sc == sc_value(itm.spf1))
                prtLine.onto(13, editReason(sc));
            prtLine.book(station);
        }
        if ((itm.spf1 & M_ERRCOR) == 0) {
            if (itm.prcom > 0)
                prtLine.init(' ').push(marks.charAt(5)).push(editNum(itm.prcom, 8)).book(station & 3);
            if (itm.prpos != itm.prpov) {
                lREG.read(reg.find(3, 1), lREG.LOCAL);
                prtLine.init(lREG.text).onto(13, editReason(1)).onto(20, editPrice(itm.prpov)).skip()
                        .push(marks.substring(8, 10)).book(station & 3);
            }
        }
        if ((itm.flag & F_WEIGHT) > 0)
            GdScale.book(station);
        else if (itm.qty != 1 || itm.prm > 0 || itm.unit != 10 || (option & 0x80) > 0) {
            prtLine.init(' ');
            if (itm.prm == 0) {
                if (itm.unit != 10) {
                    prtLine.push(editTxt(itm.cnt, 4) + " x").upto(12, editDec(itm.unit, 1));
                } else
                    prtLine.push(editTxt(itm.cnt, 12));
            } else
                prtLine.upto(12, editDec(itm.dec, 3));
            prtLine.upto(15, itm.ptyp).skip().push(itm.ext > 0 ? '/' : marks.charAt(0)).skip(3);
            if (itm.ext == 0)
                prtLine.push(editPrice(itm.prpos)).skip();
            prtLine.push(marks.substring(10, 12));
            prtLine.book(station);
        }
        if (itm.number.length() > 0) {
            prtLine.init(editIdent(itm.number, (itm.flag & F_SPCSLS) > 0));
            int msk = (option & 0x10) > 0 ? 1 : 7;
            if ((option & 0x20) > 0)
                msk &= 6;
            prtLine.book(station & msk);
        }
        if (itm.serial.length() > 0)
            prtLine.init(Mnemo.getText(76)).onto(20, itm.serial).book(station);
        if (itm.eXline.length() > 0)
            prtLine.init(itm.eXline).book(station);
        itm_edit();
        if (itm.mark != ' ')
            prtLine.push(itm.mark);
        else if (itm.prpov != itm.price)
            prtLine.push(marks.charAt(3));
        if ((option & 0x20) > 0 && itm.number.length() > 0) {
            prtLine.book(station & 6);
            prtLine.onto(0, editIdent(itm.number, (itm.flag & F_SPCSLS) > 0));
            prtLine.book(station & 1);
        } else
            prtLine.book(station);
        if (itm.qual.length() > 0)
            prtLine.init(itm.qual).book(station);
        // EMEA-UPB-DMA
        if (itm.operationID > 0) {
            prtLine.init(Mnemo.getText(82)).onto(20, String.format("%016d", itm.operationID)).book(station);
        }
        // EMEA-UPB-DMA
        // PSH-ENH-001-AMZ#BEG -- aux line in printer
        if (itm.redemptionDsc.length() > 0)
            prtLine.init(itm.redemptionDsc).book(station);
        if (itm.gCardDsc.length() > 0) {
            prtLine.init(itm.gCardDsc).book(station);
            prtLine.init(Mnemo.getText(91)).onto(20, itm.gCardBal).book(station);
        }
        if (itm.utilityEnglishText.length() > 0) {
            GdPsh.printText(itm.utilityEnglishText);
        }
        // PSH-ENH-001-AMZ#END
        // PSH-ENH-20151120-CGA#A BEG
        /*if (!itm.utilityTransaction.equals("")) {
            prtLine.init(Mnemo.getText(76) + itm.utilityTransaction).book(station);
        } */
        // PSH-ENH-20151120-CGA#A END
    }

    static void itm_line() {
        int qty = itm.ext > 0 ? 1 : itm.qty, sc = sc_value(itm.spf1);
        long price;

        itm.cnt = itm.qty;
        itm.amt = qty * set_price(itm, itm.prpos);
        itm.pos = itm.amt - qty * set_price(itm, itm.prpov);
        itm.pov = itm.amt - itm.pos - qty * set_price(itm, itm.price);
        itm.com = itm.prcom;
        if (sc > 3)
            itm.rcd = rcd_tbl[sc - 1];
        itm.sign();
        itm.rpt += itm.cnt;
        itm.crd = itm.pnt = 0;
        if ((itm.spf1 & M_ERRCOR) > 0)
            TView.indexEC = itm.index;
        tra.vItems.addElement('S', itm);
        if (itm.spf1 > 0)
            accumReg(2, sc, itm.cnt, itm.amt - itm.pos);
        else
            accumReg(1, 1, itm.cnt, itm.amt - itm.pos);
        accumDpt(0, itm.cnt, itm.amt - itm.pos);
        accumSlm(0, itm.cnt, itm.amt - itm.pos);
        if (itm.prcom != 0) {
            accumReg(8, 24, itm.cnt, itm.com);
            Itmdc.IDC_write('P', trx_pres(), 0, editNum(tra.slm_prs, 8), itm.cnt, itm.com);
        }
        itm_collect(sc);
        //TAU-20160816-SBE#A BEG
        if ((itm.flag & F_WEIGHT) > 0 && itm.prlbl > 0) {
            TView.append('S', 0x00, itm.text, editInt(itm.cnt), editMoney(0, itm.prlbl), editMoney(0, itm.amt), "");
        } else {
            TView.append('S', 0x00, itm.text, editInt(itm.cnt), editMoney(0, itm.prpos), editMoney(0, itm.amt), "");
        }
        //TAU-20160816-SBE#A END
        if (itm.prpov != itm.price) {
            accumReg(8, 21 + itm.spf3, itm.cnt, itm.pov);
            Itmdc.IDC_write('C', 0, itm.spf3 + 1, itm.number, itm.cnt, itm.pov);
        }
        if (itm.prpos != itm.prpov) {
            accumReg(3, 1, itm.cnt, itm.pos);
            accumDpt(2, itm.cnt, itm.pos);
            accumSlm(2, itm.cnt, itm.pos);
            Itmdc.IDC_write('C', 1, 0, itm.number, itm.cnt, itm.pos);
            itm_reason(1, itm.pos);
        }
        itm.dsc = getDiscount();
        Itmdc.IDC_write('C', 9, itm.sit, itm.number, itm.cnt, itm.dsc);
        itm.idc = lTRA.getSize();
        if (itm.dsc != 0)
            itm_discount();
        itm_tally(itm.cnt, itm.amt);
        if (itm.flatax > 0) {
            Sales tax = new Sales();
            tax.items = itm.cnt * itm.unit / 10;
            tax.total = tax.items * itm.flatax;
            accumTax(10, itm.flat, tax.items, tax.total);
            accumTax(10, itm.vat, 0, -tax.total);
        }
        if ((itm.spf1 & 0xF0) == 0 && (itm.flag & F_DPOSIT) == 0)
            tra.cnt += itm.cnt;
        tra.amt += itm.amt;
        itm_print(3);
        if (itm.prpnt != 0) {
            Itemdata sav = itm;
            itm = sav.copy();
            itm.text = Mnemo.getText(63);
            itm.pnt = itm.cnt * itm.prpnt;
            if (itm.spf1 > 31)
                itm.pnt = 0 - itm.pnt;
            pnt_line(sc_points(3));
            itm = sav;
        }
        if (tra.code == 0 && ctl.mode != M_RENTRY) {
            Match.rbt_match(lCIN);
            Match.rbt_match(lCGR);
            Match.rbt_match(lRLU);
            Promo.sellItem();
            if (itm.mmt > 0)
                Match.mix_match();
            if (itm.prm == 0)
                UpSet.mix_match();
        }
        price = roundBy(Math.abs(itm.amt + itm.crd) * 10 / qty, 10);
		//AMAZON-COMM-CGA#A BEG
		if (!ECommerce.isAutomaticAmazonItem()) {
			dspLine.init(' ').upto(5, editInt(itm.rpt)).onto(9, 'X').upto(20, editMoney(0, price));
		}
		//AMAZON-COMM-CGA#A END
        if (itm.prm == 0 && itm.unit == 10)
            dspLine.upto(8, itm.ptyp);
        dspBmap = "DPT_" + editKey(itm.dpt_nbr, 4);
		//AMAZON-COMM-CGA#A BEG
		if (!ECommerce.isAutomaticAmazonItem()) {
			oplLine.init(itm.text).show(10);
		}
		//AMAZON-COMM-CGA#A END
        if ((itm.flag & F_WEIGHT) > 0) {
            dspLine.init(GdScale.editWeight(itm.dec)).upto(20, editMoney(0, itm.amt)).show(11);
            //TAU-20160816-SBE#A BEG
            if (itm.prlbl > 0) {
                oplLine.upto(20, ' ' + GdScale.unitPrice(itm.prlbl)).show(10);
            } else {
                oplLine.upto(20, ' ' + GdScale.unitPrice(itm.prpos)).show(10);
            }
            //TAU-20160816-SBE#A END
        } else
            cusLine.init(' ').upto(20, editMoney(0, itm.amt + itm.crd)).show(11);

        GdTrans.tra_balance();
        if (!DevIo.station(4))
            return;
        if ((itm.flag & F_ONSLIP) == 0)
            return;
        if (tra.code > 0 || tra.res > 0)
            return;
        DevIo.slpInsert(0);
        itm_print(4);
        GdRegis.set_trailer();
        prtLine.type(4);
        DevIo.slpRemove();
    }

    static void itm_ssco(Itemdata sscoItem, Itemdata sscoLinkedItem) {
        SscoItem item = new SscoItem();
        SscoPosManager sscoPosManager = SscoPosManager.getInstance();

        if (sscoPosManager.isUsed()) {
            sscoPosManager.updateTotalAmount((int) tra.amt, (int) tra.bal, tra.cnt, 0);

            Vector<Itemdata> itemPromotions = sscoPosManager.getCurrentItemPromotions();
           sscoPosManager.resetCurrentItemPromotions();

            for (Itemdata sconto : itemPromotions) {
                if (sconto.amt != 0) {
                    SscoItemPromotion promo = new SscoItemPromotion(sconto, sscoPosManager.itemNumberSetting(), -(int) sconto.amt, item.getItemNumber(), sconto.text, 1, 1);
                    sscoPosManager.addPromotion(promo);
                }
            }

            if (sscoItem.spf1 != M_VOID) {
                item = new SscoItem(sscoItem.eanupc.trim(), 0, "" + Integer.parseInt(editKey(sscoItem.dpt_nbr, 4)), -1, sscoItem.text, (int)sscoItem.amt, sscoItem.qty);

                if (sscoItem.eXline != null) {
                    item.setAdditionalDescription(sscoItem.eXline);
                }
                if (sscoItem.IsWeightItem()) {
                    item.setWeight(sscoItem.dec);
                    item.setWeightPrice(sscoItem.prlbl);
                }

                item.setEntryId(itm.index + 1);
                item.setItemNumber(sscoPosManager.itemNumberSetting());
                item.setPriceChanged(itm.pos != 0);

                sscoPosManager.addItem(item);
                sscoPosManager.setItemResponse(item);
                sscoPosManager.itemResponse();

                if (sscoLinkedItem != null) {
                    item = new SscoItem(sscoLinkedItem.eanupc.trim(), 0, "" + sscoLinkedItem.dpt_nbr, -1, sscoLinkedItem.text, (int) (long) sscoLinkedItem.amt, sscoLinkedItem.qty); // AMZ-FLANE#ADD -- fix quantity su vendita articolo
                    if (sscoLinkedItem.IsWeightItem()) {
                        item.setWeight(sscoLinkedItem.dec);
                        item.setWeightPrice(sscoLinkedItem.prlbl);
                    }
                    item.setEntryId(itm.index + 1);
                    item.setItemNumber(sscoPosManager.itemNumberSetting());

                    sscoPosManager.addItem(item);
                    sscoPosManager.setItemResponse(item);
                    sscoPosManager.itemResponse();
                }
            } else {
                sscoPosManager.voidItemResponse();
            }
        }
    }

    static int itm_register() {
        pit = itm;
        Itemdata sscoLinkedITem = null;

        if (itm.link > 0) {
            ref.spf1 = itm.spf1;
            ref.stat |= itm.stat;
            ref.qty = itm.qty;
            ref.mark = itm.mark;
            ref.prpos = ref.prpov = ref.price;
            itm = ref;
            itm_line();
            itm = pit;
        }
        itm_line();
        itm_ssco(itm, ref);

        if (tra.res > 0)
            return GdTrans.itm_clear();
        if (DevIo.station(4))
            if (tra.mode == M_GROSS && tra.spf1 == 0)
                for (int sc = 0; sc++ < 8; ) {
                    if ((itm.spf1 & 256 >> sc) > 0)
                        continue;
                    int rec = reg.find(2, sc);
                    if (rec == 0)
                        continue;
                    lREG.read(rec, lREG.LOCAL);
                    if ((lREG.tflg & 0x20) == 0)
                        continue;
                    cusLine.init(center(lREG.text, 20, '*')).show(2);
                    DevIo.slpInsert(0);
                    if (itm.link > 0) {
                        itm = ref;
                        itm_print(4);
                        itm = pit;
                    }
                    itm_print(4);
                    GdRegis.set_trailer();
                    prtLine.type(4);
                    DevIo.slpRemove();
                }
        return GdTrans.itm_clear();
    }

    static void pnt_line(int sc) {
        itm.pnt = pts_valid(itm.pnt);
        if (itm.pnt == 0)
            return;
        tra.vItems.addElement('G', itm);
        accumPts(sc, itm.pnt, itm.amt);
        Itmdc.IDC_write('G', sc, 0, itm.number, itm.pnt, itm.amt);
        if (itm.promo.length() > 0)
            Itmdc.IDC_write('K', trx_pres(), itm.flag, itm.promo, itm.rew_qty, itm.rew_amt);
        if (sc < 5 || itm.promo.length() == 0)
            showPoints(tra.pnt += itm.pnt);
        // PSH-ENH-001-AMZ#BEG
        if (itm.prpnt != 0) {
            tra.prpnt -= itm.prpnt;
        }
        // PSH-ENH-001-AMZ#END
        prtLine.init(itm.text).onto(20, editPoints(itm.pnt, false)).book(3);
    }

    /**
     * item clear
     **/
    int action0(int spec) {
        if (tra.isActive()) {
            dspLine.init(' ');
            dspBmap = "DPT_0000";
            return GdTrans.itm_clear();
        }
        event.nxt = event.alt;
        return GdTrans.tra_clear();
    }

    /**
     * item preselect
     **/
    int action1(int spec) {
        int sts;

        // PSH-ENH-001-AMZ#BEG -- topup item preselect
        if (spec == 10) {
            if (!GdPsh.isEnabled()) {
                return 7; // unavailable
            }
            // AMZ-2017-003-004#BEG
            if (!GdPsh.isEnabledSMASH()) {
                return 7; // unavailable
            }
            // AMZ-2017-003-004#END
            itm.gCardTopup = true;
            dspLine.init(Mnemo.getDiag(GdPsh.MNEMO_DIAGS_BASE + 1));
            dspBmap = "DPT_TOPU";
            return 0;
        }
        // PSH-ENH-001-AMZ#END
        if (spec != 6)
            if ((sts = pre_valid(0x11)) > 0)
                return sts;
        if (spec == 1) {
            if ((itm.spf1 & M_VOID) > 0)
                return 5;
            if ((sts = sc_checks(2, 7)) > 0)
                return sts;
            if ((itm.spf1 | tra.spf1) == 0)
                if ((sts = Match.chk_reason(7)) > 0)
                    return sts;
            itm.spf1 |= M_VOID;
            return itm_displ();
        }
        if (spec == 2) {
            if ((itm.spf1 & M_RETURN + M_VOID) > 0)
                return 5;
            if ((tra.spf1 & M_TRRTRN) > 0)
                return 5;
            if ((sts = sc_checks(2, 4)) > 0)
                return sts;
            if (itm.spf1 == 0)
                if ((sts = Match.chk_reason(4)) > 0)
                    return sts;
            itm.spf1 |= M_RETURN;
            return itm_displ();
        }
        if (spec == 3) {
            if (itm.prpov > 0 || itm.prpos > 0)
                return 5;
            if ((sts = sc_checks(8, 21)) > 0)
                return sts;
        }
        if (spec == 4) {
            if (itm.prpos > 0)
                return 5;
            if ((sts = sc_checks(3, 1)) > 0)
                return sts;
            if ((sts = Match.chk_reason(1)) > 0)
                return sts;
            itm.spf2 = M_PRCRED;
        }
        if (spec == 5) {
            if (itm.prcom > 0)
                return 5;
            if ((sts = sc_checks(8, 24)) > 0)
                return sts;
        }
        if (spec == 6) {
            if ((itm.spf1 | itm.prcom) > 0)
                return 5;
        }
        if (spec > 5) {
            if ((itm.prpov | itm.prpos) > 0)
                return 5;
        }
        dspLine.init(Mnemo.getMenu(event.dec));
        dspBmap = "DPT_" + editKey(itm.dpt_nbr, 4);
        return 0;
    }

    /**
     * new price
     **/
    int action2(int spec) {
        int price = input.scanNum(input.num);

        if (price == 0)
            return 8;
        if (itm.spf2 > 0)
            itm.prpos = price;
        else
            itm.prpov = price;
        return itm_displ();
    }

    /**
     * commission
     **/
    int action3(int spec) {
        int value = input.scanNum(input.num);

        if (value == 0)
            return 8;
        itm.prcom = value;
        return itm_displ();
    }

    /**
     * credit rate
     **/
    int action4(int spec) {
        int sc = sc_value(M_ITMDSC), sts;
        int rate = 0, limit = rbt[pit.sit].rate_item;

        if (limit == 0 || pit.spf2 > 0 || pit.prpnt != 0)
            return 7;
        if (input.num > 0) {
            input.dec++;
            if ((sts = input.adjust(1)) > 0)
                return sts;
            if ((rate = input.scanNum(input.num)) == 0)
                return 8;
            if (rate > limit)
                return 8;
        }
        if ((sts = sc_checks(3, sc)) > 0)
            return sts;
        if (rate == 0)
            rate = lREG.rate;
        else if (rate > lREG.rate)
            if ((lREG.tflg & 0x40) == 0)
                return 46;
        if (rate > limit)
            rate = limit;

        long amt = Math.abs(pit.amt + pit.crd);
        long credit = roundBy(amt * rate, 1000);
        if (credit == 0 || credit >= amt)
            return 8;
        if ((sts = Match.chk_reason(sc)) > 0)
            return sts;
        pit.spf2 = M_ITMDSC;
        itm = pit.copy();
        itm.rate = rate;
        return crd_register(credit);
    }

    /**
     * credit amount
     **/
    int action5(int spec) {
        int sc = sc_value(M_ITMCRD), sts;
        int limit = rbt[pit.sit].rate_ival;
        long amt = Math.abs(pit.amt + pit.crd);
        long credit = input.scanNum(input.num);

        if (limit == 0 || pit.spf2 > 0 || pit.prpnt != 0)
            return 7;
        if (credit == 0 || credit >= amt)
            return 8;
        if (credit > roundBy(amt * limit, 1000))
            return 46;
        if ((sts = sc_checks(3, sc)) > 0)
            return sts;
        if ((sts = Match.chk_reason(sc)) > 0)
            return sts;
        pit.spf2 = M_ITMCRD;
        itm = pit.copy();
        itm.rate = 0;
        return crd_register(credit);
    }

    /**
     * repetition
     **/
    int action6(int spec) {
		if (GdPsh.isUtility(pit) || GdPsh.isGiftCard(pit)) {
			logger.info("inserted item code - the item is utility");
			logger.info("code: " + pit.number);

			return 7;
		}

        if (TView.syncIndex(pit.index) != pit.index)
            return 7;
        if ((pit.spf1 & M_VOID) > 0)
            return 7;
        if ((pit.spf2 & 0x60) > 0)
            return 7;
        if (pit.serial.length() > 0)
            return 5;
        if ((pit.flag & F_WEIGHT) > 0)
            return 5;
        if (pit.spec == 'P' || pit.spec == 'X')
            return 7;
        if (pit.qual.length() > 0)
            return 7;
        itm = pit.copy();
        itm.qty = 1;
        // EMEA-UPB-DMA#A BEG
        if (itm.operationType > 0) {
            int res = WinUpb.getInstance().upb_autho(itm);
            if (res != 0)
                switch (res) {
                    case UPB_TIMEOUT_ERROR:
                        return 78;
                    default:
                        return 75;
                }
        }
        // EMEA-UPB-DMA#A END
        if (itm.link > 0)
            ref = ref.copy();
        itm.mark = Mnemo.getText(60).charAt(7);
        return itm_register();
    }

    /**
     * error correct
     **/
    int action7(int spec) {
        int ind = spec > 0 ? -1 : pit.index, sts;

        if ((ind = TView.syncIndex(ind)) < 0)
            return 7;
        Itemdata ptr = tra.vItems.getElement(ind);
        if ((sts = sc_checks(2, sc_value(M_ERRCOR))) > 0)
            return sts;
        if ((options[O_VdCtl] & 1) > 0) {
            if (ptr.cnt == tra.cnt) {
                stsLine.init(lREG.text).show(2);
                if ((sts = GdSigns.chk_autho(Mnemo.getInfo(38))) > 0)
                    return sts;
            }
        }
        if (ptr.spf1 == 0)
            if ((sts = Match.chk_reason(8)) > 0)
                return sts;
        // PSH-ENH-001-AMZ#BEG -- cancel gift card error correct
        if (GdPsh.isGiftCard(ptr)) {
            // for topup item too
            if ((sts = GdPsh.cancelGiftCard(ptr)) > 0) {
                return sts;
            }
        }
        // PSH-ENH-001-AMZ#END
        if ((sts = GdSpinneys.getInstance().handleCoupon(ptr)) > 0) {

            logger.debug("Response code error: " + sts);
            //SPINNEYS-13032018-CGA#A BEG
            if (sts == 120) {
                panel.clearLink(GdSpinneys.getInstance().getMsgError(), 1);
                return 0;
            }
            //SPINNEYS-13032018-CGA#A END
            return sts;
        }
        //PSH-ENH-20151120-CGA#A BEG
        if (GdPsh.isUtility(ptr)) {
            logger.info("error correct - delete item from list");
            // for topup item too
            if ((sts = GdPsh.cancelBuyUtility(ptr)) > 0) {
                logger.info("response from server: " + sts);
                return sts;
            }
        }
        //PSH-ENH-20151120-CGA#A END

        itm = ptr.copy();
        itm.qual = "";
        itm.rpt = 0;
        itm.spf1 |= M_ERRCOR;
        // EMEA-UPB-DMA#A BEG
        if (itm.operationType > 0) {
            int res = WinUpb.getInstance().upb_autho(itm);
            // if (res > 0) return 7;
        }
        // EMEA-UPB-DMA#A END
        if (itm.link > 0) {
            ref = tra.vItems.getElement('S', ind, -1).copy();
            ref.rpt = 0;
        }
        itm.mark = Mnemo.getText(60).charAt(6);
        itm_register();
        if ((ptr.spf2 & 0x60) > 0) {
            for (itm.index = ind; (itm.spf2 & 0x60) == 0; ) {
                itm = tra.vItems.getElement('C', itm.index, 1).copy();
            }
            itm.spf1 |= M_ERRCOR;
            crd_register(Math.abs(itm.amt));
        }
        return 0;
    }

    /**
     * slip validation
     **/
    int action8(int spec) {
        if (!DevIo.station(4))
            return 7;
        DevIo.slpInsert(0);
        if (pit.link > 0) {
            itm = ref;
            itm_print(4);
        }
        itm = pit;
        itm_print(4);
        if ((itm.spf2 & 0x70) > 0) {
            itm = tra.vItems.getElement('C', itm.index, 1);
            crd_print(4);
        }
        GdRegis.set_trailer();
        prtLine.type(4);
        DevIo.slpRemove();
        return GdTrans.itm_clear();
    }

    /**
     * quantity
     **/
    int action9(int spec) {
        int qty = itm.qty > 0 ? itm.qty : 1, sts;
        long dec = input.scanNum(input.num);

        // AMZ-2017-004-002#BEG -- supervisor check
        if (GdSarawat.getQuantityBySupervisor() > 0 && !GdSarawat.getForceAcceptQuantityKey()) {
            if ((input.lck & 0x04) == 0) {
                // Supervisor needed
                return 1; // Keylock position
            }
            GdSarawat.activateQuantityKeyForcedAccept();
        }

        // AMZ-2017-004-002#BEG
        if ((sts = pre_valid(0x11)) > 0)
            return sts;
        if (input.dec > 0) {
            if (itm.prm >= event.dec)
                return 5;
            for (sts = 10; --input.dec > 0; sts *= 10)
                ;
            if ((dec = roundBy(dec * itm.dec, sts)) < 1)
                return 8;
            if (dec * qty > 9999999)
                return 2;
            itm.dec = (int) dec;
            itm.prm++;
        } else {
            if (itm.qty > 0)
                return 5;
            if (input.num > event.max - event.dec)
                return 2;
            if (dec < 1)
                return 8;
            if (dec * itm.dec > 9999999)
                return 2;
            itm.qty = (int) dec;
        }
        if (SscoPosManager.getInstance().isUsed()) {
            SscoPosManager.getInstance().getProcessor().setAdditionalProcessType(AdditionalProcessType.QTY);
            SscoPosManager.getInstance().getProcessor().additionalProcess();
        }
        return itm_displ();
    }
}
