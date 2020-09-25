import javax.comm.*;
import java.io.*;
import java.util.Date;


class PrinterSpeed
{
	  static SerialPort port = null;	      	
      static int version = 0;
      static int ind = 0;	  
      static byte function = 0;
      static byte mode = 0;
	  
	  public static void main (String[] args){ 
 	  String name = null;
	  String baudrate = null;

	  if (args.length < 5) System.exit(255);
	  
	  name = args[0];
	  version = Integer.parseInt(args[1]);
	  baudrate = args[2];
      
	  function = Byte.parseByte(args[3],16);
	  mode = Byte.parseByte(args[4],16);		  
	  
	  if (name.substring(0,4).equals("COM0")){
          name = "COM" + name.substring(4,5);
       }

	  if ((name.equals("COM0") || name.equals("/dev/ttyS00") || (version == 0))){
          System.out.println("No Printer defined");
          System.exit(255);
       }	  
      
	  if (name.length()>8 && name.substring(0,9).equals("/dev/ttyS")){
		  Integer Port = new Integer(name.substring(9,11));
		  Port = new Integer (Port.intValue()-1);
		  name = "/dev/ttyS" + Port; 
       }

      if (baudrate == null){
          System.out.println("No BaudRate defined");
          System.exit(255);
       }	  
      
      if (!((version == 7197) || (version == 7198) || (version == 7167) || (version == 7168))) {
           System.out.println("Printer not supported!");
           System.exit(230);
        }
	  try 
	  {
	   
	   CommPortIdentifier.addPortName(name, CommPortIdentifier.PORT_SERIAL, null);
	   CommPortIdentifier cid = CommPortIdentifier.getPortIdentifier (name);
	   port = (SerialPort) cid.open ("PoS", 0);
	            System.out.println("setSerialPortParams");
				if (args.length > 4)
					port.setSerialPortParams(new Integer(baudrate).intValue(), port.DATABITS_8,
							port.STOPBITS_1, port.PARITY_NONE);
				System.out.println("setFlowControlMode");
				port.setFlowControlMode(port.FLOWCONTROL_NONE);
				System.out.println("setDTR");
				port.setDTR(true);
				OutputStream out = port.getOutputStream();

				if (args.length > 4) {
					System.out.println("func");
					byte[] func = { 0x1F, 0x11, function, mode, (byte) 0xFF };
					out.write(func);
					System.out.println("nach func");
					//byte[] data2 = { 0x1D, (byte) 0xFF };// RESET PRINTER
					//out.write(data2);
				}
				System.out.println("out.close");
				out.close();
				System.out.println("port.close");
				port.close();
				System.out.println("port.closed");
		}	
	  catch (Exception e) {
	  System.out.println("####"); 
	  System.out.println (e);
	  System.out.println("###"); } 
	 } 
   
}
