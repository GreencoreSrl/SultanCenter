package com.ncr;

import com.ncr.ssco.communication.entities.pos.SscoError;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.util.Properties;

class Motor extends DatIo implements Runnable {
	private static final Logger logger = Logger.getLogger(Motor.class);
    int base, dpos, idle, last, menu;
	int nbr, key, nxt, alt, lck, max, min, dec, spc, act;
	String comment;
	static ConIo input = Action.input;

	Motor(String name) {
		super("TBL", 0, 71);
		open(null, name + ".TMP", 1);
	}

	int read(int line) {
		if (super.read(line + 1) > 0)
			try {
				nbr = skip(3).scanNum(3);
				key = skip(4).scanHex(4);
				nxt = scan(',').scanNum(3);
				alt = scan(',').scanNum(3);
				lck = skip(3).scanHex(2);
				max = scan(',').scanNum(3);
				min = scan(',').scanNum(3);
				dec = scan(',').scanNum(3);
				spc = scan(',').scanNum(4);
				act = scan(',').scanNum(3);
				comment = skip(2).scan(15);
			} catch (NumberFormatException e) {
				error(e, true);
			}
		else
			nbr = -1;
		if (nbr != line)
			error(new NumberFormatException("event line " + line), true);
		if (dec == 99)
			dec = dpos;
		return line;
	}

	int next(int line) {
		for (read(++line); key == 0 && nxt != 0; line = read(nxt))
			;
		return line;
	}

	int find(int code, int base) {
		for (int line = next(base); key > 0; line = next(line)) {
			if (key == code)
				return line;
		}
		return 0;
	}

	void mainDyna(DynakeyGroup dyna, int state) {
		int ind = dyna.keys.length;

		state = Action.get_state(state);
		if (input.sel == 0 || state != 3) {
			input.sel = -1;
			dyna.showTouch(false);
		}
		if (!input.hasDyna())
			return;
		if (input.sel > 0)
			if (state == 3) {
				if (dyna.substate == input.sel)
					return;
				dyna.substate = input.sel;
				dyna.setState(state);
				while (ind > 0)
					Action.showDynaTch(--ind);
				dyna.select(0);
				return;
			}
		if (dyna.substate > 0) {
			dyna.substate = 0;
			input.dky = -1;
		}
		if (input.dky != state) {
			dyna.setState(input.dky = state);
			while (ind > 0)
				Action.showDynakey(--ind);
		} else if (base == last)
			return;
		int code[] = input.dynas[state], line;
		boolean visible[] = new boolean[dyna.keys.length];
		for (line = next(base); key > 0; line = next(line)) {
			for (ind = 0; ind < visible.length; ind++) {
				visible[ind] |= menu == 0 ? key == code[ind] : min == ind + 1;
			}
		}
		last = read(base);
		for (ind = 0; ind < visible.length; ind++) {
			boolean b = visible[ind] || code[ind] == 0;
			if (dyna.keys[ind].isEnabled() ^ b)
				dyna.keys[ind].setEnabled(b);
		}
	}

	void main(int sts) {
		int line = base, dyna = input.dky;
		GdPos panel = Action.panel;
		logger.debug("Code: " + sts);

		if (sts == 0) {
			if (dyna == Action.get_state(lck & 0x0F))
				dyna = 0;
			logger.debug("input.key: " + input.key);
			if (input.key == input.CLEAR) {
				panel.pnlView.toFront(0); /* first card = journal */
				if (dyna > 0)
					nxt = base;
				else if (menu > 0)
					nxt = menu;
				else
					sts--;
			} else
				sts--;
			while (sts < 0) {
				line = next(line);

				if (key == 0) {
					sts = 5;
					if (input.isEmpty()) {
						if (input.key > 0x13 && input.key < 0x16) {
							if (menu < 1)
								dyna = input.key - 0x13;
							input.key = input.CLEAR;
						} else {
							sts = Action.spec(sts);
							logger.debug("Exit from spec: " + sts);
						}
					}
					break;
				}
				if (menu > 0) {
					if (input.isEmpty()) {
						if (input.key == min + 0x80) {
							input.reset(Integer.toString(key));
							input.key = input.ENTER;
						}
					}
					if (input.num < 1 || input.key != input.ENTER)
						continue;
					if (key != Integer.parseInt(input.pb))
						continue;
					if ((sts = input.adjust(0)) > 0)
						break;
					sts++;
					if ((lck & input.lck) == 0)
						break;
					input.num = 0;
				} else {
					if (key != input.key)
						continue;
					if (!input.isEmpty()) {
						if (max == 0)
							continue;
					} else if (min > 0)
						continue;
					if ((lck & 0x80) > 0)
						if (spc != Integer.parseInt(input.pb))
							continue;
					if ((sts = input.adjust(dec)) > 0)
						break;
					sts++;
					if ((lck & input.lck) == 0)
						break;
					sts++;
					if (max < input.num)
						break;
					sts++;
					if (min > input.num)
						break;
				}
				sts = Action.group[act / 10].exec();
				logger.debug("Exit from group action: " + sts);
				if (nxt < 0)
					panel.eventStop(sts);
			}
		}


		if (sts > 0) {
			line = base;
			if (input.key != input.CLEAR) {
                // ENH-20160107-CGA#A BEG
                logger.info("Motor - error code: " + sts);
				if (SscoPosManager.getInstance().isEnabled()) {
					SscoPosManager.getInstance().error(new SscoError(sts, Mnemo.getInfo (sts) ) );
				} else {
					if (sts >= 1000) {
						String msg = readFileErrorCode(String.valueOf(sts));

						logger.info("error message: " + msg);
						panel.clearLink(msg, 1);
					} else { // ENH-20160107-CGA#A END
						logger.info("error message: " + Mnemo.getInfo(sts));
						panel.clearLink(Mnemo.getInfo(sts), 1);
					}
				}
            }
            //	panel.clearLink(Mnemo.getInfo(sts), 1);  // SARAWAT-ENH-20150507-CGA#D
		} else {
			if (menu == 0)
				menu = base;
			line = nxt;
			dyna = 0;
		}
		base = read(line);
		if (lck < 0xF0) {
			menu = 0;
			Struc.dspLine.show(1);
			Action.showShort("POS", spc);
		} else {
			lck &= 0x0F;
			if (act > 0)
				panel.display(1, Mnemo.getMenu(act));
			Action.showShort("MNU", spc);
		}
		if ((lck & 0x80) > 0)
			Struc.oplLine.show(2);
		if (alt > 0 || min < 1)
			input.prompt = Mnemo.getText(alt);
		input.init(lck, max, min, dec);
		mainDyna(panel.dyna, dyna > 0 ? dyna : lck & 0x0F);
		panel.dspPicture(Struc.dspBmap);
	}

	public void run() {
		while (true)
			try {
				Thread.sleep(250);
				if (idle == 0) {
					idle--;
					gui.postAction("TICK");
				}
			} catch (InterruptedException e) {
				break;
			}
	}

	void dispatch(String cmd, Modal modal) {
		if (cmd.equals("TICK")) {
			try {
				idle = 1;
				Action.idle();
				idle = 0;
				return;
			} catch (Exception e) {
				logConsole(0, "< IdleStateFailure >", "section base=" + editNum(base, 3));
				e.printStackTrace();
				gui.eventStop(254);
			}
		}
		if (cmd.startsWith("SSCO")) {
			logger.info("SSCO command: " + cmd);

			Action.input.lck = 0xFF;
			String[] fields = cmd.split(":");

			if( fields[2].equals("4F4F") ) {
				cmd = "RDR0" + fields[1];
			}
			else {
				Action.input.reset(fields[1]);
				cmd = "CODE" + fields[2];
			}
		}
		if (cmd.startsWith("CODE")) {
			logger.info("Command: " + cmd);

			int code = Integer.parseInt(cmd.substring(4), 16);
			if (modal != null)
				return;
			code = input.accept(code);
			if (code >= 0)
				main(code);
			return;
		}
		input.logConsole(32, "actionCmd=" + cmd, null);
		if (cmd.startsWith("LIST")) {
			int code = Integer.parseInt(cmd.substring(9));
			if (modal != null)
				return;
			code = Match.chk_item(cmd.substring(4, 8), code);
			if (code < 1)
				code = input.accept(0x0e);
			main(code);
		}
		if (cmd.startsWith("BIO")) {
			int code = Integer.parseInt(cmd.substring(3));
			if (modal != null)
				BirIo.status(code);
		}
		if (cmd.startsWith("LCK")) {
			input.keyLock(Integer.parseInt(cmd.substring(3)));
			Action.showAutho();
		}
		if (cmd.startsWith("RDR")) {
			logger.info("scanner cmd: " + cmd);

			//QRCODE-SELL-CGA#A BEG
			if (cmd.indexOf("\n") >= 0) {
				logger.info("found separator");
				String[] itmsList = cmd.split("\\r?\\n");

				input.qrcode = cmd.substring(4);
				logger.info("input.qrcode: " + input.qrcode);

				cmd = (itmsList[0].contains(";")) ? (itmsList[0].split(";")[0]) : (itmsList[0]);
				logger.info("cmd: " + cmd);
			}
			//QRCODE-SELL-CGA#A END

			int code = input.label(cmd);
			if (modal == null)
				main(code);
			else
				modal.modalMain(code);
		}
		if (cmd.startsWith("MSR")) {
			int code = input.track(cmd);
			if (code < 0)
				return;
			if (modal == null)
				main(code);
			else
				modal.modalMain(code);
		}
		if (cmd.startsWith("SCA")) {
			if (GdScale.weight(cmd))
				modal.quit();
		}
	}

    // SARAWAT-ENH-20150507-CGA#A BEG
    public String readFileErrorCode(String code) {
        logger.debug("Enter");

        Properties prop = new Properties();
        String errorMessage = "";

        try {
            prop.load(new FileInputStream("conf/errorCodes.properties"));
            errorMessage = prop.getProperty(code);

            if (errorMessage == null || errorMessage.equals("")) {
                logger.info("default value");
                errorMessage = prop.getProperty("default");
            }
        } catch(Exception e) {
            logger.info("EXCEPTION " + e.getMessage());
        }

        logger.debug("Exit - return: " + errorMessage);
        return errorMessage;
    }
    // SARAWAT-ENH-20150507-CGA#A END
}
