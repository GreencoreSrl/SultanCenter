import java.io.*;
import javax.comm.*;

abstract class DevIo extends Struc

{  static int prin_id = 0, till_id = 0x10;
   static int drw_state, drw_timer = -1;

   static CusIo cdsp, odsp;
   static RdrIo rdr1, rdr2;
   static Wedge wdge;
   static BioIo biom;
   static PrnIo prin = null;
   static Device mfptr = new Device ("MFPTR");
   static Device scale = new Device ("SCALE");
   static LineMap lMap = new LineMap ("PrtLine");

   //WINEPTS-CGA#A BEG
   private static Vector creditCardVoucher = new Vector();
   private static Vector voucherCopyNumber = new Vector();
   private static boolean voucherFiscalReceipt = false;
   private static final int PRINTNORMAL = 1;
   private static final int PRINTCOMMENTAFTERLOGO = 16;
   private static final int BEGINNORMAL = 0;
   private static final int ENDNORMAL = 2;
   private static final int PRINTFIXEDOUTPUT = 32;
   private static final int PRINTTRAILERLINE = 33;
   //WINEPTS-CGA#A END
   static boolean needGraphic (String data)
   {  int opt = options[O_Graph] << 8;

      if (opt > 0) for (int ind = data.length (); ind-- > 0;)
      {  if ((data.charAt (ind) & 0xff00) == opt) return true;
      }
      return false;
   }

   static void tpmImage (int dev, String name)
   {  if (dev != 2) return;
      prin.paperState ();
      prin.bitmap (localFile ("bmp", name).getPath ());
   }

   static void tpmPrint (int dev, int lfs, String data)
   {  if (tra.slip > 0) dev &= ~2;
      if (! station (dev)) return;
      if (dev == 4) if (mfptr.state < 0) return;
      prin.lfeed (dev, lfs);
      if (needGraphic (data))
      {  if (dev != 2) return;
         String name = lMap.update (data);
         if (name != null)
         {  prin.bitmap (name); return;
      }  }
      StringBuffer sb = new StringBuffer (66);
      if (data.length () > 0)
      {  if (data.charAt (1) == '@')
         {  tpmImage (dev, data.substring (2).trim ());
            return;
         }
         prin.ldata (dev, data, sb);
      }
      prin.write (sb.append ('\n').toString ());
   }

   static void tpmLabel (int dev, String nbr)
   {  char type = 'I'; int len = nbr.length ();

      if (! station (dev)) return;
      if (len == 13) type = 'C'; /* ean13 */
      if (len == 12) type = 'A'; /* upc-A */
      if (len == 9) type = 'E'; /* Code39 */
      if (len == 8)
      {  if (nbr.charAt (0) == '0')
         {  type = 'B'; /* upc-E */
            nbr = upcSpreadE (nbr);
         }
         else type = 'D'; /* ean-8 */
      }
      if (type == 'I') nbr = "{B" + nbr;
      prin.label (dev, type, nbr);
   }

   static void tpmCheque (int ind, String nbr, long value)
   {  int dec = tnd[ind].dec, base = 1;

      if (! station (4)) return;
      if (value < 0) value = -value;
      String dig[] = new String[6];
      String amt = editTxt (editDec (value, dec), 10).replace (' ', '*');

      while (dec-- > 0) base *= 10;
      value /= base;
      for (int x = (int) value; ++dec < dig.length; x /= 10)
         dig[dec] = chk_nbr[x % 10].substring (3);
      slpInsert (options[O_chk42]);
      if (mfptr.state > 0) prin.bold (1);
      if (prin.slpColumns > 60)
      {  LinIo slpLine = new LinIo ("SLP", 1, prin.recColumns == 44 ? 57 : 54);
         if (value > 999999) gui.clearLink (Mnemo.getInfo (2), 1);
         slpLine.init (" *" + dig[5] + dig[4] + dig[3] + dig[2] + dig[1] + dig[0])
                .onto (35, tnd[ind].symbol).upto (51, amt).type (4);
         slpLine.init (' ').onto (12, tra.number).type (4);
         slpLine.init (' ').onto (12, chk_line).type (4);
         slpLine.init (' ').onto (12, editNum (ctl.tran, 4)).skip ()
                           .push (editNum (ctl.sto_nbr, 4)).push ('/')
                           .push (editKey (ctl.reg_nbr, 3)).push ('/')
                           .push (editNum (ctl.ckr_nbr, 3)).type (4);
         slpLine.init (' ').upto (32, nbr).onto (35, editDate (ctl.date))
                           .upto (52, editTime (ctl.time / 100)).type (4);
      }
      else
      {  LinIo slpLine = new LinIo ("SLP", 1, 44);
         if (value > 9999) gui.clearLink (Mnemo.getInfo (2), 1);
         slpLine.init (dig[3] + dig[2] + dig[1] + dig[0])
                .onto (23, tnd[ind].symbol).upto (40, amt).type (4);
         slpLine.init (tra.number).type (4);
         slpLine.init (chk_line).type (4);
         slpLine.init (' ').push (editNum (ctl.tran, 4)).skip ()
                           .push (editNum (ctl.sto_nbr, 4)).push ('/')
                           .push (editKey (ctl.reg_nbr, 3)).push ('/')
                           .push (editNum (ctl.ckr_nbr, 3)).type (4);
         slpLine.init (' ').upto (20, nbr).onto (23, editDate (ctl.date))
                           .upto (40, editTime (ctl.time / 100)).type (4);
      }
      if (mfptr.state > 0) prin.bold (0);
      slpRemove ();
      createVirtualVoucher(ind, nbr, amt, dig);  //WINEPTS-CGA#A
   }

   static boolean tpmMICRead ()
   {  for (int sts = ERROR; sts != 0; slpRemove ())
      {  gui.display (2, Mnemo.getInfo (60));
         prin.paperState ();
         do
         {  if (gui.clearLink (Mnemo.getInfo (18), 5) > 1) return false;
         }  while ((prin.slipState () & 1) < 1);
         prin.select (mfptr.state = 4);
         if ((sts = prin.readMICR (prtLine)) == 0)
            continue;
         if (sts > 0) logConsole (0, "MICRstatus=" + sts, null);
         gui.display (2, Mnemo.getInfo (9));
      }
      return true;
   }

   static void cutPaper ()
   {  if ((prin_id & 2) == 0) return;
      prin.knife (prin_id);
      if ((prin.paperState () & 2) > 0)
         gui.clearLink (Mnemo.getInfo (12), 1);
      if (prin.logo.exists ()) prin.center ("\u001d/\u0000");
   }

   static void slpInsert (int lfs)
   {  prin.paperState ();
      mfptr.state = ERROR;
      do
      {  if (gui.clearLink (Mnemo.getInfo (18), 5) > 1) return;
      }  while ((prin.slipState () & 1) < 1);
      prin.select (mfptr.state = 4);
      prin.lfeed (4, lfs);
      prin.write ("\u001bK" + (char) prin.slpTopzone);
   }

   static void slpRemove ()
   {  if (mfptr.state > 0)
      {  prin.paperState ();
         prin.write ("\u000c");
      }
      else gui.display (2, Mnemo.getInfo (23));
      mfptr.state = 0;
      do
      {  gui.clearLink (Mnemo.getInfo (19), 1);
      }  while ((prin.slipState () & 3) > 0);
      gui.display (2, editTxt ("", 20));
      prin.select (2);
   }

   static boolean station (int dev)
   {  return (prin_id & dev) > 0;
   }

   /***************************************************************************
   *  sound tone using utility SPEAKER (wedge or 3rd-party),
   *  if unavailable by Toolkit (sound device or speaker)
   *
   *  @param type  0 = error, 1 = alert
   ***************************************************************************/
   static void alert (int type)
   {  if (! wdge.kbdTone (type))
      {  java.awt.Toolkit.getDefaultToolkit ().beep ();
      }
      // System.err.print ('\7'); /* by Java Console */
   }

   static boolean drwOpened ()
   {  if ((till_id & 0x10) > 0) return true;
      dspLine.show (1);
      return prin.tillState () < 1;
   }

   /***************************************************************************
   *  open cashdrawer
   *
   *  @param nbr   cashdrawer id 1 or 2 (0=both)
   ***************************************************************************/
   static void drwPulse (int nbr)
   {  if (ctl.mode > 0)
      {  if (ctl.mode == M_RENTRY) return;
         else if ((till_id & 2) == 0) return;
      }
      drw_state = nbr > 0 ? nbr : 3;
      if ((till_id & 0x10) > 0) return;
      prin.paperState ();
      if (nbr < 1)
      {  prin.pulse (nbr = 1);
      }
      prin.pulse (--nbr);
   }

   static void drwCheck (int ticks)
   {  if (drw_state > 0) if (drwOpened ())
      {  drw_timer = ticks;
         gui.clearLink (Mnemo.getInfo (10), (till_id & 0x11) > 0 ? 0x11 : 0x10);
         drw_state = 0; drw_timer = ERROR;
      }
      if (mon.adv_rec < 0)
      {  cdsp.clear ();
         mon.adv_rec = 0;
      }
//    power_check ();
      if (station (1))
      {  if ((prin.paperState () & 1) > 0)
            gui.clearLink (Mnemo.getInfo (11), 1);
   }  }

   static boolean drwWatch (int ticks)
   {  if (drw_timer >= 0)
      {  if (! drwOpened ()) return true;
         if (drw_timer > 0) if (--drw_timer < 1)
         {  drw_timer = ticks; alert (1);
      }  }
      return false;
   }

   static void cusDisplay (int line, String data)
   {  cdsp.write (line, data);
   }
   static void oplDisplay (int line, String data)
   {  if (data.length () != 20)
         data = rightFill (data, 20, ' ');
      if (odsp != null) odsp.write (line, data);
   }
   static void oplSignal (int lamp, int mode)
   {  if (odsp != null) odsp.blink (lamp, mode);
   }

   static boolean hasKeylock ()
   {  return wdge.keyLock ();
   }

   static void start ()
   {  RdrIo.scale = scale;
      wdge = new Wedge ();
      biom = new BioIo ();
      odsp = new CusIo (0);
      cdsp = new CusIo (1);
      rdr1 = new RdrIo (1);
      rdr2 = new RdrIo (2);
      wdge.init (); /* Jpos devices msr and scanner */
      biom.init (); /* Jpos device biometrics */
      prin = new PrnIo (mfptr);
      if (prin.port != null)
      {  prin_id = 0xDF;
         if (mfptr.version >= 7190) prin_id &= ~0x04;
         if (mfptr.version != 7162) prin_id &= ~0x01;
         till_id = options[O_xTill];
      }
      prin_id &= ~Integer.parseInt (System.getProperty ("NOP", "0"), 16);
      if ((prin_id & 0x10) == 0) if (prin.recCompressed == 0)
         prin.recCompressed = prin.recColumns - 2;
   }
   static void stop ()
   {  if (prin != null) prin.stop ();
      biom.stop ();
      rdr1.stop ();
      rdr2.stop ();
      cdsp.stop ();
      odsp.stop ();
      wdge.stop ();
   }

   static void setAlerted (int nbr)
   {  int msk = Integer.getInteger ("RDR_BEEP", 0).intValue ();
      RdrIo.alert |= 1 << nbr & msk;
   }
   static void setEnabled (boolean state)
   {  if (rdr1 != null) rdr1.setEnabled (state);
      if (rdr2 != null) rdr2.setEnabled (state);
      wdge.setEnabled (state);
   }

   //WINEPTS-CGA#A BEG
   static void createVirtualVoucher(int ind, String nbr, String amt, String dig[]) {
      LinIo slpLine = new LinIo("SLP", 1, prin.recColumns == 44 ? 57 : 54);
      CreditCardVoucher LineToAdd = new CreditCardVoucher();

      LineToAdd.setTypeOfLine('B');
      LineToAdd.setPrintedLineDescription("");
      pushVirtualVoucherElements(LineToAdd);
      CreditCardVoucher LineToAdd1 = new CreditCardVoucher();

      LineToAdd1.setTypeOfLine('D');
      slpLine.init(" *").onto(2, dig[5]).push(dig[4]).push(dig[3]).push(dig[2]).push(dig[1]).push(dig[0])
              .onto(35, tnd[ind].symbol).upto(51, amt);
      LineToAdd1.setPrintedLineDescription(slpLine.toString());
      pushVirtualVoucherElements(LineToAdd1);
      CreditCardVoucher LineToAdd2 = new CreditCardVoucher();

      LineToAdd2.setTypeOfLine('D');

      slpLine.init(' ').onto(12, tra.number);
      LineToAdd2.setPrintedLineDescription(slpLine.toString());
      pushVirtualVoucherElements(LineToAdd2);
      CreditCardVoucher LineToAdd3 = new CreditCardVoucher();

      LineToAdd3.setTypeOfLine('D');
      slpLine.init(' ').onto(12, chk_line);
      LineToAdd3.setPrintedLineDescription(slpLine.toString());
      pushVirtualVoucherElements(LineToAdd3);
      CreditCardVoucher LineToAdd4 = new CreditCardVoucher();

      LineToAdd4.setTypeOfLine('D');
      slpLine.init(' ').onto(12, editNum(ctl.tran, 4)).skip().push(editNum(ctl.sto_nbr, 4)).push('/')
              .push(editKey(ctl.reg_nbr, 3)).push('/').push(editNum(ctl.ckr_nbr, 3));
      LineToAdd4.setPrintedLineDescription(slpLine.toString());
      pushVirtualVoucherElements(LineToAdd4);
      CreditCardVoucher LineToAdd5 = new CreditCardVoucher();

      LineToAdd5.setTypeOfLine('D');
      slpLine.init(' ').upto(32, nbr).onto(35, editDate(ctl.date)).skip(3).push(editTime(ctl.time / 100));
      LineToAdd5.setPrintedLineDescription(slpLine.toString());
      pushVirtualVoucherElements(LineToAdd5);
      CreditCardVoucher LineToAdd6 = new CreditCardVoucher();

      LineToAdd6.setTypeOfLine('E');
      LineToAdd6.setPrintedLineDescription("");
      pushVirtualVoucherElements(LineToAdd6);

   }

   static void removeCreditCardVoucher() {
      if (creditCardVoucher.isEmpty()) {
         return;
      }

      creditCardVoucher.removeAllElements();
      voucherCopyNumber.removeAllElements();
   }

   static public boolean ThereIsVoucher() {
      return !creditCardVoucher.isEmpty();
   }

   static int getVoucherCopyNumber(boolean firstcopyonreceipt) {
      int num = 0;

      if (!voucherCopyNumber.isEmpty()) {
         if (firstcopyonreceipt) {
            num = ((Integer) voucherCopyNumber.elementAt(0)).intValue();
            for (int i = 0; i < voucherCopyNumber.size(); i++) {
               voucherCopyNumber
                       .setElementAt(new Integer(((Integer) voucherCopyNumber.elementAt(i)).intValue() - 1), i);
            }
         } else {
            num = ((Integer) voucherCopyNumber.remove(0)).intValue();
         }
      }

      return num;
   }

   static void haveToPrintCreditCardVoucher() {
      voucherFiscalReceipt = false;
   }

   static void hateToPrintCreditCardVoucher(boolean firstcopyonreceipt) {
      while (PrintCCV(firstcopyonreceipt)) {
      }
   }

   static boolean PrintCCV(boolean firstcopyonreceipt) {
      // First see if there's anything in the vector. Quit if so.
      if (((tra.mode & M_CANCEL) > 0) || ((tra.mode & M_SUSPND) > 0)) {
         firstcopyonreceipt = false;
      }
      if (creditCardVoucher.isEmpty()) {
         return false;
      }
      if (tra.mode != 2) {
         PosGPE.deleteEptsVoidFlag();
      }

      // Number of voucher copy to print
      int NumberofVoucher, printtype = 0;
      int maxNumberOfVoucher = 0;

      NumberofVoucher = getVoucherCopyNumber(firstcopyonreceipt);
      if (!firstcopyonreceipt) {
         logger.info("NumberofVoucher = " + NumberofVoucher);
         if (((tra.mode & M_CANCEL) > 0) || ((tra.mode & M_SUSPND) > 0)) {
            NumberofVoucher = 2;
         }
         maxNumberOfVoucher = NumberofVoucher;
         logger.info("NumberofVoucher = " + NumberofVoucher);
         printtype = PRINTNORMAL;
      } else {
         if (tra.mode != M_VOID && tra.mode != M_SUSPND) {
            NumberofVoucher = 1;
            maxNumberOfVoucher = NumberofVoucher;
            printtype = PRINTCOMMENTAFTERLOGO;

            DevIo.tpmPrint(2, 0, "");
         }
      }
      Vector tmp = new Vector();
      int nov = 0;

      while (nov < creditCardVoucher.size()) {
         CreditCardVoucher ccv = (CreditCardVoucher) creditCardVoucher.elementAt(nov);

         tmp.add(ccv);
         if (!firstcopyonreceipt) {
            creditCardVoucher.remove(ccv);
            nov--;
            if (ccv.getTypeOfLine() == 'E') {
               break;
            }
         }
         nov++;
      }
      if (!firstcopyonreceipt) {
         if (NumberofVoucher == 0) {

            return (creditCardVoucher.size() > 0);
         }
      }
      while ((NumberofVoucher--) > 0) {
         for (int counter = 0; counter < tmp.size(); counter++) {
            CreditCardVoucher ccv = (CreditCardVoucher) tmp.elementAt(counter);

            logger.info("ccv.getTypeOfLine () = " + ccv.getTypeOfLine());
            if (ccv.getPrintedLineDescription().equals("SKIP VOUCHER")) {
               if ((!firstcopyonreceipt) && ((NumberofVoucher + 1) != maxNumberOfVoucher)) {
                  break;
               }
            }
            switch (ccv.getTypeOfLine()) {
               case 'B':
						/*if (printerObject.GetCapSlpPresent()) {
							slpInsert(options[O_chk42]);
						} else {*/
                  if (!firstcopyonreceipt) {
                     DevIo.tpmPrint(2, 0, "");
                  }
                  //}
                  break;

               case 'E':
                  GdRegis.set_trailer();
						/*if (printerObject.GetCapSlpPresent()) {
							DevIo.tpmPrint(4, 0, prtLine.toString());
							slpRemove();
						} else {*/
                  if (!firstcopyonreceipt) {
                     DevIo.tpmPrint(2, 0, prtLine.toString());
                     DevIo.tpmPrint(2, 0, ccv.getPrintedLineDescription());
                  }
                  //}
                  break;

               case 'D':
               default:
						/*if (printerObject.GetCapSlpPresent()) {
							DevIo.tpmPrint(4, 0, ccv.getPrintedLineDescription());
						} else {*/
                  DevIo.tpmPrint(2, 0, ccv.getPrintedLineDescription());
                  //}
                  break;
            }
         }
         GdRegis.hdr_print();
      }


      return (creditCardVoucher.size() > 0 && (!firstcopyonreceipt));
   }

   static void pushVirtualVoucherElements(CreditCardVoucher element) {
      creditCardVoucher.addElement(element);
   }

   static void addVoucherCopyNumber(int copyNumber) {
      voucherCopyNumber.add(new Integer(copyNumber));
   }

   static void printCreditCardVoucher() {
      while (PrintCCV(voucherFiscalReceipt)) {
      }
   }
   static void printCreditCardVoucher(int inFiscalReceipt) {

      if ((inFiscalReceipt == 0 && voucherFiscalReceipt) || (inFiscalReceipt == 1)) {
         while (PrintCCV((inFiscalReceipt == 0 && voucherFiscalReceipt))) {
         }
      }

   }

	/*public static void setScannersEnabled(boolean state) {
		if (wdge != null) {
			wdge.setScannersEnabled(state);
		}
	}*/
   //WINEPTS-CGA#A END

}

class PrnIo extends FmtIo
{  SerialPort port = null;
   Device device;
   int recColumns = 44, jrnColumns = 40;
   int slpColumns = 66, slpTopzone = 60;
   int recCompressed = 0, inProgress = 0;
   File logo = localFile ("bmp", "P_REGELO.BMP");

   PrnIo (Device dev)
   {  if (dev.version < 1) return;
      device = dev;
      if (dev.version == 7166 || dev.version == 7196 || dev.version == 6000)
      {  recColumns = 42; slpTopzone = 72;
      }
      if (dev.version == 7167 || dev.version == 6000)
      {  slpColumns = 45;
      }
      if (dev.version == 7162)
      {  recColumns = jrnColumns; recCompressed = 40;
      }
      for (connect (); port == null; connect ())
      {  if (gui.clearLink (Mnemo.getInfo (17), 5) > 1)
            gui.eventStop (255);
      }
      paperState ();
      if (logo.exists ()) downLoad (logo.getAbsolutePath ());
   }

   void connect ()
   {  if (port != null) port.close ();
      try
      {  port = device.open (device.baud, port.DATABITS_8, port.STOPBITS_1, port.PARITY_NONE);
         send ("\u001bc1\u0007\u001b2"); /* 6 lines per inch */
      }
      catch (Exception e)
      {  device.error (e);
   }  }

   void stop ()
   {  if (port != null)
      {  port.close (); port = null;
   }  }

   void error (Exception e)
   {  logConsole (0, "MFPTR:" + e.toString (), null);
      gui.clearLink (Mnemo.getInfo (17), 1);
      if (! port.isCTS ()) connect ();
   }

   void send (String data) throws IOException
   {  OutputStream out = port.getOutputStream ();

      if (! port.isDSR ())
      {//if (recColumns > 40)
         // out.write (new byte [] { 0x10, 0x05, 0x01 });
         throw new IOException ("no DSR");
      }
      out.write (data.getBytes (oem));
   }

   void write (String data)
   {  while (true) try
      {  send (data);
         break;
      }
      catch (IOException e)
      {  error (e);
   }  }

   int status (String data) throws IOException
   {  InputStream in = port.getInputStream ();

      in.skip (in.available ());
      send (data);
      while (in.available () < 1)
      {  if (! port.isDSR ()) throw new IOException ("no DSR");
         try
         {  Thread.sleep (100);
         }
         catch (InterruptedException e) {}
      }
      int sts = in.read ();
      if (sts < 0) throw new IOException ("timeout");
      inProgress = 0;
      return sts;
   }

   int readMICR (LinIo io)
   {  int chr = '?', ind = 0, sts;
      byte record[] = new byte[io.dataLen ()];

      while (true) try
      {  sts = status ("\u001bw\u0001"); /* read MICR data and transmit */
         break;
      }
      catch (IOException e)
      {  error (e);
      }
      if (sts < 2) try
      {  InputStream in = port.getInputStream ();
         for (; chr >= ' '; record[ind++] = (byte) chr)
         {  if ((chr = in.read ()) < 0) throw new InterruptedIOException ();
            if (ind == record.length) throw new IOException ("data overrun");
         }
         if (ind < 1) throw new IOException ("noise");
         if (sts < 1) io.pb = new String (record, io.index = 0, ind - 1, oem);
      }
      catch (IOException e)
      {  error (e);
         return ERROR;
      }
      return sts;
   }

   int paperState ()
   {  while (true) try
      {  return status (recColumns > 40 ? "\u001dr1" : "\u001bv");
      }
      catch (IOException e)
      {  error (e);
   }  }
   int slipState ()
   {  return ~paperState () >> 5 & 3;
   }
   int tillState ()
   {  try
      {  return status (recColumns > 40 ? "\u001dr2" : "\u001bu\u0000") & 1;
      }
      catch (IOException e)
      {  logConsole (0, "Drawer:" + e.toString (), null);
      }
      gui.display (1, Mnemo.getInfo (17));
      if (! port.isCTS ()) connect ();
      return 0;
   }

   void select (int dev)
   {  if (dev == 4)
         write ("\u001bf\u0000\u0001"); /* slip waiting time */
      write ("\u001bc0" + (char) dev + pitch (dev, false));
   }
   void bold (int mode)
   {  write ("\u001bE" + (char) mode);
   }
   void center (String data)
   {  write ("\u001ba1" + data + "\u001ba0");
   }

   void label (int dev, char type, String nbr)
   {  if (recColumns == 40) return;
      select (dev);
      if (type == 'E')
      {   write ("\n\u001dhM\u001dH0");
          center ("\u001dk\u0004" + ipcBase32 (nbr) + "\u0000");
          center (dwide (dev, "A" + nbr + '\n'));
          return;
      }
      write ("\n\u001dhM\u001dH2");
      center ("\u001dk" + (char)(type - 'A') + nbr + "\u0000");
   }

   void bitmap (String name)
   {  BmpIo bmp = new BmpIo (name);
      int wide = bmp.width, high = 24, clip = wide * (high >> 3);

      for (int line = 0; line < bmp.height; line += high)
      {  byte[][] dots = new byte[wide][high >> 3];
         bmp.getColumns (dots, line, high, false);
         write ("\u001ba1\u001b*!");
         try
         {  BufferedOutputStream out = new BufferedOutputStream (port.getOutputStream (), 2 + clip);
            out.write (wide & 255); out.write (wide >> 8);
            for (int ind = 0; ind < wide; out.write (dots[ind++]));
            out.close ();
         }
         catch (IOException e)
         {  error (e);
         }
         write ("\u001bJ\u0000\u001ba0");
      }
      bmp.close ();
   }

   void ldata (int dev, String data, StringBuffer sb)
   {  int cols = dev > 1 ? recColumns : jrnColumns;

      if (inProgress > 15) paperState (); /* avoid buffer full */
      if (dev == 4) /* right aligned on slip */
      {  for (cols = slpColumns - data.length (); cols-- > 0; sb.append (' '));
         if (data.charAt (1) != '>') sb.append (data);
         else sb.append (' ' + dwide (dev, data.substring (2, 22)));
         inProgress += 2;
         return;
      }
      if (recCompressed > 0) cols = recCompressed;
      if (cols == 40)
      {  if (data.charAt (1) != '>') sb.append (data.substring (1, 41));
         else sb.append (dwide (dev, data.substring (2, 22)));
         inProgress++;
         return;
      }
      for (cols = cols - 42 >> 1; cols-- > 0; sb.append (' '));
      if (data.charAt (1) != '>') sb.append (data);
      else sb.append (' ' + dwide (dev, data.substring (2, 22)));
   }

   void lfeed (int dev, int lfs)
   {  if (dev < 4) select (dev);
      if (lfs > 0) write ("\u001bd" + (char) lfs);
   }

   void pulse (int nbr)
   {  write ("\u001bp" + (char) nbr + "\u0008\u0008");
   }

   void knife (int msk)
   {  int lfs = (msk & 0x80) > 0 ? 2 : 0;

      if ((msk & 0x40) != 0) select (2);
      else lfeed (2, lfs + (recColumns > 40 ? 3 : 7));
      if ((msk & 0x80) != 0)
         write (recColumns > 40 ? "\u001dV1" : "\u001bm");
   }

   String pitch (int dev, boolean dwide)
   {  int size = dwide ? 32 : 0;
      if (dev < 4) if (recCompressed > 0) size++;
      return "\u001b!" + (char) size;
   }
   String dwide (int dev, String data)
   {  return pitch (dev, true) + data + pitch (dev, false);
   }

   void downLoad (String name)
   {  BmpIo bmp = new BmpIo (name);
      int wide = bmp.width, high = bmp.height;

      if (wide < 1) return;
      if (wide > 72 * 8 || high > 64 * 8) /* ensure GS*XY 7-bit */
      {  logConsole (0, name + " too big", null);
         /* 80mm in 7158/7194,7167/7197 576x512 */
         /* 80mm in 7156/7193 emulation 432x512 */
         /* 58mm in 7158/7194,7167/7197 424x512 */
         /* 58mm in 7156/7193 emulation 312x512 */
         /* 80mm in 7166/7196 TM-T88    512x384 */
         /* 58mm in 7166/7196 TM-T88    360x384 */
         /* 80mm in 7156/7193           448x384 */
         return;
      }
      byte[][] dots = new byte[wide + 7 & ~7][high + 7 >> 3];
      bmp.getColumns (dots, 0, high, false);
      bmp.close ();
      wide = dots.length; high = dots[0].length << 3;
      logConsole (1, null, name = "loading logo " + wide + "x" + high);
      gui.display (2, name);
      write ("\u001d*" + (char)(wide >> 3) + (char)(high >> 3));
      try
      {  for (int ind = 0; ind < wide; ind++)
         {  port.getOutputStream ().write (dots[ind]);
            if ((ind & 7) == 7) port.getOutputStream ().flush (); 
      }  }
      catch (IOException e)
      {  error (e);
}  }  }
