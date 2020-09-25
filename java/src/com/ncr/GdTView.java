package com.ncr;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

class GdTView extends Panel implements AdjustmentListener {
	int rows;
	TrxList list = new TrxList();
	Scrollbar sbar = new Scrollbar() {
		public Dimension getPreferredSize() {
			Dimension d = getParent().getSize();
			d.width = getFontMetrics(getFont()).charWidth(' ') * 3;
			return d;
		}
	};
	GdLabel head = new GdLabel("", GdLabel.STYLE_HEADER) {
		public Dimension getPreferredSize() {
			Dimension d = getParent().getSize();
			d.height -= 6;
			d.height = d.height / rows + d.height % rows;
			return d;
		}
	};
	static Color colorScrollbar = Color.getColor("COLOR_SCROLLBAR", SystemColor.scrollbar);

	GdTView(int rows, int cols, boolean active) {
		super(new BorderLayout());
		this.rows = rows;
		list.cols = cols;
		list.setName("trxList");
		head.setName("trxHead");
		head.setEnabled(false);
		head.setBackground(colorScrollbar);
		head.setText((Struc.view_txt[0] + Struc.view_txt[1]).substring(0, cols));
		if (active) {
			sbar.addAdjustmentListener(this);
			sbar.setBackground(colorScrollbar);
			list.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if ((e.getModifiers() & e.BUTTON1_MASK) == 0)
						return;
					Dimension d = list.getSize();
					updateView(e.getY() / (d.height / list.rows) + list.top + 1);
					e.consume();
				}
			});
		}
	}

	void updateView(int sel) {
		int size = TView.trxData.size();
		if (sel > size)
			return;
		if (sel < 1)
			list.sel = 0;
		if (sel <= list.top || sel > list.top + list.rows)
			list.top += sel - list.sel;
		if (list.top > size - list.rows)
			list.top = size - list.rows;
		if (list.top < 0)
			list.top = 0;
		sbar.setValues(list.sel = sel, list.rows, 1, list.rows + size);
		list.repaint();
	}

	void init(int option) {
		if (list.dwide = (option & 0x01) > 0) {
			list.rows = rows / 3;
			list.cols = 32;
			list.sgle = getFont().deriveFont(AffineTransform.getScaleInstance(1.75, 1.5));
			list.dble = getFont().deriveFont(AffineTransform.getScaleInstance(2.8, 1.5));
		} else {
			list.rows = rows - 1;
			add(head, BorderLayout.NORTH);
			list.sgle = getFont();
			list.dble = getFont().deriveFont(AffineTransform.getScaleInstance(2, 1));
		}
		list.step = getFontMetrics(list.sgle).charWidth(' ');
		add(Border.around(list, -3), BorderLayout.CENTER);
		if ((option & 0x08) > 0) {
			add(sbar, BorderLayout.EAST);
			sbar.setBackground(Color.getColor("COLOR_SCROLLBAR", SystemColor.scrollbar));
			sbar.setBlockIncrement(list.rows);
			sbar.setValues(0, list.rows, 1, list.rows);
			head.setText(head.getText() + "   ");
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension();
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		updateView(sbar.getValue());
	}

	int scroll(int vkey) {
		int val = sbar.getValue();

		if (!sbar.isEnabled())
			return 0;
		switch (vkey) {
		case KeyEvent.VK_UP:
			val--;
			break;
		case KeyEvent.VK_DOWN:
			val++;
			break;
		case KeyEvent.VK_PAGE_UP:
			val -= sbar.getBlockIncrement();
			break;
		case KeyEvent.VK_PAGE_DOWN:
			val += sbar.getBlockIncrement();
			break;
		case KeyEvent.VK_HOME:
			val = sbar.getMinimum();
			break;
		case KeyEvent.VK_END:
			val = sbar.getMaximum() - list.rows;
			break;
		default:
			return 0;
		}
		sbar.setValue(val);
		val = sbar.getValue();
		return val == list.sel ? 0 : val;
	}
}

class TrxData {
	int state; /* 0=text, 1=item, 2=e/c, 3=gone */
	int color; /* 0=gray, 1=black, 2=red */
	int index; /* item reference */
	boolean under;
	String text;

	TrxData(int state, int color, boolean under, int index, String text) {
		this.state = state;
		this.color = color;
		this.under = under;
		this.index = index;
		this.text = text;
	}
}

class TrxList extends Canvas {
	int cols, rows, step;
	int sel, top;
	boolean dwide;
	Font sgle, dble;
	Point pad;
	Dimension box;
	TrxData td;

	Color getColors(Graphics g, boolean selected) {
		Color bg = GdLabel.colorWindow, fg = GdLabel.colorWindowText;
		if (td.color == 0)
			fg = Color.gray;
		if (selected) {
			if (td.state == 1) {
				bg = GdLabel.colorActiveCaption;
				fg = GdLabel.colorActiveCaptionText;
			} else {
				fg = GdLabel.colorInactiveCaptionText;
				bg = GdLabel.colorInactiveCaption;
			}
		}
		if (td.color == 2)
			fg = Color.red;
		g.setColor(bg);
		return fg;
	}

	public String getText(int ind) {
		if (ind >= rows)
			return null;
		return TView.getLineItem(top + ind).text;
	}

	public synchronized void paint(Graphics g) {
		int line = 0;

		box = getSize();
		box.height /= rows;
		pad = new Point(box.width - cols * step >> 1, line);
		for (int ind = 0; ind < rows;) {
			td = TView.getLineItem(top + ind++);
			Color fg = getColors(g, top + ind == sel);
			g.fillRect(0, line, box.width, box.height);
			line += box.height;
			if (td.under) {
				g.setColor(Color.lightGray);
				g.drawLine(0, line - 1, box.width - 1, line - 1);
			}
			if (td.text == null)
				continue;
			g.setColor(fg);
			drawText(g, td.text, line, box.height);
		}
	}

	void drawText(Graphics g, String s, int line, int high) {
		int x = pad.x, y = line;

		if (dwide)
			y -= high >>= 1;
		y -= high + 3 >> 2;
		if (s.charAt(0) == '>') {
			g.setFont(dble);
			if (dwide)
				y += high >> 1;
			g.drawString(s.substring(1), x, y);
		} else {
			g.setFont(sgle);
			if (dwide) {
				g.drawString(s.substring(0, 32), x, y);
				g.drawString(s.substring(32), x + 8 * step, y + high);
			} else {
				g.drawString(s.substring(0, 40), x, y);
				g.drawString(s.substring(40, cols), x + 40 * step, y);
			}
		}
		if (td.state == 3) {
			g.setColor(Color.red);
			y = line - (high >> 1);
			g.drawLine(0, y, box.width, y);
			if (dwide) {
				g.drawLine(0, y - high, box.width, y - high);
			}
		}
	}
}
