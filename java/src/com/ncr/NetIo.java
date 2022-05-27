package com.ncr;

import java.io.*;
import java.net.*;
import java.util.*;

class NetIo extends LinIo implements Runnable, A2aServer {
	int idle = 0, sts;
	int srv = 0, state = SRV.equals("000") ? 3 : 1;
	boolean newsReset = false;
	byte record[];
	byte x_area[] = new byte[4 + 1024];
	private LinIo asyLine;
	private SeqIo rFile[];
	private StringBuffer mirror;
	private int versionORG, versionDAT;

	private Socket sock;
	private InputStream rcv;
	private OutputStream snd;
	private Thread pump = new Thread(this, project + ":hotpump");
	private Thread care = new Thread(new HotCare(), project + ":hotcare");
	private Thread note = new Thread(new HotNote(), project + ":hotnote");

	static TimeZone tz = TimeZone.getDefault();

	String lanHost(String reg) {
		String id[] = { "SRV", "BUS", "REG" };
		return id[reg.equals(SRV) ? srv : 2] + reg;
	}

	NetIo(String id, int blocks) {
		super(id, 0, 4 + 1024);
		net = this;
		asyLine = new LinIo("ASY", 0, 2 + blocks * (10 + Total.length));
		mirror = new StringBuffer(asyLine.dataLen());
		mirror.append("T:");
	}

	private void open(String reg) {
		if (state != 1)
			return;
		try {
			String sid = lanHost(SRV);
			sock = new Socket(InetAddress.getByName(sid), lanPort(SRV), InetAddress.getLocalHost(), lanPort(reg));
			state = 4;
			error("online to " + sid);
			sock.setSoTimeout(8000);
			sock.setSoLinger(true, 0);
			rcv = sock.getInputStream();
			snd = sock.getOutputStream();
			record = ("!:" + lanHost(reg) + ":" + editNum(versionORG, 4) + ":" + editNum(versionDAT, 4) + ":"
					+ editNum(rFile[0].getSize(), 8) + ":" + editNum(rFile[1].getSize(), 8) + ":00000000" + ":"
					+ editNum(rFile[2].getSize(), 8) + ":" + editNum(rFile[3].getSize(), 8) + ":00000000")
							.getBytes(oem);
			snd.write(record);
			if ((sts = rcv.read(record)) != 58 || record[0] != 0x21)
				throw new SocketException("no reply");
			asyLine.pb = new String(record, asyLine.index = 0, sts, oem);
			int ind = 0;
			rFile[ind].recno = asyLine.skip(2).scanNum(8) + 1; /* IDC */
			rFile[++ind].recno = asyLine.scan(':').scanNum(8) + 1; /* JRN */
			asyLine.scan(':').skip(8); /* MNT */
			rFile[++ind].recno = asyLine.scan(':').scanNum(8) + 1; /* DTL */
			rFile[++ind].recno = asyLine.scan(':').scanNum(8) + 1; /* RCP */
			asyLine.scan(':').skip(8); /* IMP */
			if (rFile[0].getSize() >= rFile[0].recno)
				while (++ind < rFile.length)
					rFile[ind].recno = 1;
			newsReset = rFile[0].recno == 1;
			state = asyLine.scan(':').scanHex(2);
		} catch (IOException e) {
			error(e.toString());
			kill();
			if (state == 1)
				srv ^= 1;
		}
	}

	private int send() {
		if ((state & 5) > 0)
			return ERROR;
		try {
			record = toString(0, index).getBytes(oem);
			int len = record.length;
			if (peek(0) == 'Y' && peek(2) == 'D') {
				System.arraycopy(record, 0, x_area, 0, len);
				snd.write(x_area, 0, len + sts);
			} else
				snd.write(record);
			if (peek(0) == 'X') {
				if ((len = rcv.read(x_area)) < 4)
					throw new SocketException("xcopy data");
				pb = new String(x_area, 0, (peek(2) == 'D' ? 4 : len), oem);
			} else {
				if (rcv.read(record) != len)
					throw new SocketException("record size");
				pb = new String(record, oem);
			}
			if (pb.charAt(index = 0) != peek(0))
				return 0;
			return len;
		} catch (IOException e) {
			error(e.toString());
			kill();
			return ERROR;
		}
	}

	private synchronized void kill() {
		if ((state & 1) > 0)
			return;
		try {
			state = 1;
			sock.close();
		} catch (IOException e) {
			error(e.toString());
		}
	}

	private void error(String msg) {
		System.err.println(new Date());
		System.err.println(id + ":" + msg);
	}

	private synchronized void async(SeqIo io) {
		int recs = 1;
		char id = io.id.charAt(0);

		if (io.read(io.recno) < 1)
			return;
		asyLine.onto(0, id).push(':');
		if (io.fixSize > 0) {
			asyLine.poke(0, 't');
			int key = io.scanKey(4);
			io.skip(io.fixSize - 4);
			for (int ind = 0; io.index < io.pb.length(); ind++) {
				asyLine.push(id).push(editHex(ind, 1)).push(editNum(io.recno, 3)).push(':').push(editKey(key, 4))
						.push(io.scan(Total.length));
			}
		} else
			for (;; recs++) {
				if (id == 'I')
					if (io.pb.charAt(32) == 'Y') {
						String name = io.pb.substring(43, 59).trim();
						if (xferF2f(name, name) < 0)
							logConsole(0, name + " xfer failed", null);
					}
				asyLine.push(io.pb);
				if (asyLine.index + io.record.length > asyLine.dataLen())
					break;
				if (io.read(io.recno + recs) < 1)
					break;
			}
		try {
			record = asyLine.toString(0, asyLine.index).getBytes(oem);
			snd.write(record);
			if (rcv.read(record) < 1 || record[0] != 0x16)
				throw new SocketException("no Ack in pump");
			io.recno += recs;
		} catch (IOException e) {
			error(e.toString());
			kill();
		}
	}

	void start(DatIo lFile[], int org, int dat) {
		versionORG = org;
		versionDAT = dat;
		rFile = new SeqIo[lFile.length];
		for (int ind = 0; ind < rFile.length; ind++) {
			DatIo io = lFile[ind];
			rFile[ind] = new SeqIo(io.id, io.fixSize, io.record.length);
		}
		pump.start();
		care.start();
		note.start();
	}

	boolean isCopied(int recs) {
		return rFile[0].recno > recs;
	}

	public void run() {
		while (true) {
			SeqIo io = null;
			for (open(REG); state == 0; async(io)) {
				int ind = rFile.length;
				while (ind-- > 0) {
					io = rFile[ind];
					if (io.fixSize > 0) {
						if (io.recno == 0)
							continue;
						if (io.getSize() >= io.recno)
							break;
						io.recno = 0;
					} else if (io.getSize() >= io.recno)
						break;
				}
				if (ind < 0)
					break;
				Thread.yield();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				break;
			}
		}
		kill();
	}

	void stop() {
		if (!pump.isAlive())
			return;
		try {
			pump.interrupt();
			pump.join();
			care.interrupt();
			care.join();
			note.interrupt();
			note.join();
		} catch (InterruptedException e) {
			System.out.println(e);
		}
		for (int ind = 0; ind < rFile.length; ind++) {
			rFile[ind].close();
		}
	}

	synchronized int declare(int sel, int ckr, int ac) {
		onto(0, "E:");
		push(editKey(sel, 3)).push(':');
		push(editNum(ckr, 3)).push(':');
		push(editNum(ac, 2));
		if ((sts = send()) < 0)
			return 16;
		return sts == 0 ? 7 : 0;
	}

	synchronized int hotval(String nbr) {
		init(' ').push("P:R:B").push(editTxt(nbr, 19)).skip(58);
		if ((sts = send()) < 1)
			return sts;
		return skip(25).scanNum(1) + 1;
	}

	synchronized int regckr(int sel, int ckr, int ac) {
		onto(0, "C:").push(ac < 3 ? editNum(sel, 3) : editKey(sel, 3));
		push(':').push(editNum(ckr, 3)).push(':').push(editNum(ac, 2));
		if ((sts = send()) < 0)
			return 16;
		return sts == 0 ? 7 : 0;
	}

	public synchronized int readSeq(int rec, int sel, LinIo io) {
		init(' ').push("R:").push(io.id.charAt(0)).push(editNum(rec, 8)).push(':').push(editKey(sel, 3)).push(':')
				.skip(io.dataLen());
		if ((sts = send()) < 1)
			return sts;
		io.pb = skip(16).scan(sts - 16);
		io.index = 0;
		return io.pb.length();
	}

	synchronized int findTra(int nbr, int sel) {
		onto(0, "R:T").push(editNum(nbr, 4)).push(':').push(editKey(sel, 3)).push(':').push(editNum(0, 8));
		if ((sts = send()) < 0)
			return sts;
		return skip(12).scanNum(8);
	}

	synchronized int readMnt(char id, int rec, LinIo io) {
		init(' ').push("M:").push(id).push(editNum(rec, 8)).push(':').skip(io.dataLen());
		if ((sts = send()) < 0)
			return sts;
		io.recno = skip(3).scanNum(8);
		io.pb = skip().scan(io.dataLen());
		io.index = 0;
		return sts > 0 ? io.pb.length() : 0;
	}

	public synchronized int readHsh(char type, String key, LinIo io) {
		init(' ').push("P:").push(type).push(':').push(key);
		index = 4 + io.dataLen();
		if ((sts = send()) < 1)
			return sts;
		io.pb = skip(4).scan(sts - 4);
		io.index = 0;
		return io.pb.length();
	}

	public synchronized int readSls(int rec, int sel, LinIo io) {
		init(' ').push("R:").push(io.id.charAt(0)).push(editNum(rec, 4));
		push(':').push(editKey(sel, 3)).push(':').skip(io.dataLen());
		if ((sts = send()) < 1)
			return sts;
		io.pb = skip(12).scan(sts - 12);
		io.index = 0;
		return io.pb.length();
	}

	public synchronized void writeSls(int rec, int blk, SlsIo io) {
		int len = mirror.length();
		if (len == asyLine.dataLen() || rec == 0 && len > 2) {
			onto(0, mirror.toString());
			if (state == 0)
				sts = send();
			mirror.setLength(2);
		}
		if (rec == 0)
			return;
		mirror.append(io.id.substring(0, 1) + editHex(blk, 1)).append(editNum(rec, 3)).append(':')
				.append(editKey(io.key, 4)).append(io.toString(0, io.index));
	}

	public synchronized int updNews(int rec, LinIo io) {
		onto(0, "U:").push(io.id.charAt(0));
		push(editNum(rec, 8)).push(":000:").push(io.toString());
		if ((sts = send()) < 1)
			return sts;
		io.recno = skip(3).scanNum(8);
		return io.dataLen();
	}

	synchronized long mnyget(int ckr, int ind, long money) {
		init(' ').push("R:M" + editNum(ind + 2, 4)).push(':');
		push(editNum(ckr, 3)).push(':').skip(8 + Total.length * 8);
		if (send() < 1)
			return money;
		money = 0;
		skip(20);
		for (int sc = 0; ++sc < 8;) {
			Total tb = new Total();
			tb.scan(this);
			if (sc > 3 && sc < 7)
				continue;
			money += sc == 3 ? -tb.total : tb.total;
		}
		return money;
	}

	synchronized int eodPoll(int sel) {
		if (declare(sel, 0, 0) > 0)
			return 0;
		return skip(6).scanNum(3);
	}

	synchronized int copyF2f(String pathName, String local, boolean remove) {
		File f = new File(localPath(local)); // long utim;

		onto(0, "X:F:").push(pathName);
		if ((sts = send()) < 1)
			return sts < 0 ? 16 : ERROR;
		try {
			FileOutputStream file = new FileOutputStream(f);
			do {
				onto(0, "X:D:");
				if ((sts = send()) < 1) {
					file.close();
					return 16;
				}
				file.write(x_area, 4, sts -= 4);
			} while (sts == 1024);
			file.close();
			if (remove) {
				onto(0, "X:R:").push(pathName);
				if ((sts = send()) < 1)
					return sts < 0 ? 16 : 7;
			}
		} catch (IOException e) {
			error(e, true);
		}
		return 0;
	}

	synchronized int xferF2f(String pathName, String local) {
		File f = new File(localPath(local));

		try {
			FileInputStream file = new FileInputStream(f);
			onto(0, "Y:O:").push(editNum((int) f.length(), 10)).push(':').push(pathName);
			if ((sts = send()) < 1) {
				file.close();
				return sts < 0 ? 16 : ERROR;
			}
			while ((sts = file.read(x_area, 4, 1024)) > 0) {
				onto(0, "Y:D:");
				if ((sts = send()) < 1) {
					file.close();
					return sts < 0 ? 16 : ERROR;
				}
			}
			file.close();
			int gmt = (int) ((f.lastModified() + tz.getRawOffset()) / 1000);
			if (tz.inDaylightTime(new Date()))
				gmt += 3600;
			onto(0, "Y:C:").push(editNum(gmt + 28800, 10)) /* pacific std */
					.push(':').push(pathName);
			if ((sts = send()) < 1)
				return sts < 0 ? 16 : ERROR;
		} catch (IOException e) {
			error(e, false);
			return 15;
		}
		return 0;
	}
}
