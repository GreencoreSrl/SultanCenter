import java.io.*;
import javax.comm.*;

class RdrIo implements Runnable
{  Device dev;
   SerialPort port = null;
   Thread tick = new Thread (this);
   static int alert = 0;
   static Device scale = null;

   RdrIo (int nbr)
   {  dev = new Device ("RDR" + nbr);
      if (dev.name == null) return;
      try
      {  port = dev.open (dev.baud, port.DATABITS_7, port.STOPBITS_1, port.PARITY_ODD);
         port.setRTS (true);
         setEnabled (true);
         tick.setName ("RdrIo:" + port.getName ());
         tick.start ();
         if (singleCable ()) WghIo.rdr = this;
      }
      catch (Exception e)
      {  dev.error (e);
   }  }

   boolean singleCable ()
   {  if (port == null) return false;
      return dev.id.equals (scale.name);
   }

   void weigh (int cmd)
   {  byte ctrl[] = { 0x31, (byte) (0x30 + cmd), 0x03, 0x00 };

      ctrl[3] = dev.bcc (ctrl, 3);
      dev.write (port, ctrl, ctrl.length);
   }

   public void run ()
   {  int offs = singleCable () ? 2 : 0;
      byte record[] = new byte[80];
      while (true)
      {  try
         {  int chr = '?', ind = 0;
            InputStream in = port.getInputStream ();
            for (; chr > 16; record[ind++] = (byte) chr)
            {  if ((chr = in.read ()) < 0) throw new InterruptedIOException ();
               if (ind == record.length) throw new IOException ("data overrun");
            }
            if (ind - offs < 2) throw new IOException ("noise");
            if (offs > 0)
            {  if ((chr = in.read ()) < 0) throw new InterruptedIOException ();
               if (chr != dev.bcc (record, ind))
                  throw new IOException ("wrong bcc");
               if (record[0] == 0x31) dev.postInput ("SCA" + new String (record, 1, ind - 2), null);
               if (record[1] != 0x38) continue;
            }
            dev.postInput (dev.id + new String (record, offs, ind - offs - 1), null);
         }
         catch (InterruptedIOException e)
         {  break;
         }
         catch (IOException e)
         {  dev.error (e);
   }  }  }

   void setEnabled (boolean state)
   {  int offs = singleCable () ? 0 : 1;
      int mask = 1 << (dev.id.charAt (3) & 3); 
      byte ctrl[] = { 0x33, 0x32, 0x42, 0x03, 0x00 };

      if (port == null) return;
      if (state) ctrl[2] = 0x33;
      else if ((mask & alert) > 0)
      {  alert ^= mask;
         ctrl[1] = 0x33; ctrl[2] = 0x46;
      }
      ctrl[4] = dev.bcc (ctrl, 4);
      if (dev.version == 3030) /* Scantech Nexus */
         ctrl[offs = 2] = (byte)(state ? 0x0E : 0x0F);
      try
      {  port.getOutputStream ().write (ctrl, offs, ctrl.length - offs - offs);
      }
      catch (IOException e)
      {  dev.error (e);
   }  }

   void stop ()
   {  if (port == null) return;
      setEnabled (false);
      try
      {  tick.interrupt ();
         tick.join ();
      }
      catch (InterruptedException e)
      {  System.out.println (e);
      }
      port.close (); port = null;
   }
}

class WghIo extends FmtIo
{
   private static WghIo sca1 = null;
   static RdrIo rdr = null; /* reader (1/2) with scale */ 

   static void initialize (Device dev)
   {  if (dev.version == 0) return;
      if (dev.name.startsWith ("RDR")) sca1 = new WghIo ();
      else sca1 = (WghIo) dev.loadProtocol ();
      if (sca1 != null) sca1.start (dev);
   }
   static void terminate ()
   {  if (sca1 != null)
      {  sca1.stop (); sca1 = null;
   }  }
   static void setItemData (int price, String text)
   {  if (sca1 != null)
      {  sca1.pricePerUnit = price;
         sca1.itemText = text;
   }  }
   static void control (int cmd)
   {  if (sca1 != null)
      {  sca1.write (cmd); /* 1=weigh 2=cancel 4=monitor */
   }  }

   Device dev;
   String itemText;
   int pricePerUnit;

   void start (Device d)
   {  dev = d;
   }
   void write (int cmd) /* 1=weigh 2=cancel 4=monitor */
   {  if (cmd == 1) dev.state = -1; /* stop monitor */
      if (cmd == 2) if (dev.state != -1) return;
      if (rdr != null) rdr.weigh (cmd);
   }
   void stop ()
   {
   }
}
