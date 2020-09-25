package com.ncr;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

class GdPos extends Border implements Graphical, ActionListener,
                                      AdjustmentListener, MouseWheelListener
{  Frame frame;
   Modal modal;
   ConIo input = Action.input;
   Motor event = Action.event;
   Thread tick = new Thread (event, event.project + ":ticker");
   Button idle = new Button ()
   {  public Container getParent ()
      {  return modal; /* null in main frame */
   }  };
   Border pnlCard = new Border (0); /* 0 = reg+store/ckr, 1 = bonuspoints */
   Border pnlView = new Border (0); /* 0 = journal, 1 = customer/versions */
   Border pnlRoll = new Border (0); /* 0 = legacy journal, 1 = trans view */

   CidIo cid;
   DynakeyGroup dyna;
   HardkeyGroup hard;

   GdLabel dspArea[] = new GdLabel[6];
   GdLabel msgArea[] = new GdLabel[2];
   GdLabel pntArea[] = new GdLabel[5];
   GdLabel sinArea[] = new GdLabel[2];
   GdLabel stsArea[] = new GdLabel[6];  //WINEPTS-CGA#A
   GdLabel cusArea[] = new GdLabel[8];

   GdElJrn journal = new GdElJrn (42, 12);
   GdLabel picture = new GdLabel (null, GdLabel.STYLE_WINDOW);
   GdLabel sticker = new GdLabel ("   ", GdLabel.STYLE_HEADER);
   GdLabel version = new GdLabel ("   ", GdLabel.STYLE_HEADER);
   GdLabel trxCard = new GdLabel (null, 0);
   GdTView trxView = null;

   Font                       /*2x20*/font20,/*head*/font40,/*list*/font60,/*tiny*/font80,/*View*/font54;/*dynakey font*/
   static int fontSize[][] = {{ 00, 35, 00 },{ 00, 30, 00 },{ 00, 20, 00 },{ 00, 15, 00 },{ 00, 15, 00 },{ 00, 17, 00 }};
   static int xfntSize[][] = {{ 00, 34, 00 },{ 00, 28, 00 },{ 00, 20, 00 },{ 00, 14, 00 },{ 00, 15, 00 },{ 00, 16, 00 }};

   GdPos (Frame f)
   {  super (0);
      frame = f;
   }

   void init (Dimension d)
   {  int ind, type = d.width / 160 - 4;

      if (File.separatorChar == '/') fontSize = xfntSize;
      font54 = Config.getFont (Font.BOLD,  fontSize[4][type]);
      font80 = Config.getFont (Font.PLAIN, fontSize[3][type]);
      font60 = Config.getFont (Font.BOLD,  fontSize[2][type]);
      font40 = Config.getFont (Font.BOLD,  fontSize[1][type]);
      font20 = Config.getFont (Font.BOLD,  fontSize[0][type]);

      setFont (font80);
      dyna = new DynakeyGroup (new Dimension (d.width >> 2, d.height));
      dyna.setFont (Config.getFont (Font.PLAIN, fontSize[5][type]));
      dyna.setForeground (Color.getColor ("COLOR_DYNATEXT"));
      dyna.dble = font40;
      cid = new CidIo (frame);

      dspArea[1] = new GdLabel ("                    ", GdLabel.STYLE_RAISED);
      dspArea[2] = new GdLabel (null,                   GdLabel.STYLE_WINDOW);
      dspArea[3] = new GdLabel ("                 ",    GdLabel.STYLE_HEADER);
      dspArea[4] = new GdLabel (null,                   GdLabel.STYLE_HEADER);
      dspArea[5] = new GdLabel (null,                   GdLabel.STYLE_HEADER);
      dspArea[0] = new GdLabel ("enabling peripherals", GdLabel.STYLE_HEADER);
      dspArea[0].setFont (font40);
      dspArea[1].setFont (font20);
      dspArea[2].setFont (font20);
      dspArea[5].setFont (font60);
      dspArea[0].setEnabled (false);
      dspArea[3].setEnabled (false);
      dspArea[4].setEnabled (false);
      dspArea[5].setEnabled (false);
      dspArea[5].setAlignment (Label.RIGHT);

      stsArea[0] = new GdLabel (null,     GdLabel.STYLE_STATUS);
      stsArea[1] = new GdLabel ("SRV000", GdLabel.STYLE_STATUS);
      stsArea[2] = new GdLabel ("[---]",  GdLabel.STYLE_STATUS);
      stsArea[3] = new GdLabel ("autho",  GdLabel.STYLE_STATUS);
      stsArea[4] = new GdLabel ("[slip]", GdLabel.STYLE_STATUS);
      stsArea[5] = new GdLabel ("Epts",   GdLabel.STYLE_STATUS);//WINEPTS-CGA#A

      trxView = new GdTView (12, 56, true);
      
      for (ind = 0; ind < dspArea.length; ind++)
      {  dspArea[ind].setName ("dspArea" + ind);
      }

      Panel pnlNews = new Panel (new GridLayout (1, 0));
      Panel pnlSins = new Panel (new GridLayout (0, 1));
      for (ind = 0; ind < 2; ind++)
      {  msgArea[ind] = new GdLabel (null, GdLabel.STYLE_RAISED);
         msgArea[ind].setName ("msgArea" + ind);
         pnlNews.add (Border.around (msgArea[ind], 2));
         sinArea[ind] = new GdLabel (null, GdLabel.STYLE_STATUS);
         sinArea[ind].setName ("sinArea" + ind);
         pnlSins.add (sinArea[ind]);
      }

      Border pnlSlot = new Border (0);
      pnlSlot.setLayout (new GridLayout (1, 0, 4, 0));
      pnlSlot.setFont (font40);
      for (ind = 0; ind < pntArea.length; ind++)
      {  pntArea[ind] = new GdLabel (" ", GdLabel.STYLE_STATUS);
         pntArea[ind].setName ("pntArea" + ind);
         pnlSlot.add (Border.around (pntArea[ind], -2));
      }

      Panel pnlHead = new Panel (new GridLayout (0, 1));
      pnlHead.add (dspArea[3]);
      pnlHead.add (dspArea[4]);
      pnlCard.setLayout (new CardLayout ());
      pnlCard.add (pnlHead, "info");
      pnlCard.add (pnlSlot, "slot");

      Panel pnlStat = new Panel (new BorderLayout ());
      Panel pnlLite = new Panel (new GridLayout (1, 0));
      for (ind = 0; ind < stsArea.length; ind++)
      {  stsArea[ind].setName ("stsArea" + ind);
         if (ind > 0) pnlLite.add (Border.around (stsArea[ind], -1));
         else pnlStat.add (Border.around (stsArea[ind], -1), BorderLayout.CENTER);
         stsArea[ind].setEnabled (false);
      }
      stsArea[0].setAlignment (Label.LEFT);
      pnlStat.add (pnlLite, BorderLayout.EAST);

      sticker.setFont (font20);
      sticker.setName ("sticker");
      version.setName ("version");
      version.setPicture ("VERSION");
      picture.setName ("picture");

      Panel pnlCust = new Border (3);
      Panel pnl = new Panel (new GridLayout (0, 1));
      for (ind = 0; ind < cusArea.length; ind++)
      {  if (ind < 4) pnl.add (cusArea[ind] = new GdLabel (null, GdLabel.STYLE_RAISED));
         else pnl.add (Border.around (cusArea[ind] = new GdLabel (null, GdLabel.STYLE_STATUS), -1));
         cusArea[ind].setName ("cusArea" + ind);
      }
      pnlCust.setLayout (new BorderLayout ());
      pnlCust.add (pnl, BorderLayout.SOUTH);
      pnlCust.setBackground (cusArea[0].getBackground ());

      pnlView.setLayout (new CardLayout ());
      pnlView.add (Border.around (journal, -3), "roll");
      pnlView.add (pnlCust, "info");
      pnlView.setFont (font60);
      pnl = new Panel (new BorderLayout ());
      pnl.add (pnlView, BorderLayout.WEST);
      pnl.add (journal.bar, BorderLayout.CENTER);
      pnlRoll.setLayout (new CardLayout ());
      pnlRoll.add (pnl, "journal");
      pnlRoll.add (trxView, "trxView");
      pnlRoll.add (trxCard, "trxCard");
      trxView.setFont (font54);

      hard = new HardkeyGroup ();
      File f = Config.localFile ("ts1024", "GROUND.GIF");
      if (f.exists ()) ground = new Ground (hard, f);
      hard.ground = dyna.ground = ground;
      hard.init (this);
      pnlSlot.ground = picture.ground = ground;
      sticker.ground = version.ground = ground;
      dspArea[0].ground = dspArea[1].ground = ground;
      dspArea[3].ground = dspArea[4].ground = dspArea[5].ground = ground;
      sinArea[0].ground = sinArea[1].ground = ground;

      setLayout (null);
      add (dyna.chooser);
      dyna.chooser.setBounds (16, 182, 560, 386);
      add (pnlCard);
      pnlCard.setBounds (16, 6, 156, 48);
      add (pnlSins);
      pnlSins.setBounds (202, 6, 374, 48);
      add (dspArea[1]);
      dspArea[1].setBounds (32, 121, 428, 40);
      add (sticker);
      sticker.setBounds (476, 121, 84, 40);
      add (dspArea[2]);
      dspArea[2].setBounds (32, 74, 428, 40);
      add (version);
      version.setBounds (476, 74, 84, 40);
      add (pnlRoll);
      pnlRoll.setBounds (16, 182, 560, 282);
      add (picture);
      picture.setBounds (16, 468, 156, 100);
      add (dspArea[0]);
      dspArea[0].setBounds (202, 468, 374, 42);
      add (dspArea[5]);
      dspArea[5].setBounds (202, 510, 374, 34);
      add (pnlNews);
      pnlNews.setBounds (202, 544, 374, 24);
      add (pnlStat);
      pnlStat.setBounds (16, 572, 560, 24);
      add (dyna);
      dyna.setLocation (600, 0);
      setBounds (0, 0, d.width, d.height);

      idle.addActionListener (this);
      journal.bar.addAdjustmentListener (this);
      frame.addMouseWheelListener (this);
      frame.addKeyListener (new KeyAdapter ()
      {  public void keyPressed (KeyEvent e)
         {  if (modal == null)
            {  int code = input.keyBoard (e);
               if (code >= 0) event.main (code);
               else ElJrn.roll (e.getKeyCode ());
      }  }  }  );
      frame.addFocusListener (new FocusAdapter ()
      {  public void focusLost (FocusEvent e)
         {  if (modal == null) frame.requestFocus ();
      }  }  );
      stsArea[2].addMouseListener (new MouseAdapter ()
      {  public void mousePressed (MouseEvent e)
         {  int step = (e.getModifiers () & e.BUTTON1_MASK) > 0 ? -1 : 1;
            if (! DevIo.hasKeylock ())
            {  postAction ("LCK" + (input.posLock + step));
      }  }  }  );
   }

   public void postAction (String cmd)
   {  EventQueue queue = getToolkit ().getSystemEventQueue ();
      queue.postEvent (new ActionEvent (idle, ActionEvent.ACTION_PERFORMED, cmd));
   }

   public void actionPerformed (ActionEvent e)
   {  event.dispatch (e.getActionCommand (), modal);
      hard.click (0, 0);
   }
   public void adjustmentValueChanged (AdjustmentEvent e)
   {  ElJrn.view (false);
   }
   public void mouseWheelMoved (MouseWheelEvent e)
   {  if (e.getScrollType () == e.WHEEL_UNIT_SCROLL)
      {  int ind = e.getWheelRotation ();
         while (ind++ < 0) ElJrn.roll (KeyEvent.VK_UP);
         while (--ind > 0) ElJrn.roll (KeyEvent.VK_DOWN);
   }  }

   void eventInit ()
   {  Thread.currentThread ().setName (event.project + ":dispatch");
      if (input.hasDyna ()) dyna.kbrd = frame;
      else dyna.setState (0);
      Action.init (this);
      event.main (-1);
      tick.start ();
   }

   public void eventStop (int sts)
   {  Action.stop ();
      try
      {  tick.interrupt ();
         tick.join ();
      }
      catch (InterruptedException e)
      {  System.out.println (e);
      }
      journal.stop ();
      System.exit (sts);
   }

   public int clearLink (String msg, int type)
   {  int line = type >> 4;

      DevIo.alert (0);
      if (! Thread.currentThread ().getName ().endsWith ("dispatch"))
      {  DevIo.oplDisplay (1, msg);
         dspStatus (0, msg, true, true);
         return 0;
      }
      if (event.idle > 0) return 0;
      if (input.key == 0x4f4f)
      {  DevIo.setAlerted (input.label.charAt (3) & 3);
      }
      DevIo.oplSignal (15, 1);
      Action.oplToggle (line & 3, msg);
      ClrDlg dlg = new ClrDlg (msg, type & 7);
      if ((type & 0x80) > 0) dlg.input = new ConIo (20);
      dlg.input.init (0x80, 0, 0, 0);
      dlg.show ("CLR");
      Action.oplToggle (line & 1, null);
      DevIo.oplSignal (15, 0);
      return dlg.code;
   }

   void innerVoice(int action) {
      if (modal != null)
         modal.quit();
      else
         postAction("CODE" + Integer.toHexString(action));
   }


   public void display (int line, String data)
   {  if (line > 9)
      {  Action.cusDisplay (line - 10, data);
         return;
      }
      if (line == 3) pnlCard.toFront (0); /* first card = ids */
      dspArea[line].setText (data);
      if (line > 0 && line < 3)
         DevIo.oplDisplay (line - 1, data);
   }

   void dspNotes (int line, String data)
   {  if (line < 1)
         msgArea[line].setAlerted (data != null);
      msgArea[line].setText (data);
   }

   void dspShort (int line, String data)
   {  sinArea[line].setText (data);
   }

   void dspShopper (int line, String data)
   {  if (line == 0) pnlView.toFront (1); /* first card = shopper */
      cusArea[line].setText (data);
   }

   void dspPicture (String name)
   {  picture.setPicture (name);
      picture.setText (picture.image == null ? name : null);
   }

   void dspPoints (String data)
   {  int len = data.length ();
      pnlCard.toFront (1); /* first card = points */
      for (int ind = pntArea.length; ind-- > 0; len--)
      {  pntArea[ind].setText (data.substring (len - 1, len));
   }  }

   void dspSymbol (String data)
   {  sticker.setPicture ("SYM_" + data);
      sticker.setText (sticker.image == null ? FmtIo.editTxt (data, 3) : null);
   }

   public void dspStatus (int nbr, String data, boolean enabled, boolean alerted)
   {  GdLabel lbl = stsArea[nbr];
      lbl.setAlerted (alerted);
      lbl.setEnabled (enabled);
      if (data != null) lbl.setText (data);
   }

   void jrnPicture (String name)
   {  pnlView.toFront (0); /* first card = journal */
      journal.setPicture (name);
   }

   public void print (int station, String data)
   {  if ((station & FmtIo.ELJRN) > 0)
         ElJrn.write (station, data);
      for (int dev = 8; dev > 0; dev >>= 1)
         if ((station & dev) > 0) DevIo.tpmPrint (dev, 0, data);
   }

   public void select (int ind)
   {  if (modal == null) dyna.select (ind);
   }
   public void feedBack (KeyEvent e)
   {  // System.out.println (e.paramString () + " at " + e.getWhen ());
      final int mask = e.CTRL_MASK | e.SHIFT_MASK;
      hard.click (e.getKeyCode (), e.getModifiers () & mask);
   }

   public static void main (String[] args)
   {  Frame f = new Frame ("JPos++");
      GdPos panel = new GdPos (f);
      Dimension d = new Dimension (800, 600);

      Param.init ();
      PosGPE.Init(); //WINEPTS-CGA#A
      panel.init (d);

      UnDeco.show (panel, 1024, 768);
      Config.logConsole (1, null, "client " + d.width + "x" + d.height);

//    System.getProperties ().list (System.out);
   }

   //WINEPTS-CGA#A BEG
   public void updateEpts(boolean active) {
      if (stsArea[5] != null) {
         stsArea[5].setText(active ? "WON" : "WOFF");
         stsArea[5].setAlerted(!active);
      }
   }
   //WINEPTS-CGA#A END


}
