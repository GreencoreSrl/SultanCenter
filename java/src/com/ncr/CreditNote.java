package com.ncr;

// TSC-MOD2014-AMZ#added entire module
/*******************************************************************
 *
 * Access to Black List file (at server only)
 *
 *******************************************************************/
class RmoteNLU extends LinIo {

    private int status;
    private int genDate;
    private int expDate;
    private int useDate;
    private long amount;

    /***************************************************************************
     *  Constructor
     *
     *  @param id      String (3 chars) used as unique identification
     ***************************************************************************/
    RmoteNLU(String id) {
        super(id, 0, 78);
    }

    public int getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    public int getExpDate() {
        return expDate;
    }

    public int getGenDate() {
        return genDate;
    }

    public int getUseDate() {
        return useDate;
    }


    /***************************************************************************
     *  read black list record
     *
     *  @param key   file access key
     *  @return      <0=offline, 0=not found, >0=unique counter
     ***************************************************************************/
    int find(String key) {
        init(' ').push(id.charAt(0)).upto(16, key);
        int len = net.readHsh('R', toString(), this);
        if (len > 0) {
            try {
                status = skip(16).scan(':').scanNum(1);
                amount = scan(':').scanDec(8);
                genDate = scan(':').scanNum(6);
                expDate = scan(':').scanNum(6);
                useDate = scan(':').scanNum(6);
            } catch (NumberFormatException e) {
                error(e, true);
            }
        }
        return len;
    }
}

class CreditNote extends Action {

    public static final int COPIES = 0;
    public static final int DURATION = 1;
    public static final int RETURN = 2;
    public static final int CREDITNOTEUSE = 3;
    public static final int CREDITNOTEPRINT = 4;
    private static int[] creditNoteOptions = new int[20];
    private static String[] txtLine = new String[16];
    private static RmoteNLU rNLU = new RmoteNLU("NLU");
    private static boolean printed;
    private static boolean rejected;
    public static final int CREDIT_NOTE_CANCELLED = 23;
    public static final String CREDIT_NOTE_DISABLED = "000000000000000";
    private static long amount;
    private static int status;
    private static int generationDay,  generationMonth,  generationYear;
    private static int expirationDay,  expirationMonth,  expirationYear;
    private static int usageDay,  usageMonth,  usageYear;
    private static String number;

    public static void init(long amount) {
        int index;

        CreditNote.amount = amount;

        generationDay = ctl.date % 100;
        generationMonth = (ctl.date / 100) % 100;
        generationYear = ctl.date / 10000;

        expirationMonth = generationMonth + creditNoteOptions[DURATION] % 12;
        expirationYear = generationYear + creditNoteOptions[DURATION] / 12;
        if (expirationMonth > 12) {
            expirationMonth -= 12;
            generationYear++;
        }
        expirationDay = generationDay;
        switch (expirationMonth) {
            case 4:
            case 6:
            case 9:
            case 11:
                if (expirationDay > 30) {
                    expirationDay = 30;
                }
                break;
            case 2:
                if (expirationDay > 28) {
                    if (expirationYear % 4 != 0) {
                        expirationDay = 28;
                    } else {
                        expirationDay = 29;
                    }
                }
                break;
            default:
                break;
        }
        usageDay = usageMonth = usageYear = 0;

        status = 0;

        if ((index = reg.find(10 + getOption(CREDITNOTEPRINT), 1)) > 0) {
            lREG.read(index, lREG.LOCAL);
            number = editNum(generationDay, 2) + editNum(generationMonth, 2) + editNum(generationYear, 2) + editNum(ctl.reg_nbr, 3) + editNum(lREG.block[0].trans + ctl.tran, 6);
        } else {
            number = CREDIT_NOTE_DISABLED;
        }
    }

    public static void init(String input) {
        int sts = rNLU.find(input);
        status = 0;
        if (sts < 1) {
            status = 138;
        } else {
            if (rNLU.getStatus() != 0) {
                status = 139;
            } else {
                amount = rNLU.getAmount();
                number = input;
                
                generationDay = rNLU.getGenDate() % 100;
                generationMonth = (rNLU.getGenDate() / 100) % 100;
                generationYear = rNLU.getGenDate() / 10000;

                usageDay = rNLU.getUseDate() % 100;
                usageMonth = (rNLU.getUseDate() / 100) % 100;
                usageYear = rNLU.getUseDate() / 10000;

                expirationDay = rNLU.getExpDate() % 100;
                expirationMonth = (rNLU.getExpDate() / 100) % 100;
                expirationYear = rNLU.getExpDate() / 10000;

                if (cmpDates(rNLU.getExpDate(), ctl.date) < 0) {
                    status = 140;
                } else {
                    if (usageDay != 0) {
                        status = 139;
                    }
                }
            }
        }
    }

    public static void readCNOP(int fld, int val) {
        if (fld == DURATION) {
            if (val == 0) {
                val = 2;
            }
        }
        creditNoteOptions[fld] = val;
    }

    public static int getOption(int fld) {
        return creditNoteOptions[fld];
    }

    public static void readCNTX(int ind, String record) {
        txtLine[ind] = record;
    }

    public static String getTxtLine(int ind) {
        return txtLine[ind];
    }

    public static long getAmount() {
        return amount;
    }

    public static void setAmount(long amount) {
        CreditNote.amount = amount;
    }

    public static String getNumber() {
        return number;
    }

    public static int getStatus() {
        return status;
    }

    /**
     * @return the rejected
     */
    public static boolean isRejected() {
        return rejected;
    }

    public static void setRejected(boolean rejected) {
        CreditNote.rejected = rejected;
    }

    public static int printText() {
        int sts = 0;

        System.out.println("[" + number + "]");
        if (number.equals(CREDIT_NOTE_DISABLED)) {
            sts = CREDIT_NOTE_CANCELLED;
        } else {
            if (!DevIo.station(4)) {
                sts = CREDIT_NOTE_CANCELLED;
            } else {
                for (int cnt = 0; cnt < creditNoteOptions[COPIES]; cnt++) {
                    DevIo.slpInsert(options[O_chk42]);
                    if (DevIo.mfptr.state < 0) {
                        DevIo.mfptr.state = 0;
                        sts = CREDIT_NOTE_CANCELLED;
                    } else {
                        for (int index = 0; index < 10; index++) {
                            if (head_txt[index] != null) {
                                prtLine.init(head_txt[index]).type(4);
                            }
                        }
                        for (int index = 0; index < 16; index++) {
                            if (txtLine[index] != null) {
                                prtLine.init(buildText(txtLine[index])).type(4);
                            }
                        }
                        GdRegis.set_trailer();
                        prtLine.type(4);
                        DevIo.slpRemove();
                    }
                }
            }
        }
        return sts;
    }

    private static String buildText(String inText) {
        String outText = inText;
        int index = -1;

        if ((index = inText.indexOf("%NUM")) >= 0) {
            outText = inText.substring(0, index).concat(number).concat(inText.substring(index + 4));
        }
        if ((index = inText.indexOf("%AMT")) >= 0) {
            String amt = editDec(amount, tnd[0].dec).trim();
            outText = inText.substring(0, index).concat(amt).concat(inText.substring(index + 4));
        }
        if ((index = inText.indexOf("%EXP")) >= 0) {
            String exp = editNum(expirationDay, 2) + "/" + editNum(expirationMonth, 2) + "/" + editNum(2000 + expirationYear, 4);
            outText = inText.substring(0, index).concat(exp).concat(inText.substring(index + 4));
        }
        return outText.substring(0, 40);
    }

    /**
     * @return the printed
     */
    public static boolean isPrinted() {
        return printed;
    }

    /**
     * @param aPrinted the printed to set
     */
    public static void setPrinted(boolean aPrinted) {
        printed = aPrinted;
    }
}
