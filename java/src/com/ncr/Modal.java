package com.ncr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Modal extends Dialog implements ItemListener, ActionListener {
	ConIo input = Action.input;
	GdPos panel = Action.panel;

	List list = null;
	Component kbrd = this;
	Component bounds = panel.pnlRoll;
	boolean block = true, touchy;
	int code = input.ENTER;

	Modal(String title) {
		super(Action.panel.frame, title, true);
		setResizable(false);
		setFont(panel.font60);
		addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				kbrd.requestFocus(); /* for EVM and MS */
			}

			public void windowClosing(WindowEvent e) {
				modalMain(input.key = 0);
			}
		});
	}

	void setBounds(Component c) {
		setSize(c.getSize());
		setLocation(c.getLocationOnScreen());
	}

	void scroll(int vkey) {
		if (list == null)
			return;

		int ind = list.getSelectedIndex();
		int max = list.getItemCount() - 1;
		switch (vkey) {
		case KeyEvent.VK_UP:
			ind = ind < 0 ? max : ind - 2;
		case KeyEvent.VK_DOWN:
			ind++;
			break;
		case KeyEvent.VK_PAGE_UP:
			ind -= list.getRows();
			break;
		case KeyEvent.VK_PAGE_DOWN:
			ind += list.getRows();
			break;
		case KeyEvent.VK_HOME:
			ind = 0;
			break;
		case KeyEvent.VK_END:
			ind = max;
			break;
		default:
			return;
		}
		if (ind < 0)
			ind = 0;
		if (ind > max)
			ind = max;
		if (ind != list.getSelectedIndex())
			itemEcho(ind);
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == e.SELECTED)
			itemEcho(-1);
	}

	public void actionPerformed(ActionEvent e) {
		input.key = code;
		getToolkit().beep();
		modalMain(0);
	}

	public void show(String sin) {
		panel.modal = this;
		touchy = panel.dyna.chooser.isVisible();
		if (touchy)
			panel.dyna.showTouch(false);
		if (block)
			DevIo.setEnabled(false);
		setBounds(bounds);
		Action.showShort(sin, 0);
		kbrd.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary())
					kbrd.requestFocus();
			}
		});
		kbrd.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int code = input.keyBoard(e);
				if (code >= 0)
					modalMain(code);
				else
					scroll(e.getKeyCode());
			}
		});
		super.show();
	}

	public void quit() {
		WindowEvent e = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
		getToolkit().getSystemEventQueue().postEvent(e);
		code = -1; /* result code = auto */
	}

	void itemEcho(int index) {
		if (index >= 0 && index < list.getItemCount()) {
			list.select(index);
			if (File.separatorChar == '/')
				list.makeVisible(index);
		}
		String s = list.getSelectedItem();
		if (s == null)
			return;
		index = s.indexOf('.');
		input.reset(s.substring(0, index).trim());
		DevIo.oplDisplay(0, s.substring(index + 1).trim());
		input.max = 0;
	}

	void modalMain(int sts) {
		if (input.key == input.CLEAR && sts > 0) {
			if (input.max > 0 && input.flg < 0x80) {
				input.reset("");
				return;
			}
		}
		if (code >= 0)
			code = sts;
		if (list != null) {
			list.removeItemListener(this);
			list.removeActionListener(this);
		}
		panel.modal = null;
		dispose();
		if (touchy)
			panel.dyna.showTouch(true);
		if (block)
			DevIo.setEnabled(true);
		Action.showShort("DLG", 0);
	}
}

class ClrDlg extends Modal {
	int border = panel.font40.getSize() >> 1;
	GdLabel info = new GdLabel(null, GdLabel.STYLE_RAISED);
	GdLabel key1 = new GdLabel(Action.key_txt[1], GdLabel.STYLE_RAISED);
	GdLabel key2 = new GdLabel(Action.key_txt[0], GdLabel.STYLE_RAISED);

	ClrDlg(String title, int type) {
		super("PoS Info Message");

		String ico = "CLR40";
		if ((type & 4) > 0) {
			code = input.ABORT;
			key2.setText(Action.key_txt[2]);
			if ((type & 1) < 1)
				ico = "ABORT";
		}
		setFont(panel.font40);
		setUndecorated(true);
		if (title.length() <= 20) {
			title = "      " + title;
			ico = (type & 2) > 0 ? "ENTER" : "CLEAR";
		} else
			info.setFont(panel.font60);
		info.setEnabled(false);
		info.setPicture(ico);
		info.setText(title);
		add(info, BorderLayout.CENTER);
		add(Border.around(key1, border), BorderLayout.NORTH);
		add(Border.around(key2, border), BorderLayout.SOUTH);
		key1.setEnabled((type & 1) > 0);
		key2.setEnabled((type & 6) > 0);
		if (!key1.isEnabled())
			key1.setText(null);
		if (!key2.isEnabled())
			key2.setText(null);
		key1.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				input.key = input.CLEAR;
				modalMain(0);
			}
		});
		key2.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				input.key = code;
				modalMain(0);
			}
		});
	}

	void modalMain(int sts) {
		if (sts > 0)
			return;
		if (sts == 0) {
			if (input.key > 0)
				sts = 5;
			if (key1.isEnabled()) {
				if (input.key == 0)
					input.key = input.CLEAR;
				else if (input.key == input.CLEAR)
					sts = 1;
			}
			if (key2.isEnabled()) {
				if (input.key == 0)
					input.key = code;
				else if (input.key == code)
					sts = 2;
			}
		}
		if (sts < 3)
			super.modalMain(sts);
	}
}

class SelDlg extends Modal {
	boolean sorted = false;

	SelDlg(String title) {
		super(title);
		add(new GdLabel(null, GdLabel.STYLE_STATUS), BorderLayout.CENTER);
		list = new List(File.separatorChar == '/' ? 9 : 10);
		add(list, BorderLayout.SOUTH);
		list.addItemListener(this);
		list.addActionListener(this);
	}

	private int getIndex(int pos, String text) {
		int rec, top = 0, end = list.getItemCount();

		while ((rec = (top + end) >> 1) < end) {
			String s = list.getItem(rec).substring(pos);
			if (Config.loc.compare(s, text) > 0)
				end = rec;
			else
				top = rec + 1;
		}
		return rec;
	}

	void modalMain(int sts) {
		int ind = list.getSelectedIndex();

		if (input.key == input.NORTH) {
			if (ind < 0)
				ind = list.getItemCount();
			itemEcho(--ind);
			return;
		}
		if (input.key == input.SOUTH) {
			itemEcho(++ind);
			return;
		}
		super.modalMain(sts);
	}

	void add(int pos, String key, String text) {
		int ind = sorted ? getIndex(pos + 1, text) : -1;

		String s = FmtIo.editTxt(key, pos) + ".";
		list.add(text == null ? s : s + text, ind);
	}
}

class PluDlg extends Modal {
	int border = panel.font40.getSize() >> 1;
	GdLabel key1 = new GdLabel(Action.key_txt[1], GdLabel.STYLE_RAISED);
	BarCode labl = new BarCode();
	GdLabel head = new GdLabel(Action.inq_line, GdLabel.STYLE_STATUS);
	GdLabel data = new GdLabel(null, GdLabel.STYLE_WINDOW);

	PluDlg(String title) {
		super(title);
		key1.setFont(panel.font40);
		Panel info = new Panel(new BorderLayout());
		info.add(head, BorderLayout.NORTH);
		info.add(Border.around(data, -1), BorderLayout.SOUTH);
		labl.setBackground(head.getBackground());
		add(Border.around(key1, border), BorderLayout.NORTH);
		add(labl, BorderLayout.CENTER);
		add(info, BorderLayout.SOUTH);
		key1.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				input.key = input.CLEAR;
				modalMain(0);
			}
		});
	}

	void modalMain(int sts) {
		if (sts > 0)
			return;
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.key == input.CLEAR)
			super.modalMain(0);
	}
}

class ModDlg extends Modal {
	GdLabel line[] = new GdLabel[3];

	ModDlg(String name) {
		super("PoS Enter Data");
		Panel info = new Panel(new GridLayout(0, 1));
		info.setFont(panel.font40);
		info.add(Border.around(line[0] = new GdLabel(null, GdLabel.STYLE_RAISED), -3));
		info.add(line[1] = new GdLabel(name, GdLabel.STYLE_STATUS));
		info.add(Border.around(line[2] = new GdLabel(null, GdLabel.STYLE_RAISED), -3));
		add(info, BorderLayout.CENTER);
	}
}

class WghDlg extends Modal {
	GdLabel area = new GdLabel(null, GdLabel.STYLE_STATUS);

	WghDlg(String title) {
		super("PoS Scale");
		GdLabel info = new GdLabel(title, GdLabel.STYLE_STATUS);
		info.setFont(panel.font40);
		add(info, BorderLayout.SOUTH);
		add(Border.around(area, -1), BorderLayout.CENTER);
		area.setPicture("SCALE");
	}
}

class SpyDlg extends Modal implements AdjustmentListener, MouseWheelListener {
	GdElJrn area = new GdElJrn(42, 11);

	SpyDlg(String title) {
		super(title);
		Panel info = new Panel(new BorderLayout());
		info.add(Border.around(area, -3), BorderLayout.WEST);
		info.add(area.bar, BorderLayout.CENTER);
		add(info, BorderLayout.SOUTH);
		area.bar.addAdjustmentListener(this);
		addMouseWheelListener(this);
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				Notes.watch(0);
			}
		});
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		Notes.watch(area.bar.getValue() + 1);
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollType() == e.WHEEL_UNIT_SCROLL) {
			int ind = e.getWheelRotation();
			while (ind++ < 0)
				scroll(KeyEvent.VK_UP);
			while (--ind > 0)
				scroll(KeyEvent.VK_DOWN);
		}
	}

	void scroll(int vkey) {
		if (area.scroll(vkey))
			adjustmentValueChanged(null);
	}

	void modalMain(int sts) {
		if (sts > 0)
			return;
		if (input.key == input.NORTH)
			scroll(KeyEvent.VK_UP);
		if (input.key == input.SOUTH)
			scroll(KeyEvent.VK_DOWN);
		if (input.key == input.ENTER)
			scroll(KeyEvent.VK_END);
		if (input.key == 0)
			input.key = input.CLEAR;
		if (input.key == input.CLEAR)
			super.modalMain(sts);
	}

	void add(String data) {
		int ind = 0;

		while (ind < area.rows - 1)
			area.list[ind] = area.list[++ind];
		area.list[ind] = data;
	}
}

class TchDlg extends Modal {
	int cols = 6, rows = 4;
	GdLabel area = new GdLabel(null, 0);

	TchDlg(String title) {
		super(title);
		bounds = panel.getParent();
		if (bounds != panel.frame)
			bounds = panel;
		add(area, BorderLayout.CENTER);
		area.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if ((e.getModifiers() & e.BUTTON1_MASK) == 0)
					return;
				Dimension d = getSize();
				int x = e.getX() / (d.width / cols);
				int y = e.getY() / (d.height / rows);
				if (x == cols || y == rows)
					return;
				if (input.isEmpty()) {
					input.reset(FmtIo.editNum(x + y * cols + 1, 2));
					actionPerformed(null);
				} else
					modalMain(2);
			}
		});
	}
}

class BcrDlg extends Modal {
	int cols = 5;
	Panel area = new Panel(new GridLayout(0, cols, 1, 1));
	GdLabel[] info = new GdLabel[2];
	Border pnl;
	GdLabel lbl;

	BcrDlg(String title) {
		super(title);
		for (int ind = 0; ind < info.length; ind++) {
			info[ind] = new GdLabel("", GdLabel.STYLE_HEADER);
			info[ind].setFont(panel.font20);
			info[ind].setEnabled(false);
		}
		add(Border.around(info[0], -4), BorderLayout.NORTH);
		add(area, BorderLayout.CENTER);
		add(Border.around(info[1], -4), BorderLayout.SOUTH);
		bounds = panel.getParent();
		if (bounds != panel.frame)
			bounds = panel;
	}

	void add(String text, String name) {
		area.add(pnl = new Border(-4));
		pnl.setLayout(new BorderLayout());
		pnl.add(lbl = new GdLabel("", GdLabel.STYLE_RAISED), BorderLayout.CENTER);
		lbl.setPicture(name);
		lbl.setEnabled(false);
		lbl.setText(lbl.image == null ? text : null);
		pnl.add(lbl = new GdLabel("", GdLabel.STYLE_HEADER), BorderLayout.SOUTH);
		lbl.setEnabled(false);
		lbl.setFont(panel.font40);
	}

	void setText(int ind, String text, boolean alert) {
		if (ind >= area.getComponentCount())
			return;
		pnl = (Border) area.getComponent(ind);
		lbl = (GdLabel) pnl.getComponent(1);
		if (text.equals(lbl.getText()))
			return;
		lbl.setAlerted(alert);
		lbl.setText(text);
	}
}

class AbcDlg extends Modal {
	int cols = 10, rows = 4;
	Border area = new Border(-3);
	String state = "  [Caps Lock]";
	private int mode = 0; /* 0=normal, 1=shift, 2=lock */

	void touch(MouseEvent e, int type) {
		if ((e.getModifiers() & e.BUTTON1_MASK) == 0)
			return;
		char c = ((GdLabel) e.getComponent()).getText().charAt(0);
		KeyEvent k = new KeyEvent(kbrd, type, e.getWhen(), e.getModifiers(), KeyEvent.VK_UNDEFINED, c);
		getToolkit().getSystemEventQueue().postEvent(k);
		e.consume(); /* SUN: key events can be mouse food */
		if (type == KeyEvent.KEY_PRESSED)
			getToolkit().beep();
		else if (mode == 1)
			setMode(0);
	}

	void setMode(int nbr) {
		String title = getTitle();
		int ind = title.indexOf(state);

		mode = nbr % 3;
		if (ind >= 0)
			title = title.substring(0, ind);
		if (mode == 2)
			title += "  [Caps Lock]";
		setTitle(title);
		for (ind = area.getComponentCount(); ind > 0;) {
			char c = input.alpha.charAt(--ind);
			GdLabel key = (GdLabel) area.getComponent(ind);
			if (mode > 0)
				c = Character.toUpperCase(c);
			key.setText(String.valueOf(c));
		}
	}

	void modalMain(int sts) {
		switch (input.key) {
		case ConIo.NORTH:
			mode--;
		case ConIo.SOUTH:
			setMode(mode + 2);
			input.key = 0;
			break;
		default:
			super.modalMain(sts);
		}
	}

	AbcDlg(String title) {
		super(title);
		add(area);
		setFont(panel.font40);
		area.setLayout(new GridLayout(rows, cols, 2, 2));
		for (int y = 0; y < rows; y++)
			for (int x = 0; x < cols; x++) {
				char c = input.alpha.charAt(y * cols + x);
				GdLabel key = new GdLabel(String.valueOf(c), 0);
				area.add(key);
				if (c > ' ')
					key.setPicture("ALPHA");
				else
					key.setVisible(false);
				key.addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent e) {
						touch(e, KeyEvent.KEY_PRESSED);
					}

					public void mouseReleased(MouseEvent e) {
						touch(e, KeyEvent.KEY_RELEASED);
					}
				});
			}
	}
}
