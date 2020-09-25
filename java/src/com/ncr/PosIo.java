package com.ncr;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import jpos.*; // JavaPOS generics
import jpos.events.*; // JavaPOS events

abstract class PosIo extends FmtIo implements JposConst, ScannerConst {
	static int codePage = oem == null ? 0 : Integer.parseInt(oem.substring(2));

	static String[] sdtNCR = { "A", "E", "FF", "F", "B1", "B2", "B3", "]C1", "]e0", "]e0", };
	static final int sdtJPOS[] = { SCAN_SDT_UPCA, SCAN_SDT_UPCE, SCAN_SDT_EAN8, SCAN_SDT_EAN13, SCAN_SDT_Code39,
			SCAN_SDT_ITF, SCAN_SDT_Code128, SCAN_SDT_EAN128, SCAN_SDT_RSS14, SCAN_SDT_RSS_EXPANDED, };

	static String barcodeId(int jposId) {
		for (int ind = sdtJPOS.length; ind-- > 0;)
			if (sdtJPOS[ind] == jposId)
				return sdtNCR[ind];
		return "";
	}

	static void jposError(JposException je, Object co) {
		int code = je.getErrorCode();

		if (code == JPOS_E_NOSERVICE)
			return;
		logConsole(0, co.getClass().getName(), je.getMessage());
	}

	static boolean jposActive(BaseControl co) {
		if (co == null)
			return false;
		return co.getState() != JPOS_S_CLOSED;
	}

	static void jposEnable(BaseControl co) throws JposException {
		int version = co.getDeviceServiceVersion();
		co.setDeviceEnabled(true);
		String s = " Version " + version / 1000000 + "." + version / 1000 % 1000 + "." + version % 1000;
		logConsole(1, null, co.getDeviceServiceDescription() + s);
	}

	static String jposOemCode(String uniCode) {
		byte oem[] = oemBytes(uniCode); /* perfect output data */
		StringBuffer pseudo = new StringBuffer(oem.length);
		for (int ind = 0; ind < oem.length; pseudo.append((char) (oem[ind++] & 0xff)))
			;
		uniCode = pseudo.toString(); /* NCRJavaPos is a UniCode pretender */
		return uniCode;
	}

	static void jposOpen(String name, BaseControl co, boolean claim) throws JposException {
		co.open(name);
		while (true) {
			try {
				if (claim)
					if (!co.getClaimed())
						co.claim(0);
				jposEnable(co);
				break;
			} catch (JposException je) {
				if (je.getErrorCode() == JPOS_E_NOTCLAIMED) {
					if (gui.clearLink("Claim problem " + name, 5) < 2)
						continue;
				} else if (gui.clearLink("Enable failed " + name, 5) < 2)
					continue;
				jposClose(co);
				throw je;
			}
		}
	}

	static void jposClose(BaseControl co) {
		if (!jposActive(co))
			return;
		try {
			co.close();
		} catch (JposException je) {
			jposError(je, co);
		}
	}
}

class CusIo extends PosIo {
	LineDisplay co;

	CusIo(int nbr) {
		try {
			co = new LineDisplay();
			jposOpen("LineDisplay." + nbr, co, true);
			if (codePage > 0)
				co.setCharacterSet(codePage);
		} catch (JposException je) {
			jposError(je, co);
		}
		clear();
	}

	void clear() {
		if (!jposActive(co))
			return;
		try {
			co.clearText();
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	void stop() {
		clear();
		jposClose(co);
	}

	void blink(int lamp, int mode) {
		if (!jposActive(co))
			return;
		try {
			if (co.getCapDescriptors())
				co.setDescriptor(lamp, mode);
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	void write(int line, String data) {
		if (!jposActive(co))
			return;
		try {
			if (line >= co.getDeviceRows())
				return;
			co.displayTextAt(line, 0, jposOemCode(data), 0);
		} catch (JposException je) {
			jposError(je, co);
		}
	}
}

class RdrIo extends PosIo implements DataListener, ErrorListener {
	String dev;
	Scanner co;
	static int alert = 0;
	static Device scale = null;

	RdrIo(int nbr) {
		dev = "RDR" + nbr;
		try {
			co = new Scanner();
			co.addDataListener(this);
			co.addErrorListener(this);
			jposOpen("Scanner." + nbr, co, true);
			co.setDecodeData(true);
			co.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	void setEnabled(boolean state) {
		int mask = 1 << (dev.charAt(3) & 3);

		if (jposActive(co)) {
			try {
				co.clearInput();
				if ((alert & mask) > 0) {
					alert ^= mask;
					co.directIO(508, null, null);
				}
				co.setDeviceEnabled(state);
			} catch (JposException je) {
				jposError(je, co);
			}
		}
	}

	void stop() {
		jposClose(co);
	}

	public void dataOccurred(DataEvent e) {
		try {
			String sdt = barcodeId(co.getScanDataType());
			Device.postInput(dev + sdt, co.getScanDataLabel());
			co.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	public void errorOccurred(ErrorEvent e) {
		try {
			co.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, co);
		}
	}
}

class WghIo extends PosIo implements DataListener {
	private static WghIo sca1 = null;

	static void initialize(Device dev) {
		if (dev.version > 0) {
			sca1 = new WghIo();
			sca1.start(dev);
		}
	}

	static void terminate() {
		if (sca1 != null) {
			sca1.stop();
			sca1 = null;
		}
	}

	static void setItemData(int price, String text) {
		if (sca1 != null) {
			sca1.pricePerUnit = price;
			sca1.itemText = text;
		}
	}

	static void control(int cmd) {
		if (sca1 != null) {
			sca1.write(cmd); /* 1=weigh 2=cancel 4=monitor */
		}
	}

	Scale co;
	Device dev;
	int wght[] = new int[1];
	String itemText;
	int pricePerUnit;

	void start(Device d) {
		dev = d;
		try {
			co = new Scale();
			co.addDataListener(this);
			jposOpen("Scale.1", co, true);
			co.setAsyncMode(true);
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	void display(String txt) {
		try {
			int len = co.getMaxDisplayTextChars();
			if (len > 0)
				co.displayText(rightFill(txt, len, ' '));
		} catch (JposException je) {
			jposError(je, co);
		}
	}

	void write(int cmd) /* 1=weigh 2=cancel 4=monitor */
	{
		if (itemText != null) {
			display(itemText);
			itemText = null;
		}
		try {
			if (cmd == 1) {
				dev.state = -1; /* stop monitor */
				co.setDataEventEnabled(true);
				co.readWeight(wght, -1);
			}
			if (cmd == 2) {
				co.clearInput();
				display("");
			}
		} catch (JposException je) {
			jposError(je, co);
		}
		if (cmd < 4)
			return;
		try {
			co.directIO(604, wght, null);
		} catch (JposException je) {
			wght[0] = 0;
		}
		Device.postInput("SCA4" + cmd + wght[0], null);
	}

	public void dataOccurred(DataEvent e) {
		display("");
		Device.postInput("SCA1" + e.getStatus(), null);
	}

	void stop() {
		jposClose(co);
	}
}

class Wedge extends PosIo implements DataListener, ErrorListener, StatusUpdateListener, MSRConst {
	Keylock lock;
	MSR imsr;
	Scanner scan;
	ToneIndicator tone;

	Wedge() {
		try {
			lock = new Keylock();
			lock.addStatusUpdateListener(this);
			lock.open("KeyLock.1");
			jposEnable(lock);
			Device.postInput("LCK" + lock.getKeyPosition(), null);
		} catch (JposException je) {
			jposError(je, lock);
		}
		try {
			tone = new ToneIndicator();
			tone.open("ToneIndicator.1");
			jposEnable(tone);
		} catch (JposException je) {
			jposError(je, tone);
		}
	}

	void init() {
		try {
			imsr = new MSR();
			imsr.addDataListener(this);
			imsr.addErrorListener(this);
			jposOpen("MSR.1", imsr, true);

			//SAFP-20170224-CGA#A BEG
			if (GdSaf.isEnabled()) {
				imsr.setTracksToRead(MSR_TR_1_2_3);
			} else {
				imsr.setTracksToRead (MSR_TR_2_3);
			}
			//SAFP-20170224-CGA#A END

			imsr.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, imsr);
		}

		try {
			scan = new Scanner();
			scan.addDataListener(this);
			scan.addErrorListener(this);
			jposOpen("Scanner.0", scan, true);
			scan.setDecodeData(true);
			scan.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, scan);
		}
	}

	boolean filter(KeyEvent e) {
		return false;
	}

	void setEnabled(boolean state) {
		if (jposActive(scan)) {
			try {
				scan.clearInput();
				if ((RdrIo.alert & 1) > 0) {
					RdrIo.alert ^= 1;
					scan.directIO(508, null, null);
				} else
					scan.setDeviceEnabled(state);
			} catch (JposException je) {
				jposError(je, scan);
			}
		}
	}

	void stop() {
		jposClose(imsr);
		jposClose(scan);
		jposClose(lock);
		jposClose(tone);
	}

	boolean kbdTone(int type) {
		try {
			tone.soundImmediate();
			return true;
		} catch (JposException je) {
			return false;
		}
	}

	boolean keyLock() {
		return jposActive(lock);
	}

	public void dataOccurred(DataEvent e) {
		try {
			if (imsr.equals(e.getSource())) {
				int sts = e.getStatus();
				if ((sts & 0x0000ff) > 0)
					Device.postInput("MSR1", imsr.getTrack1Data());
				if ((sts & 0x00ff00) > 0)
					Device.postInput("MSR2", imsr.getTrack2Data());
				if ((sts & 0xff0000) > 0)
					Device.postInput("MSR3", imsr.getTrack3Data());
				imsr.setDataEventEnabled(true);
			} else {
				String sdt = barcodeId(scan.getScanDataType());
				Device.postInput("RDR0" + sdt, scan.getScanDataLabel());
				scan.setDataEventEnabled(true);
			}
		} catch (JposException je) {
			jposError(je, e.getSource());
		}
	}

	public void errorOccurred(ErrorEvent e) {
		try {
			if (imsr.equals(e.getSource()))
				imsr.setDataEventEnabled(true);
			else
				scan.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, e.getSource());
		}
	}

	public void statusUpdateOccurred(StatusUpdateEvent e) {
		Device.postInput("LCK" + e.getStatus(), null);
	}
}

class BioIo extends PosIo implements DataListener, ErrorListener, StatusUpdateListener, BiometricsConst {
	Biometrics biom = new Biometrics();
	byte[] dataBIR;
	Image dataRAW;
	ColorModel icm;

	void init() {
		try {
			biom.addDataListener(this);
			biom.addErrorListener(this);
			biom.addStatusUpdateListener(this);
			jposOpen("Biometrics.1", biom, true);
			biom.setDataEventEnabled(true);
			byte rgb[] = colors(32);
			icm = new IndexColorModel(biom.getSensorBPP(), rgb.length, rgb, rgb, rgb);
		} catch (JposException je) {
			jposError(je, biom);
		}
	}

	void stop() {
		jposClose(biom);
	}

	byte[] colors(int size) {
		int shift = 8;
		byte rgb[] = new byte[size];
		for (int ind = size; (ind >>= 1) > 0; shift--)
			;
		while (size-- > 0)
			rgb[size] = (byte) (size << shift);
		return rgb;
	}

	int sensor() {
		try {
			return biom.getCapSensorType();
		} catch (JposException je) {
			return 0;
		}
	}

	boolean capture(int type) {
		try {
			if (type == BIO_DATA_VERIFY)
				biom.beginVerifyCapture();
			else
				biom.beginEnrollCapture(null, null);
			return true;
		} catch (JposException je) {
			jposError(je, biom);
			return false;
		}
	}

	boolean enroll() {
		try {
			if (biom.getCapRawSensorData()) {
				MemoryImageSource is = new MemoryImageSource(biom.getSensorWidth(), biom.getSensorHeight(), icm,
						biom.getRawSensorData(), 0, biom.getSensorWidth());
				dataRAW = Toolkit.getDefaultToolkit().createImage(is);
			}
			dataBIR = biom.getBIR();
			return true;
		} catch (JposException je) {
			jposError(je, biom);
			return false;
		}
	}

	boolean verify(byte[] bir) {
		boolean result[] = new boolean[1];
		int FARachieved[] = new int[1];
		int FRRachieved[] = new int[1];
		byte adapted[][] = new byte[1][];
		byte payload[][] = new byte[1][];
		try {
			biom.verifyMatch(0x20C49B, -1, BIO_FAR_PRECEDENCE, biom.getBIR(), bir, adapted, result, FARachieved,
					FRRachieved, payload);
		} catch (JposException je) {
			jposError(je, biom);
		}
		return result[0];
	}

	void cancel() {
		try {
			biom.endCapture();
		} catch (JposException je) {
			jposError(je, biom);
		}
	}

	public void dataOccurred(DataEvent e) {
		try {
			Device.postInput("BIO0", null);
			biom.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, biom);
		}
	}

	public void errorOccurred(ErrorEvent e) {
		try {
			biom.setDataEventEnabled(true);
		} catch (JposException je) {
			jposError(je, biom);
		}
	}

	public void statusUpdateOccurred(StatusUpdateEvent e) {
		Device.postInput("BIO" + e.getStatus(), null);
		System.err.println("BiometricsStatusUpdateEvent=" + e.getStatus());
	}
}
