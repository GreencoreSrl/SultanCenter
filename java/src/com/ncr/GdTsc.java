package com.ncr;

// TSC-MOD2014-AMZ#added entire module
/**
 *
 * @author Stefano.Bertarello
 */
class GdTsc extends Action {

    public static String SSD = System.getProperty("SSD", "88888");
    private static boolean enabled = false;
    private static boolean ctrlExitEnabled = false;
    private static boolean barcodePrintingEnabled = false;
    private static boolean eodWaringEnabled = false;
    private static int eodForcedCloseEnabled = 0;
    private static int eodForcedCloseDelay = 0;
    private static boolean supMoneyControlEnabled = false;
    private static boolean roundingEnabled = false;
    private static boolean authCodeEnabled = false;
    private static int custCodeEnabled = 0;
    private static int authcodeMinDigits = 0;
    private static int authcodeMaxDigits = 0;
    private static boolean custCodeDateCheckEnabled = false;
    private static boolean quantityCheckEnabled = false;
    private static int tiCon0 = 0;
    private static boolean printCustomerAddress = false;    // TSC-ENH2014-7-AMZ#ADD
    private static boolean simpleReceipt = false;
    private static boolean standardFidelity = true;

    public static void readPTSC(String record) {
        enabled = true;
        ctrlExitEnabled = record.charAt(0) == '1';
        barcodePrintingEnabled = record.charAt(1) == '1';
        eodWaringEnabled = record.charAt(2) == '1';
        eodForcedCloseEnabled = Integer.parseInt(record.substring(3, 4));
        eodForcedCloseDelay = Integer.parseInt(record.substring(4, 8));
        supMoneyControlEnabled = record.charAt(8) == '1';
        roundingEnabled = record.charAt(9) == '1';
        authCodeEnabled = record.charAt(10) == '1';
        custCodeEnabled = Integer.parseInt(record.substring(11, 12));
        custCodeDateCheckEnabled = record.charAt(12) == '1';
        if ((authcodeMinDigits = Integer.parseInt(record.substring(13, 15))) < 1) {
            authcodeMinDigits = 1;
        }
        if ((authcodeMaxDigits = Integer.parseInt(record.substring(15, 17))) > 16) {
            authcodeMaxDigits = 16;
        }
        quantityCheckEnabled = record.charAt(17) == '1';
        printCustomerAddress = record.charAt(18) == '1';    // TSC-ENH2014-7-AMZ#ADD
        simpleReceipt = record.charAt(19) == '1';
        standardFidelity = record.charAt(20) == '0';
    }

    /**
     *
     * @param record contains info on how to process blacklisted code
     */
    public static void readSTND(String record) {
        for (int index = 0; index < 40; index++) {
            tnd[index].toSlip = record.charAt(index) == '1';
        }
    }

    public static boolean isAuthCodeEnabled(char type) {
        boolean enabled = authCodeEnabled;
        if (type != 'C') {
            enabled = false;
        }
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isQuantityCheckEnabled() {
        return quantityCheckEnabled;
    }

    public static boolean isCtrlExitEnabled() {
        return ctrlExitEnabled;
    }

    public static int getCustCodeEnabled() {
        return custCodeEnabled;
    }

    public static boolean isRoundingEnabled() {
        return roundingEnabled;
    }

    public static boolean isBarcodePrintingEnabled() {
        return barcodePrintingEnabled;
    }

    public static int getEodForcedCloseDelay() {
        return eodForcedCloseDelay;
    }

    public static int getEodForcedCloseEnabled() {
        return eodForcedCloseEnabled;
    }

    public static boolean isEodWaringEnabled() {
        return eodWaringEnabled;
    }

    public static boolean isSupMoneyControlEnabled() {
        return supMoneyControlEnabled;
    }

    public static boolean isCustCodeDateCheckEnabled() {
        return custCodeDateCheckEnabled;
    }

    public static boolean isPrintCustomerAddress() {
        return printCustomerAddress;
    }

    public static boolean isSimpleReceipt() {
        return simpleReceipt;
    }

    public static boolean isStandardFidelity() {
        return enabled && standardFidelity;
    }

    public static long roundDown(long value, int base) {
        if (base == 0) {
            return value;
        }
        return (value - value % base);
    }

    public static int chkEod() {
        int sts = 0;

        if (netio.eodPoll(ctl.reg_nbr) >= 800) {
            if (tiCon0 == 0) {
                tiCon0 = ctl.time;
            }
 
            if (ctl.ckr_nbr > 0) {
                if ((ctl.time - tiCon0) / 100 < eodForcedCloseDelay) {
                    if (eodWaringEnabled) {
                        sts = 144;
                    }
                } else {
                    switch (eodForcedCloseEnabled) {
                        case 0:
                            break;
                        case 1:
                            sts = 145;
                            break;
                        case 2:
                        case 3:
                            sts = -1;
                            break;
                    }
                }
            }
        }
        return sts;
    }

    public static void issueRoundDsc() {
        if (isRoundingEnabled()) {
            int rec, sc = sc_value(M_RNDEXC);
            itm.dsc = tra.bal - GdTsc.roundDown(tra.bal, tnd[0].coin);
            if (itm.dsc != 0) {
                tra.bal -= itm.dsc;
                if ((rec = reg.find(4, sc)) > 0) {
                    itm.cnt = signOf(itm.amt);
                    accumReg(4, sc, itm.cnt, -itm.dsc);
                    itm.dpt_nbr = keyValue(String.valueOf(itm.tnd));
                    Itmdc.IDC_write('D', sc, 0, "", itm.cnt, -itm.dsc);
                    lREG.read(rec, lREG.LOCAL);
                    prtLine.init(lREG.text).onto(20, tnd[0].symbol).upto(40, editMoney(0, itm.dsc)).book(3);
                    dspLine.init(Mnemo.getText(55)).upto(20, editMoney(0, tra.amt - itm.dsc));
                    if ((options[O_DWide] & 1) > 0) {
                        prtDwide(ELJRN + 2, dspLine.toString());
                    }
                }
            }
        }
    }

    public static int authorizationCode() {
        ModDlg dlg = new ModDlg(Mnemo.getMenu(112));
        int opt = options[O_CardX] & 15;
        String nbr = opt > 0 ? leftMask(ecn.credit, opt, '*') : ecn.credit;
        dlg.line[0].setText(Mnemo.getText(31) + nbr);
        dlg.line[1].setText(Mnemo.getText(70) + editNum(ecn.yymm % 100, 2) + "/" + editNum(ecn.yymm / 100, 2));
        dlg.line[2].setText(Mnemo.getText(36) + editMoney(itm.tnd, itm.amt));
        // input.prompt = Mnemo.getText(82); // TSC-MOD2014-AMZ#DEL
        input.prompt = Mnemo.getText(107); // TSC-MOD2014-AMZ#ADD
        input.init(0x00, authcodeMaxDigits, authcodeMaxDigits, 0);
        dlg.show("AUT");
        if (input.key == 0) {
            input.key = input.CLEAR;
        }
        if (input.num < authcodeMinDigits || input.key != input.ENTER) {
            input.prompt = Mnemo.getText(36);
            return 5;
        }
        itm.number = input.pb;
        // itm.text = Mnemo.getText(82); // TSC-MOD2014-AMZ#DEL
        itm.text = Mnemo.getText(107); // TSC-MOD2014-AMZ#ADD
        return 0;
    }

    public static boolean customerCardDateChk(String card) {
        boolean ret = false;
        int ind, nbr = 32;

        if (custCodeDateCheckEnabled) {
            while (nbr > 0) {
                String msr = editTxt(card, 20);
                String rule = msr_20spec[--nbr];
                if (rule == null) {
                    continue;
                }
                for (ind = 0; ind < 20; ind++) {
                    char c = rule.charAt(ind);
                    if (msr.charAt(ind) != c) {
                        if (c == ' ' || msr.charAt(ind) == ' ') {
                            break;
                        }
                        if (Character.isDigit(c)) {
                            break;
                        }
                    }
                }
                if (ind >= 8) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    static void tnd_slip() {
        int ind = itm.tnd;
        int rec, sc = sc_value(M_RNDEXC);

        if (!DevIo.station(4)) {
            return;
        }
        if (tra.mode > M_GROSS || tra.res > 0) {
            return;
        }
        DevIo.slpInsert(0);

        // Cashier information
        lCTL.read (ctl.ckr, lCTL.LOCAL);
        prtLine.init(Mnemo.getText(ctl.ckr_nbr < 800 ? 1 : 2)).upto(17, editNum(ctl.ckr_nbr, 3)).onto(20, lCTL.text).book(4);
        prtLine.init(' ').book(4);

        // Transcation total
        dspLine.init(Mnemo.getText(24)).upto(20, editMoney(0, tra.amt));
        prtDwide(4, dspLine.toString());

        itm.dsc = tra.amt - GdTsc.roundDown(tra.amt, tnd[0].coin);
        if (itm.dsc != 0) {
            if ((rec = reg.find(4, sc)) > 0) {
                lREG.read(rec, lREG.LOCAL);
                prtLine.init(lREG.text).onto(20, tnd[0].symbol).upto(40, editMoney(0, itm.dsc)).book(4);
                dspLine.init(Mnemo.getText(55)).upto(20, editMoney(0, tra.amt - itm.dsc));
                prtDwide(4, dspLine.toString());
            }
        }
        prtLine.init(' ').book(4);

        // Tender amount
        prtLine.init(tnd[ind].text);
        if (tnd[ind].unit > 0) {
            if ((tnd[ind].xflg & 0x20) > 0) {
                prtLine.onto(20, tnd[ind].editXrate(true));
            }
            prtLine.book(4);
            prtLine.init(tnd[ind].symbol).upto(17, editMoney(ind, itm.pos)).onto(20, tnd[0].symbol).upto(40, editMoney(0, itm.amt));
        } else {
            prtLine.onto(20, tnd[ind].symbol).upto(40, editMoney(ind, itm.amt));
        }
        prtLine.push(itm.mark).book(4);
        GdRegis.set_trailer();
        prtLine.type(4);
        DevIo.slpRemove();
    }

    public int action0(int spec) {

        if (input.pb.equals(SSD)) {
            event.nxt = -1;
        } else {
            spec = 8;
        }
        return spec;
    }

    public int action1(int spec) {
        int sts = 0;
        int rec = 0;
        long amt = 0;

        if (spec > 0 && eodForcedCloseEnabled > 2) {
            // perform cashier automatic pick-up
            input.lck |= 0x10;
            //event.dec = 5;//MEA-ENH-SULTN-DBA#D
            //sts = group[8].action1(1103);//MEA-ENH-SULTN-DBA#D
            event.dec = 6;//MEA-ENH-SULTN-DBA#A
            sts = group[8].action1(1504);//MEA-ENH-SULTN-DBA#A

            // performs pickup for all tenders
            itm.tnd = 0;
            while (++itm.tnd < tnd.length) {
                if ((rec = reg.findTnd(itm.tnd, tra.stat)) < 1) {
                    continue;
                }
                amt = 0L;
                for (int ind = 1; ind < 6; ind++) {
                    if ((rec = reg.findTnd(itm.tnd, ind)) < 1) {
                        continue;
                    }
                    if ((sts = ckrRead(rec, 0)) > 0) {
                        if ((lREG.flag & 0x10) > 0) {
                            amt -= lREG.block[0].total;
                        }
                        if ((lREG.flag & 0x20) > 0) {
                            amt += lREG.block[0].total;
                        }

                    }
                }
                if (amt != 0L) {
                    input.reset(Long.toString(amt));
                    group[8].action4(0);
                }
            }

            sts = group[8].action6(0);
        }
        return group[0].action4(2);
    }
}
