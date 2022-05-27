package com.ncr;

import java.awt.*;
import java.io.*;

class CidIo extends Window {
	int type;

	Ground ground = null;
	GdLabel cusArea[] = new GdLabel[4];
	GdLabel proArea = new GdLabel("        ", 0);
	GdLabel bigArea = new GdLabel(null, 0);
	GdTView trxView = new GdTView(12, 52, false);

	Border pnlCus12 = new Border(12);
	Border pnlCus34 = new Border(-12);
	Border pnlCard = new Border(0);

	/* 4 x 20 */ /* trxView */
	static int fontSize[][] = { { 46, 56, 80 }, { 12, 15, 19 } };
	static int xfntSize[][] = { { 46, 56, 80 }, { 11, 15, 18 } };

	private void hotImage(GdLabel lbl, String name) {
		File hot = Config.localFile("hot", name + ".GIF");

		if (hot.exists()) {
			File f = Config.localFile("gif", hot.getName());
			lbl.setPicture(null);
			Config.logConsole(2, "use " + hot.getPath(), null);
			Config.localMove(hot, f);
		}
		if (lbl.image == null)
			lbl.setPicture(name);
	}

	CidIo(Frame parent) {
		super(parent);

		if (File.separatorChar == '/')
			fontSize = xfntSize;
		File f = Config.localFile("gif", "CIDWIN.GIF");
		if (f.exists()) {
			ground = new Ground(this, f);
		}
		for (int ind = 0; ind < cusArea.length; ind++) {
			if (ind < 2)
				pnlCus12.add(cusArea[ind] = new GdLabel(null, GdLabel.STYLE_STATUS));
			else
				pnlCus34.add(cusArea[ind] = new GdLabel(null, GdLabel.STYLE_RAISED));
			cusArea[ind].ground = ground;
			cusArea[ind].setEnabled(false);
			cusArea[ind].setName("cusArea" + ind);
		}
		pnlCus12.ground = pnlCus34.ground = ground;
		add(pnlCus12, BorderLayout.NORTH);
		add(pnlCard, BorderLayout.CENTER);
		Panel pnl = new Panel(new BorderLayout());
		pnl.add(pnlCus34, BorderLayout.SOUTH);
		pnl.add(proArea, BorderLayout.EAST);
		pnl.add(trxView, BorderLayout.CENTER);
		proArea.setName("proArea");
		pnlCard.setLayout(new CardLayout());
		pnlCard.add(bigArea, "base");
		pnlCard.add(pnl, "info");
		Dimension c = Config.frameSize("CidFrame");
		setBounds(Config.frameSize("MainFrame").width, 0, c.width, c.height);
		type = c.width / 160 - 4;
	}

	void init() {
		if (type < 0)
			return;
		setFont(Config.getFont(Font.BOLD, fontSize[0][type]));
		trxView.setFont(Config.getFont(Font.BOLD, fontSize[1][type]));
		trxView.init(0);
		hotMaint();
		show();
		Dimension pro = proArea.getSize(), big = bigArea.getSize();
		Config.logConsole(1, null, "CID Advertising Space " + pro.width + "x" + pro.height);
		Config.logConsole(1, null, "CID ClosedState Space " + big.width + "x" + big.height);
	}

	void display(int line, String data) {
		cusArea[line].setText(data);
	}

	void hotMaint() {
		if (type < 0)
			return;
		hotImage(bigArea, "CINFO");
		hotImage(proArea, "PROMO");
	}

	void clear(boolean onDuty) {
		for (int ind = cusArea.length; ind-- > 0; display(ind, null))
			;
		pnlCard.toFront(onDuty ? 1 : 0);
	}
}
