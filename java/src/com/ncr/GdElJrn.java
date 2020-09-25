package com.ncr;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;

public class GdElJrn extends Canvas {
	int cols, rows;

	Image image = null;
	Font dble, sgle;
	String list[];
	Point pad = new Point(4, 0);
	Scrollbar bar = new Scrollbar();
	Bouncer bouncer = new Bouncer();

	static Color colorScrollbar = Color.getColor("COLOR_SCROLLBAR", SystemColor.scrollbar);

	GdElJrn(int x, int y) {
		this.cols = x;
		this.rows = y;
		this.list = new String[rows];
		setName("journal");
		setBackground(Color.white);
		setForeground(Border.color);
		bar.setBackground(colorScrollbar);
		bar.setBlockIncrement(rows);
		bar.setValues(0, rows, 0, rows);
	}

	boolean scroll(int vkey) {
		int val = bar.getValue(), prv = val;

		if (!bar.isEnabled())
			return false;
		switch (vkey) {
		case KeyEvent.VK_UP:
			val--;
			break;
		case KeyEvent.VK_DOWN:
			val++;
			break;
		case KeyEvent.VK_PAGE_UP:
			val -= bar.getBlockIncrement();
			break;
		case KeyEvent.VK_PAGE_DOWN:
			val += bar.getBlockIncrement();
			break;
		case KeyEvent.VK_HOME:
			val = bar.getMinimum();
			break;
		case KeyEvent.VK_END:
			val = bar.getMaximum() - rows;
			break;
		default:
			return false;
		}
		bar.setValue(val);
		return prv != bar.getValue();
	}

	private Dimension getCharSize() {
		Font f = getFont();
		FontMetrics fm = getFontMetrics(f);

		return new Dimension(fm.charWidth(' '), f.getSize());
	}

	public Dimension getPreferredSize() {
		Dimension d = getCharSize();
		d.height += 2;
		d.width *= cols;
		d.height *= list.length;
		d.width += pad.x << 1;
		d.height += pad.y << 1;
		return d;
	}

	public String getText(int ind) {
		return ind < list.length ? list[ind] : null;
	}

	void setPicture(String name) {
		if (image != null) {
			image.flush();
			image = null;
		} else if (name == null)
			return;
		if (name != null) {
			File f = Config.localFile("gif", name + ".GIF");
			if (f.exists()) {
				image = getToolkit().getImage(f.getAbsolutePath());
				prepareImage(image, null);
			}
		}
		repaint();
	}

	public void update(Graphics g) {
		paint(g);
	}

	public synchronized void paint(Graphics g) {
		Dimension d = getSize();

		bouncer.hide();
		if (image != null) {
			if ((checkImage(image, this) & ALLBITS + FRAMEBITS) == 0)
				return;
			Color bg = getBackground(); // JView: avoid flickering
			bg = new Color(bg.getRGB()); // EVM can't handle SystemColor
			g.drawImage(image, 0, 0, d.width, d.height, bg, this);
			return;
		}
		Dimension chr = getCharSize();
		int ind = 0, line = pad.y;
		int high = (d.height - line - line) / rows;
		while (ind < rows) {
			String s = list[ind++];
			g.setColor((ind & 1) < 1 ? colorScrollbar : getBackground());
			g.fillRect(0, line, d.width, high);
			g.setColor(getForeground());
			line += high;
			if (s == null)
				continue;
			drawText(g, s, line, chr.width, high);
			if (s.charAt(0) > ' ') {
				g.drawLine(0, line - 1, d.width - 1, line - 1);
				g.drawLine(0, line - 2, d.width - 1, line - 2);
			}
		}
	}

	void drawText(Graphics g, String s, int y, int wide, int high) {
		int x = pad.x, mid = cols >> 1;
		boolean quarter = rows < list.length;

		if (sgle == null)
			sgle = getFont();
		if (dble == null)
			dble = sgle.deriveFont(AffineTransform.getScaleInstance(2, 1));
		if (quarter)
			y -= high >>= 1;
		y -= high + 3 >> 2;
		if (s.charAt(1) == '>') {
			g.setFont(dble);
			if (quarter) {
				g.drawString(s.substring(2, 2 + (mid >> 1)), x, y);
				g.drawString(s.substring(2 + (mid >> 1), 1 + mid), x, y + high);
			} else
				g.drawString(s.substring(2, mid + 1), x + wide, y);
		} else {
			g.setFont(sgle);
			if (quarter) {
				if (s.substring(mid).trim().length() == 0)
					s = s.substring(mid) + s.substring(1, mid + 1);
				g.drawString(s.substring(1, mid), x, y);
				g.drawString(s.substring(mid), x, y + high);
			} else
				g.drawString(s, x, y);
		}
	}

	void init(int option) {
		if ((option & 0x01) > 0) {
			rows /= 3;
			bar.setBlockIncrement(rows);
			bar.setValues(0, rows, 0, rows);
			sgle = getFont().deriveFont(AffineTransform.getScaleInstance(2, 1.5));
			dble = getFont().deriveFont(AffineTransform.getScaleInstance(4, 1.5));
		}
		bouncer.init(this, Config.localFile("gif", "BOUNCE.GIF"));
	}

	void stop() {
		bouncer.exit();
	}

	void setBouncer(boolean active) {
		bouncer.enabled = active;
	}

	void setScrollbar(boolean enabled) {
		bar.setEnabled(enabled);
	}
}
