package com.ncr;

// TSC-MOD2014-AMZ#added entire module
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

class BlAbcDlg extends AbcDlg {

    private int selected;
    private Color defForeground;
    private Font defFont;
    private Font bigFont;

    BlAbcDlg(String title) {
        super(title);
        selected = 0;
        defForeground = area.getComponent(selected).getForeground();
        area.getComponent(selected).setForeground(Color.orange);

        defFont = area.getComponent(selected).getFont();
        bigFont = defFont.deriveFont((float) (defFont.getSize() * 1.5));
        area.getComponent(selected).setFont(bigFont);
    }

    void touch(MouseEvent e, int type) {
        area.getComponent(selected).setForeground(defForeground);
        area.getComponent(selected).setFont(defFont);
        //selected = e.getComponent().getParent().getComponentZOrder(e.getComponent());
        for (int index = 0; index < e.getComponent().getParent().getComponentCount(); index++) {
            if (e.getComponent().getParent().getComponent(index).equals(e.getComponent())) {
                selected = index;
            }
        }
        area.getComponent(selected).setForeground(Color.orange);
        area.getComponent(selected).setFont(bigFont);
        super.touch(e, type);
    }

    void modalMain(int sts) {
        if (input.key == input.NORTH) {
            area.getComponent(selected).setForeground(defForeground);
            area.getComponent(selected).setFont(defFont);
            do {
                selected--;
                if (selected == -1) {
                    selected = area.getComponentCount() - 1;
                }
            } while (((GdLabel) area.getComponent(selected)).getText().charAt(0) <= ' ');
            area.getComponent(selected).setForeground(Color.orange);
            area.getComponent(selected).setFont(bigFont);
            return;
        }
        if (input.key == input.SOUTH) {
            area.getComponent(selected).setForeground(defForeground);
            area.getComponent(selected).setFont(defFont);
            do {
                selected++;
                if (selected == area.getComponentCount()) {
                    selected = 0;
                }
            } while (((GdLabel) area.getComponent(selected)).getText().charAt(0) <= ' ');
            area.getComponent(selected).setForeground(Color.orange);
            area.getComponent(selected).setFont(bigFont);
            return;
        }
        if (input.key == 0x19) {
            char c = ((GdLabel) area.getComponent(selected)).getText().charAt(0);
            KeyEvent k1 = new KeyEvent(kbrd, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, c);
            getToolkit().getSystemEventQueue().postEvent(k1);
            KeyEvent k2 = new KeyEvent(kbrd, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, c);
            getToolkit().getSystemEventQueue().postEvent(k2);
            return;
        }
        super.modalMain(sts);
    }
}

class BlClrDlg extends ClrDlg {

    GdLabel line[] = new GdLabel[5];

    BlClrDlg(String name, int type) {
        super(name, type);

        Panel additionalInfo = new Panel(new GridLayout(0, 1));
        additionalInfo.setFont(panel.font60);
        additionalInfo.add(line[0] = new GdLabel(name, GdLabel.STYLE_STATUS));
        additionalInfo.add(Border.around(line[1] = new GdLabel(null, GdLabel.STYLE_RAISED), -3));
        additionalInfo.add(line[2] = new GdLabel(name, GdLabel.STYLE_STATUS));
        additionalInfo.add(Border.around(line[3] = new GdLabel(null, GdLabel.STYLE_RAISED), -3));
        additionalInfo.add(line[4] = new GdLabel(name, GdLabel.STYLE_STATUS));
        for (int index = 1; index <= 4; index++) {
            line[index].setAlignment(Label.LEFT);
        }
        add(additionalInfo, BorderLayout.CENTER);
    }
}

/*******************************************************************
 *
 * Access to Black List file (at server only)
 *
 *******************************************************************/
class RmoteBLU extends LinIo {

    private int status;
    private String text1;
    private String text2;
    private String text3;

    public int getStatus() {
        return status;
    }

    public String getText1() {
        return text1;
    }

    public String getText2() {
        return text2;
    }

    public String getText3() {
        return text3;
    }

    /***************************************************************************
     *  Constructor
     *
     *  @param id      String (3 chars) used as unique identification
     ***************************************************************************/
    RmoteBLU(String id) {
        super(id, 0, 78);
    }

    /***************************************************************************
     *  read black list record
     *
     *  @param key   file access key
     *  @return      <0=offline, 0=not found, >0=unique counter
     ***************************************************************************/
    int find(String key) {
        init(' ').push(id.charAt(0)).upto(20, key);
        int len = net.readHsh('R', toString(), this);
        if (len > 0) {
            try {
                status = skip(20).scan(':').scanNum(1);
                text1 = scan(20);
                text2 = scan(20);
                text3 = scan(16);
            } catch (NumberFormatException e) {
                error(e, true);
            }
        }
        return len;
    }
}

/**
 *
 * @author Stefano.Bertarello
 */
class BlackList extends Action {

    public static final int OPERATOR = 0;
    public static final int SUPERVISOR = 1;
    public static final int BLOCKED = 2;
    private static int[] operationType = new int[10];
    private static String[][] statusMessage = new String[10][2];
    private static RmoteBLU rBLU = new RmoteBLU("BLU");
    private static int found = 0;
    public static final int BL_FOUND = 0;
    public static final int BL_NOTFOUND = 1;
    public static final int BL_OFFLINE = 2;
    private static int status = 0;

    /**
     *
     * @param record contains info on how to process blacklisted code
     */
    public static void readBTND(String record) {
        for (int index = 0; index < 40; index++) {
            tnd[index].blackListed = record.charAt(index) == '1';
        }
    }

    /**
     *
     * @param record contains the list of black list active tenders
     */
    public static void readBTNP(String record) {
        for (int index = 0; index < 10; index++) {
            operationType[index] = record.charAt(index) - '0';
        }
    }

    public static void readBTM(String record, int key, int pos) {
        if (key <= 9 && pos <= 1) {
            statusMessage[key][pos] = record;
        }
    }

    public static int getFound() {
        return found;
    }

    public static int getStatus() {
        return status;
    }

    public static int checkCustomerStatus() {
        int sts = 0;
        int answer = 0;

        input.init(0x10, 10, 10, 0);
        input.alpha = kbd_alpha[0];
        BlAbcDlg dlg = new BlAbcDlg("Alphanumeric Keyboard");
        input.reset(input.pb);
        dlg.block = false;
        dlg.show("ABC");
        if (dlg.code > 0) {
            return dlg.code;
        }
        if (input.key == 0) {
            input.key = input.CLEAR;
        }
        if (input.num < 1 || input.key != input.ENTER) {
            return 5;
        }

        String code = input.pb;
        sts = rBLU.find(code);
        if (sts < 1) {
            /* Customer not found or lan problems */
            if (sts != 0 && ctl.lan <= 2) {
                found = BL_OFFLINE;
                sts = 85;
            } else {
                found = BL_NOTFOUND;
            }
            status = 0;
        } else {
            found = BL_FOUND;
            status = rBLU.getStatus();
            /* Customer record found in remote BLU */
            BlClrDlg blDlg = new BlClrDlg(Mnemo.getInfo(105), 3);
            blDlg.line[0].setText(code);
            blDlg.line[1].setText(statusMessage[status][0]);
            blDlg.line[2].setText(statusMessage[status][1]);
            blDlg.line[3].setText(rBLU.getText1());
            blDlg.line[4].setText(rBLU.getText2());

            blDlg.input.init(0x80, 0, 0, 0);
            blDlg.show(code);

            sts = 0;
            answer = blDlg.code == 2 ? 1 : 0;

            switch (operationType[status]) {
                case OPERATOR:
                    if (blDlg.code == 1) {
                        sts = 142;
                    }
                    break;
                case SUPERVISOR:
                    if (blDlg.code == 2) {
                        if ((sts = GdSigns.chk_autho(Mnemo.getInfo(38))) > 0) {
                            sts = 142;
                        }
                    } else {
                        sts = 142;
                    }
                    break;
                case BLOCKED:
                    sts = 142;
                    break;
                default:
                    break;
            }
        }
        // Write IDC based on sts
        Itmdc.IDC_write('b', 0, 0, code, answer, 0L);
        return sts;
    }
}
