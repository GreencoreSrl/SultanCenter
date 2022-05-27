package com.ncr;

/*******************************************************************
 *
 * Base class for I/O operations with lines of formatted ascii data. Provides means to consecutively parse input data
 * and edit output data lines.
 *
 *******************************************************************/

public class LinIo extends FmtIo {
	/**
	 * unique device / file identification (3 chars)
	 **/
	String id;
	/**
	 * parse buffer reference (needs to be set before usage)
	 **/
	public String pb;
	/**
	 * character position for parsing / editing
	 **/
	int index = 0;
	/**
	 * record number for long term functions
	 **/
	int recno = 0;

	private int inset;
	private char data[];

	/***************************************************************************
	 * Constructor
	 *
	 * @param id
	 *            String (3 chars) used as unique identification
	 * @param inset
	 *            left margin in edit operations
	 * @param size
	 *            output buffer size in chars
	 ***************************************************************************/
	LinIo(String id, int inset, int size) {
		this.inset = inset;
		this.id = id;
		data = new char[size];
	}

	/***************************************************************************
	 * Initiate output (erase data and reset index).
	 *
	 * @param c
	 *            character to be written to all positions
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo init(char c) {
		for (index = data.length; index > 0; data[--index] = c)
			;
		return this;
	}

	/***************************************************************************
	 * Initiate output (erase data with an initial text and reset index).
	 *
	 * @param s
	 *            initial string at position zero
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo init(String s) {
		init(' ');
		push(s);
		index = 0;
		return this;
	}

	/***************************************************************************
	 * Increment index by one char.
	 *
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo skip() {
		return skip(1);
	}

	/***************************************************************************
	 * Increase index by the specified value.
	 *
	 * @param pos
	 *            number of char positions
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo skip(int pos) {
		index += pos;
		return this;
	}

	/***************************************************************************
	 * Set index and put a character there.
	 *
	 * @param pos
	 *            character position
	 * @param c
	 *            char to be written
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo onto(int pos, char c) {
		index = pos;
		return push(c);
	}

	/***************************************************************************
	 * Set index and put a string there.
	 *
	 * @param pos
	 *            character position
	 * @param s
	 *            String to be written
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo onto(int pos, String s) {
		index = pos;
		return push(s);
	}

	/***************************************************************************
	 * Set index and put a string in front of it.
	 *
	 * @param pos
	 *            character position
	 * @param s
	 *            String to be written
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo upto(int pos, String s) {
		if ((index = pos - s.length()) < 0) {
			s = s.substring(-index);
			index = 0;
		}
		return push(s);
	}

	/***************************************************************************
	 * Write a character at the current position.
	 *
	 * @param c
	 *            char to be written
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo push(char c) {
		data[inset + index++] = c;
		return this;
	}

	/***************************************************************************
	 * Write a string at the current position.
	 *
	 * @param s
	 *            String to be written
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo push(String s) {
		int len = s.length();
		int rem = data.length - inset - index;
		if (len > rem)
			len = rem;
		s.getChars(0, len, data, inset + index);
		index += len;
		return this;
	}

	/***************************************************************************
	 * Write a zerofilled decimal value at the current position ("+0012345").
	 *
	 * @param value
	 *            long decimal value
	 * @param len
	 *            the target length including the sign
	 * @return this LinIo object
	 ***************************************************************************/
	public LinIo pushDec(long value, int len) {
		push(value < 0 ? '-' : '+');
		if (value < 0)
			value = -value;
		return push(leftFill(Long.toString(value, 10), --len, '0'));
	}

	/***************************************************************************
	 * Write a zerofilled time value at the current position.
	 *
	 * @param time
	 *            hhmmss
	 * @param len
	 *            the output length
	 * @return this LinIo object
	 ***************************************************************************/
	LinIo pushTim(int time, int len) {
		while (len++ < 6)
			time /= 10;
		return push(editNum(time, len));
	}

	/***************************************************************************
	 * Read one char at the specified position (index unchanged).
	 *
	 * @param pos
	 *            character position
	 * @return char from the output buffer
	 ***************************************************************************/
	char peek(int pos) {
		return data[inset + pos];
	}

	/***************************************************************************
	 * Write one char at the specified position (index unchanged).
	 *
	 * @param pos
	 *            character position
	 * @param c
	 *            char to be written
	 ***************************************************************************/
	void poke(int pos, char c) {
		data[inset + pos] = c;
	}

	/***************************************************************************
	 * Return the length of the output data buffer.
	 *
	 * @return the output buffer size
	 ***************************************************************************/
	int dataLen() {
		return data.length;
	}

	/***************************************************************************
	 * Parse the next character.
	 *
	 * @return the character at the current position
	 ***************************************************************************/
	char scan() {
		return pb.charAt(index++);
	}

	/***************************************************************************
	 * Parse a string.
	 *
	 * @param len
	 *            number of characters
	 * @return the string from the current position
	 ***************************************************************************/
	String scan(int len) {
		index += len;
		return pb.substring(index - len, index);
	}

	/***************************************************************************
	 * Check and skip a delimiter.
	 *
	 * @param c
	 *            the character expected
	 * @return this LinIo object
	 * @throws NumberFormatException
	 *             on mismatch
	 ***************************************************************************/
	LinIo scan(char c) throws NumberFormatException {
		if (scan() != c)
			throw new NumberFormatException("invalid separator");
		return this;
	}

	/***************************************************************************
	 * Parse a decimal number.
	 *
	 * @param len
	 *            number of characters
	 * @return the int value of the string at the current position
	 ***************************************************************************/
	int scanNum(int len) {
		if (len == 0)
			return 0;
		String s = scan(len);
		return Integer.parseInt(s.replace(' ', '0'), 10);
	}

	/***************************************************************************
	 * Parse a hexdecimal number.
	 *
	 * @param len
	 *            number of characters
	 * @return the hex value of the string at the current position
	 ***************************************************************************/
	int scanHex(int len) {
		if (len == 0)
			return 0;
		String s = scan(len);
		return Integer.parseInt(s.replace(' ', '0'), 16);
	}

	/***************************************************************************
	 * Parse a zero/asterisk-filled key ("**01").
	 *
	 * @param len
	 *            number of characters
	 * @return the hex value of the string at the current position
	 ***************************************************************************/
	int scanKey(int len) {
		if (len == 0)
			return 0;
		return keyValue(scan(len));
	}

	/***************************************************************************
	 * Parse a decimal value from a sales total block ("+0012345").
	 *
	 * @param len
	 *            number of characters
	 * @return the long value of the string at the current position
	 ***************************************************************************/
	long scanDec(int len) {
		char c = scan();
		long value = Long.parseLong(scan(--len), 10);
		return c == '-' ? -value : value;
	}

	/***************************************************************************
	 * Parse a percentage (" 12.3%").
	 *
	 * @param len
	 *            number of characters
	 * @return the int value 123
	 ***************************************************************************/
	int scanRate(int len) {
		int rate = scanNum(len - 3) * 10;
		rate += skip().scanNum(1);
		index++;
		return rate;
	}

	/***************************************************************************
	 * Parse a zerofilled time value.
	 *
	 * @param len
	 *            number of characters
	 * @return the int value hhmmss
	 ***************************************************************************/
	int scanTime(int len) {
		int time = scanNum(len);
		while (len++ < 6)
			time *= 10;
		return time;
	}

	/***************************************************************************
	 * Parse a date from operator input
	 *
	 * @param len
	 *            number of characters (5 or 6)
	 * @return the integer date value yymmdd
	 ***************************************************************************/
	int scanDate(int len) {
		int xx, dd = scanNum(len - 4), mm = scanNum(2), yy = scanNum(2);
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
		return (yy * 100 + mm) * 100 + dd;
	}

	/***************************************************************************
	 * Display the output data buffer.
	 *
	 * @param area
	 *            window number (0 - 9 operator, 10 - 13 customer)
	 ***************************************************************************/
    public void show(int area) {
		gui.display(area, toString());
	}

	/***************************************************************************
	 * Print the output data buffer and add it to the electronic journal.
	 *
	 * @param station
	 *            combination of physical print stations (1=journal 2=receipt 4=slip 8=validation)
	 ***************************************************************************/
	public void book(int station) {
		type(ELJRN | station);
	}

	/***************************************************************************
	 * Print the output data buffer
	 *
	 * @param station
	 *            combination of print stations (1=journal 2=receipt 4=slip 8=validation 16=electronic journal)
	 ***************************************************************************/
	public void type(int station) {
		gui.print(station, toString());
	}

	/***************************************************************************
	 * Return a portion of the output data buffer as a string.
	 *
	 * @param pos
	 *            the relative position
	 * @param len
	 *            the number of characters
	 * @return the specified substring
	 ***************************************************************************/
	public String toString(int pos, int len) {
		return String.valueOf(data, inset + pos, len);
	}

	/***************************************************************************
	 * Return the output data buffer as a string.
	 *
	 * @return the complete buffer (including the left margin)
	 ***************************************************************************/
	public String toString() {
		return String.valueOf(data);
	}

	/***************************************************************************
	 * Operator intervention showing the id of this LinIo.
	 *
	 * @param e
	 *            the exception to be logged / shown
	 * @param abort
	 *            true if recovery is not selectable
	 ***************************************************************************/
	void error(Exception e, boolean abort) {
		String msg = "< ACCESS ERROR " + id + " >";
		if (id == "DPT" && (this.getClass().getCanonicalName() == "BinIo"))
			msg = "<ACCESS ERR PLUDPT!>";
		if (id == "SLM" && (this.getClass().getCanonicalName() == "BinIo"))
			msg = "<ACCESS ERR PLUSLM!>";
		logConsole(0, msg, e.toString());
		if (e instanceof RuntimeException)
			if (pb != null)
				logConsole(0, null, pb);
		if (gui != null) {
			gui.clearLink(msg, abort ? 0x84 : 0x81);
			if (abort)
				gui.eventStop(0);
		} else if (abort)
			System.exit(255);
	}
}

/*******************************************************************
 *
 * Total block definition class. A total block provides the storage for three values: transaction count, item count, and
 * amount.
 *
 *******************************************************************/
class Total {
	/**
	 * transaction count (number of customers)
	 **/
	int trans;
	/**
	 * item count
	 **/
	int items;
	/**
	 * total amount
	 **/
	long total;

	static private final int s1 = 7, s2 = 8, s3 = 11;
	static final int length = s1 + s2 + s3;

	/***************************************************************************
	 * Reset this total block to zero.
	 ***************************************************************************/
	void reset() {
		total = trans = items = 0;
	}

	/***************************************************************************
	 * Accumulate item count and amount, increment transaction count.
	 *
	 * @param items
	 *            items to add
	 * @param total
	 *            amount to add
	 ***************************************************************************/
	void update(int items, long total) {
		trans++;
		this.items += items;
		this.total += total;
	}

	/***************************************************************************
	 * Edit this total block into the output buffer of a LinIo object.
	 *
	 * @param io
	 *            the reference to the LinIo object
	 ***************************************************************************/
	void edit(LinIo io) {
		io.pushDec(trans, s1).pushDec(items, s2).pushDec(total, s3);
	}

	/***************************************************************************
	 * Parse this total block from the input buffer of a LinIo object.
	 *
	 * @param io
	 *            the reference to the LinIo object
	 ***************************************************************************/
	void scan(LinIo io) {
		trans = (int) io.scanDec(s1);
		items = (int) io.scanDec(s2);
		total = io.scanDec(s3);
	}
}
