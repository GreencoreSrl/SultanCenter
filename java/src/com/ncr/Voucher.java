package com.ncr;
// TSC-MOD2014-AMZ#added entire module

/*******************************************************************
 *
 * Access to Voucher file (at server only)
 *
 *******************************************************************/
class RmoteGLU extends LinIo {

    private int status;
    private int fromDate;
    private int toDate;
    private long amount;
    private String filler;

    /***************************************************************************
     *  Constructor
     *
     *  @param id      String (3 chars) used as unique identification
     ***************************************************************************/
    RmoteGLU(String id) {
        super(id, 0, 78);
    }

    /***************************************************************************
     *  read voucher record
     *
     *  @param key   file access key
     *  @return      <0=offline, 0=not found, >0=unique counter
     ***************************************************************************/
    int find(String key) {
        init(' ').push(id.charAt(0)).upto(16, key);
        int len = net.readHsh('R', toString(), this);
        if (len > 0) {
            try {
                fromDate = skip(16).scan(':').scanNum(6);
                toDate = scan(':').scanNum(6);
                amount = scan(':').scanDec(8);
                filler = scan(':').skip(8).scan(':').scan(27);
                status = scan(':').scanNum(1);
            } catch (NumberFormatException e) {
                error(e, true);
            }
        }
        return len;
    }

    int update() {
        pb = pb.substring(0, 49) + filler + ":" + editNum(status, 1);
        init(' ').push(pb);
        return net.readHsh('W', toString(), this);
    }

    public long getAmount() {
        return amount;
    }

    public int getFromDate() {
        return fromDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getToDate() {
        return toDate;
    }

    public String getFiller() {
        return filler;
    }

    public void setFiller(String filler) {
        this.filler = filler;
    }
}

class Voucher extends Action {

    public static final int TNDVOUCHER = 0;
    
    private static RmoteGLU rGLU = new RmoteGLU("GLU");
    private static int[] voucherOptions = new int[20];
    private static long amount;

    public static void readVOOP(int fld, int val) {
        voucherOptions[fld] = val;
    }

    public static int getOption(int fld) {
        return voucherOptions[fld];
    }

    public static long getAmount() {
        return amount;
    }

    public static int gluCheck(String number, int flag) {
        int sts = rGLU.find(number);
        String filler = rGLU.getFiller();

        if (sts < 1) {
            sts = sts == 0 || ctl.lan > 2 ? 7 : 16;
        } else {
            sts = 0;
            
            if (flag > 0) {
                amount = rGLU.getAmount();
                if (rGLU.getStatus() > 0) {
                    sts = 7;
                } else {
                    if (cmpDates(rGLU.getFromDate(), ctl.date) > 0 || cmpDates(rGLU.getToDate(), ctl.date) < 0) {
                        sts = 8;
                    } else {
                        filler = editNum(ctl.sto_nbr, 4) + ":" + editNum(ctl.reg_nbr, 3) + ":" +
                                editNum(ctl.tran, 4) + ":" + editNum(ctl.date, 6) + ":" +
                                editNum(ctl.time, 6);
                        rGLU.setFiller(filler);
                        rGLU.setStatus(flag);
                    }
                }
            } else {
                rGLU.setStatus(0);
            }
            if (sts == 0 && ctl.mode == 0) {
                sts = rGLU.update();
                if (sts < 1) {
                    sts = sts == 0 || ctl.lan > 2 ? 7 : 16;
                } else {
                    sts = 0;
                }
            }
        }
        return sts;
    }
}
