import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Dynakey extends Picture
{  int nbr;
   String txt2;
   Rectangle rect = new Rectangle (0, 0, 200, 75);
   DynakeyGroup dyna;

   Dynakey (DynakeyGroup d)
   {  this.dyna = d;
      d.keys[nbr = d.getComponentCount ()] = this;;
      setName ("dynaKey" + nbr);
      rect.y += nbr * rect.height;
      addMouseListener (new MouseAdapter ()
      {  public void mousePressed (MouseEvent e)
         {  touch (e, KeyEvent.KEY_PRESSED);
         }
         public void mouseReleased (MouseEvent e)
         {  touch (e, KeyEvent.KEY_RELEASED);
      }  }  );
   }

   public String getText2 ()
   {  return txt2;
   }

   public void setEnabled (boolean state)
   {  image = state ? dyna.image : null;
      ground = dyna.ground;
      super.setEnabled (state);
   }

   void setText (int align, String text, String txt2)
   {  this.align = align;
      this.text = text; this.txt2 = txt2;
   }

   public void paint (Graphics g)
   {  Dimension d = getSize ();

      if (image != null && dyna.downKey != nbr)
      {  if ((checkImage (image, this) & ALLBITS + FRAMEBITS) == 0) return;
         g.drawImage (image, 0, 0, d.width, d.height, rect.x, rect.y,
                      rect.x + rect.width, rect.y + rect.height, null);
      }
      else if (ground != null) ground.paintOn (this, g);
      if (text == null) return;
      if (! isEnabled ()) return;
      int high = (d.height - 15) / 5;
      int x = d.width / 20, y = high * 3 + nbr + nbr;
      x += x * align;
      if (txt2 != null)
      {  g.drawString (txt2, x, y + high);
         y -= high;
      }
      else if (text.length () < 9)
      {  y += 4;
         g.setFont (dyna.dble);
      }
      g.drawString (text, x, y);
   }

   void touch (MouseEvent e, int type)
   {  if ((e.getModifiers () & e.BUTTON1_MASK) == 0) return;

      if (e.getY () < nbr * 2 || e.getY () > 61 + nbr * 2) return;
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
   Ground ground = null;
   Dynakey keys[] = new Dynakey[8];
   GdLabel chooser = new GdLabel (null, GdLabel.STYLE_WINDOW);
   int downKey, substate = 0;

   DynakeyGroup (Dimension d)
   {  setSize (d);
      setLayout (new GridLayout (0, 1));
      for (int ind = keys.length; ind-- > 0; add (new Dynakey (this)));
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

   public Dimension getPreferredSize ()
   {  return getSize ();
   }

   void showTouch (boolean visible)
   {  chooser.setVisible (visible);
   }

   void setTouch (String list)
   {  chooser.setName ("LIST" + list);
      chooser.setImage (Config.localFile ("cafe", "TCH_" + list + ".GIF"));
      showTouch (chooser.image != null);
   }

   void select (int ind)
   {  String name = keys[ind].text;
      if (name == null) return;
      if (downKey >= 0) keys[downKey].repaint ();
      keys[downKey = ind].repaint ();
      setTouch (name.substring (12));
   }

   void setState (int state)
   {  String name = substate > 0 ? "POS_DS" : "POS_TK";
      
      File f = Config.localFile ("d800", name + FmtIo.editNum (state, 2) + ".GIF");
      if (f.exists ())
      {  image = getToolkit ().getImage (f.getAbsolutePath ());
         prepareImage (image, null);
      }
      else image = null;
      downKey = -1;
      if (kbrd == null)
      {  setEnabled (false);
         for (int ind = keys.length; ind-- > 0; keys[ind].setEnabled (true));
}  }  }
