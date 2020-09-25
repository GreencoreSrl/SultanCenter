package com.ncr;

import com.ncr.io.Files;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;

class LineMap extends FmtIo {
	private static final Logger logger = Logger.getLogger(LineMap.class);
	BmpIo bmp;
	Point base;
	Font sgle, dble;
	BufferedImage bim;
	/* CourierNew char width B10,B11,B12,B13 */
	static int fontsize[] = { 17, 18, 20, 22 };

	LineMap(String filename) {
		File file = localFile("bmp", filename + ".BMP");
		if (!file.exists())
			return;
		bmp = new BmpIo(file.getPath());
		base = new Point(bmp.width / 42, bmp.height * 3 >> 2);
		if (base.x < 10 || base.x > 13)
			return;
		double factor = bmp.height / 24.0;
		sgle = getFont(Font.BOLD, fontsize[base.x - 10]);
		dble = sgle.deriveFont(AffineTransform.getScaleInstance(2, factor));
		sgle = sgle.deriveFont(AffineTransform.getScaleInstance(1, factor));
		bim = new BufferedImage(bmp.width, bmp.height, BufferedImage.TYPE_BYTE_BINARY);
		bmp.close();
	}

	String update(String text) {
		return update(text, null);
	}

	String update(String text, String appendix) {
		if (bim == null)
			return null;
		Graphics g = bim.getGraphics();
		g.setFont(sgle);
		g.setColor(Color.white);
		g.fillRect(0, 0, bmp.width, bmp.height);
		g.setColor(Color.black);
		if (text.charAt(1) == '>') {
			g.setFont(dble);
			g.drawString(text.substring(2, 22), base.x, base.y);
		} else
			g.drawString(text, 0, base.y);
		try {
			String pathname = bmp.pathname;
			bmp.file = new RandomAccessFile(bmp.pathname, "rw");
			bmp.data = ((DataBufferByte) bim.getData().getDataBuffer()).getData();
			bmp.rewrite();
			bmp.file.close();
			if (appendix != null) {
				pathname = pathname.substring(0, pathname.indexOf(".BMP")) + appendix + ".BMP";
				fileCopy(bmp.pathname, pathname);
			}
			return pathname;
		} catch (IOException e) {
			logConsole(0, bmp.pathname, e.toString());
			return null;
		}
	}

	public void fileCopy(String source, String dest) {
		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			inStream = new FileInputStream(new File(source));
			outStream = new FileOutputStream(new File(dest));

			byte[] buffer = new byte[1024];

			int length;
			while ((length = inStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, length);
			}

			System.out.println("File is copied successful!");
		} catch (Exception e) {
			logger.error("Error: ", e);
		} finally {
			try {
				inStream.close();
				outStream.close();
			} catch (Exception e) {
				logger.error("Error closing files: ", e);
			}
		}
	}

	public static void main(String[] args) {
		LineMap lm = new LineMap("PrtLine");

		if (args.length > 0)
			lm.update(" >20 chars double-wide                    ");
		else
			lm.update("The line of 42 characters ends right here.");
	}
}
