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
   {  new Dimension ( 52,  58), /* hardkey 01 */
      new Dimension (106,  58), /* hardkey 02 */
      new Dimension ( 80,  60), /* hardkey 03 */
      new Dimension ( 80,  60), /* hardkey 04 */
      new Dimension (120, 120), /* hardkey 05 */
      new Dimension ( 52,  52), /* hardkey 06 */
      new Dimension ( 52,  52), /* hardkey 07 */
      new Dimension ( 52,  52), /* hardkey 08 */
      new Dimension ( 52,  52), /* hardkey 09 */
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
      File f = Config.localFile ("ts800", "POS_HK" + FmtIo.editNum (type, 2) + ".GIF");
      hkey.setImage (f);
      hkey.ground = ground;
      hkey.setBounds (x, y, d.width, d.height);
      hkey.vmod = vmod;
      hkey.vkey = vkey;
      add (hkey);
   }

   void init (Panel pnl)
   {  add (pnl);
      add (1,                   0, KeyEvent.VK_NUMPAD7,   640, 182);
      add (1,                   0, KeyEvent.VK_NUMPAD8,   694, 182);
      add (1,                   0, KeyEvent.VK_NUMPAD9,   748, 182);
      add (1,                   0, KeyEvent.VK_NUMPAD4,   640, 242);
      add (1,                   0, KeyEvent.VK_NUMPAD5,   694, 242);
      add (1,                   0, KeyEvent.VK_NUMPAD6,   748, 242);
      add (1,                   0, KeyEvent.VK_NUMPAD1,   640, 302);
      add (1,                   0, KeyEvent.VK_NUMPAD2,   694, 302);
      add (1,                   0, KeyEvent.VK_NUMPAD3,   748, 302);
      add (2,                   0, KeyEvent.VK_NUMPAD0,   640, 362);
      add (1,                   0, KeyEvent.VK_DECIMAL,   748, 362);
      add (6,                   0, KeyEvent.VK_UP,        694,   7);
      add (7,                   0, KeyEvent.VK_DOWN,      694,  61);
      add (8,                   0, KeyEvent.VK_LEFT,      640,  33);
      add (9,                   0, KeyEvent.VK_RIGHT,     748,  33);
      add (3,                   0, KeyEvent.VK_ESCAPE,    640, 120);
      add (3,                   0, KeyEvent.VK_MULTIPLY,  720, 120);
      add (5,                   0, KeyEvent.VK_ENTER,     660, 420);
      add (3, KeyEvent.SHIFT_MASK, KeyEvent.VK_F9,        640, 540);
      add (3, KeyEvent.SHIFT_MASK, KeyEvent.VK_F10,       720, 540);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F3,          0, 480);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F4,          0, 540);
      add (4,                   0, KeyEvent.VK_INSERT,     80, 480);
      add (4,                   0, KeyEvent.VK_DELETE,     80, 540);
      add (4,                   0, KeyEvent.VK_HOME,      160, 480);
      add (4,                   0, KeyEvent.VK_END,       160, 540);
      add (4,                   0, KeyEvent.VK_PAGE_UP,   240, 480);
      add (4,                   0, KeyEvent.VK_PAGE_DOWN, 240, 540);
      add (4,                   0, KeyEvent.VK_BACK_SPACE,320, 480);
      add (4,                   0, KeyEvent.VK_SPACE,     320, 540);
      add (4,                   0, KeyEvent.VK_ADD,       400, 480);
      add (4,                   0, KeyEvent.VK_SUBTRACT,  400, 540);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F1,        480, 480);
      add (4,  KeyEvent.CTRL_MASK, KeyEvent.VK_F1,        480, 540);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F2,        560, 480);
      add (4,  KeyEvent.CTRL_MASK, KeyEvent.VK_F2,        560, 540);
}  }
