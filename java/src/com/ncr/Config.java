package com.ncr;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.log4j.Logger;

public abstract class Config {
	/**
	 * package name
	 **/
	static final String project = "GdPos";
	private static final Logger logger = Logger.getLogger(Config.class);

	/**
	 * Add package properties (GdPos.env) to system properties.
	 **/
	static {
		try {
			System.getProperties().load(new FileInputStream(project + ".env"));
		} catch (IOException e) {
			System.out.println(e.toString());
			System.exit(255);
		}
	}

	/**
	 * terminal number for file name construction
	 **/
	static String REG = System.getProperty("REG", "123");
	/**
	 * terminal group for report selections
	 **/
	static String GRP = System.getProperty("GRP", "00");
	/**
	 * terminal number of server / bus for connections
	 **/
	static String SRV = System.getProperty("SRV", "000");
	/**
	 * store number of terminal for IDC headers
	 **/
	static String STO = System.getProperty("STO", "1234");

	/***************************************************************************
	 * assign TCP/IP service number to a LAN client
	 *
	 * @param xxx
	 *            terminal number 001 - 999
	 * @return port number
	 ***************************************************************************/
	static int lanPort(String xxx) {
		return 20000 + Integer.parseInt(xxx);
	}

	/***************************************************************************
	 * get file object in all environments (NT / Linux / CE)
	 *
	 * @param path
	 *            relative or absolute path
	 * @param name
	 *            file name
	 * @return abstract representation of file
	 ***************************************************************************/
	static File localFile(String path, String name) {
		File f = new File(path, name.replace('*', 'X'));
		return new File(f.getAbsolutePath()); /* make EVM use user.dir */
	}

	/***************************************************************************
	 * adapt path to local environment (adjust separator characters)
	 *
	 * @param name
	 *            relative or absolute path
	 * @return path with backslashes in Windows, slashes in Linux
	 ***************************************************************************/
	static String localPath(String name) {
		char c = File.separatorChar;
		return name.replace((char) (c ^ '/' ^ '\\'), c);
	}

	/***************************************************************************
	 * move file in local file system (delete target, rename source)
	 *
	 * @param source
	 *            source file name
	 * @param target
	 *            target file name
	 * @return success
	 ***************************************************************************/
	static boolean localMove(File source, File target) {
		if (target.exists())
			if (!target.delete()) {
				logConsole(0, target.getPath() + " delete failed", null);
				return false;
			}
		if (source != null)
			if (!source.renameTo(target)) {
				logConsole(0, target.getPath() + " rename failed", null);
				return false;
			}
		return true;
	}

	/**
	 * locale-sensitive String comparison
	 **/
	static Collator loc = Collator.getInstance();

	/**
	 * codepage info for all file and device data
	 **/
	public static String oem = System.getProperty("file.encoding");

	/***************************************************************************
	 * character to byte conversion using system property "file.encoding" (default char encoding in java.lang.String is
	 * platform dependent: windows = ANSI character codes, linux = platform default ISO8859_x)
	 *
	 * @param s
	 *            source string (unicode)
	 * @return byte array (oem code)
	 ***************************************************************************/
	static byte[] oemBytes(String s) {
		try {
			return s.getBytes(oem);
		} catch (IOException e) {
			return s.getBytes();
		}
	}

	/***************************************************************************
	 * get frame size from environment
	 *
	 * @param env
	 *            environment variable (e.g. MainFrame=640x480)
	 * @return size of frame (default height is 600)
	 ***************************************************************************/
	static Dimension frameSize(String env) {
		String s = System.getProperty(env, "0");
		int x = s.indexOf('x'), high = 600;

		if (x >= 0) {
			high = Integer.parseInt(s.substring(x + 1));
			s = s.substring(0, x);
		}
		return new Dimension(Integer.parseInt(s), high);
	}

	/***************************************************************************
	 * get monospaced font from awt
	 *
	 * @param style
	 *            style constant for the Font (PLAIN/BOLD/ITALIC)
	 * @param size
	 *            the point size of the Font
	 * @return the Font instance
	 ***************************************************************************/
	static Font getFont(int style, int size) {
		return new Font("Monospaced", style, size);
	}

	/**
	 * logging activation mask
	 **/
	static int logMasking = Integer.getInteger("LOG", 3).intValue();

	/***************************************************************************
	 * log (implemented as output to stdout and supposedly redirected) using platform specific character encoding:
	 * windows = OEM codepage according to file.encoding linux = platform default character set ISO8859_x
	 *
	 * @param type
	 *            01=configs 02=hotMaint 16=kbdInput 32=actionCommands
	 * @param info
	 *            information to be accompanied with time-stamp
	 * @param data
	 *            optional second data line
	 * @return type active/inactive by masking
	 ***************************************************************************/
	static boolean logConsole(int type, String info, String data) {
		logger.info("" + info + data);
		if (type > 0) /* errorInfo always active */
		{
			if ((type & logMasking) == 0)
				return false;
		}
		if (info != null)
			System.out.println(new Date() + " " + info);
		if (data != null)
			System.out.println(data);
		return true;
	}
}