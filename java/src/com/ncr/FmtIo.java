package com.ncr;

import java.text.*;
import java.util.*;

/*******************************************************************
 *
 * Utility class for decimal calculations and editing. Provides means to generate strings of fixed lengths for formatted
 * I/O operations on local/remote files and retail peripherals.
 *
 *******************************************************************/
public abstract class FmtIo extends Config {
	/**
	 * selection code for LAN based operations
	 **/
	static final int LOCAL = -1;
	/**
	 * result code of operations on I/O devices
	 **/
	static final int ERROR = -1;
	/**
	 * the electronic journal as pseudo print station
	 **/
	static final int ELJRN = 16;

	/**
	 * unique specification to show date and time (screen / receipt / reports)
	 **/
	static String timeFormat = "dd.mm.yy hh:mm ";
	/**
	 * Code 39 symbology used by Italian pharmacies
	 **/
	static String base32 = "0123456789BCDFGHJKLMNPQRSTUVWXYZ";
	/**
	 * convenience references
	 **/
	static DecimalFormatSymbols dfs = ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols();
	/**
	 * aid to extend year with century
	 **/
	static SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");

	/**
	 * implementation of remote file services (a2a)
	 **/
	static A2aServer net = null;

	/**
	 * implementation of graphical user interface (main panel)
	 **/
	public static Graphical gui = null;

	/***************************************************************************
	 * Extends a string to the right
	 *
	 * @param s
	 *            source string
	 * @param len
	 *            target length
	 * @param suffix
	 *            character to be appended zero ore more times
	 * @return new string
	 ***************************************************************************/
	static String rightFill(String s, int len, char suffix) {
		int ind = s.length() - len;
		if (ind >= 0)
			return s.substring(0, len);
		StringBuffer sb = new StringBuffer(len);
		for (sb.append(s); ind++ < 0; sb.append(suffix))
			;
		return sb.toString();
	}

	/***************************************************************************
	 * Extends a string to both ends
	 *
	 * @param s
	 *            source string
	 * @param len
	 *            target length
	 * @param prefix
	 *            character to be inserted/appended zero ore more times
	 * @return new string
	 ***************************************************************************/
	static String center(String s, int len, char prefix) {
		int ind = s.length() - len;
		if (ind >= 0) {
			ind >>= 1;
			return s.substring(ind, ind + len);
		}
		StringBuffer sb = new StringBuffer(len);
		for (len = -ind; ind++ < 0; sb.append(prefix))
			;
		return sb.insert(len >> 1, s).toString();
	}

	/***************************************************************************
	 * Extends a string to the left
	 *
	 * @param s
	 *            source string
	 * @param len
	 *            target length
	 * @param prefix
	 *            character to be inserted zero ore more times
	 * @return new string
	 ***************************************************************************/
	static String leftFill(String s, int len, char prefix) {
		int ind = s.length() - len;
		if (ind >= 0)
			return s.substring(ind, ind + len);
		StringBuffer sb = new StringBuffer(len);
		while (ind++ < 0)
			sb.append(prefix);
		return sb.append(s).toString();
	}

	/***************************************************************************
	 * Mask a string on the left
	 *
	 * @param s
	 *            source string
	 * @param len
	 *            length of plain text remaining
	 * @param prefix
	 *            masking character
	 * @return new string
	 ***************************************************************************/
	static String leftMask(String s, int len, char prefix) {
		StringBuffer sb = new StringBuffer(s);
		for (len -= s.length(); len++ < 0; sb.setCharAt(-len, prefix))
			;
		return sb.toString();
	}

	/***************************************************************************
	 * Returns the string representation of the argument
	 *
	 * @param value
	 *            integer source
	 * @return new string with a leading space or minus
	 ***************************************************************************/
	static String editInt(int value) {
		StringBuffer sb = new StringBuffer(20);
		if (value >= 0)
			sb.append(' ');
		sb.append(Integer.toString(value));
		return sb.toString();
	}

	/***************************************************************************
	 * Returns the string representation of the argument
	 *
	 * @param value
	 *            long source
	 * @param dec
	 *            number of digits after the decimal point
	 * @return new string with a leading space or minus
	 ***************************************************************************/
	static String editDec(long value, int dec) {
		StringBuffer sb = new StringBuffer(20);
		if (value >= 0)
			sb.append(' ');
		sb.append(Long.toString(value));
		if (dec > 0) {
			while (sb.length() - 2 < dec)
				sb.insert(1, '0');
			sb.insert(sb.length() - dec, '.');
		}
		return sb.toString();
	}

	/***************************************************************************
	 * Returns the zerofilled decimal string representation of the argument
	 *
	 * @param value
	 *            integer source
	 * @param len
	 *            target length
	 * @return new string with zero or more leading zeroes
	 ***************************************************************************/
	public static String editNum(int value, int len) {
		return leftFill(String.valueOf(value), len, '0');
	}

	public static String editNum(long value, int len) {
		return leftFill(String.valueOf(value), len, '0');
	}

	/***************************************************************************
	 * Returns the zerofilled hexdecimal string representation of the argument
	 *
	 * @param value
	 *            integer source
	 * @param len
	 *            target length
	 * @return new string with zero or more leading zeroes
	 ***************************************************************************/
	static String editHex(int value, int len) {
		return leftFill(Integer.toHexString(value).toUpperCase(), len, '0');
	}

	/***************************************************************************
	 * Returns the zero/asterisk-filled string representation of the argument
	 *
	 * @param value
	 *            integer source (0xf123 --> "*123")
	 * @param len
	 *            target length
	 * @return new string with zero or more leading zeroes
	 ***************************************************************************/
	static String editKey(int value, int len) {
		String s = Integer.toHexString(value);
		return leftFill(s.replace('f', '*'), len, '0');
	}

	/***************************************************************************
	 * Returns the spacefilled rightjustified string representation of the argument
	 *
	 * @param value
	 *            integer source
	 * @param len
	 *            target length
	 * @return new string with zero or more leading spaces
	 ***************************************************************************/
	static String editTxt(int value, int len) {
		return leftFill(Integer.toString(value), len, ' ');
	}

	/***************************************************************************
	 * Returns the spacefilled and rightjustified argument
	 *
	 * @param s
	 *            source string
	 * @param len
	 *            target length
	 * @return new string with zero or more leading spaces
	 ***************************************************************************/
	static String editTxt(String s, int len) {
		return leftFill(s, len, ' ');
	}

	/***************************************************************************
	 * Returns the string representation of the date argument
	 *
	 * @param value
	 *            integer date (yymmdd)
	 * @return new string formatted as defined by timeFormat
	 ***************************************************************************/
	static String editDate(int value) {
		int xx = 100;
		int dd = value % xx, mm = (value /= xx) % xx, yy = value / xx;

		if (timeFormat.startsWith("mm")) {
			xx = dd;
			dd = mm;
			mm = xx;
		}
		if (timeFormat.startsWith("yy")) {
			xx = dd;
			dd = yy;
			yy = xx;
		}
		return editNum(dd, 2) + timeFormat.charAt(2) + editNum(mm, 2) + timeFormat.charAt(5) + editNum(yy, 2);
	}

	/***************************************************************************
	 * Returns the string representation of the time argument
	 *
	 * @param value
	 *            integer time (hhmm)
	 * @return new string formatted as defined by timeFormat
	 ***************************************************************************/
	static String editTime(int value) {
		int xx = 100;
		int mm = value % xx, hh = value / xx;

		return editNum(hh, 2) + timeFormat.charAt(11) + editNum(mm, 2);
	}

	/***************************************************************************
	 * Returns the integer value of a zero/asterisk-filled String
	 *
	 * @param s
	 *            String record key
	 * @return integer value ("*123" --> 0xf123)
	 ***************************************************************************/
	static int keyValue(String s) {
		return Integer.parseInt(s.replace('*', 'f'), 16);
	}

	/***************************************************************************
	 * Calculate checkdigit
	 *
	 * @param data
	 *            the ean/upc number
	 * @param weight
	 *            right justified string of factors (3131313131313131)
	 * @param mod
	 *            integer value for modulo operation (10)
	 * @return integer value of checkdigit (0 <= cdg < mod)
	 ***************************************************************************/
	static int cdgCheck(String data, String weight, int mod) {
		int cdg = 0, ind = data.length();

		for (int pos = weight.length() - ind; ind-- > 0;)
			cdg += (data.charAt(ind) & 15) * (weight.charAt(pos + ind) & 15);
		return cdg % mod;
	}

	/***************************************************************************
	 * Calculate inner checkdigit (price / quantity / weight)
	 *
	 * @param data
	 *            the ean/upc number (16 characters)
	 * @param pos
	 *            start index of inner area
	 * @return integer checkdigit (0 <= cdg < 10)
	 ***************************************************************************/
	static int cdgPrice(String data, int pos) {
		int cdg = 0, ind = data.length() - 1;

		while (ind-- > pos) {
			int x = ind % 3;
			int val = (data.charAt(ind) & 15) * (x == 2 ? 2 : 5);
			cdg += val + val / (x == 1 ? 10 : -10);
		}
		return cdg % 10;
	}

	/***************************************************************************
	 * Replace last digit of string with checkdigit
	 *
	 * @param data
	 *            the ean/upc number
	 * @param weight
	 *            right justified string of factors (3131313131313131)
	 * @param mod
	 *            integer value for modulo operation (10)
	 * @return new string with correct checkdigit
	 ***************************************************************************/
	static String cdgSetup(String data, String weight, int mod) {
		data = data.substring(0, data.length() - 1);
		return data + (mod - cdgCheck(data + 0, weight, mod)) % 10;
	}

	/***************************************************************************
	 * Returns the UPC-A representation of UPC-E
	 *
	 * @param upc
	 *            String UPC-E number (zero, 6 digits, checkdigit)
	 * @return new string formatted as UPC-A (12 digits)
	 ***************************************************************************/
	static String upcSpreadE(String upc) {
		char rule = upc.charAt(6);
		String s = upc.substring(3, 6) + "0000" + rule;
		if (rule <= '2')
			s = rule + "0000" + upc.substring(3, 6);
		if (rule == '3')
			s = upc.charAt(3) + "00000" + upc.substring(4, 6);
		if (rule == '4')
			s = upc.substring(3, 5) + "00000" + upc.charAt(5);
		return upc.substring(0, 3) + s + upc.charAt(7);
	}

	/***************************************************************************
	 * Returns the Base32 representation of the number
	 *
	 * @param upc
	 *            Italian Pharmacy code (9 digits)
	 * @return new String edited for Code39 (6 characters)
	 ***************************************************************************/
	static String ipcBase32(String upc) {
		int value = Integer.parseInt(upc);
		StringBuffer sb = new StringBuffer();

		for (int ind = 6; ind-- > 0; value >>= 5) {
			sb.insert(0, base32.charAt(value & 0x1f));
		}
		return sb.toString();
	}

	/***************************************************************************
	 * Returns the decimal representation of the Base32 number
	 *
	 * @param code39
	 *            Italian Pharmacy code (6 characters)
	 * @param index
	 *            the beginning index, inclusive
	 * @return new numeric String with leading zeros (9 digits)
	 ***************************************************************************/
	static String ipcDecode(String code39, int index) {
		int value = 0;

		while (index < code39.length()) {
			value <<= 5;
			value += base32.indexOf(code39.charAt(index++));
		}
		return editNum(value, 9);
	}

	/***************************************************************************
	 * Compare two dates (interval < 50 years)
	 *
	 * @param date1
	 *            first date (yymmdd)
	 * @param date2
	 *            second date (yymmdd)
	 * @return -1=date1<date2 0=date1=date2 1=date1>date2
	 ***************************************************************************/
	static int cmpDates(int date1, int date2) {
		if (date1 == date2)
			return 0;

		int yy = date1 / 10000 - date2 / 10000;
		if (yy == 0)
			return date1 < date2 ? -1 : 1;
		return yy > (yy > 0 ? 50 : -50) ? -1 : 1;
	}

	/***************************************************************************
	 * compute day of the week (1 = sunday, 2 = monday, ..., 7 = saturday)
	 *
	 * @param date
	 *            yymmdd
	 * @return 0 < integer week-day < 8
	 ***************************************************************************/
	static int weekDay(int date) {
		Calendar c = sdf.getCalendar();
		try {
			c.setTime(sdf.parse(editNum(date, 6)));
		} catch (ParseException e) {
		}
		return c.get(c.DAY_OF_WEEK);
	}

	/***************************************************************************
	 * edit integer number with Roman figures
	 *
	 * @param value
	 *            integer decimal > 0
	 * @return new Roman String
	 ***************************************************************************/
	static String toRoman(int value) {
		final String roman[][] = { { "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" },
				{ "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC" },
				{ "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" }, };
		StringBuffer sb = new StringBuffer(value < 0 ? "-" : " ");

		value = Math.abs(value);
		for (int ind = roman.length; ind > 0; value /= 10)
			sb.insert(1, roman[--ind][value % 10]);
		while (value-- > 0)
			sb.insert(1, "M");
		return sb.toString();
	}

	/***************************************************************************
	 * Rounding after division by base
	 *
	 * @param value
	 *            long dividend
	 * @param base
	 *            integer divisor
	 * @return long rounded quotient
	 ***************************************************************************/
	static long roundBy(long value, int base) {
		int mod = base >> 1;
		if (mod == 0)
			return value;
		return (value < 0 ? value - mod : value + mod) / base;
	}

	/***************************************************************************
	 * Rounding up after division by base
	 *
	 * @param value
	 *            long dividend
	 * @param base
	 *            integer divisor
	 * @return long rounded quotient
	 ***************************************************************************/
	static long roundUp(long value, int base) {
		return (value + base - 1) / base;
	}

	/***************************************************************************
	 * Return sign of argument
	 *
	 * @param value
	 *            long operand
	 * @return integer -1 or 0 or 1
	 ***************************************************************************/
	static int signOf(long value) {
		if (value == 0)
			return 0;
		return value < 0 ? -1 : 1;
	}

	/***************************************************************************
	 * Return arithmetic limit specified by the argument
	 *
	 * @param halo
	 *            int xy (x = 1st digit / y = number of right 0's)
	 * @return long limit (x * 10 to the power of y)
	 ***************************************************************************/
	static long limitBy(int halo) {
		long value = halo >> 4;

		for (halo &= 15; halo-- > 0; value *= 10)
			;
		return value;
	}
}
