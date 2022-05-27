package com.ncr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Ground {
	Image image;
	Container base;

	Ground(Container c, File f) {
		base = c;
		image = c.getToolkit().getImage(f.getAbsolutePath());
		c.prepareImage(image, null);
		while ((c.checkImage(image, null) & c.ALLBITS) == 0)
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
	}

	public synchronized void paintOn(Component c, Graphics g) {
		if (!c.isShowing())
			return;
		Dimension d = c.getSize();
		Insets i = base.getInsets();
		Point src = c.getLocationOnScreen();
		Point org = base.getLocationOnScreen();
		Color bg = c.getBackground();
		bg = new Color(bg.getRGB()); // EVM can't handle SystemColor
		src.translate(i.left - org.x, i.top - org.y);
		g.drawImage(image, 0, 0, d.width, d.height, src.x, src.y, src.x + d.width, src.y + d.height, bg, null);
	}
}

class Border extends Panel {
	int size;
	boolean raised;
	Ground ground = null;
	static Color color = Color.getColor("COLOR_CONTROLSHADOW", SystemColor.controlShadow);

	static Border around(Component c, int size) {
		Border border = new Border(size);
		border.add(c);
		border.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Component c = ((Border) e.getComponent()).getComponent(0);
				if (c.isEnabled())
					c.dispatchEvent(e);
			}
		});
		return border;
	}

	Border(int size) {
		super(new GridLayout(0, 1));
		raised = size > 0;
		this.size = raised ? size : 0 - size;
	}

	public Insets getInsets() {
		return new Insets(size, size, size, size);
	}

	public void toFront(int ind) {
		while (!getComponent(ind).isVisible())
			((CardLayout) getLayout()).next(this);
	}

	public void paint(Graphics g) {
		if (ground == null) {
			int ind = -1;
			Dimension d = getSize();
			g.setColor(color);
			while (++ind < size)
				g.draw3DRect(ind, ind, --d.width - ind, --d.height - ind, raised);
			if (ind-- > 0) {
				g.drawLine(0, 0, ind, ind);
				g.drawLine(d.width, d.height, d.width + ind, d.height + ind);
			}
		} else
			ground.paintOn(this, g);
		super.paint(g);
	}
}

class Bouncer extends Thread implements Runnable {
	Component base;
	Dimension d;
	Image image, stamp, frame;
	Graphics g, h;
	Rectangle ball = new Rectangle();
	boolean enabled, visible;

	void init(Component c, File f) {
		if (!f.exists())
			return;
		base = c;
		d = c.getSize();
		stamp = c.createImage(d.width, d.height);
		h = stamp.getGraphics();
		h.setColor(Color.white);
		h.setXORMode(h.getColor());
		image = c.getToolkit().getImage(f.getAbsolutePath());
		c.prepareImage(image, null);
		g = c.getGraphics();
		g.setXORMode(c.getBackground());
		setName("Bouncer:" + c.getName());
		start();
	}

	void exit() {
		if (image == null)
			return;
		interrupt();
		try {
			join();
		} catch (InterruptedException e) {
		}
		hide();
		g.dispose();
	}

	void hide() {
		if (!visible)
			return;
		g.drawImage(stamp, 0, 0, null);
		h.drawImage(frame, ball.x, ball.y, null);
		visible = false;
	}

	public void run() {
		int dx = 4, dy = 4;

		while (true)
			try {
				sleep(20);
				if ((base.checkImage(image, null) & base.ALLBITS + base.FRAMEBITS) == 0)
					continue;
				if (frame == null)
					frame = base.createImage(ball.width = image.getWidth(null), ball.height = image.getHeight(null));
				synchronized (base) {
					if (enabled) {
						ball.translate(dx, dy);
						if (dx > 0 && ball.x + ball.width > d.width || dx < 0 && ball.x < 0)
							ball.x += dx = 0 - dx;
						if (dy > 0 && ball.y + ball.height > d.height || dy < 0 && ball.y < 0)
							ball.y += dy = 0 - dy;
						frame.getGraphics().drawImage(image, 0, 0, null);
						h.drawImage(frame, ball.x, ball.y, null);
						g.drawImage(stamp, 0, 0, null);
						if (visible) {
							h.clearRect(0, 0, d.width, d.height);
							h.drawImage(frame, ball.x, ball.y, null);
						} else
							visible = true;
					} else
						hide();
				}
			} catch (InterruptedException e) {
				break;
			}
	}
}

public abstract class Picture extends Canvas {
	public String text;
	public Image image = null;
	public Ground ground = null;
	public int align = Label.CENTER;

	public int getAlignment() {
		return align;
	}

	public void setAlignment(int align) {
		this.align = align;
	}

	public void setEnabled(boolean state) {
		super.setEnabled(state);
		repaint(); /* for JView only */
	}

	public String getText() {
		return text;
	}

	public void setImage(File f) {
		if (image != null) {
			image.flush();
			image = null;
			repaint();
		}
		if (f == null)
			return;
		if (f.exists()) {
			image = getToolkit().getImage(f.getAbsolutePath());
			prepareImage(image, this);
		}
	}

	public synchronized void setText(String s) {
		if (s != null)
			if (s.equals(text))
				return;
		text = s;
		Graphics g = getGraphics();
		if (g != null) {
			paint(g);
			g.dispose();
		}
	}

	public void update(Graphics g) /* MS with imageUpdate only */
	{
		paint(g);
	}
}
