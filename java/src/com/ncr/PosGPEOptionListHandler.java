package com.ncr;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ncr.gpe.DefaultGpe;
import com.ncr.gpe.GpeResultProcessorInterface;
import com.ncr.gpe.GpeResult_ListOfOptionsElementInterface;
import com.ncr.gpe.GpeResult_ListOfOptionsInterface;
import org.apache.log4j.Logger;

import static com.ncr.Basis.input;

public class PosGPEOptionListHandler implements GpeResultProcessorInterface {
	private static final Logger logger = Logger.getLogger(PosGPEOptionListHandler.class);

	public void processResult(Map messageMap) {
		logger.info("ENTER processResult");

		PosGPE.sts = 1;
		int chosenOption = 0, countChoice = 0;
		GpeResult_ListOfOptionsInterface data = DefaultGpe.createListOfOptions(messageMap);

		do {
			chosenOption = 0;
			countChoice = 0;
			List options = data.getListOfOptions();

			try {
				//Modal dlg = DialogFactory.create(DialogFactory.SELECTION_DIALOG, "OPZIONI"); //WINEPTS-CGA#D
				SelDlg dlg = new SelDlg("OPZIONI");  //WINEPTS-CGA#A
				for (Iterator iter = options.iterator(); iter.hasNext();) {
					GpeResult_ListOfOptionsElementInterface pp = (GpeResult_ListOfOptionsElementInterface) iter.next();

					if (pp.getValue().intValue() >= 0) {
						dlg.add(10, "" + pp.getValue(), pp.getText());
						countChoice++;
					} else {
						dlg.add(14, "", pp.getText());
					}
				}
				input.prompt = Mnemo.getText(106);
				//WINEPTS-CGA#A BEG
				//input.init(0x00, 2, 2, 0, false);
				//dlg.show("DLG", false, false);
				input.init(0x00, 2, 2, 0);
				dlg.show("DLG");
				//WINEPTS-CGA#A END
			} catch (Exception e) {

				e.printStackTrace();
			}

			// Disabilito lo scanner che viene riabilitato alla distruzione
			// dell'oggetto dlg (vedere modalMain della classe Modal)
			//DevIo.setScannersEnabled(false); // BAS-FIX-2130238-SBR#A
			chosenOption = input.scanNum(input.num);
			input.skip(-input.num);
			if (input.key == 13) {
				if ((input.num != 0) && (chosenOption >= -1)
						&& (chosenOption < countChoice)) {
					break;
				}
			}
			if ((input.key == 0) && (chosenOption == 0)) {
				break;
			}
			if (input.key == 0x001b) {
				break;
			}
		} while (true);

		logger.info("EXIT processResult");
	}
}
