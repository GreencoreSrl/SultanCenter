package com.ncr;

import java.awt.*;
import java.awt.event.*;

abstract class UnDeco {
	static void show(final GdPos panel, int width, int height) {
		Frame f = panel.frame;
		Container c = panel.getParent(); /* HardkeyGroup */

		Window w = new Window(f);
		w.add(c == null ? panel : c);
		w.setSize(width, height);
		w.setBackground(Color.getColor("COLOR_DESKTOP", SystemColor.desktop));
		w.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				panel.eventInit();
			}
		});
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				panel.eventStop(1);
			}
		});
		f.setVisible(true);
		w.setVisible(true);
		f.toFront(); /* SUN: after early kbrd input */
		Config.logConsole(1, null, "window " + width + "x" + height);
	}
}
