package com.ncr;

/**
 * Created by Administrator on 02/11/15.
 */
public class GdSaf {
    private static boolean enabled = false;

    static void readPregpar(String txt, int ind) throws Exception {
        if (ind != 0) {
            throw new Exception("Bad SAFP line number in p_regpar, must be SAFP0");
        }
        enabled = (Integer.parseInt(txt.substring(0, 2)) == 1);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        GdSaf.enabled = enabled;
    }
}
