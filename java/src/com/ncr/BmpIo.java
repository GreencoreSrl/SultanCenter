package com.ncr;

import java.io.*;

class BmpIo extends FmtIo {
	String pathname;
	RandomAccessFile file;
	byte[] data;
	int width, height, rowSize;
	static final int hdrSize = 62;

	int dword(int offs) {
		int size = 4, value = data[offs + --size];
		while (size > 0) {
			value <<= 8;
			value += data[offs + --size] & 0xff;
		}
		return value;
	}

	BmpIo(String name) {
		try {
			file = new RandomAccessFile(pathname = name, "r");
			file.read(data = new byte[hdrSize]);
			width = dword(18);
			height = dword(22);
			rowSize = width + 31 >> 5 << 2;
			if (dword(2) != hdrSize + rowSize * height)
				throw new IOException("format error");
		} catch (IOException e) {
			logConsole(0, pathname, e.toString());
			width = height = 0;
		}
	}

	void close() {
		if (file != null)
			try {
				file.close();
			} catch (IOException e) {
				logConsole(0, pathname, e.toString());
			}
	}

	void getColumns(byte[][] dots, int top, int rows, boolean x01top) {
		int offs = hdrSize + (height - top) * rowSize;
		try {
			for (int row = 0; rows-- > 0; row++) {
				if (top + row >= height)
					break;
				file.seek(offs -= rowSize);
				file.read(data = new byte[rowSize]);
				int bit = x01top ? 0x01 << (row & 7) : 0x80 >> (row & 7);
				for (int ind = 0; ind < width; ind++) {
					int msk = 0x80 >> (ind & 7);
					if ((data[ind >> 3] & msk) == 0) {
						dots[ind][row >> 3] |= bit;
					}
				}
			}
		} catch (IOException e) {
			logConsole(0, pathname, e.toString());
		}
	}

	void rewrite() throws IOException {
		int offs = data.length, len = offs / height;
		for (int row = 0; row < height; row++) {
			file.seek(hdrSize + row * rowSize);
			file.write(data, offs -= len, len);
		}
	}
}
