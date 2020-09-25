package com.ncr; /*******************************************************************/
/*                                                                 */
/*     module name: hashl.c            creation date 04/dec/91     */
/*     author name: h. bufe            ncr augsburg ssd-retail     */
/*                                                                 */
/*     apply plu maintenance from pos server (hsh_lmnt)            */
/*     main module                                                 */
/*                                                                 */
/*******************************************************************/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/*******************************************************************/

class LanIo extends LinIo implements A2aServer {
	int srv = 0, sts;
	int state = SRV.equals("000") ? 3 : 1;
	byte record[];

	Socket sock;
	InputStream rcv;
	OutputStream snd;
	Label lbl = new Label(lanHost(REG) + " <------- " + lanHost(SRV), Label.CENTER);

	String lanHost(String reg) {
		String id[] = { "SRV", "BUS", "REG" };
		return id[reg.equals(SRV) ? srv : 2] + reg;
	}

	LanIo(String id, int hdrSize, int recSize) {
		super(id, 0, hdrSize + recSize);
	}

	void open(String reg) {
		if (state != 1)
			return;
		try {
			String sid = lanHost(SRV);
			sock = new Socket(InetAddress.getByName(sid), lanPort(SRV), InetAddress.getLocalHost(), lanPort(reg));
			state = 0;
			error(new SocketException("online"));
			sock.setSoTimeout(8000);
			sock.setSoLinger(true, 0);
			rcv = sock.getInputStream();
			snd = sock.getOutputStream();
			record = (".:" + lanHost(reg)).getBytes(oem);
			snd.write(record);
			if (rcv.read(record) < 1 || record[0] != 0x2e)
				throw new SocketException("no reply");
		} catch (IOException e) {
			kill();
			error(e);
			if (state == 1)
				srv ^= 1;
		}
	}

	int send() {
		if ((state & 1) > 0)
			return -1;
		try {
			record = toString(0, index).getBytes(oem);
			snd.write(record);
			if (rcv.read(record) != record.length)
				throw new SocketException("record size");
			pb = new String(record, oem);
			if (pb.charAt(index = 0) != peek(0))
				return 0;
			return record.length;
		} catch (IOException e) {
			kill();
			error(e);
			return ERROR;
		}
	}

	void kill() {
		if ((state & 1) > 0)
			return;
		try {
			state = 1;
			sock.close();
		} catch (IOException e) {
			error(e);
		}
	}

	void error(Exception e) {
		Color bg = state > 0 ? Color.yellow : Color.green;
		String s = lbl.getText();

		lbl.setBackground(bg);
		lbl.setText(s.substring(0, 16) + lanHost(SRV));
		System.err.println(e);
	}

	int regckr(int sel, int ckr, int ac) {
		onto(0, "C:").push(editKey(sel, 3));
		push(':').push(editNum(ckr, 3)).push(':').push(editNum(ac, 2));
		return sts = send();
	}

	public int readSeq(int rec, int sel, LinIo io) {
		init(' ').push("R:").push(io.id.charAt(0)).push(editNum(rec, 8)).push(':').push(editKey(sel, 3)).push(':')
				.skip(io.dataLen());
		if ((sts = send()) < 1)
			return sts;
		io.pb = skip(16).scan(sts - 16);
		io.index = 0;
		return io.pb.length();
	}

	int readMnt(char id, int rec) {
		init(' ').push("M:").push(id);
		push(editNum(rec, 8)).push(':').skip(dataLen() - index);
		if ((sts = send()) < 0)
			return sts;
		recno = skip(3).scanNum(8);
		pb = pb.substring(index + 1);
		index = 0;
		return sts > 0 ? pb.length() : 0;
	}

	public int readHsh(char type, String key, LinIo io) {
		init(' ').push("P:").push(type).push(':').push(key);
		index = 4 + io.dataLen();
		if ((sts = send()) < 1)
			return sts;
		io.pb = skip(4).scan(sts - 4);
		io.index = 0;
		return io.pb.length();
	}

	public int readSls(int rec, int sel, LinIo io) {
		init(' ').push("R:").push(io.id.charAt(0)).push(editNum(rec, 4));
		push(':').push(editKey(sel, 3)).push(':').skip(io.dataLen());
		if ((sts = send()) < 1)
			return sts;
		io.pb = skip(12).scan(sts - 12);
		io.index = 0;
		return io.pb.length();
	}

	public void writeSls(int rec, int blk, SlsIo io) {
		return;
	}

	public int updNews(int rec, LinIo io) {
		return 0;
	}
}

class LanMaintPanel extends Panel implements Runnable {
	int abort = 0; /* 0 = forever, 1 = runonce, -1 = aborted */
	Thread a2a = new Thread(this);

	LanIo net = new LanIo("MNT", 12, 88);
	HshIo lPLU = new HshIo("PLU", 16, 80);
	HshIo lCLU = new HshIo("CLU", 16, 80);
	HshIo lGLU = new HshIo("GLU", 16, 80);

	Button b1 = new Button("++++++++");
	Button b2 = new Button("00000000");
	Button b3 = new Button("--------");

	LanMaintPanel() {
		super(new BorderLayout());

		Panel pnl = new Panel(new GridLayout());
		pnl.add(b1);
		pnl.add(b2);
		pnl.add(b3);
		add(net.lbl, BorderLayout.NORTH);
		add(pnl, BorderLayout.CENTER);
		setEnabled(false);
		setFont(Config.getFont(Font.BOLD, 23));
	}

	public void run() {
		int sts;
		int mnt_add = 0, mnt_chg = 0, mnt_del = 0;
		int reg_nbr = Integer.parseInt(DatIo.REG, 16);
		int grp_nbr = Integer.parseInt(DatIo.GRP, 16) + 0x0F00;
		int sto_nbr = Integer.parseInt(DatIo.STO, 10);

		while (true) {
			net.open(DatIo.REG);
			for (net.recno = 0; net.state == 0; net.recno++) {
				if (net.readMnt('A', net.recno) < 1)
					break;
				if (abort < 0)
					break;
				try /* validate 10 bytes header */
				{
					int nbr = net.skip().scanNum(4);
					if (nbr > 0 && nbr != sto_nbr)
						continue;
					nbr = net.scan(':').scanKey(3);
					if (nbr != reg_nbr && nbr != grp_nbr)
						if (nbr > 0)
							continue;
					net.scan(':');
				} catch (NumberFormatException e) {
					net.error(e, false);
					continue;
				}
				char id = net.pb.charAt(net.index);
				if (id == '*') {
					net.logConsole(2, "hot", net.pb);
					continue;
				}
				HshIo io = id == 'C' ? lCLU : lPLU;
				if (id == 'G')
					io = lGLU;
				String data = net.pb.substring(net.index);
				if (data.charAt(io.fixSize - 1) < '0')
					continue;
				if ((sts = io.find(net.scan(io.fixSize))) < 0)
					continue;
				if (net.scan() != '-') {
					io.push(data);
					io.rewrite(io.recno, 0);
					if (sts == 0)
						show(b1, ++mnt_add);
					else
						show(b2, ++mnt_chg);
				} else if (sts > 0) {
					io.delete(io.recno);
					show(b3, ++mnt_del);
				}
			}
			if (abort > 0)
				if (net.state == 0)
					break;
			if (net.recno > 0) {
				lPLU.sync();
				lCLU.sync();
				lGLU.sync();
			}
			try {
				a2a.sleep(5000);
			} catch (InterruptedException e) {
				break;
			}
		}
		net.kill();
		lPLU.close();
		lCLU.close();
		if (abort > 0)
			System.exit(0);
	}

	void show(Button b, int value) {
		char prefix = b.getLabel().charAt(0);
		b.setLabel(FmtIo.leftFill(Integer.toString(value), 8, prefix));
	}

	void stop() {
		try {
			abort = -1;
			a2a.interrupt();
			a2a.join();
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}
}

public class LanMaint {
	static LanMaintPanel pnl = new LanMaintPanel();

	public static void main(String args[]) {
		Frame f = new Frame("LAN maintenance");

		if (args.length < 1)
			pnl.abort = 1;
		f.add(pnl, BorderLayout.CENTER);
		f.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				pnl.a2a.start();
			}

			public void windowClosing(WindowEvent e) {
				pnl.stop();
				System.exit(1);
			}
		});
		f.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == e.VK_ESCAPE || code == e.VK_F4 && e.isShiftDown()) {
					pnl.stop();
					System.exit(0);
				}
			}
		});
		f.setLocation(200, 200);
		f.pack();
		f.show();
		f.toFront(); /* SUN: after early kbrd input */
	}
}
