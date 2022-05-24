package com.ncr;

/*******************************************************************
 *
 * tender table entry
 *
 *******************************************************************/
public class TndMedia extends FmtIo {

    /**
     * smallest coin
     **/
    int coin;

    // TSC-ENH2014-3-AMZ#BEG
    boolean customerFavour;
    // TSC-ENH2014-3-AMZ#END
    /**
     * 0=controlled, 4=uncontrolled
     **/
    int ctrl = 4;
    /**
     * decimal places in currency
     **/
    public int dec;
    /**
     * tender flags
     **/
    int flag;
    /**
     * function lockout mask
     **/
    int flom;
    /**
     * second tender flag byte
     **/
    int flg2;
    /**
     * item count available
     **/
    int icnt;
    /**
     * cashdrawer number
     **/
    int till = 1;
    /**
     * lead currency
     **/
    int club;
    /**
     * foreign currency base for exchange value
     **/
    int unit;
    /**
     * decimal places in exchange rate
     **/
    int xflg;
    /**
     * auto discount rate
     **/
    int rate;
    /**
     * auto surcharge rate
     **/
    int xtra;
    /**
     * amount of money in cashdrawer
     **/
    long alert;
    /**
     * foreign currency exchange value
     **/
    long value;
    /**
     * tender limitations
     **/
    long limit[] = new long[9];
    /**
     * tender type (input sequence)
     **/
    char type = 'A';
    /**
     * national currency symbol
     **/
    char xsym = ' ';
    /**
     * international currency symbol (3 chars)
     **/
    String symbol = "";
    /**
     * tender description (reports)
     **/
    String text = "";
    /**
     * tender description (receipt)
     **/
    String tx20 = "";
    /**
     * foreign currency rate description (8 chars)
     **/
    String xtext;
    /**
     * denomination table
     **/
    CshDenom dnom[] = new CshDenom[32];
    // TSC-MOD2014-AMZ#BEG
    /**
     * black list checking
     **/
    boolean blackListed = false;
    /**
     * black list checking
     **/
    boolean toSlip = false;
    // TSC-MOD2014-AMZ#END
    // TAMI-ENH-20140526-SBE#A BEG
    // States if tender must start eftTerminal communication
    // boolean eftTerminal = false; //TAMI-ENH-20140526-CGA#D
    String eftPlugin = ""; // TAMI-ENH-20140526-CGA#A
    // TAMI-ENH-20140526-SBE#A END

    /**
     * table base ref for lead currency access
     **/
    static TndMedia tbl[] = null;

    /**************************************************************************
     * find tender of given type
     *
     * @param type
     *            tender type
     * @return tender number (0 = not found)
     ***************************************************************************/
    static int find(char type) {
        int ind = tbl.length;
        while (--ind > 0 && tbl[ind].type != type)
            ;
        return ind;
    }

    /***************************************************************************
     * Constructor
     ***************************************************************************/
    TndMedia() {
        int ind = dnom.length;
        while (ind > 0)
            dnom[--ind] = new CshDenom();
    }

    /**************************************************************************
     * initialize tender with params
     *
     * @param sc
     *            subcode
     * @param ptr
     *            S_REG data record
     ***************************************************************************/
    void init(int sc, LocalREG ptr) {
        if (sc == 1) {
            text = ptr.text;
            icnt = ptr.flag & 3;
            dec = tbl[0].dec;
            xsym = tbl[0].xsym;
            symbol = tbl[0].symbol;
        } else if (sc < 6)
            ctrl = 0;
        if (sc == 8) {
            dec = ptr.rate % 10;
            club = ptr.rate / 10;
            xflg = ptr.tflg;
            xsym = ptr.text.charAt(11);
            xtext = ptr.text.substring(0, 8);
            symbol = ptr.text.substring(8, 11).trim();
            unit = ptr.block[0].items;
            value = unit > 0 ? ptr.block[0].total : 0;
        }
        if (sc == 15)
            rate = ptr.rate; /* auto discount */
        if (sc == 16)
            xtra = ptr.rate; /* auto surcharge */
    }

    /***************************************************************************
     * round tender to smallest coin
     *
     * @param total
     *            the monitary amount in tender currency
     * @return the rounded result (0 no, 1-4 down, 5-9 up)
     ***************************************************************************/
    long round(long total) {
        if (coin < 2)
            return total;
        if (customerFavour) {
            if (GdSarawat.getRoundReturnsCustomerFavour()) {
                if (Struc.tra.bal < 0) {
                    return ((total / coin) + ((total % coin) > 0 ? 1 : 0)) * coin;
                }
            }
            return (total / coin) * coin;
        }
        return roundBy(total, coin) * coin;
    }

    /***************************************************************************
     * round change to smallest coin
     *
     * @param total
     *            the monitary amount in tender currency
     * @return the rounded result (1-5 down, 6-9 up, 0 no)
     ***************************************************************************/
    long change(long total) {
        return change(total, false);
    }

    long change(long total, boolean negativeTransaction) {
        if (customerFavour) {
            if (GdSarawat.getRoundReturnsCustomerFavour()) {
                return (total - coin + 1) / coin * coin;
            }
            return total > 0 ? (total + coin - 1) / coin * coin : (total - coin + 1) / coin * coin;

        }
        int mod = coin - 1 >> 1;

        if (mod < 1)
            return total;
        return (total < 0 ? total - mod : total + mod) / coin * coin;
    }

    /***************************************************************************
     * foreign currency exchange to home currency
     *
     * @param total
     *            the monitary amount in tender currency
     * @return the rounded result in home currency
     ***************************************************************************/
    long fc2hc(long total) {
        if (unit < 1)
            return total;
        total *= 10;
        if (club > 0) {
            TndMedia lead = tbl[club];
            if (lead.unit > 0)
                total = total * lead.unit / lead.value;
        }
        return roundBy(total * unit / value, 10);
    }

    /***************************************************************************
     * home currency exchange to foreign currency
     *
     * @param total
     *            the monitary amount in home currency
     * @return the rounded result in tender currency
     ***************************************************************************/
    long hc2fc(long total) {
        if (unit < 1)
            return total;
        total *= 10;
        if (club > 0) {
            TndMedia lead = tbl[club];
            if (lead.unit > 0)
                total = total * lead.value / lead.unit;
        }
        return roundBy(total * value / unit, 10);
    }

    /***************************************************************************
     * edit foreign currency exchange rate
     *
     * @param full
     *            including descriptive text and currency symbol if true
     * @return new String as defined by REG sc08
     ***************************************************************************/
    String editXrate(boolean full) {
        String s = editDec(club > 0 ? value : unit, xflg & 0x0f);
        return full ? xtext + symbol + s : s;
    }
}
