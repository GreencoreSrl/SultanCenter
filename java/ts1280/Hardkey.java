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
{  Hardkey key[] = new Hardkey[40], down;

   static Dimension dims[] =
   {  new Dimension ( 89,  89), /* hardkey 01 */
      new Dimension (179,  89), /* hardkey 02 */
      new Dimension (134,  91), /* hardkey 03 */
      new Dimension (100,  91), /* hardkey 04 */
      new Dimension (176, 176), /* hardkey 05 */
      new Dimension ( 89,  89), /* hardkey 06 */
      new Dimension ( 89,  89), /* hardkey 07 */
      new Dimension ( 89,  89), /* hardkey 08 */
      new Dimension ( 89,  89), /* hardkey 09 */
      new Dimension (132, 132), /* hardkey 10 */
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
      File f = Config.localFile ("ts1280", "POS_HK" + FmtIo.editNum (type, 2) + ".GIF");
      hkey.setImage (f);
      hkey.ground = ground;
      hkey.setBounds (x, y, d.width, d.height);
      hkey.vmod = vmod;
      hkey.vkey = vkey;
      add (hkey);
   }

   void init (Panel pnl)
   {  add (pnl);
      add (1,                   0, KeyEvent.VK_NUMPAD7,   944, 293);
      add (1,                   0, KeyEvent.VK_NUMPAD8,  1034, 293);
      add (1,                   0, KeyEvent.VK_NUMPAD9,  1124, 293);
      add (1,                   0, KeyEvent.VK_NUMPAD4,   944, 382);
      add (1,                   0, KeyEvent.VK_NUMPAD5,  1034, 382);
      add (1,                   0, KeyEvent.VK_NUMPAD6,  1124, 382);
      add (1,                   0, KeyEvent.VK_NUMPAD1,   944, 471);
      add (1,                   0, KeyEvent.VK_NUMPAD2,  1034, 471);
      add (1,                   0, KeyEvent.VK_NUMPAD3,  1124, 471);
      add (2,                   0, KeyEvent.VK_NUMPAD0,   944, 560);
      add (1,                   0, KeyEvent.VK_DECIMAL,  1124, 560);
      add (6,                   0, KeyEvent.VK_UP,       1034,  11);
      add (7,                   0, KeyEvent.VK_DOWN,     1034, 101);
      add (8,                   0, KeyEvent.VK_LEFT,      944,  56);
      add (9,                   0, KeyEvent.VK_RIGHT,    1124,  56);
      add (3,                   0, KeyEvent.VK_ESCAPE,    944, 201);
      add (3,                   0, KeyEvent.VK_MULTIPLY, 1079, 201);
      add (5,                   0, KeyEvent.VK_ENTER,     990, 666);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F1,        640,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F2,        740,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F3,         40,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F4,        140,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F5,        240,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F6,        340,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F7,        440,   5);
      add (4, KeyEvent.SHIFT_MASK, KeyEvent.VK_F8,        540,   5);
      add (10,KeyEvent.SHIFT_MASK, KeyEvent.VK_F9,        946, 862);
      add (10,KeyEvent.SHIFT_MASK, KeyEvent.VK_F10,      1079, 862);
      add (10,                  0, KeyEvent.VK_INSERT,     44, 730);
      add (10,                  0, KeyEvent.VK_DELETE,     44, 862);
      add (10,                  0, KeyEvent.VK_HOME,      176, 730);
      add (10,                  0, KeyEvent.VK_END,       176, 862);
      add (10,                  0, KeyEvent.VK_PAGE_UP,   308, 730);
      add (10,                  0, KeyEvent.VK_PAGE_DOWN, 308, 862);
      add (10,                  0, KeyEvent.VK_BACK_SPACE,440, 730);
      add (10,                  0, KeyEvent.VK_SPACE,     440, 862);
      add (10,                  0, KeyEvent.VK_ADD,       572, 730);
      add (10,                  0, KeyEvent.VK_SUBTRACT,  572, 862);
      add (10, KeyEvent.CTRL_MASK, KeyEvent.VK_F1,        704, 730);
      add (10, KeyEvent.CTRL_MASK, KeyEvent.VK_F2,        704, 862);
}  }
