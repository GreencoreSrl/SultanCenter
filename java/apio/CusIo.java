// line displays (2 x 20 characters)
// versions supported:
// 5972 = 5972 + 5975-1xxx NCR customer display
// 5975 =        5975-2xxx NCR customer display
// 7402 = 7402 + 7403 integrated 2x20 linedisplay
// 7446 = 7443 + 7446-K453 2x20 linedisplay
// 7445 = Epson compatible (default)

import java.io.*;
import javax.comm.*;

class CusIo
{  Device dev;
   byte out[];
   SerialPort port = null;

   CusIo (int nbr)
   {  dev = new Device ("CUS" + nbr);
      if (dev.version == 0) return;
      try
      {  port = dev.open (dev.baud, port.DATABITS_8, port.STOPBITS_1, port.PARITY_NONE);
         out = new byte[] { 0x1b, 0x40 };
         if (dev.version == 5972)
            out = new byte[] { 0x1b, 0x17, 0x05, 0x1b, 0x0c, 0x1b, 0x05 };
         if (dev.version == 5975)
            out = new byte[] { 0x1b, 0x48, 0x50, 0x1b, 0x45, 0x1b, 0x40, 0x00 };
         if (dev.version == 7402) out = new byte[] { 0x13 };
         dev.write (port, out, out.length);
      }
      catch (Exception e)
      {  dev.error (e);
      }
      clear ();
   }

   void clear ()
   {  if (port == null) return;
      out = new byte[] { 0x0c };
      if (dev.version == 5972) out = new byte[] { 0x1b, 0x02 };
      if (dev.version == 5975) out = new byte[] { 0x1b, 0x44, 0x00 };
      if (dev.version == 7402) out = new byte[] { 0x12 };
      dev.write (port, out, out.length);
   }

   void stop ()
   {  clear ();
      if (port == null) return;
      port.close (); port = null;
   }

   void blink (int lamp, int mode)
   {  return; /* 5972 w/o descriptors */
   }

   void write (int line, String data)
   {  if (port == null) return;
      if (line > 1) return;
      out = new byte[] { 0x1f, 0x24, 0x01, (byte) (line + 1) };
      if (dev.version == 5972) out = new byte[] { 0x1b, 0x13, (byte) (line * 20) };
      if (dev.version == 5975) out = new byte[] { 0x1b, 0x60, 0x00, (byte) (line * 20) };
      if (dev.version == 7402) out = new byte[] { 0x10, (byte) (line * 20) };
      if (dev.version == 7446) out = new byte[] { 0x1b, 0x50, 0x01, (byte) (line + 1) };
      dev.write (port, out, out.length);
      out = Config.oemBytes (data);
      dev.write (port, out, out.length);
   }

   void bitmap (int wp, String name)
   {  if (port == null) return;
      int high = 64;
      BmpIo bmp = new BmpIo (name);

      byte[][] dots = new byte[bmp.width][high >> 3];
      bmp.getColumns (dots, 0, high, true);
      dev.write (port, new byte[] { 0x1b, 0x40, (byte)(wp << 2) }, 3);
      dev.write (port, new byte[] { 0x1b, 0x50 }, 2);
      for (int col = 0; col < dots.length; col++)
      {  for (int row = 0; row < high >> 3; row++)
         {  dev.write (port, new byte[] { 0x1b, 0x52, dots[col][row] }, 3);
      }  }
      dev.write (port, new byte[] { 0x1b, 0x40, 0x00 }, 3);
      bmp.close ();
}  }
