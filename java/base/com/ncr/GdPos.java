package com.ncr;

import com.ncr.ssco.communication.entities.TableElement;
import com.ncr.ssco.communication.manager.SscoPosManager;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import static com.ncr.FmtIo.editKey;
import static com.ncr.Table.dpt;
import static com.ncr.Table.lDPT;

public class GdPos extends Panel implements Graphical, ActionListener, AdjustmentListener, MouseWheelListener {

	//ECOMMERCE-MSOUK#A BEG
	private static final Logger logger = Logger.getLogger(GdPos.class);
	//ECOMMERCE-MSOUK#A END

	Frame frame;
	Modal modal;
	ConIo input = Action.input;
	Motor event = Action.event;
	Thread tick = new Thread(event, event.project + ":ticker");
	Button idle = new Button() {
		public Container getParent() {
			return modal; /* null in main frame */
		}
	};
	Border pnlCard = new Border(0); /* 0 = reg+store/ckr, 1 = bonuspoints */
	Border pnlView = new Border(0); /* 0 = journal, 1 = customer/versions */
	Border pnlList = new Border(0); /* 0 = news+journal, 1 = item chooser */
	Border pnlRoll = new Border(0); /* 0 = legacy journal, 1 = trans view */
	public EventQueue queue = getToolkit().getSystemEventQueue();

	CidIo cid;
	DynakeyGroup dyna;

	GdLabel dspArea[] = new GdLabel[6];
	GdLabel msgArea[] = new GdLabel[2];
	GdLabel pntArea[] = new GdLabel[5];
	GdLabel sinArea[] = new GdLabel[2];
	GdLabel stsArea[] = new GdLabel[6];  //WINEPTS-CGA#A
	GdLabel cusArea[] = new GdLabel[8];

	GdElJrn journal = new GdElJrn(42, 12);
	GdLabel picture = new GdLabel(null, 0);
	GdLabel sticker = new GdLabel("   ", GdLabel.STYLE_RAISED);
	GdLabel trxCard = new GdLabel(null, 0);
	GdTView trxView = null;

	Font /* 2x20 */ font20, /* head */font40, /* list */font60, /* tiny */font80, /* View */font54;/* vert.gaps */
	static int fontSize[][] = { { 50, 65, 84 }, { 26, 36, 44 }, { 17, 22, 29 }, { 13, 18, 25 }, { 14, 17, 22 },
			{ 2, 3, 5 } };
	static int xfntSize[][] = { { 48, 61, 79 }, { 24, 34, 42 }, { 17, 21, 28 }, { 15, 20, 25 }, { 13, 16, 21 },
			{ 2, 5, 11 } };
	private SscoPosManager posManager;

	GdPos(Frame f) {
		super();
		frame = f;
	}

	private void sscoInitialize() {
		posManager = SscoPosManager.getInstance();
		posManager.initialize(idle, queue, editKey(Struc.ctl.reg_nbr, 3));

		for (int index = 1; index < dpt.key.length; index++) {
			lDPT.read(index, lDPT.LOCAL);
			if (!editKey(lDPT.key, 4).startsWith("*")) {
				posManager.getDepartmentsTable().add(new TableElement(lDPT.text, editKey(lDPT.key, 4)));
			}
		}
	}

	void init(Dimension d) {
		int ind, type = d.width / 160 - 4;

		if (File.separatorChar == '/')
			fontSize = xfntSize;

		int vgap = fontSize[5][type];
		font54 = Config.getFont(Font.BOLD, fontSize[4][type]);
		font80 = Config.getFont(Font.PLAIN, fontSize[3][type]);
		font60 = Config.getFont(Font.BOLD, fontSize[2][type]);
		font40 = Config.getFont(Font.BOLD, fontSize[1][type]);
		font20 = Config.getFont(Font.BOLD, fontSize[0][type]);

		setFont(font80);
		setLayout(new BorderLayout(0, vgap));
		dyna = new DynakeyGroup(d);
		dyna.setFont(Config.getFont(Font.BOLD, 13));
		dyna.dble = Config.getFont(Font.BOLD, fontSize[1][0]);
		cid = new CidIo(frame);

		dspArea[1] = new GdLabel("                    ", GdLabel.STYLE_RAISED);
		dspArea[2] = new GdLabel(null, GdLabel.STYLE_WINDOW);
		dspArea[3] = new GdLabel("                   ", GdLabel.STYLE_STATUS);
		dspArea[4] = new GdLabel(null, GdLabel.STYLE_STATUS);
		dspArea[5] = new GdLabel(null, GdLabel.STYLE_HEADER);
		dspArea[0] = new GdLabel("enabling peripherals", GdLabel.STYLE_HEADER);
		dspArea[0].setFont(font40);
		dspArea[0].setEnabled(false);
		dspArea[3].setEnabled(false);
		dspArea[4].setEnabled(false);

		stsArea[0] = new GdLabel(null, GdLabel.STYLE_STATUS);
		stsArea[1] = new GdLabel("SRV000", GdLabel.STYLE_STATUS);
		stsArea[2] = new GdLabel("[---]", GdLabel.STYLE_STATUS);
		stsArea[3] = new GdLabel("autho", GdLabel.STYLE_STATUS);
		stsArea[4] = new GdLabel("[slip]", GdLabel.STYLE_STATUS);
		stsArea[5] = new GdLabel("Epts", GdLabel.STYLE_STATUS);//WINEPTS-CGA#A

		trxView = new GdTView(12, 56, true);

		for (ind = 0; ind < dspArea.length; ind++) {
			dspArea[ind].setName("dspArea" + ind);
		}

		Panel pnlSlot = new Panel(new GridLayout(1, 0, 4, 0));
		pnlSlot.setFont(font40);
		for (ind = 0; ind < pntArea.length; ind++) {
			pntArea[ind] = new GdLabel(" ", GdLabel.STYLE_STATUS);
			pntArea[ind].setName("pntArea" + ind);
			pnlSlot.add(Border.around(pntArea[ind], -2));
		}

		Panel pnl = new Border(-1);
		pnl.add(dspArea[3]);
		pnl.add(dspArea[4]);
		pnl.setFont(font60);
		pnlCard.setLayout(new CardLayout());
		pnlCard.add(pnl, "info");
		pnlCard.add(pnlSlot, "slot");

		Panel pnlNews = new Panel(new GridLayout(0, 1));
		Panel pnlSins = new Panel(new GridLayout(0, 1));
		for (ind = 0; ind < 2; ind++) {
			msgArea[ind] = new GdLabel("              ", GdLabel.STYLE_RAISED);
			msgArea[ind].setName("msgArea" + ind);
			pnlNews.add(Border.around(msgArea[ind], 1));
			sinArea[ind] = new GdLabel(null, GdLabel.STYLE_RAISED);
			sinArea[ind].setName("sinArea" + ind);
			pnlSins.add(Border.around(sinArea[ind], 1));
			sinArea[ind].setForeground(GdLabel.colorInactiveCaptionText);
		}
		pnlSins.setFont(font60);

		Panel pnlStat = new Panel(new BorderLayout(2, 0));
		Panel pnlLite = new Panel(new GridLayout(1, 0, 2, 0));
		for (ind = 0; ind < stsArea.length; ind++) {
			stsArea[ind].setName("stsArea" + ind);
			if (ind > 0)
				pnlLite.add(Border.around(stsArea[ind], -1));
			else
				pnlStat.add(Border.around(stsArea[ind], -1), BorderLayout.CENTER);
			stsArea[ind].setEnabled(false);
		}
		stsArea[0].setAlignment(Label.LEFT);
		pnlStat.add(pnlLite, BorderLayout.EAST);

		Panel pnl2x20 = new Panel(new BorderLayout(0, vgap));
		pnl2x20.add(Border.around(dspArea[1], 3), BorderLayout.NORTH);
		pnl2x20.add(Border.around(dspArea[2], -3), BorderLayout.CENTER);
		pnl2x20.add(pnlStat, BorderLayout.SOUTH);
		dspArea[1].setFont(font20);
		dspArea[2].setFont(font20);

		Panel pnlInfo = new Panel(new BorderLayout(0, 2));
		pnlInfo.add(pnlNews, BorderLayout.NORTH);
		pnlInfo.add(picture, BorderLayout.CENTER);
		pnlInfo.add(Border.around(sticker, 3), BorderLayout.SOUTH);

		sticker.setFont(font20);
		sticker.setName("sticker");
		picture.setName("picture");

		Panel pnlCust = new Border(3);
		pnl = new Panel(new GridLayout(0, 1));
		for (ind = 0; ind < cusArea.length; ind++) {
			if (ind < 4)
				pnl.add(cusArea[ind] = new GdLabel(null, GdLabel.STYLE_RAISED));
			else
				pnl.add(Border.around(cusArea[ind] = new GdLabel(null, GdLabel.STYLE_STATUS), -1));
			cusArea[ind].setName("cusArea" + ind);
		}
		pnlCust.setLayout(new BorderLayout());
		pnlCust.add(pnl, BorderLayout.SOUTH);
		pnlCust.setBackground(cusArea[0].getBackground());

		pnlView.setLayout(new CardLayout());
		pnlView.add(Border.around(journal, -3), "roll");
		pnlView.add(pnlCust, "info");
		pnlView.setFont(font60);
		pnl = new Panel(new BorderLayout(vgap, 0));
		pnl.add(pnlView, BorderLayout.WEST);
		pnl.add(journal.bar, BorderLayout.CENTER);
		pnlRoll.setLayout(new CardLayout());
		pnlRoll.add(pnl, "journal");
		pnlRoll.add(trxView, "trxView");
		pnlRoll.add(trxCard, "trxCard");
		trxView.setFont(font54);

		pnlList.setLayout(new CardLayout());
		pnlList.add(pnlRoll, "roll");
		pnlList.add(dyna.chooser, "list");

		Panel pnlBase = new Panel(new BorderLayout(vgap, vgap));
		pnlBase.add(pnlList, BorderLayout.CENTER);
		pnlBase.add(pnlInfo, BorderLayout.EAST);
		pnlBase.add(pnl2x20, BorderLayout.SOUTH);

		add(pnlCard, BorderLayout.WEST);
		add(Border.around(dspArea[0], -1), BorderLayout.EAST);
		add(pnlBase, BorderLayout.SOUTH);

		sscoInitialize();
		idle.addActionListener(this);
		journal.bar.addAdjustmentListener(this);
		frame.addMouseWheelListener(this);
		frame.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (modal == null) /* jview needs protection from pending events */
				{
					int code = input.keyBoard(e);
					if (code >= 0)
						event.main(code);
					else
						ElJrn.roll(e.getKeyCode());
				}
			}
		});
		if (SscoPosManager.getInstance().isEnabled()) {
			frame.setState(Frame.ICONIFIED);
		} else {
			frame.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					if (modal == null)
						frame.requestFocus();
				}
				// PSH-ENH-000-AMZ-TEST#BEG -- for emulators
				/*
				 * public void focusGained(FocusEvent e) { frame.requestFocus (); }
				 */
				// PSH-ENH-000-AMZ-TEST#END -- for emulators
			});
		}
		stsArea[2].addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int step = (e.getModifiers() & e.BUTTON1_MASK) > 0 ? -1 : 1;
				if (!DevIo.hasKeylock()) {
					postAction("LCK" + (input.posLock + step));
				}
			}
		});
	}

	public void postAction(String cmd) {
		//EventQueue queue = getToolkit().getSystemEventQueue();
		queue.postEvent(new ActionEvent(idle, ActionEvent.ACTION_PERFORMED, cmd));
	}

	public void actionPerformed(ActionEvent e) {
		event.dispatch(e.getActionCommand(), modal);
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		ElJrn.view(false);
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollType() == e.WHEEL_UNIT_SCROLL) {
			int ind = e.getWheelRotation();
			while (ind++ < 0)
				ElJrn.roll(KeyEvent.VK_UP);
			while (--ind > 0)
				ElJrn.roll(KeyEvent.VK_DOWN);
		}
	}

	void eventInit() {
		Thread.currentThread().setName(event.project + ":dispatch");
		Action.init(this);
		event.main(-1);
		frame.requestFocus();
		tick.start();
	}

	public void eventStop(int sts) {
		Action.stop();
		try {
			tick.interrupt();
			tick.join();
		} catch (InterruptedException e) {
			System.out.println(e);
		}
		journal.stop();
		System.exit(sts);
	}

	public int clearLink(String msg, int type) {
		int line = type >> 4;

		//ECOMMERCE-MSOUK#A BEG
		if (ECommerceManager.getInstance().hidePopup()) {
			logger.info("Not showing popup " + msg);
			return 0;
		}
		//ECOMMERCE-MSOUK#A END

		DevIo.alert(0);
		if (!Thread.currentThread().getName().endsWith("dispatch")) {
			DevIo.oplDisplay(1, msg);
			dspStatus(0, msg, true, true);
			return 0;
		}
		if (event.idle > 0)
			return 0;
		if (input.key == 0x4f4f) {
			DevIo.setAlerted(input.label.charAt(3) & 3);
		}
		DevIo.oplSignal(15, 1);
		Action.oplToggle(line & 3, msg);
		ClrDlg dlg = new ClrDlg(msg, type & 7);
		if ((type & 0x80) > 0)
			dlg.input = new ConIo(20);
		dlg.input.init(0x80, 0, 0, 0);
		dlg.show("CLR");
		Action.oplToggle(line & 1, null);
		DevIo.oplSignal(15, 0);
		return dlg.code;
	}

	void innerVoice(int action) {
		if (modal != null)
			modal.quit();
		else
			postAction("CODE" + Integer.toHexString(action));
	}

	public void display(int line, String data) {
		if (line > 9) {
			Action.cusDisplay(line - 10, data);
			return;
		}
		if (line == 3)
			pnlCard.toFront(0); /* first card = ids */
		dspArea[line].setText(data);
		if (line > 0 && line < 3)
			DevIo.oplDisplay(line - 1, data);
	}

	void dspNotes(int line, String data) {
		if (line < 1)
			msgArea[line].setAlerted(data != null);
		msgArea[line].setText(data);
	}

	void dspShort(int line, String data) {
		sinArea[line].setText(data);
	}

	void dspShopper(int line, String data) {
		if (line == 0)
			pnlView.toFront(1); /* first card = shopper */
		cusArea[line].setText(data);
	}

	void dspPicture(String name) {
		picture.setPicture(name);
		picture.setText(picture.image == null ? name : null);
	}

	void dspPoints(String data) {
		int len = data.length();
		pnlCard.toFront(1); /* first card = points */
		for (int ind = pntArea.length; ind-- > 0; len--) {
			pntArea[ind].setText(data.substring(len - 1, len));
		}
	}

	void dspSymbol(String data) {
		sticker.setPicture("SYM_" + data);
		sticker.setText(sticker.image == null ? FmtIo.editTxt(data, 3) : null);
	}

	public void dspStatus(int nbr, String data, boolean enabled, boolean alerted) {
		GdLabel lbl = stsArea[nbr];
		lbl.setAlerted(alerted);
		lbl.setEnabled(enabled);
		if (data != null)
			lbl.setText(data);
	}

	void jrnPicture(String name) {
		pnlView.toFront(0); /* first card = journal */
		journal.setPicture(name);
	}

	public void print(int station, String data) {
		if ((station & FmtIo.ELJRN) > 0)
			ElJrn.write(station, data);
		for (int dev = 8; dev > 0; dev >>= 1)
			if ((station & dev) > 0)
				DevIo.tpmPrint(dev, 0, data);
	}

	public void select(int ind) {
		if (modal == null)
			dyna.select(ind);
	}

	public void feedBack(KeyEvent e) { // System.out.println (e.paramString () + " at " + e.getWhen ());
	}

	public static void main(String[] args) {
		// TSC-MOD2014-AMZ#BEG
		// Frame f = new Frame("JPos++");
		Frame f = new Frame(PosVersion.windowTitle());
		// TSC-MOD2014-AMZ#END
		final GdPos panel = new GdPos(f);
		Dimension d = Config.frameSize("MainFrame");

		if (d.width < 640 || d.width > 1024) {
			Config.logConsole(1, null, "screen " + d.width + "x" + d.height);
			d.width = 1024;
			d.height = 768;
		}
		Param.init();
		PosGPE.Init(); //WINEPTS-CGA#A
		panel.init(d);

		f.add(panel);
		f.setBackground(Color.getColor("COLOR_DESKTOP", SystemColor.desktop));
		f.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				panel.eventInit();
			}

			public void windowClosing(WindowEvent e) {
				panel.eventStop(1);
			}
		});
		f.setSize(d);
		f.setLocation(0, 0);
		f.setResizable(false);
		f.show();
		if (!d.equals(f.getSize())) {
			f.setSize(d); /* Linux JRE1.3 */
			f.validate(); /* second try with real insets */
		}
		f.toFront(); /* SUN: after early kbrd input */
		Config.logConsole(1, null, "window " + d.width + "x" + d.height);
		d = panel.getSize();
		Config.logConsole(1, null, "client " + d.width + "x" + d.height);

		// System.getProperties ().list (System.out);
		Config.logConsole(1, null, PosVersion.windowTitle());
	}

	//WINEPTS-CGA#A BEG
	public void updateEpts(boolean active) {
		if (stsArea[5] != null) {
			stsArea[5].setText(active ? "WON" : "WOFF");
			stsArea[5].setAlerted(!active);
		}
	}
	//WINEPTS-CGA#A END
}
