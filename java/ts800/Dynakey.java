import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Dynakey extends Picture
{  int nbr;
   String txt2;
   Rectangle rect = new Rectangle (6, 6, 148, 48);
   DynakeyGroup dyna;

   Dynakey (DynakeyGroup d)
   {  this.dyna = d;
      d.keys[nbr = d.getComponentCount ()] = this;;
      setName ("dynaKey" + nbr);
      rect.y += nbr * 60;
      addMouseListener (new MouseAdapter ()
      {  public void mousePressed (MouseEvent e)
         {  touch (e, KeyEvent.KEY_PRESSED);
         }
         public void mouseReleased (MouseEvent e)
         {  touch (e, KeyEvent.KEY_RELEASED);
      }  }  );
   }

   public Dimension getPreferredSize ()
   {  return rect.getSize ();
   }

   public String getText2 ()
   {  return txt2;
   }

   public void setEnabled (boolean state)
   {  image = state ? dyna.image : null;
      super.setEnabled (state);
   }

   void setText (int align, String text, String txt2)
   {  this.align = align;
      this.text = text; this.txt2 = txt2;
   }

   public void paint (Graphics g)
   {  Dimension d = getSize ();

      if (image != null)
      {  if ((checkImage (image, this) & ALLBITS + FRAMEBITS) == 0) return;
         g.drawImage (image, 0, 0, d.width, d.height, rect.x, rect.y,
                      rect.x + rect.width, rect.y + rect.height, null);
      }
      else g.clearRect (0, 0, d.width, d.height);
      if (text == null) return;
      if (! isEnabled ()) return;
      
      int high = d.height / 5 + 2;
      int x = d.width - 144 >> 1, y = high * 3 - 5;
      x += align << 3;
      if (txt2 != null)
      {  g.drawString (txt2, x, y + high);
         y -= high;
      }
      else if (text.length () < 9)
      {  y += 2;
         g.setFont (dyna.dble);
      }
      g.drawString (text, x, y);
   }

   void touch (MouseEvent e, int type)
   {  if ((e.getModifiers () & e.BUTTON1_MASK) == 0) return;

      KeyEvent k = new KeyEvent (dyna.kbrd, type, e.getWhen (), e.getModifiers (),
                                 KeyEvent.VK_F1 + nbr, KeyEvent.CHAR_UNDEFINED);
      getToolkit ().getSystemEventQueue ().postEvent (k);
      e.consume (); /* SUN: key events can be mouse food */
      if (type == KeyEvent.KEY_PRESSED) getToolkit().beep ();
}  }

class DynakeyGroup extends Panel
{  Font dble;
   Component kbrd;
   Image image = null;
   Point pad = new Point (4, 0);
   Dynakey keys[] = new Dynakey[8];
   GdLabel chooser = new GdLabel (null, GdLabel.STYLE_WINDOW);
   int downKey = -1, substate = 0;

   DynakeyGroup (Dimension d)
   {  super ();

      int nbr = keys.length, vgap;
      pad.y += d.height - nbr * 52;
      vgap = pad.y / --nbr & ~1;
      pad.y -= vgap * nbr; pad.y >>= 1;
      setLayout (new GridLayout (0, 1, 0, vgap));
      while (nbr-- >= 0)
         add (Border.around (new Dynakey (this), 2));
      chooser.setVisible (false);
      chooser.addMouseListener (new MouseAdapter ()
      {  public void mousePressed (MouseEvent e)
         {  if ((e.getModifiers () & e.BUTTON1_MASK) == 0) return;
            Dimension d = chooser.getSize ();
            Dimension grid = new Dimension (5, 4); 
            int x = e.getX () / (d.width / grid.width);
            int y = e.getY () / (d.height / grid.height);
            x += y * grid.width + 1;
            Device.postInput (chooser.getName () + ":" + x, null);
            e.consume ();
            getToolkit().beep ();
      }  }  ); 
   }

   public Insets getInsets ()
   {  return new Insets (pad.y, pad.x, pad.y, pad.x);
   }

   void setBorder (int ind, boolean state)
   {  Border b = (Border) keys[ind].getParent ();
      b.raised = state;
      b.repaint ();
   }

   void showTouch (boolean visible)
   {  ((Border) chooser.getParent ()).toFront (visible ? 1 : 0);
   }

   void setTouch (String list)
   {  chooser.setName ("LIST" + list);
      chooser.setImage (Config.localFile ("cafe", "TCH_" + list + ".GIF"));
      showTouch (chooser.image != null);
   }

   void select (int ind)
   {  String name = keys[ind].text;
      if (name == null) return;
      if (downKey >= 0) setBorder (downKey, true);
      setBorder (downKey = ind, false);
      setTouch (name.substring (12));
   }

   void setState (int state)
   {  String name = substate > 0 ? "POS_DS" : "POS_DK";
      
      File f = Config.localFile ("dyna", name + FmtIo.editNum (state, 2) + ".GIF");
      if (f.exists ())
      {  image = getToolkit ().getImage (f.getAbsolutePath ());
         prepareImage (image, null);
      }
      else image = null;
      if (downKey >= 0)
      {  setBorder (downKey, true);
         downKey = -1;
      }
      if (kbrd == null)
      {  setEnabled (false);
         for (int ind = keys.length; ind-- > 0; keys[ind].setEnabled (true));
}  }  }
