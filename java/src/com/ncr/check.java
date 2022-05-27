package com.ncr;

/*******************************************************************
 *
 * Base class for editing the check value in words
 *
 *******************************************************************/

abstract class Check {
	/**
	 * output parts (xxx ones, xxx thousands, xxx millions)
	 **/
	String txt1 = "", txt2 = "", txt3 = "";
	/**
	 * editing aids (separators and words)
	 **/
	String space, minus, words[];
	/**
	 * indices into array of words
	 **/
	static final int HUNDRED = 28, THOUSAND = 38, MILLION = 39;

	/***************************************************************************
	 * Constructor
	 *
	 * @param words
	 *            array of words for digits and numbers
	 * @param space
	 *            separator before thousand and hundred
	 * @param minus
	 *            separator between tens and ones
	 ***************************************************************************/
	Check(String words[], String space, String minus) {
		this.words = words;
		this.space = space;
		this.minus = minus;
	}

	/***************************************************************************
	 * edit a digit pair
	 *
	 * @param xx
	 *            integer value (21 - 99)
	 * @return value in words
	 ***************************************************************************/
	String toString(int xx) {
		String s = words[xx / 10 + 18];
		if ((xx %= 10) == 0)
			return s;
		return s + minus + words[xx];
	}

	/***************************************************************************
	 * edit a digit triple
	 *
	 * @param xxx
	 *            integer value (000 - 999)
	 * @return value in words
	 ***************************************************************************/
	String inWords(int xxx) {
		String s = "";
		if (xxx == 100)
			return words[HUNDRED];
		if (xxx > 100) {
			s = words[HUNDRED + xxx / 100];
			if ((xxx %= 100) > 0)
				s += space;
		}
		if (xxx > 20)
			s += toString(xxx);
		else if (xxx > 0)
			s += words[xxx];
		return s;
	}

	/***************************************************************************
	 * edit the check value into txt3, txt2, txt1
	 *
	 * @param value
	 *            integer check value
	 ***************************************************************************/
	void setValue(int value) {
		int ind;

		txt1 = txt2 = txt3 = "";
		if (value < 0)
			value = 0 - value;
		if ((value %= 1000000000) == 0)
			txt1 = words[value];
		if ((ind = value / 1000000) > 0) {
			txt3 = inWords(ind) + space + words[MILLION];
			value %= 1000000;
		}
		if ((ind = value / 1000) > 0) {
			txt2 = inWords(ind) + space + words[THOUSAND];
			value %= 1000;
		}
		if (value > 0)
			txt1 = inWords(value);
	}

	/***************************************************************************
	 * return the concatenation of txt3, txt2, txt1
	 *
	 * @return check value in words
	 ***************************************************************************/
	public String toString() {
		String s = txt3;
		s += (s.length() > 0 && txt2.length() > 0 ? space : "") + txt2;
		s += (s.length() > 0 && txt1.length() > 0 ? space : "") + txt1;
		return s;
	}

	/***************************************************************************
	 * main method for testing
	 *
	 * @param args
	 *            param0=language(2chars) param1=from, param2=count
	 ***************************************************************************/
	public static void main(String args[]) throws Exception {
		int val = 0, cnt = 10;
		Check check = (Check) Class.forName("Check_" + args[0]).newInstance();

		if (args.length > 1)
			val = Integer.parseInt(args[1]);
		if (args.length > 2)
			cnt = Integer.parseInt(args[2]);
		for (; cnt-- > 0; val++) {
			check.setValue(val);
			System.out.println(val + " = " + check);
		}
	}
}

class Check_DE extends Check {
	private static String words[] = { "NULL", "EIN", "ZWEI", "DREI", "VIER", "FUENF", "SECHS", "SIEBEN", "ACHT", "NEUN",
			"ZEHN", "ELF", "ZWOELF", "DREIZEHN", "VIERZEHN", "FUENFZEHN", "SECHZEHN", "SIEBZEHN", "ACHTZEHN",
			"NEUNZEHN", "ZWANZIG", "DREISSIG", "VIERZIG", "FUENFZIG", "SECHZIG", "SIEBZIG", "ACHTZIG", "NEUNZIG",
			"EINHUNDERT", "EINHUNDERT", "ZWEIHUNDERT", "DREIHUNDERT", "VIERHUNDERT", "FUENFHUNDERT", "SECHSHUNDERT",
			"SIEBENHUNDERT", "ACHTHUNDERT", "NEUNHUNDERT", "TAUSEND", "MILLIONEN", };

	Check_DE() {
		super(words, "", "UND");
	}

	String toString(int xx) {
		String s = words[xx / 10 + 18];
		if ((xx %= 10) == 0)
			return s;
		return words[xx] + minus + s;
	}

	void setValue(int value) {
		super.setValue(value);
		if (txt3.equals("EINMILLIONEN"))
			txt3 = "EINEMILLION";
		if (txt1.endsWith(words[1]))
			txt1 += "S";
	}
}

class Check_EN extends Check {
	private static String words[] = { "ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
			"TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN",
			"NINETEEN", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY", "ONE HUNDRED",
			"ONE HUNDRED", "TWO HUNDRED", "THREE HUNDRED", "FOUR HUNDRED", "FIVE HUNDRED", "SIX HUNDRED",
			"SEVEN HUNDRED", "EIGHT HUNDRED", "NINE HUNDRED", "THOUSAND", "MILLION", };

	Check_EN() {
		super(words, " ", "-");
	}
}

class Check_ES extends Check {
	private static String words[] = { "CERO", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
			"DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
			"VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA", "CIEN", "CIENTO",
			"DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS",
			"NOVECIENTOS", "MIL", "MILLONES", };

	Check_ES() {
		super(words, " ", " Y ");
	}

	String toString(int xx) {
		if (xx < 30)
			return "VEINTI" + words[xx % 10];
		return super.toString(xx);
	}

	void setValue(int value) {
		super.setValue(value);
		if (txt3.equals("UNO MILLONES"))
			txt3 = "UN MILLON";
		if (txt2.startsWith(words[1]))
			txt2 = txt2.substring(4);
	}
}

class Check_FR extends Check {
	private static String words[] = { "ZERO", "UN", "DEUX", "TROIS", "QUATRE", "CINQ", "SIX", "SEPT", "HUIT", "NEUF",
			"DIX", "ONZE", "DOUZE", "TREIZE", "QUATORZE", "QUINZE", "SEIZE", "DIX-SEPT", "DIX-HUIT", "DIX-NEUF",
			"VINGT", "TRENTE", "QUARANTE", "CINQUANTE", "SOIXANTE", "SEPTANTE", "QUATRE-VINGT", "NONANTE", "CENT",
			"CENT", "DEUX CENT", "TROIS CENT", "QUATRE CENT", "CINQ CENT", "SIX CENT", "SEPT CENT", "HUIT CENT",
			"NEUF CENT", "MILLE", "MILLION", };

	Check_FR() {
		super(words, " ", "-");
	}

	String toString(int xx) {
		String s = words[xx / 10 + 18];
		if (xx > 60) {
			s = words[xx / 20 + 9 << 1];
			if ((xx %= 20) == 0)
				return s;
		} else if ((xx %= 10) == 0)
			return s;
		return s + minus + words[xx];
	}

	void setValue(int value) {
		super.setValue(value);
		if (txt2.startsWith(words[1]))
			txt2 = txt2.substring(3);
	}
}

class Check_IT extends Check {
	private static String words[] = { "ZERO", "UNO", "DUE", "TRE", "QUATTRO", "CINQUE", "SEI", "SETTE", "OTTO", "NOVE",
			"DIECI", "UNDICI", "DODICI", "TREDICI", "QUATTORDICI", "QUINDICI", "SEDICI", "DICIASSETTE", "DICIOTTO",
			"DICIANNOVE", "VENTI", "TRENTA", "QUARANTA", "CINQUANTA", "SESSANTA", "SETTANTA", "OTTANTA", "NOVANTA",
			"CENTO", "CENTO", "DUECENTO", "TRECENTO", "QUATTROCENTO", "CINQUECENTO", "SEICENTO", "SETTECENTO",
			"OTTOCENTO", "NOVECENTO", "MILA", "MILIONI", };

	Check_IT() {
		super(words, "", "");
	}

	String toString(int xx) {
		String s = super.toString(xx);
		int ind = s.length() - words[xx %= 10].length();
		if (xx != 1 && xx != 8)
			return s;
		return s.substring(0, ind - 1) + s.substring(ind);
	}

	void setValue(int value) {
		super.setValue(value);
		if (txt3.equals("UNOMILIONI"))
			txt3 = "UNMILIONE";
		if (txt2.startsWith(words[1]))
			txt2 = "MILLE";
	}
}
