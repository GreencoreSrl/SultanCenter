package com.ncr;

import java.awt.*;

class GdLabel extends Picture {
	static final int STYLE_RAISED = 1;
	static final int STYLE_STATUS = 2;
	static final int STYLE_WINDOW = 3;
	static final int STYLE_HEADER = 4;

	private int style;
	private Point pad = new Point(7, 3);

	static Color colorMenu = Color.getColor("COLOR_MENU", SystemColor.menu);
	static Color colorMenuText = Color.getColor("COLOR_MENUTEXT", SystemColor.menuText);
	static Color colorWindow = Color.getColor("COLOR_WINDOW", SystemColor.window);
	static Color colorWindowText = Color.getColor("COLOR_WINDOWTEXT", SystemColor.windowText);
	static Color colorControl = Color.getColor("COLOR_CONTROL", SystemColor.control);
	static Color colorControlText = Color.getColor("COLOR_CONTROLTEXT", SystemColor.controlText);
	static Color colorActiveCaption = Color.getColor("COLOR_ACTIVECAPTION", SystemColor.activeCaption);
	static Color colorActiveCaptionText = Color.getColor("COLOR_ACTIVECAPTIONTEXT", SystemColor.activeCaptionText);
	static Color colorInactiveCaption = Color.getColor("COLOR_INACTIVECAPTION", SystemColor.inactiveCaption);
	static Color colorInactiveCaptionText = Color.getColor("COLOR_INACTIVECAPTIONTEXT",
			SystemColor.inactiveCaptionText);

	GdLabel(String text, int style) {
		this.style = style;
		setAlerted(false);
		setText(text);
	}

	Dimension getCharSize() {
		Font f = getFont();
		FontMetrics fm = getFontMetrics(f);
		return new Dimension(fm.charWidth(' '), f.getSize());
	}

	public Dimension getPreferredSize() {
		Dimension d = getCharSize();
		if (text != null)
			d.width *= text.length();
		d.height += d.height >> 3;
		d.width += pad.x << 1;
		d.height += pad.y << 1;
		return d;
	}

	void setAlerted(boolean state) {
		Color fg = getForeground(), bg = getBackground();

		if (style == STYLE_RAISED) {
			fg = colorInactiveCaptionText;
			bg = state ? Color.red : colorInactiveCaption;
		}
		if (style == STYLE_STATUS) {
			fg = colorControlText;
			bg = state ? Color.yellow : colorControl;
		}
		if (style == STYLE_WINDOW) {
			fg = colorWindowText;
			bg = state ? Color.green : colorWindow;
		}
		if (style == STYLE_HEADER) {
			fg = colorMenuText;
			bg = state ? Color.orange : colorMenu;
		}
		setForeground(fg);
		setBackground(bg);
	}

	void setPicture(String name) {
		if (name == null)
			setImage(null);
		else
			setImage(Config.localFile("gif", name + ".GIF"));
	}

	public void paint(Graphics g) {
		Dimension d = getSize();
		Color bg = getBackground();

		if (image != null) {
			if ((checkImage(image, this) & ALLBITS + FRAMEBITS) == 0)
				return;
			bg = new Color(bg.getRGB()); // EVM can't handle SystemColor
			g.drawImage(image, 0, 0, d.width, d.height, bg, null);
		} else if (ground != null)
			ground.paintOn(this, g);
		else
			g.clearRect(0, 0, d.width, d.height);
		if (text == null)
			return;

		Dimension chr = getCharSize();
		int x = pad.x;
		int y = (d.height >> 1) + (chr.height >> 2);
		int len = d.width - chr.width * text.length();
		if (align == Label.CENTER)
			x = len >> 1;
		else if (align == Label.RIGHT)
			x = len - x;
		g.setColor(getForeground());
		if (!isEnabled()) {
			g.setColor(bg.darker());
			g.drawString(text, x + 1, y + 1);
			g.setColor(bg.brighter());
		}
		g.drawString(text, x, y);
	}
}
