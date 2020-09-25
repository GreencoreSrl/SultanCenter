package com.ncr;

import java.io.*;
import java.net.*;
import java.util.*;

class SafePay extends BcrIo implements Runnable {
	Message packet = new Message(256);
	Message pollev = new Message(256);
	byte snd_frame[] = new byte[packet.data.length + 6];
	byte snd_seqn = 0;
	static final int polynomial = 0x8005;

	private Thread poll = new Thread(this, project + ":hotcash");
	private Socket sock = null;
	private String codepage = "Cp1252";

	class Message {
		byte data[];
		int index;

		Message(int size) {
			data = new byte[size];
		}

		Message init(char type) {
			index = 0;
			return pushHex(type, 1);
		}

		Message pushHex(int val, int len) {
			int ind = index;
			for (index += len; len-- > 0; val >>= 8) {
				data[ind + len] = (byte) val;
			}
			return this;
		}

		Message push(String s) {
			byte b[] = null;
			if (codepage != null)
				try {
					b = s.getBytes(codepage);
				} catch (IOException e) {
				}
			if (b == null)
				b = s.getBytes();
			for (int ind = 0; ind < s.length(); data[index++] = b[ind++])
				;
			data[index++] = 0;
			return this;
		}

		int scanHex(int len) {
			int val = data[index++];
			while (--len > 0) {
				val <<= 8;
				val += data[index++] & 0xff;
			}
			return val;
		}

		String scan(int len) {
			index += len;
			if (codepage != null)
				try {
					return new String(data, index - len, len - 1, codepage);
				} catch (IOException e) {
					System.err.println(codepage + "not found");
				}
			return new String(data, index - len, len - 1);
		}
	}

	public void run() {
		while (true) {
			for (open(); sock != null; Thread.yield()) {
				pollev.init('R').pushHex(16, 2);
				if (snd_packet(pollev.data, pollev.index) < 0)
					break;
				pollev.index = 0;
				int type = pollev.scanHex(1);
				if (type == 'X')
					if (pollev.scanHex(4) == 0)
						continue;
				if (type == 'W' && pollev.scanHex(2) == 16) {
					deposit = pollev.scanHex(4);
				} else
					deposit = 0;
				break;
			}
			try {
				Thread.sleep(sock == null ? 5000 : 1000);
			} catch (InterruptedException e) {
				break;
			}
		}
		kill();
	}

	private void open() {
		if (sock != null)
			return;
		try {
			sock = new Socket(InetAddress.getByName(dev.name), 20703);
			error("online to " + dev.name);
			sock.setSoTimeout(2000);
			sock.setSoLinger(true, 0);
		} catch (IOException e) {
			error(e.toString());
			kill();
		}
	}

	private synchronized int send(int len) {
		if (sock == null)
			return ERROR;
		try {
			bcr_log(2, snd_frame, len);
			sock.getOutputStream().write(snd_frame, 0, len);
			if ((len = sock.getInputStream().read(snd_frame)) < 6) {
				throw new SocketException("no response");
			}
			bcr_log(1, snd_frame, len);
			return len;
		} catch (IOException e) {
			error(e.toString());
			kill();
			bcr_log(0, new byte[] { -1, -1 }, 2);
			return ERROR;
		}
	}

	private synchronized void kill() {
		if (sock != null)
			try {
				sock.close();
			} catch (IOException e) {
				error(e.toString());
			}
		sock = null;
		deposit = 0;
	}

	private void error(String msg) {
		System.err.println(new Date());
		System.err.println(dev.id + ":" + msg);
	}

	/*******************************************************************/

	private static int crc_16(byte chr, int crc) {
		crc ^= chr << 8;
		for (int bits = 8; bits-- > 0;) {
			crc <<= 1;
			if ((crc & 0x10000) > 0)
				crc ^= polynomial;
		}
		return crc & 0xffff; /* big endian */
	}

	private int snd_byte(byte chr, int ind) {
		if (chr == 0x02 || chr == 0x03 || chr == 0x10)
			snd_frame[ind++] = 0x10;
		snd_frame[ind++] = chr;
		return ind;
	}

	private int snd_error(String msg) {
		logConsole(0, dev.id + ": protocol error", msg);
		return -1;
	}

	private synchronized int snd_packet(byte msg[], int len) {
		int crc = 0, ind = 0;
		byte chr = snd_seqn;

		crc = crc_16(snd_frame[ind++] = 0x02, crc);
		crc = crc_16(snd_frame[ind++] = 0x05, crc);
		crc = crc_16(snd_frame[ind++] = chr, crc);
		for (int i = 0; i < len; crc = crc_16(chr, crc)) {
			ind = snd_byte(chr = msg[i++], ind);
		}
		ind = snd_byte((byte) (crc >> 8), ind);
		ind = snd_byte((byte) crc, ind);
		snd_frame[ind++] = 0x03;
		snd_seqn ^= 1;
		if ((len = send(ind)) < 1)
			return -1;
		return rcv_packet(msg, len);
	}

	private int rcv_packet(byte msg[], int len) {
		int crc = 0, size = 0;

		byte chr = snd_frame[0];
		for (int ind = 0; ind < len - 1; chr = snd_frame[++ind]) {
			if (chr == 0x10)
				chr = snd_frame[++ind];
			if (ind == 0)
				if (chr != 0x02)
					return -1;
			if (ind == 1)
				if (chr != 0x06)
					return -1;
			if (ind == 2)
				if (chr != snd_seqn)
					return -1;
			if (ind > 2)
				msg[size++] = chr;
			crc = crc_16(chr, crc);
		}
		if (chr != 0x03 || crc > 0)
			return -1;
		return size - 2;
	}

	int getProperty(int idx) {
		while (true) {
			packet.init('R');
			packet.pushHex(idx, 2);
			int sts = snd_packet(packet.data, packet.index);
			if (sts < 0)
				return sts;
			packet.index = 0;
			int type = packet.scanHex(1);
			if (type == 'X') {
				if ((bcr_err = packet.scanHex(4)) == 0)
					continue;
				return 0;
			}
			if (type != 'W' || packet.scanHex(2) != idx)
				return snd_error("Read Property " + idx);
			return sts - 3;
		}
	}

	int setProperty(int idx, int len, Object obj) {
		while (true) {
			packet.init('W');
			packet.pushHex(idx, 2);
			if (len == 0)
				packet.push((String) obj);
			else
				packet.pushHex(((Integer) obj).intValue(), len);
			int sts = snd_packet(packet.data, len = packet.index);
			if (sts < 0)
				return sts;
			if (sts == 0)
				return len - 3;
			packet.index = 0;
			if (packet.scanHex(1) == 'X') {
				if ((bcr_err = packet.scanHex(4)) == 0)
					continue;
				return 0;
			}
			return snd_error("Write Property " + idx);
		}
	}

	int exeCommand(int idx) {
		while (true) {
			packet.init('C');
			packet.pushHex(idx, 2);
			int sts = snd_packet(packet.data, packet.index);
			if (sts < 0)
				return sts;
			packet.index = 0;
			if (packet.scanHex(1) == 'X') {
				if ((bcr_err = packet.scanHex(4)) == 3)
					continue;
				return bcr_err == 0 ? 3 : 0;
			}
			return snd_error("Command Request " + idx);
		}
	}

	/***************************************************************************
	 * open communications and log
	 ***************************************************************************/
	void start() {
		super.start();
		poll.start();
	}

	/***************************************************************************
	 * close communications and log
	 ***************************************************************************/
	void stop() {
		super.stop();
		if (!poll.isAlive())
			return;
		try {
			poll.interrupt();
			poll.join();
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}

	/***************************************************************************
	 * open operator at BCR device
	 ***************************************************************************/
	void ckrOpen() {
		int sts;
		if (dev.version == 0)
			return;
		while (dev.state > 0) {
			sts = exeCommand(6); /* EndDeposit */
			if (sts >= 0)
				sts = exeCommand(8); /* OpenSP */
			if (sts >= 0)
				dev.state = 0;
			else if (comError(sts, true) > 1)
				return;
		}
		sts = setProperty(17, 0, editNum(ctl.ckr_nbr, 3) + ":" + lCTL.text);
		if (sts > 0)
			sts = setProperty(18, 0, editNum(ctl.cent, 2) + editNum(ctl.date, 6) + editNum(ctl.time, 6));
		if (sts > 0)
			sts = exeCommand(7);
		endOrder(sts);
	}

	/***************************************************************************
	 * terminate session with BCR device
	 ***************************************************************************/
	void ckrClose() {
		if (dev.state > 0)
			return;
		exeCommand(6); /* EndDeposit */
		endOrder(exeCommand(8)); /* CloseSP */
		dev.state = 2;
	}

	/***************************************************************************
	 * inform BCR device about end of transaction
	 ***************************************************************************/
	void endTrans() {
		if (dev.state > 0)
			return;
		exeCommand(6); /* EndDeposit */
		int len, sts = exeCommand(4); /* BeginDeposit */
		if (exeCommand(9) > 0) /* CheckHealth */
		{
			while ((len = getProperty(14)) > 0) {
				String text = packet.scan(len);
				if (text.length() < 1)
					break;
				gui.clearLink(text, 0x11);
			}
		}
		if (sts == 0)
			sts = exeCommand(4); /* retry BeginDeposit */
		endOrder(sts);
	}

	/***************************************************************************
	 * fill cashcount array
	 ***************************************************************************/
	int cashcount() {
		if (sock == null)
			return -1;
		CshDenom ptr[] = tnd[bcr_tnd].dnom;
		int ind = dnom_tbl.length, top;
		int sts = exeCommand(3); /* ReadCashCount */
		if (sts > 0)
			sts = getProperty(9);
		if (sts < 1)
			return sts;
		for (stock = 0; ind > 0; dnom_tbl[--ind] = 0)
			;
		String pairs = packet.scan(sts).replace(';', ',');
		for (sts = pairs.length(); sts > 0; sts = top) {
			top = pairs.lastIndexOf(':', sts - 1);
			int cnt = Integer.parseInt(pairs.substring(top + 1, sts));
			top = pairs.lastIndexOf(',', sts = top);
			int val = Integer.parseInt(pairs.substring(top + 1, sts));
			stock += cnt * (long) val;
			for (ind = 0; ind < dnom_tbl.length; ind++) {
				if (val == ptr[ind].value)
					dnom_tbl[ind] += cnt;
			}
		}
		return bcr_tnd;
	}

	/***************************************************************************
	 * freeze deposit dispense change
	 *
	 * @param value
	 *            amount to pay (back if < 0)
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int payOrder(long value) {
		if (dev.state > 0)
			return 7;

		long back = 0 - value;
		int sts = exeCommand(5); /* FixDeposit */
		if (sts > 0)
			sts = getProperty(16);
		if (sts > 0) {
			back += packet.scanHex(4);
			sts = exeCommand(6); /* EndDeposit */
		}
		if (sts < 1)
			return endOrder(sts);

		int type = value < 0 ? 1 : 0;
		if (tra.mode == 0)
			type = 2;
		else if (signOf(tra.bal) != signOf(value))
			type = 5;
		sts = setProperty(24, 2, new Integer(type));
		if (sts > 0)
			sts = setProperty(15, 4, new Integer((int) back));
		if (sts > 0)
			sts = exeCommand(2); /* DispenseAmount */
		if (sts > 0)
			sts = getProperty(15);
		if (sts > 0) {
			itm.pov = back - packet.scanHex(4);
			sts = exeCommand(4); /* BeginDeposit */
		}
		return endOrder(sts);
	}

	/***************************************************************************
	 * service menu
	 * 
	 * @param sc
	 *            subcode (1-9)
	 * @return integer result (0=success, >0=error message nbr)
	 ***************************************************************************/
	int srvFuncs(int sc) {
		if (dev.version == 0)
			return 7;
		int sts = 0;
		if (sc == 1 || sc == 2) {
			exeCommand(6); /* EndDeposit */
			sts = setProperty(19, 2, new Integer(sc - 1)); /* SelectCassette */
		}
		if (sc >= 3 && sc <= 6) {
			if ((sts = cashcount()) > 0) {
				long delta = 0 - stock;
				sts = setProperty(21, 2, new Integer(sc - 3)); /* DumpMode */
				if (sts > 0)
					sts = exeCommand(12); /* DumpNotes */
				if (sts > 0)
					if ((sts = cashcount()) > 0) {
						tra.amt += tnd[bcr_tnd].fc2hc(delta += stock);
						prtLine.upto(40, editMoney(bcr_tnd, delta));
					}
			}
		}
		if (sc == 7)
			sts = exeCommand(10); /* ReleaseCassette */
		if (sc == 8)
			sts = exeCommand(13); /* LatchCassette */
		if (sc == 9) {
			sts = setProperty(20, 2, new Integer(1));
			if (sts > 0)
				sts = exeCommand(11); /* ReleaseUnitCover */
		}
		return endOrder(sts);
	}
}
