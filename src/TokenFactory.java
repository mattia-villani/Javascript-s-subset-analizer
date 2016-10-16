import javafx.util.Pair;

import java.util.HashMap;

interface IPreAssignmentOperation {
    public String getLexema();
}

interface IToken {
}

/**
 * Created by Joe on 16/10/2016.
 */
public class TokenFactory {

    static public IToken create(Token t, GlobalTableOfSymbols ts) {
        return createFromKind(t.kind, t.val, ts);
    }

    static private IToken createFromKind(int kind, String val, GlobalTableOfSymbols ts) {

        switch (kind) {
            case SemiColonToken.Type:
                return new SemiColonToken();
            case OpenBraceToken.Type:
                return new OpenBraceToken();
            case CloseBraceToken.Type:
                return new CloseBraceToken();
            case OpenBracketToken.Type:
                return new OpenBracketToken();
            case CloseBracketToken.Type:
                return new CloseBracketToken();
            case StringToken.Type:
                return new StringToken(val);
            case IdentifierToken.Type:
                Pair<Integer, Integer> pair = ts.queryLexema(val);
                if (pair.getKey() == -1)
                    return ReservedWordToken.create(val);
                else
                    return new IdentifierToken(val, pair);
            case NumberToken.Type:
                return new NumberToken(val);
            case ArithmeticOperatorToken.Type:
                if (val.equals("+"))
                    return new ArithmeticPlusOperatorToken();
                else if (val.equals("-"))
                    return new ArithmeticMinusOperatorToken();
                else if (val.equals("*"))
                    return new ArithmeticMultiplyOperatorToken();
                else if (val.equals("/"))
                    return new ArithmeticDivideOperatorToken();
                else
                    return new ArithmeticModuloOperatorToken();
            case BinaryLogicalOperatorToken.Type:
                if (val.equals("&&"))
                    return new BooleanAndToken();
                else
                    return new BooleanOrToken();
            case BooleanNotToken.Type:
                return new BooleanNotToken();
            case AssignmentOperatorToken.Type:
                if (val.equals("="))
                    return new AssignmentOperatorToken();
                else {
                    String subVal = val.substring(0, 1);
                    IToken token = createFromKind(ArithmeticOperatorToken.Type, subVal, ts);
                    if (token != null) return new ComplexOperatorToken((ArithmeticOperatorToken) token);
                    else {
                        token = createFromKind(BinaryLogicalOperatorToken.Type, subVal + subVal, ts);
                        if (token != null) return new ComplexOperatorToken((BinaryLogicalOperatorToken) token);
                        else throw new RuntimeException("Not reachable");
                    }
                }
            case QuestionOperatorToken.Type:
                return new QuestionOperatorToken();
            case ColonOperatorToken.Type:
                return new ColonOperatorToken();
            case DoubleOperatorToken.Type:
                if (val.equals(PlusDoubleOperatorToken.Lexema))
                    return new PlusDoubleOperatorToken();
                else
                    return new MinusDoubleOperatorToken();

            default:
                throw (new RuntimeException("Unrecognised Token"));
        }
    }
}

class SemiColonToken extends UnvaluedToken {
    static public final int Type = 1;

    public SemiColonToken() {
        super(Type, "SEMICOLON");
    }
}

class OpenBraceToken extends UnvaluedToken {
    static public final int Type = 2;

    public OpenBraceToken() {
        super(Type, "OPENBRACE");
    }
}

class CloseBraceToken extends UnvaluedToken {
    static public final int Type = 3;

    public CloseBraceToken() {
        super(Type, "CLOSEBRACE");
    }
}

class OpenBracketToken extends UnvaluedToken {
    static public final int Type = 4;

    public OpenBracketToken() {
        super(Type, "OPENBRACKET");
    }
}

class CloseBracketToken extends UnvaluedToken {
    static public final int Type = 5;

    public CloseBracketToken() {
        super(Type, "CLOSEBRACKET");
    }
}

class StringToken extends ValuedToken {
    static public final int Type = 6;

    public StringToken(String val) {
        super(Type, "STRING", val.substring(1, val.length() - 1));
    }
}

class NumberToken extends ValuedToken {
    static public final int Type = 8;
    private int intVal;

    public NumberToken(String val) {
        super(Type, "NUM", val);
        intVal = Integer.valueOf(val);
    }
};

class ArithmeticOperatorToken extends ValuedToken implements IPreAssignmentOperation {
    static public final int Type = 9;
    protected String lexema;

    public ArithmeticOperatorToken(String specificName, String lexema) {
        super(Type, "ARITOP", specificName);
        this.lexema = lexema;
    }

    @Override
    public String getLexema() {
        return lexema;
    }
};

class ArithmeticPlusOperatorToken extends ArithmeticOperatorToken {
    public ArithmeticPlusOperatorToken() {
        super("PLUS", "+");
    }
};

class ArithmeticMinusOperatorToken extends ArithmeticOperatorToken {
    public ArithmeticMinusOperatorToken() {
        super("MINUS", "-");
    }
};

class ArithmeticMultiplyOperatorToken extends ArithmeticOperatorToken {
    public ArithmeticMultiplyOperatorToken() {
        super("MULTIPLY", "*");
    }
};

class ArithmeticDivideOperatorToken extends ArithmeticOperatorToken {
    public ArithmeticDivideOperatorToken() {
        super("DIVIDE", "/");
    }
}

class ArithmeticModuloOperatorToken extends ArithmeticOperatorToken {
    public ArithmeticModuloOperatorToken() {
        super("MODULO", "%");
    }
}

class BinaryLogicalOperatorToken extends ValuedToken implements IPreAssignmentOperation {
    static public final int Type = 10;
    private String lexema;

    public BinaryLogicalOperatorToken(String name, String lexema) {
        super(Type, name, lexema);
        this.lexema = lexema;
    }

    public String getLexema() {
        return lexema;
    }
}

class BooleanAndToken extends BinaryLogicalOperatorToken {
    static public final String Lexema = "&&";

    public BooleanAndToken() {
        super("AND", Lexema);
    }
}

class BooleanOrToken extends BinaryLogicalOperatorToken {
    static public final String Lexema = "||";

    public BooleanOrToken() {
        super("OR", Lexema);
    }
}

class BooleanNotToken extends UnvaluedToken {
    static public final int Type = 11;

    public BooleanNotToken() {
        super(Type, "NOT");
    }
}

class AssignmentOperatorToken extends ValuedToken {
    static public final int Type = 12;

    public AssignmentOperatorToken() {
        this("=");
    }

    public AssignmentOperatorToken(String lexema) {
        super(Type, "ASSIGN", lexema);
    }
}

class ComplexOperatorToken extends AssignmentOperatorToken {
    private IPreAssignmentOperation preAssignmentOperation;

    public ComplexOperatorToken(IPreAssignmentOperation p) {
        super(p.getLexema().substring(0, 1) + "=");
        preAssignmentOperation = p;
    }
}

class QuestionOperatorToken extends UnvaluedToken {
    static public final int Type = 13;
    static public final String Lexema = "?";

    public QuestionOperatorToken() {
        super(Type, "QUESTION");
    }
}

class ColonOperatorToken extends UnvaluedToken {
    static public final int Type = 14;
    static public final String Lexema = ":";

    public ColonOperatorToken() {
        super(Type, "COLON");
    }
}

class DoubleOperatorToken extends ValuedToken {
    static public final int Type = 15;

    public DoubleOperatorToken(String lexema) {
        super(Type, lexema, lexema);
    }
}

class PlusDoubleOperatorToken extends DoubleOperatorToken {
    static public final String Lexema = "++";

    public PlusDoubleOperatorToken() {
        super("INC");
    }
}

class MinusDoubleOperatorToken extends DoubleOperatorToken {
    static public final String Lexema = "--";

    public MinusDoubleOperatorToken() {
        super("DEC");
    }
}

class IfToken extends ReservedWordToken {
    static final public int Type = 16;

    public IfToken() {
        super(Type, "IF", "if");
    }
}

class ElseToken extends ReservedWordToken {
    static final public int Type = 17;

    public ElseToken() {
        super(Type, "ELSE", "else");
    }
}

class WhileToken extends ReservedWordToken {
    static final public int Type = 18;

    public WhileToken() {
        super(Type, "WHILE", "while");
    }
}

class DoToken extends ReservedWordToken {
    static final public int Type = 18;

    public DoToken() {
        super(Type, "DO", "do");
    }
}

class SwitchToken extends ReservedWordToken {
    static final public int Type = 18;

    public SwitchToken() {
        super(Type, "SWITCH", "switch");
    }
}

class BreakToken extends ReservedWordToken {
    static final public int Type = 18;

    public BreakToken() {
        super(Type, "BREAK", "break");
    }
}

class CaseToken extends ReservedWordToken {
    static final public int Type = 18;

    public CaseToken() {
        super(Type, "CASE", "case");
    }
}

class ForToken extends ReservedWordToken {
    static final public int Type = 18;

    public ForToken() {
        super(Type, "FOR", "for");
    }
}

class VarToken extends ReservedWordToken {
    static final public int Type = 19;

    public VarToken() {
        super(Type, "VAR", "var");
    }
}

class FunctionToken extends ReservedWordToken {
    static final public int Type = 20;

    public FunctionToken() {
        super(Type, "FUNCTION", "function");
    }
}

class ReturnToken extends ReservedWordToken {
    static final public int Type = 21;

    public ReturnToken() {
        super(Type, "RETURN", "return");
    }
}

abstract class ReservedWordToken extends UnvaluedToken {
    public static HashMap<String, ReservedWordToken> map;

    static {
        map = new HashMap<String, ReservedWordToken>();
        map.put("if", new IfToken());
        map.put("else", new ElseToken());
        map.put("while", new WhileToken());
        map.put("do", new DoToken());
        map.put("switch", new SwitchToken());
        map.put("break", new BreakToken());
        map.put("case", new CaseToken());
        map.put("for", new ForToken());
        map.put("var", new VarToken());
        map.put("function", new FunctionToken());
        map.put("return", new ReturnToken());
    }

    protected int type;
    protected String lexema;

    public ReservedWordToken(int type, String val, String lexema) {
        super(type, val);
        this.type = type;
        this.lexema = lexema;
    }

    static ReservedWordToken create(String val) {
        return map.get(val);
    }

    public String getLexema() {
        return lexema;
    }
}

class IdentifierToken extends ValuedToken {
    public static final int Type = 7;
    private Integer tableIndex;
    private Integer index;

    public IdentifierToken(String lexema, Pair<Integer, Integer> pair) {
        super(Type, "ID", "[" + pair.getKey() + ", " + pair.getValue() + "](" + lexema + ")");
        tableIndex = pair.getKey();
        index = pair.getValue();
    }
}

class ValuedToken implements IToken {
    protected String value, name;
    private int type;

    public ValuedToken(int type, String name, String value) {
        this.value = value;
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "<" + name + "(" + type + ")," + value + ">";
    }
}

class UnvaluedToken implements IToken {
    protected String name;
    private int type;

    public UnvaluedToken(int type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "<" + name + "(" + type + "), >";
    }
}