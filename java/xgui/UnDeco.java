import java.awt.*;
import java.awt.event.*;

abstract class UnDeco
{
   static void show (final GdPos panel, int width, int height)
   {  Frame f = panel.frame;
      Container c = panel.getParent (); /* HardkeyGroup */

      f.add (c == null ? panel : c);
      f.setSize (width, height);
      f.setLocation(0,0);
      f.setUndecorated (true);
      f.setBackground (Color.getColor ("COLOR_DESKTOP", SystemColor.desktop));
      f.addWindowListener (new WindowAdapter ()
      {  public void windowOpened (WindowEvent e)
         {  panel.eventInit ();
         }
         public void windowClosing (WindowEvent e)
         {  panel.eventStop (1);
      }  }  );
      f.setVisible (true);
      f.toFront (); /* SUN: after early kbrd input */
      Config.logConsole (1, null, "window " + width + "x" + height);
}  }
