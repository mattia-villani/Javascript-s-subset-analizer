

class Parser {
    public static final int _EOF = 0;
    public static final int _SEMI = 1;
    public static final int _OPENBRACE = 2;
    public static final int _CLOSEBRACE = 3;
    public static final int _OPENBRACKET = 4;
    public static final int _CLOSEBRACKET = 5;
    public static final int _STRING = 6;
    public static final int _WORD = 7;
    public static final int _NUMBER = 8;
    public static final int _OPERATOR = 9;
    public static final int _BINBOOLOP = 10;
    public static final int _UNIBOOLOP = 11;
    public static final int _ASSIGNMENT = 12;
    public static final int _QUESTION = 13;
    public static final int _COLON = 14;
    public static final int _DOUBLEOP = 15;
    public static final int maxT = 16;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;
    private static final boolean[][] set = {
            {_T, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x, _x}

    };
    public Token t;    // last recognized token
	public Token la;   // lookahead token
	public Scanner scanner;
	public Errors errors;
    int errDist = minErrDist;

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr(String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}

	void JavaScriptPL() {
        Get();
    }

	public void Parse() {
		la = new Token();
        la.val = "";
        Get();
		JavaScriptPL();
		Expect(0);

	}
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
            case 1:
                s = "SEMI expected";
                break;
            case 2:
                s = "OPENBRACE expected";
                break;
            case 3:
                s = "CLOSEBRACE expected";
                break;
            case 4:
                s = "OPENBRACKET expected";
                break;
            case 5:
                s = "CLOSEBRACKET expected";
                break;
            case 6:
                s = "STRING expected";
                break;
            case 7:
                s = "WORD expected";
                break;
            case 8:
                s = "NUMBER expected";
                break;
            case 9:
                s = "OPERATOR expected";
                break;
            case 10:
                s = "BINBOOLOP expected";
                break;
            case 11:
                s = "UNIBOOLOP expected";
                break;
            case 12:
                s = "ASSIGNMENT expected";
                break;
            case 13:
                s = "QUESTION expected";
                break;
            case 14:
                s = "COLON expected";
                break;
            case 15:
                s = "DOUBLEOP expected";
                break;
            case 16:
                s = "??? expected";
                break;
			default:
				s = "error " + n;
				break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
