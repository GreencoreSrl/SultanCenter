package com.ncr;

import java.awt.event.*;

/*******************************************************************
 *
 * The LinIo methods show, type, and error need gui implementations. Before such methods can be used, the reference to
 * the gui provider (normally the main panel) has to be set in FmtIo.gui.
 *
 *******************************************************************/
public interface Graphical {
	/***************************************************************************
	 * display the output data string
	 *
	 * @param line
	 *            window number
	 * @param data
	 *            character string
	 ***************************************************************************/
	void display(int line, String data);

	/***************************************************************************
	 * display the status data string
	 *
	 * @param nbr
	 *            window number within status bar
	 * @param data
	 *            character string
	 * @param enabled
	 *            boolean text attribute
	 * @param alerted
	 *            boolean colour attribute
	 ***************************************************************************/
	void dspStatus(int nbr, String data, boolean enabled, boolean alerted);

	/***************************************************************************
	 * print the output data string
	 *
	 * @param station
	 *            combination of print stations (1=journal 2=receipt 4=slip 8=validation 16=electronic journal)
	 * @param data
	 *            character string
	 ***************************************************************************/
	void print(int station, String data);

	/***************************************************************************
	 * operator intervention
	 *
	 * @param msg
	 *            error message
	 * @param type
	 *            enabled keys (1=clear 2=enter 4=abort) and operator display control (0x10=line2 0x20=toggle) and input
	 *            structure usage mode (0x80=temporary@error)
	 * @return response key (-1=auto, 0=cancel 1=clear 2=enter/abort)
	 ***************************************************************************/
	int clearLink(String msg, int type);

	/***************************************************************************
	 * operator feedback on key up / down events
	 *
	 * @param e
	 *            the key event
	 ***************************************************************************/
	void feedBack(KeyEvent e);

	/***************************************************************************
	 * select non-modal image table
	 *
	 * @param ind
	 *            the dynakey number (0 - 7)
	 ***************************************************************************/
	void select(int ind);

	/***************************************************************************
	 * post an ActionEvent
	 *
	 * @param cmd
	 *            command combined with data
	 ***************************************************************************/
	void postAction(String cmd);

	/***************************************************************************
	 * abort the main program
	 *
	 * @param code
	 *            error level to be returned to the command shell
	 ***************************************************************************/
	void eventStop(int code);
}
