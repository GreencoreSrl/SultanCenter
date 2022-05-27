package com.ncr;

import com.ncr.gpe.std.GpeMessagePosDisplayInterface;

public class PosGPEShowMessage extends Action implements GpeMessagePosDisplayInterface {

	public void showMessage(int messageNumber, String messageText) {

		panel.display(1, messageText);
		panel.display(2, "");
	}
}
