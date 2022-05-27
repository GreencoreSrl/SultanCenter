package com.ncr;

public class ItemAuthManager {
    private static ItemAuthManager instance;
    private int enabled;

    public static ItemAuthManager getInstance() {
        if (instance == null) {
            instance = new ItemAuthManager();
        }
        return instance;
    }

    private ItemAuthManager() {}

    public int getItemAuth(ConIo input, Itemdata plu) {
        String ruleLbl = GdPrice.ean_special('P', plu.eanupc);
        if (input.key != 0x4f4f && ruleLbl != null) {
            int auth = 0;
            switch(enabled) {
                case 1:
                    auth = GdSigns.chk_autho(Mnemo.getInfo(38));
                    if (auth > 0)
                        return auth;

                    break;
                case 2:
                    return 7;

                case 3:
                    if (plu.price == 0) {
                        auth = GdSigns.chk_autho(Mnemo.getInfo(38));
                        if (auth > 0)
                            return auth;
                    }

                    break;
                case 4:
                    if (plu.price == 0) {
                        return 7;
                    }

                    break;
                default:
                    break;
            }
        }
        return 0;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }
}
