
import java.awt.event.*;
import java.io.*;

class Wedge
{  StringBuffer data = null;
   boolean keyLock = false;
   OutputStream kbdTone = null;

   Wedge ()
   {  try
      {  keyLock = Runtime.getRuntime ().exec ("KEYLOCK").waitFor () == 0;
      }
      catch (Exception e) {}
      try
      {  kbdTone = Runtime.getRuntime ().exec ("SPEAKER").getOutputStream ();
      }
      catch (Exception e) {}
   }

   void init ()
   {
   }

   boolean filter (KeyEvent e)
   {  int code = e.getKeyCode ();
      if (data == null)
      {  if (code != e.VK_F11) return false;
         data = new StringBuffer (1 + 104);
         return true;
      }
      if (code == e.VK_SHIFT) return true;
      char key = e.getKeyChar ();
      int ind = data.length () - 1;
      if (code != e.VK_ENTER)
      {  if (e.isAltDown ())
         {  if (code != e.VK_ALT)
            {  key = (char) (data.charAt (ind) * 10 + (key & 15));
               data.setCharAt (ind, key);
               return true;
            }
            else key = 0;
         }
         data.append (Character.toUpperCase (key));
         return true;
      }
      key = data.charAt (0);
      if (key == '0') Device.postInput ("RDR" + data, null);
      else if (key < '4')
         if (ind > 2) Device.postInput ("MSR" + data, null);
      if (key == '4') if (ind == 1)
      {  key = data.charAt (ind);
         ind += "LRSX".indexOf (key);
         if (ind < 1) ind = "12234000".charAt (key & 7) & 7;
         Device.postInput ("LCK" + ind, null);
      }
      data = null;
      return true;
   }

   void setEnabled (boolean state)
   {
   }

   void stop ()
   {
   }

   boolean kbdTone (int type)
   {  if (kbdTone != null)
      {  try
         {  kbdTone.write ((type + "\n").getBytes ());
            kbdTone.flush (); /* Linux: buffered stream */
            return true;
         }
         catch (IOException e)
         {  kbdTone = null;
      }  }
      return false;
   }
   boolean keyLock ()
   {  return keyLock;
   }

   int bioSensor ()
   {  return 0;
}  }

class BioIo /* Biometrics device i/o */
{  void init ()
   {  return;
   }

   void stop ()
   {  return;
   }

   int sensor ()
   {  return 0;
}  }
