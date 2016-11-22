import javafx.util.Pair;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by Mattia and Joe on 16/10/2016.
 */

public class TokenFactory {
    interface ITableOfSymbols { Pair<Integer, Integer> queryLexema(String lexema); }
    interface ICustomInstanceGetterToken { TokenClassGetter getTokenClassGetter(String lexema); }
    interface IToken<T> {
        int getTokenType();
        String getName();
        T getValue();
    }
    interface IPreAssignmentOperation<T> extends IToken<T>{}


    abstract private static class CToken<T> implements IToken<T> {
        private Integer type;
        private String name;
        protected T value;
        public CToken<T> setValues(String value, ITableOfSymbols tableOfSymbols){
            this.value= (this.value != null) ? this.value : parseStringValueToTValue(value, tableOfSymbols);
            return this;
        }
        abstract T parseStringValueToTValue ( String value, ITableOfSymbols tableOfSymbols );
        protected abstract String getFancyValue();
        @Override
        public final int getTokenType(){
            if (type == null){
                Class<?> clazz = this.getClass();
                while(clazz.equals(Object.class) == false)
                    if ( reversedTokenRapresentants.containsKey(clazz) ){
                        type = reversedTokenRapresentants.get(clazz);
                        break;
                    }else clazz = clazz.getSuperclass();
                if ( type == null )
                    throw new RuntimeException("Token type for class "+this.getClass().getSimpleName()+" unfound");
            }
            return type;
        }
        @Override
        public T getValue(){return value;}
        @Override
        public String getName() { return (name!=null)?name:(name=this.getClass().getSimpleName().replace("Token", "").toUpperCase()); }
        @Override
        public String toString() {
            String val = getFancyValue();
            String nam = getFancyName ();
            String com = getFancyComment();
            return "<" + nam + "(" + getTokenType() + "), "+val+">"+((com==null)?"":"   // "+com);
        }
        protected String getFancyName() { return getName(); }
        protected String getFancyComment () { return null; }
    }

    abstract static class ValuedToken<T> extends CToken<T>{
        @Override
        protected String getFancyValue() {
            T val = getValue();
            return val == null ? "" : val.toString();
        }
    }

    static class UnvaluedToken extends CToken<Void> {
        @Override
        final Void parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {return null;}

        @Override
        protected String getFancyValue() {
            return "";
        }
    }

    static abstract class TokenClassGetter{
        private Class<IToken> tokenClass ;
        public TokenClassGetter(Class<IToken> tokenClass){ this.tokenClass = tokenClass; }
        public Class<IToken> getTokenClass(){ return tokenClass; }
        abstract public IToken getInstance(String lexema, ITableOfSymbols tableOfSymbols) throws IllegalAccessException, InstantiationException;
        static TokenClassGetter create(
                final Class<IToken> tokenClass,
                final int type,
                final String name
        )throws IllegalAccessException, InstantiationException{
            if ( Arrays.asList(tokenClass.getInterfaces()).contains(ICustomInstanceGetterToken.class) ) {
                final ICustomInstanceGetterToken savedInstance = (ICustomInstanceGetterToken)tokenClass.newInstance();
                return new TokenClassGetter(tokenClass) {
                    @Override
                    public IToken getInstance(String lexema, ITableOfSymbols tableOfSymbols) throws IllegalAccessException, InstantiationException {
                        return ((CToken)savedInstance
                                .getTokenClassGetter(lexema)
                                .getInstance(lexema, tableOfSymbols)
                        ).setValues(lexema, tableOfSymbols);
                    }
                };
            } else if ( tokenClass.isInstance(UnvaluedToken.class) ) {
                final IToken instance = ((CToken)tokenClass.newInstance());
                return new TokenClassGetter(tokenClass) {
                    @Override
                    public IToken getInstance(String lexema, ITableOfSymbols tableOfSymbols) {
                        return instance;
                    }
                };
            }else return new TokenClassGetter(tokenClass) {
                @Override
                public IToken getInstance(String lexema, ITableOfSymbols tableOfSymbols)
                        throws IllegalAccessException, InstantiationException {
                    return ((CToken)tokenClass.newInstance()).setValues(lexema, tableOfSymbols);
                }
            };
        }

        static class SimpleTokenClassGetter extends TokenClassGetter{
            IToken memory ;
            public SimpleTokenClassGetter ( IToken toSave ){
                super ((Class<IToken>) toSave.getClass());
                memory = toSave;
            }
            @Override
            public IToken getInstance(String lexema, ITableOfSymbols tableOfSymbols) throws IllegalAccessException, InstantiationException {
                return memory;
            }
        }
    }


    static final boolean verbose = false;
    static private HashMap<Integer,TokenClassGetter> tokenRapresentants;
    static private HashMap<Class<IToken>,Integer> reversedTokenRapresentants;

    static private boolean putInMaps ( Integer id, TokenClassGetter tcg ){
        if ( tokenRapresentants.containsKey(id) ) return false;
        tokenRapresentants.put(id,tcg);
        reversedTokenRapresentants.put(tcg.getTokenClass(), id);
        return true;
    }

    static public void init (){
        tokenRapresentants = new HashMap<Integer, TokenClassGetter>();
        reversedTokenRapresentants = new HashMap<Class<IToken>, Integer>();
        for ( Class<?> tokenClass : TokenFolder.class.getDeclaredClasses() ) {
            if ( verbose ) System.out.println("Iterating over "+tokenClass.getSimpleName());
            try {
                String tokenName = tokenClass.getSimpleName().replace("Token", "").toUpperCase();
                Field field = Parser.class.getField("_" + tokenName);
                TokenClassGetter instancer = TokenClassGetter.create((Class<IToken>)tokenClass,field.getInt(null),tokenName);
                putInMaps(field.getInt(null), instancer);
                if (verbose) System.out.println("Added " + tokenClass.getSimpleName() + " as " + field.getInt(null));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                System.err.println("The Java code's token " + tokenClass.getSimpleName() + " is missing in the grammar");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                System.err.println("Probably the name " + tokenClass.getSimpleName() + " is in use");
            } catch (ClassCastException e) {
                e.printStackTrace();
                System.err.println("Token folder bad formatted: " + tokenClass.getSimpleName() + " does not implement IToken");
            } catch (InstantiationException e) {
                e.printStackTrace();
                System.err.println("Can't make instance of the class "+tokenClass.getSimpleName());
            }
        }
    }

    static public IToken create(Token t, ITableOfSymbols ts) throws Exception {
        return createFromKind(t.kind, t.val, ts);
    }

    static private IToken createFromKind(int kind, String val, ITableOfSymbols ts) throws Exception {
        if ( tokenRapresentants == null ) init();
        if ( tokenRapresentants.containsKey(kind) == false )
            throw new RuntimeException("Error, kind of token unknown : "+ kind);
        else
            try{
                if ( verbose ) System.out.println("PARSING : "+kind+"("+val+")");
                return tokenRapresentants.get(kind).getInstance(val, ts);
            }catch (Exception e){
                e.printStackTrace();
                System.err.println("Error getting instance of "+kind);
                throw  e;
            }
    }

    public interface TokenFolder {
        class SemiToken extends UnvaluedToken {}
        class OpenBraceToken extends UnvaluedToken {}
        class CloseBraceToken extends UnvaluedToken {}
        class OpenBracketToken extends UnvaluedToken {}
        class CloseBracketToken extends UnvaluedToken {}
        class UniBoolOpToken extends UnvaluedToken {}
        class QuestionToken extends UnvaluedToken {}
        class ColonToken extends UnvaluedToken {}


        class StringToken extends ValuedToken<String> {
            @Override
            String parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {
                return value.substring(1, value.length() - 1);
            }
        }
        class NumberToken extends ValuedToken<Integer> {
            @Override
            Integer parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {
                return Integer.valueOf(value);
            }
        }
        class DoubleOpToken extends UnvaluedToken implements ICustomInstanceGetterToken{
            static class PlusDoubleOpToken extends DoubleOpToken {}
            static class MinusDoubleOpToken extends DoubleOpToken {}
            private TokenClassGetter plus;
            private TokenClassGetter minus;
            @Override
            public TokenClassGetter getTokenClassGetter(String lexema) {
                if ( plus == null ){
                    plus = new TokenClassGetter.SimpleTokenClassGetter(new PlusDoubleOpToken());
                    minus = new TokenClassGetter.SimpleTokenClassGetter(new MinusDoubleOpToken());
                }
                return lexema.equals("++") ? plus : minus;
            }
        }

        class OperatorToken extends UnvaluedToken implements IPreAssignmentOperation<Void>, ICustomInstanceGetterToken{
            class PlusOperatorToken extends DivideOperatorToken {}
            class MinusOperatorToken extends OperatorToken {}
            class MultiplyOperatorToken extends OperatorToken {}
            class DivideOperatorToken extends OperatorToken {}
            class ModuloOperatorToken extends OperatorToken {}
            HashMap<String, TokenClassGetter> stored = null;
            @Override
            public TokenClassGetter getTokenClassGetter(String lexema) {
                if ( stored == null ){
                    stored = new HashMap<String, TokenClassGetter>();
                    stored.put("+", new TokenClassGetter.SimpleTokenClassGetter(new PlusOperatorToken()));
                    stored.put("-", new TokenClassGetter.SimpleTokenClassGetter(new MinusOperatorToken()));
                    stored.put("*", new TokenClassGetter.SimpleTokenClassGetter(new MultiplyOperatorToken()));
                    stored.put("/", new TokenClassGetter.SimpleTokenClassGetter(new DivideOperatorToken()));
                    stored.put("%", new TokenClassGetter.SimpleTokenClassGetter(new ModuloOperatorToken()));
                }
                return stored.get(lexema);
            }
        }

        class BinBoolOpToken extends UnvaluedToken implements IPreAssignmentOperation<Void>, ICustomInstanceGetterToken {
            class BooleanAndToken extends BinBoolOpToken {}
            class BooleanOrToken extends BinBoolOpToken {}
            TokenClassGetter andGetter;
            TokenClassGetter orGetter;
            @Override
            public TokenClassGetter getTokenClassGetter(String lexema) {
                if ( andGetter == null ) {
                    andGetter = new TokenClassGetter.SimpleTokenClassGetter(new BooleanAndToken());
                    orGetter = new TokenClassGetter.SimpleTokenClassGetter(new BooleanOrToken());
                }
                if ( lexema.equals("&&")) return andGetter;
                if ( lexema.equals("||")) return orGetter;
                return null;
            }
        }

        class AssignmentToken<T> extends ValuedToken<T> implements ICustomInstanceGetterToken {
            private PreAssigmentToken pat = null;
            @Override
            T parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {return null;}
            class PreAssigmentToken extends AssignmentToken<IPreAssignmentOperation> {
                private OperatorToken opt = null ;
                private BinBoolOpToken bbot = null ;
                @Override
                IPreAssignmentOperation parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {
                    String subVal = value.substring(0, 1);
                    TokenClassGetter tokenGetter=(opt!=null?opt:(opt=new OperatorToken())).getTokenClassGetter(subVal);
                    if (tokenGetter == null)
                        tokenGetter=(bbot!=null?bbot:(bbot=new BinBoolOpToken())).getTokenClassGetter(subVal + subVal) ;
                    if ( tokenGetter == null) throw new RuntimeException("Not reachable");
                    try {
                        return (tokenGetter == null) ? null : (IPreAssignmentOperation)tokenGetter.getInstance(value, null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }
            @Override
            public TokenClassGetter getTokenClassGetter(String lexema) {
                if (lexema.equals("="))
                    return new TokenClassGetter.SimpleTokenClassGetter(new AssignmentToken());
                else {
                    IToken iToken = (pat!=null?pat:(pat=new PreAssigmentToken())).setValues(lexema, null);
                    if ( iToken == null) throw new RuntimeException("Not reachable");
                    return new TokenClassGetter.SimpleTokenClassGetter(iToken);
                }
            }
        }


        class WordToken<T> extends ValuedToken<T> implements ICustomInstanceGetterToken {
            @Override
            T parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {
                throw new RuntimeException("Not Reachable, a inner class should be called instead");
            }

            @Override
            public TokenClassGetter getTokenClassGetter(String lexema) {
                return new TokenClassGetter((Class<IToken>) (Class<?>) (IdentifierToken.class)) {
                    @Override
                    public IToken getInstance(String lexema, ITableOfSymbols tableOfSymbols) throws IllegalAccessException, InstantiationException {
                        final Pair<Integer, Integer> pair = tableOfSymbols.queryLexema(lexema);
                        if (pair == null) throw new RuntimeException("Something went wrong: point not reachable");
                        else if (pair.getKey() == -1)
                            return new ReservedWordToken().getTokenClassGetter(lexema).getInstance(lexema, tableOfSymbols);
                        else
                            return new IdentifierToken(pair, lexema);
                    }
                };
            }

            class IdentifierToken extends WordToken<Pair<Integer, Integer>>  {
                private String lexema ;
                public IdentifierToken(Pair<Integer,Integer> initPair, String lexema){
                    value = initPair;
                    this.lexema = lexema;
                }
                @Override
                public String getFancyComment (){
                    return this.lexema;
                }
                @Override
                Pair<Integer, Integer> parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {
                    return tableOfSymbols.queryLexema(value);
                }
                @Override
                protected String getFancyValue() {
                    Pair<Integer,Integer> pair = value;
                    return "["+pair.getKey()+"]["+pair.getValue()+"]";
                }
            }
            static class ReservedWordToken extends WordToken<Void> implements ICustomInstanceGetterToken {
                static class IfToken extends ReservedWordToken {}
                static class ElseToken extends ReservedWordToken {}
                static class WhileToken extends ReservedWordToken {}
                static class DoToken extends ReservedWordToken {}
                static class SwitchToken extends ReservedWordToken {}
                static class BreakToken extends ReservedWordToken {}
                static class CaseToken extends ReservedWordToken {}
                static class ForToken extends ReservedWordToken {}
                static class VarToken extends ReservedWordToken {}
                static class FunctionToken extends ReservedWordToken {}
                static class ReturnToken extends ReservedWordToken {}
                private static HashMap<String, TokenClassGetter> map;

                static Class<ReservedWordToken>[] getReservedWord(){
                    return (Class<ReservedWordToken>[])ReservedWordToken.class.getClasses();
                }

                static private void init() {
                    map = new HashMap<String, TokenClassGetter>();
                    for ( Class<?> clazz : ReservedWordToken.class.getClasses() )
                        if ( clazz.getSimpleName().contains("Token") )
                            try {
                                map.put(clazz.getSimpleName().replace("Token","").toLowerCase(),
                                        new TokenClassGetter.SimpleTokenClassGetter((IToken)clazz.newInstance()));
                            } catch (InstantiationException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                }
                @Override
                public TokenClassGetter getTokenClassGetter(String lexema) {
                    if ( map == null ) init();
                    if ( map.containsKey( lexema ) )
                        return map.get(lexema);
                    else throw new RuntimeException("Unknown reserved word");
                }
                public String getLexema() {
                    return this.getClass().getSimpleName().replace("Token","").toLowerCase();
                }
                @Override
                Void parseStringValueToTValue(String value, ITableOfSymbols tableOfSymbols) {
                    return null;
                }
            }
        }
    }
}


