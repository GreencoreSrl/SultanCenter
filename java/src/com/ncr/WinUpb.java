package com.ncr;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

class WinUpb extends Struc {
	private static final Logger logger = Logger.getLogger(WinUpb.class);
	private static WinUpb instance = null;
	private Properties upbParam;

	private DatagramSocket sock;
	private int port;
	private int selfPort;
	private int timeOut = 0;
	private String codepage, n5p5g3;
	private int packet = 0;
	private InetAddress inet;
	static final int hdr_size = 21;
	static final int polynomial = 0xA001;
	FileWriter UPBL = null; /* serial communication log */
	int ec_err = 0; /* most recent EFT terminal error code */
	int ec_prv = 0; /* most recent log data type (status/recv/send) */
	String upb_log = System.getProperty("UPBLOG");
	// private static ArrayList<UPBTrans> itemsVsUPB = new
	// ArrayList<UPBTrans>();

	static final int SERVICE_AVAIL_REQUEST = 0xC0;
	static final int SERVICE_AVAIL_RESPONSE = 0xC3;
	static final int TRANSACTION_CONFIRM = 0xC4;
	static final int RECEIPT_DATA = 0xC5;
	static final int UPB_ERROR = 0xC6;
	/*
	 * operation type: 1 - online recharge 2 - offline recharge 3 - offline services 5 - gift card activation 6 - gift
	 * card redemption
	 */
	private int opType = 2;

	Message sndMsg, rcvMsg;

	String ec_pan;
	int ec_cur = 'E', ec_copies;
	long ec_sls_amt, ec_csh_bck;

	public WinUpb() {
	}

	public static WinUpb getInstance() {
		if (instance == null)
			instance = new WinUpb();

		return instance;
	}

	public void setOperationType(int type) {
		opType = type;
	}

	/***************************************************************************
	 * log io data/status at serial communication port
	 * 
	 * @param fnc
	 *            data type (0=status 1=recv 2=send)
	 * @param data
	 *            array of data bytes to be logged
	 * @param size
	 *            number of data bytes
	 ***************************************************************************/
	void upb_log(int fnc, byte data[], int size) {
		if (UPBL == null)
			return;
		try {
			StringBuffer sb = new StringBuffer(20 + (size << 1));
			if (fnc == 0 || fnc != ec_prv) {
				ctl.setDatim();
				String txt[] = { "sts ", "rcv ", "snd " };
				sb.append("\r\n").append(editNum(ctl.date, 6)).append(' ').append(editNum(ctl.time, 6)).append('.')
						.append(editNum(ctl.msec, 3)).append(' ').append(txt[ec_prv = fnc]);
			}
			for (int ind = 0; ind < size; ind++)
				sb.append(editHex(data[ind], 2));
			UPBL.write(sb.toString());
		} catch (IOException e) {
			logError(e);
		}
	}

	static void logError(Exception e) {
		logConsole(0, "UPBLOG", e.toString());
	}

	public void loadParam() {
		logger.debug("ENTER loadParam()");
		upbParam = new Properties();
		try {
			upbParam.load(new FileInputStream("UpbParam.properties"));

			inet = InetAddress.getByName(upbParam.getProperty("host.address"));
			port = Integer.parseInt(upbParam.getProperty("host.port"));
			selfPort = Integer.parseInt(upbParam.getProperty("self.port"));
			timeOut = Integer.parseInt(upbParam.getProperty("timeOut", "30"));
			n5p5g3 = String.format("%05d", Integer.parseInt(upbParam.getProperty("nodeID", "1")))
					+ String.format("%05d", Integer.parseInt(upbParam.getProperty("posID", "1"))) + "000";

		} catch (Exception e) {
			logger.info("Parameter not loaded");
		}
		logger.debug("EXIT loadParam()");
	}

	static int crc_16(byte[] data, int offs, int size) {
		int crc = 0;

		while (size-- > 0) {
			crc ^= data[offs++] & 0xff;
			for (int bits = 8; bits-- > 0; crc >>= 1) {
				if ((crc & 1) > 0)
					crc ^= polynomial << 1;
			}
		}
		return (crc & 0xff) << 8 | crc >> 8; /* little endian */
	}

	private int upb_rcvMsg() {
		logger.debug("ENTER upb_rcvMsg()");
		int code, sts, type;

		{
			sts = rcvMsg.recv(timeOut);
			if (sts < 0) {
				upb_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);
				logger.debug("EXIT upb_rcvMsg()  -  sts.1: " + sts);
				return sts;
			} else
				ec_prv = -1; /* enforce record header in log */
			if (sts < 3) {
				ec_err = 1111;
				logger.debug("EXIT upb_rcvMsg()  -  return -10 ");
				return -10;
			}

			code = rcvMsg.scanHex(1);
			logger.info("UPB code: " + String.format("%x", code));

			switch (code) {
			case SERVICE_AVAIL_RESPONSE: // 0xC3
				return 0;

			case RECEIPT_DATA: // 0xC5
				return 0;

			case UPB_ERROR: // 0xC6
				sts = (int) rcvMsg.skip(22).scanNum(4);
				logger.info("upbErrorCode: " + sts);
				break;
			}
		}
		logger.debug("EXIT upb_rcvMsg()  -  sts.2: " + sts);
		return sts;
	}

	class Message {
		byte data[];
		int index;
		DatagramPacket dp;

		Message(int size) {
			data = new byte[size];
		}

		Message pushHex(int val, int len) {
			for (int ind = len; ind-- > 0; val >>= 8) {
				data[index + ind] = (byte) val;
			}
			return skip(len);
		}

		Message pushNum(long val, int len) {
			return push(leftFill(Long.toString(val), len, '0'));
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
			return this;
		}

		Message init() {
			index = hdr_size;
			return skip(2);
		}

		Message skip(int len) {
			index += len;
			return this;
		}

		int scanHex(int len) {
			int val = 0;
			while (len-- > 0) {
				val <<= 8;
				val += data[index++] & 0xff;
			}
			return val;
		}

		long scanNum(int len) {
			try {
				return Long.parseLong(scan(len));
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		String scan(int len) {
			index += len;
			if (codepage != null) {
				try {
					return new String(data, index - len, len, codepage);
				} catch (IOException e) {
					logger.error("Error: ", e);
				}
			}
			return new String(data, index - len, len);
		}

		public String toString() {
			return new String(data);
		}

		private int recv(int secs) {
			String cmd;
			dp.setLength(data.length);
			do {
				try {
					sock.setSoTimeout(secs * 1000);
					sock.receive(dp);
					upb_log(1, data, dp.getLength());
					index = 0;
					cmd = scan(3);
					if (!n5p5g3.equals(scan(13)))
						return ERROR;
				} catch (IOException e) {
					return ERROR;
				}
			} while (packet != scanNum(5));
			int len = dp.getLength() - index;

			if (len < 0 || len > 0 && len < 5)
				return -2;
			if (len > 0) {
				if (crc_16(data, index, len) != 0)
					return -3;
				len -= 2;
				if (len != scanHex(2))
					return -4;
				if (!cmd.equals("200"))
					return -5;
			} else if (!cmd.equals("210"))
				return -5;
			return len;
		}

		private int send() {
			int size = index - hdr_size;

			// n5p5g3 = "00001";
			// String hdr = "201" + n5p5g3 + editNum (++packet, 5) + "00000000";
			String hdr = "201" + n5p5g3 + editNum(++packet, 5);
			index = 0;
			push(hdr);
			if (size > 2) {
				pushHex(size, 2).skip(size - 2);
				pushHex(crc_16(data, hdr_size, size), 2);
			} else
				data[2] += 4; /* ping message */
			dp.setLength(index);
			try {
				sock.send(dp);
				upb_log(2, data, index);
				return 0;
			} catch (IOException e) {
				return ERROR;
			}
		}
	}

	public static void main(String[] args) {
		byte[] data = new byte[1];
		do {
			if ((data[0] & 15) == 0)
				System.out.println("");
			System.out.print(editHex(crc_16(data, 0, 1), 4) + " ");
		} while (++data[0] != 0);
	}

	/***************************************************************************
	 * open communications and log
	 ***************************************************************************/
	void start() {
		upb_log = "c:\\gd90\\UPB.log";
		if (upb_log != null) {
			File f = new File(upb_log);
			try {
				UPBL = new FileWriter(f.getAbsolutePath(), true);
			} catch (IOException e) {
				logError(e);
			}
		}
		try {
			sock = new DatagramSocket(selfPort);
			rcvMsg = new Message(5220);
			rcvMsg.dp = new DatagramPacket(rcvMsg.data, 0);
			sndMsg = new Message(2048);
			sndMsg.dp = new DatagramPacket(sndMsg.data, 0, inet, port);
		} catch (Exception e) {
			logger.error("Error: ", e);
		}
	}

	/***************************************************************************
	 * close communications and log
	 ***************************************************************************/
	void stop() {
		try {
			if (port != 0) sock.close();
		} catch (Exception e) {
			logger.error("Error: ", e);
		}
	}

	private int upb_rcvAck() {
		int sts = rcvMsg.recv(2); /* ack within 2 seconds */

		if (sts == 0)
			return sts;
		upb_log(0, new byte[] { (byte) (sts >> 8), (byte) sts }, 2);

		if (sts > 0)
			return -6; /* not an ACK */

		return sts;
	}

	// public int upb_autho (int type)
	public int upb_autho(Itemdata itm) {
		logger.debug("ENTER upb_autho(itm) - itm.number: >" + itm.number + "<");
		int sts = 0;
		/*
		 * sts = sndMsg.init ().send (); //ping if (sts < 0) return sts; if ((sts = upb_rcvAck ()) < 0){ return sts; }
		 */
		String productId = String.format("%32s", itm.number);

		if (itm.qty > 1) {
			logger.info("return 7 - itm.qty: " + itm.qty);
			return 7;
		}
		if ((tra.spf1 & M_TRRTRN) > 0 || (itm.spf1 & M_RETURN) > 0 || (tra.spf1 & M_TRVOID) > 0) {
			logger.info("return 7 - tra.spf1: " + tra.spf1);
			return 7;
		}

		if ((itm.spf1 & M_ERRCOR) > 0 || (itm.spf1 & M_VOID) > 0) {
			int i = findUpbTra(itm.number, (itm.spf1 & M_ERRCOR) > 0);
			if (i >= 0) {
				tra.itemsVsUPB.get(i).setVoid(true);
				itm.operationID = tra.itemsVsUPB.get(i).getOperationID();
			}
			// for (int i=0; i< tra.itemsVsUPB.size(); i++){
			// if (itm.number.equals(tra.itemsVsUPB.get(i).getEan()))
			// tra.itemsVsUPB.get(i).setVoid(true);
			// itm.operationID = tra.itemsVsUPB.get(i).getOperationID();
			// }
			if ((itm.spf1 & M_ERRCOR) > 0 && (itm.spf1 & M_VOID) > 0) {
				if (i >= 0) {
					tra.itemsVsUPB.get(i).setVoid(true);
					itm.operationID = tra.itemsVsUPB.get(i).getOperationID();
				}

				// for (int i=0; i< tra.itemsVsUPB.size(); i++){
				// if (itm.number.equals(tra.itemsVsUPB.get(i).getEan()))
				// tra.itemsVsUPB.get(i).setVoid(false);
				// itm.operationID = tra.itemsVsUPB.get(i).getOperationID();
				// }
			} else {

			}
		} else {
			logger.info("preparing send message - itm.amt: " + itm.amt + " itm.price: " + itm.price);
			sndMsg.init();
			sndMsg.pushHex(SERVICE_AVAIL_REQUEST, 1);
			sndMsg.pushHex(0x01, 1); // Version
			sndMsg.pushNum((long) (ctl.tran), 5);
			// sndMsg.pushNum(opType, 2);
			sndMsg.pushHex(opType, 2);
			sndMsg.pushHex(ec_cur, 1); // Currency
			sndMsg.pushNum(itm.price, 9);
			// sndMsg.pushNum(15000, 9);
			sndMsg.pushHex(0, 2); // Gateway ID, always = 0
			// sndMsg.pushNum(itm.providerID, 2); // Provider ID
			sndMsg.pushHex(itm.providerID, 2); // Provider ID
			// sndMsg.pushNum(0, 2); //Provider ID
			sndMsg.push(productId);

			sndMsg.pushHex(0, 1); // User data type;
			sndMsg.push("                    "); // User data
			sndMsg.pushHex(0, 2); // Extradata len
			sndMsg.pushNum(ctl.ckr_nbr, 8);
			sndMsg.pushNum((long) (ctl.tran), 8);

			logger.info("sending message");
			if ((sts = sndMsg.send()) < 0) {
				logger.info(" error sending  - sts: " + sts);
				return sts;
			}
			logger.info("receiving message - sts: " + sts);
			sts = upb_rcvMsg();
			logger.info("after receiving message - sts: " + sts);
			if (sts == 0) {
				UPBTrans trans = new UPBTrans();
				rcvMsg.skip(1); // Skip version
				trans.setEan(itm.number);
				trans.setTransUPB((int) rcvMsg.scanNum(5));
				trans.setOperationID(rcvMsg.scanNum(16));
				itm.operationID = trans.getOperationID();
				tra.itemsVsUPB.add(trans);
			}
		}
		logger.debug("EXIT upb_autho()" + sts);
		return sts;
	}

	public int upb_confirm(UPBTrans trans, int pos) {
		logger.debug("ENTER upb_confirm");
		int sts = 0;

		int result = trans.isVoid() ? 0 : 1;

		sndMsg.init();
		sndMsg.pushHex(TRANSACTION_CONFIRM, 1);
		sndMsg.pushHex(0x01, 1); // Version
		sndMsg.pushNum((long) ctl.tran, 5);
		sndMsg.pushHex(result, 1);
		sndMsg.pushNum(trans.getOperationID(), 16);
		sndMsg.pushHex(0, 2); // Extra data len

		if ((sts = sndMsg.send()) < 0)
			return sts;
		sts = upb_rcvMsg();
		logger.info("after upb_rcvMsg() - sts: " + sts);

		if (sts == 0)
			tra.itemsVsUPB.get(pos).setConfirmed(true);

		if (!trans.isVoid())
			printVoucher(trans, sts);
		return sts;
	}

	void printVoucher(UPBTrans trans, int sts) {
		logger.debug("ENTER printVoucher sts: " + sts);
		int extradataLen = 0;
		int msgLen = 0;
		String msg = "";

		switch (sts) {
		case UPB_TRA_VOID:
			rcvMsg.skip(40);
			extradataLen = rcvMsg.scanHex(2);
			logger.info("Extradatalen: " + extradataLen);
			if (extradataLen > 0)
				rcvMsg.skip(extradataLen);
			msgLen = rcvMsg.scanHex(2);
			msg = rcvMsg.scan(msgLen);
			logger.info("----UPB VOUCHER  BEG  -----");
			logger.info(msg);
			logger.info("----UPB VOUCHER  end  -----");
			for (int index = 0; index < msg.length();) {
				// index = msg.indexOf("\n\r");
				index = msg.indexOf(0xd);
				try {
					// prtLine.init(msg.substring(0, index)).book(ELJRN + 2);
					prtLine.init(msg.substring(0, index)).type(2);
					logger.info("index: " + index);
					msg = msg.substring(index + 2);
				} catch (Exception e) {
					break;
				}
			}
			// prtLine.init("####################").book(ELJRN + 2);
			prtLine.init("####################").type(2);
			break;

		case 0: // confirm ok
			rcvMsg.skip(91);

			extradataLen = rcvMsg.scanHex(2);
			logger.info("Extradatalen: " + extradataLen);
			if (extradataLen > 0)
				rcvMsg.skip(extradataLen);

			msgLen = rcvMsg.scanHex(2);
			msg = rcvMsg.scan(msgLen);
			logger.info("----UPB VOUCHER  BEG  -----");
			logger.info(msg);
			logger.info("----UPB VOUCHER  end  -----");
			for (int index = 0; index < msg.length();) {
				// index = msg.indexOf("\n\r");
				index = msg.indexOf(0xd);
				try {
					// prtLine.init(msg.substring(0, index)).book(ELJRN + 2);
					prtLine.init(msg.substring(0, index)).type(2);
					logger.info("index: " + index);
					msg = msg.substring(index + 2);

				} catch (Exception e) {
					break;
				}
			}
			// prtLine.init("####################").book(ELJRN + 2);
			prtLine.init("####################").type(2);
			break;
		default:
			printUPBConfirmErrorVoucher(trans);
		}
		logger.debug("EXIT printVoucher");
	}

	void printUPBConfirmErrorVoucher(UPBTrans trans) {
		File f = null;
		RandomAccessFile fErrorVoucher = null;

		try {
			String row = "";
			String path;
			path = "C:\\GD90\\";

			f = new File(path, "P_UPTEXT.DAT");
			fErrorVoucher = new RandomAccessFile(f, "r");
			while ((row = fErrorVoucher.readLine()) != null) {
				row = parseRow(row, trans);
				// prtLine.init(row).book(ELJRN + 2);
				prtLine.init(row).book(1);
			}

			fErrorVoucher.close();
			fErrorVoucher = null;
		} catch (IOException e) {

			logger.info("Error opening c\\gd90\\P_UPTEXT.DAT");

			String defaultReceipt[] = { "**** UPB ERROR ****", "Art   : " + trans.getEan(),
					"UpbId : " + trans.getOperationID(), "UpbTr : " + trans.getTransUpb(), "Void  : " + trans.isVoid(),
					"*******************", };

			for (int i = 0; i < defaultReceipt.length; i++) {
				// prtLine.init(defaultReceipt[i]).book(ELJRN + 2);
				prtLine.init(defaultReceipt[i]).book(1);
			}
		}
	}

	/**
	 * Returns a 41 chars len row and replaces macros
	 *
	 * Macro list: - [$ean$] = EAN code - [$operationID$] = UPB operation ID - [$transUPB$] = ID of UPB transaction -
	 * [$isVoid$] = True/false if is voided or not
	 **/
	static String parseRow(String row, UPBTrans trans) {
		String tempRow = "";

		if (row.indexOf("[$ean$]") != -1) {
			tempRow = row.substring(0, row.indexOf("[$ean$]"));
			tempRow += trans.getEan();
			tempRow += row.substring(row.indexOf("[$ean$]") + 7);
		} else if (row.indexOf("[$operationID$]") != -1) {
			tempRow = row.substring(0, row.indexOf("[$operationID$]"));
			tempRow += trans.getOperationID();
			tempRow += row.substring(row.indexOf("[$operationID$]") + 15);
		} else if (row.indexOf("[$transUPB$]") != -1) {
			tempRow = row.substring(0, row.indexOf("[$transUPB$]"));
			tempRow += trans.getTransUpb();
			tempRow += row.substring(row.indexOf("[$transUPB$]") + 12);
		} else if (row.indexOf("[$isVoid$]") != -1) {
			tempRow = row.substring(0, row.indexOf("[$isVoid$]"));
			tempRow += trans.isVoid();
			tempRow += row.substring(row.indexOf("[$isVoid$]") + 10);
		} else {
			return row;
		}

		if (tempRow.length() > 41) {
			return tempRow.substring(0, 41);
		} else {
			return tempRow;
		}
	}

	int findUpbTra(String ean, boolean backward) {
		int i = 0;
		boolean found = false;
		if (backward) {
			for (i = tra.itemsVsUPB.size() - 1; i >= 0; i--) {
				if (ean.trim().equals(tra.itemsVsUPB.get(i).getEan().trim()) && !tra.itemsVsUPB.get(i).isVoid()) {
					found = true;
					break;
				}
			}
		} else {
			for (i = 0; i < tra.itemsVsUPB.size(); i++) {
				if (ean.trim().equals(tra.itemsVsUPB.get(i).getEan().trim()) && !tra.itemsVsUPB.get(i).isVoid()) {
					found = true;
					break;
				}
			}
		}
		if (!found)
			i = -1;

		return i;
	}
}
