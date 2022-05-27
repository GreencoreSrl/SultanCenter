package com.ncr;

import java.util.Calendar;
import java.util.Date;

/*******************************************************************
 * terminal control data
 *******************************************************************/
public class Terminal extends FmtIo {
    /**
     * terminal number 3hex
     **/
    public int reg_nbr = Integer.parseInt(REG, 16);
    /**
     * server number 3hex
     **/
    public int srv_nbr = Integer.parseInt(SRV, 16);
    /**
     * terminal group number 2hex + 0xF00
     **/
    public int grp_nbr = Integer.parseInt(GRP, 16) + 0x0F00;
    /**
     * store number 4dec
     **/
    public int sto_nbr = Integer.parseInt(STO, 10);
    /**
     * accountability option 0=terminal, 1=cashier, (2=cashdrawer)
     **/
    int ability = Integer.getInteger("ACCOUNTABILITY", 1).intValue();
    /**
     * checker number 001-799=cashier, 800-999=supervisor
     **/
    public int ckr_nbr;
    /**
     * secret number of operating checker
     **/
    public int ckr_sec;
    /**
     * date of birth yymmdd
     **/
    public int ckr_age;
    /**
     * lan (local area network) status 0=online, 1=offline, 2=mismatch, 3=standalone
     **/
    int lan = 3;
    /**
     * record number of operating checker in CTL file
     **/
    int ckr;
    /**
     * record number of authorizing supervisor in CTL file
     **/
    int sup;
    /**
     * mode of operation 0=normal, 3=training, 4=re-entry
     **/
    int mode;
    /**
     * current transaction number 4dec
     **/
    public int tran;
    /**
     * number of generations since CMOS reset (day count)
     **/
    int zero;
    /**
     * current journal view 0=all transactions, 1=active transaction
     **/
    int view;
    /**
     * checkers working time in seconds until start of transaction
     **/
    int work;
    /**
     * current century 2dec cc
     **/
    int cent;
    /**
     * current date 6dec yymmdd
     **/
    int date;
    /**
     * current time 6dec hhmmss
     **/
    int time;
    /**
     * current time milliseconds 3dec
     **/
    int msec;
    /**
     * current day of week (1-7, 1=sunday)
     **/
    int wday;
    /**
     * current day of year (1-365/366)
     **/
    int yday;
    /**
     * current daily gross total
     **/
    long gross;
    /**
     * cash drawer limit exceeded
     **/
    boolean alert;
    /**
     * eod-blocking event
     **/
    boolean block;

    public String uniqueId = "";  //INTEGRATION-PHILOSHOPIC-CGA#A


    /**
     * set current date, time, msec, wday, yday
     **/
    void setDatim() {
        Calendar c = sdf.getCalendar();
        c.setTime(new Date());
        cent = (c.get(c.YEAR) / 100);
        date = (c.get(c.YEAR) % 100 * 100 + c.get(c.MONTH) + 1) * 100 + c.get(c.DATE);
        time = (c.get(c.HOUR_OF_DAY) * 100 + c.get(c.MINUTE)) * 100 + c.get(c.SECOND);
        msec = (c.get(c.MILLISECOND));
        wday = (c.get(c.DAY_OF_WEEK));
        yday = (c.get(c.DAY_OF_YEAR));
    }

    boolean tooYoung(int years, int birth) {
        if (birth > date)
            years -= 100;
        return date < birth + years * 10000;
    }
}
