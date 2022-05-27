package com.ncr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Hardkey extends Picture
{  int vkey, vmod;
   boolean pressed = false;

   Hardkey (int nbr)
   {  setName ("hardKey" + nbr);
   }

   public void setPressed (boolean state)
   {  if (state != pressed)
      {  pressed = state;
         Graphics g = getGraphics ();
         if (g != null)
         {  paint (g);
            g.dispose ();
   }  }  }

   public void paint (Graphics g)
   {  Dimension d = getSize ();

      if (image != null && pressed)
      {  if ((checkImage (image, this) & ALLBITS + FRAMEBITS) == 0) return;
         g.drawImage (image, 0, 0, d.width, d.height, this);
      }
      else if (ground != null) ground.paintOn (this, g);
}  }

class HardkeyGroup extends Border
{  Hardkey key[] = new Hardkey[36], down;

   static Dimension dims[] =
   {  new Dimension ( 72,  72), /* hardkey 01 */
      new Dimension (146,  72), /* hardkey 02 */
      new Dimension (109,  84), /* hardkey 03 */
      new Dimension (100,  84), /* hardkey 04 */
      new Dimension (144, 144), /* hardkey 05 */
      new Dimension ( 72,  72), /* hardkey 06 */
      new Dimension ( 72,  72), /* hardkey 07 */
      new Dimension ( 72,  72), /* hardkey 08 */
      new Dimension ( 72,  72), /* hardkey 09 */
  };

   HardkeyGroup ()
   {  super (0);
      setLayout (null);
      for (int ind = key.length; ind-- > 0; key[ind] = new Hardkey (ind));
   }

   void click (int vkey, int vmod)
   {  if (down != null)
      {  down.setPressed (false); down = null;
      }
      if (vkey != 0)
      {  down = find (vkey, vmod);
         if (down != null)
         {  down.setPressed (true);
   }  }  }

   Hardkey find (int vkey, int vmod)
   {  for (int ind = 0; ind < key.length; ind++)
      {  Hardkey hkey = key[ind];
         if (hkey.vkey == vkey && hkey.vmod == vmod)
            return hkey;
      }
      return null;
   }

   void add (int type, int vmod, int vkey, int x, int y)
   {  Hardkey hkey = find (0, 0);
      Dimension d = dims[type - 1];
      File f = Config.localFile ("ts1024", "POS_HK" + FmtIo.editNum (type, 2) + ".GIF");
      hkey.setImage (f);
      hkey.ground = ground;
      hkey.setBounds (x, y, d.width, d.height);
      hkey.vmod = vmod;
      hkey.vkey = vkey;
      add (hkey);
   }

   void init (Panel pnl)
   {  add (pnl);
      add (1,                   0, KeyEvent.VK_NUMPAD7,   802, 241);
      add (1,                   0, KeyEvent.VK_NUMPAD8,   876, 241);
      add (1,                   0, KeyEvent.VK_NUMPAD9,   950, 241);
      add (1,                   0, KeyEvent.VK_NUMPAD4,   802, 315);
      add (1,                   0, KeyEvent.VK_NUMPAD5,   876, 315);
      add (1,                   0, KeyEvent.VK_NUMPAD6,   950, 315);
      add (1,                   0, KeyEvent.VK_NUMPAD1,   802, 389);
      add (1,                   0, KeyEvent.VK_NUMPAD2,   876, 389);
      add (1,                   0, KeyEvent.VK_NUMPAD3,   950, 389);
      add (2,                   0, KeyEvent.VK_NUMPAD0,   802, 463);
      add (1,                   0, KeyEvent.VK_DECIMAL,   950, 463);
      add (6,                   0, KeyEvent.VK_UP,        876,   4);
      add (7,                   0, KeyEvent.VK_DOWN,      876,  78);
      add (8,                   0, KeyEvent.VK_LEFT,      802,  41);
      add (9,                   0, KeyEvent.VK_RIGHT,     950,  41);
      add (3,                   0, KeyEvent.VK_ESCAPE,    802, 155);
      add (3,                   0, KeyEvent.VK_MULTIPLY,  913, 155);
      add (5,                   0, KeyEvent.VK_ENTER,     841, 537);
      add (3, KeyEvent.SHIFT_MASK, KeyEvent.VK_F9,        802, 684);
      add (3, KeyEvent.SHIFT_MASK, KeyEvent.VK_F10,       913, 684);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F3,          0, 600);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F4,          0, 684);
      add (4,                   0, KeyEvent.VK_INSERT,    100, 600);
      add (4,                   0, KeyEvent.VK_DELETE,    100, 684);
      add (4,                   0, KeyEvent.VK_HOME,      200, 600);
      add (4,                   0, KeyEvent.VK_END,       200, 684);
      add (4,                   0, KeyEvent.VK_PAGE_UP,   300, 600);
      add (4,                   0, KeyEvent.VK_PAGE_DOWN, 300, 684);
      add (4,                   0, KeyEvent.VK_BACK_SPACE,400, 600);
      add (4,                   0, KeyEvent.VK_SPACE,     400, 684);
      add (4,                   0, KeyEvent.VK_ADD,       500, 600);
      add (4,                   0, KeyEvent.VK_SUBTRACT,  500, 684);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F1,        600, 600);
      add (4,  KeyEvent.CTRL_MASK, KeyEvent.VK_F1,        600, 684);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F2,        700, 600);
      add (4,  KeyEvent.CTRL_MASK, KeyEvent.VK_F2,        700, 684);
}  }
