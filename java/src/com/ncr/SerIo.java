package com.ncr;

import java.io.*;
import javax.comm.*;

class Device extends FmtIo {
	String id, name, protocol = null;
	int state = 0, baud = 9600, version = 0;

	Device(String env) {
		name = System.getProperty(id = env);
		if (name == null)
			return;
		logConsole(1, null, id + "=" + name);
		int ind = name.lastIndexOf(':');
		if (ind < 0)
			return;
		String args = name.substring(ind + 1);
		name = name.substring(0, ind);
		if ((ind = args.indexOf('@')) > 0) {
			baud = Integer.parseInt(args.substring(ind + 1));
			args = args.substring(0, ind);
		}
		if ((ind = args.indexOf('/')) > 0) {
			protocol = args.substring(ind + 1);
			args = args.substring(0, ind);
		}
		version = Integer.parseInt(args);
	}

	void error(Exception e) {
		logConsole(0, id + ':' + e.getMessage(), null);
	}

	byte bcc(byte data[], int len) {
		byte bcc = 0;

		for (int ind = 0; ind < len; bcc ^= data[ind++])
			;
		return bcc;
	}

	int read(SerialPort port, byte data[], int len, int timo) {
		try {
			InputStream in = port.getInputStream();
			for (timo *= 10; in.available() < len; Thread.sleep(100)) {
				if (timo-- < 1)
					throw new InterruptedException();
			}
			if (in.read(data, 0, len) < len)
				return -105;
		} catch (InterruptedException e) {
			return -105;
		} catch (IOException e) {
			error(e);
			return -191;
		}
		return 0;
	}

	int write(SerialPort port, byte data[], int len) {
		try {
			InputStream in = port.getInputStream();
			if (in != null)
				in.skip(in.available());
			port.getOutputStream().write(data, 0, len);
		} catch (IOException e) {
			error(e);
			return -199;
		}
		return 0;
	}

	SerialPort open(int baudRate, int dataBits, int stopBits, int parity) throws Exception {
		CommPortIdentifier cid = CommPortIdentifier.getPortIdentifier(name);
		SerialPort port = (SerialPort) cid.open("PoS", 200);
		port.setFlowControlMode(port.FLOWCONTROL_NONE);
		port.setSerialPortParams(baudRate, dataBits, stopBits, parity);
		port.setDTR(true);
		return port;
	}

	Object loadProtocol() {
		if (protocol != null)
			try {
				return Class.forName(protocol).newInstance();
			} catch (Exception e) {
				System.out.println(e);
			}
		return null;
	}

	static void postInput(String cmd, byte[] data) {
		if (data != null)
			cmd += new String(data);
		gui.postAction(cmd);
	}
}
