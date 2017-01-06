import java.util.*;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.*;

/**
 * Created by Joe on 10/12/2016.
 */
abstract public class Symbols {
    static public final Terminal LAMBDA = new Terminal.Lamda();
    static public final Terminal DOLLAR = new Terminal.Dollar();

    static abstract public class NonActionSymbol extends Symbols {
        private static int count = 0;
        private final int id = 0;//++count;

        protected Map<Grammar.ATT,Object> state;

        public Object getFromState(Grammar.ATT att){
            return state.get(att);
        }

        public final Object get(Grammar.ATT key){
            if ( state != null && state.containsKey(key) )
                return state.get(key);
            throw new RuntimeException("Field "+key+" of Symbol "+this+" wasn't defined. Semantic parser error");
        }
        public final <T> T get(Grammar.ATT key, Class<T> type){
            return type.cast(get(key));
        }
        public final NonActionSymbol set(Grammar.ATT key, Object val){
            if ( state == null ) state = new HashMap<>();
            state.put(key,val);
            return this;
        }

        protected String name = "";
        public String getName() { return name; }
        abstract public Symbols init();

        public int getId(){ return id; }
    }

    static public abstract class Action extends Symbols implements BiConsumer<Action.Context,Grammar.S> {
        static public class Context {
            static public Token token;
            public static final class Error {
                public final int line, col;
                public final String reason;
                public final String tk;
                public Error(int l, int c,String tk, String r){
                    line = l;
                    col = c;
                    reason = r;
                    this.tk = tk;
                }
            }
            public final static List<Error> errors = new LinkedList<>();

            public HashMap<String, NonActionSymbol> inner = new HashMap<>();
            public final NoTerminal productionRoot ;

            public Context(NoTerminal productionRoot){ this.productionRoot=productionRoot;}

            public Context put(String key, NonActionSymbol val){
                inner.put(key, val);
                return this;
            }

            public NonActionSymbol get(String key){
                if ( inner.containsKey(key) ) return inner.get(key);
                throw new RuntimeException("Unpushed symbol reference "+key+". (only "+inner.keySet()+")");
            }
            public static void err(String reason){
                errors.add( new Error(token.line, token.col, token.val, reason) );
            }
            public boolean containsKey( String key ){ return inner.containsKey(key); }
            @Override
            public String toString(){
                return inner.toString();
            }
        }

        protected Context context;
        protected Symbols setContext(Context context){
            this.context = context;
            return this;
        }
        public Symbols init( Context context ) {
            Action This = this;
            return new Action() {
                    @Override
                    public void accept(Context context, Grammar.S root) {
                        This.accept(context, root);
                    }
                }.setContext(context);
        }
    }

    static public class NoTerminal extends NonActionSymbol{

        public NoTerminal(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            int id = getId();
            return "NTS(" + name +(id==0?"":"["+id+"]")+( state==null?"":("."+state))+ ")";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof NoTerminal && getName().equals(((NoTerminal)o).getName());
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public Symbols init() {
            return new NoTerminal(name);
        }
    }

    static public class Terminal<T extends TokenFactory.IToken> extends NonActionSymbol {
        public final Class<? extends TokenFactory.IToken> tokenClass ;
        public TokenFactory.IToken token;
        public Terminal(Class<T> tc, T tk) {
            tokenClass = tc;
            if ( tokenClass != null )
                this.name = tokenClass.getSimpleName();
            token = tk;
        }
        public Terminal(T tk){
            this((Class<T>)tk.getClass(),tk);
        }
        public Terminal(Class<T> tc){ this(tc,null); }

        @Override
        public String toString() {
            int id = getId();
            return "TS(" + getName() +(id==0?"":"["+id+"]")+( state==null?"":("."+state))+")";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Terminal && getName().equals(((Terminal)o).getName());
        }

        @Override
        public int hashCode() {
            return tokenClass.getSimpleName().hashCode();
        }

        public Symbols init(){
            if ( token == null )
                return new Terminal<TokenFactory.IToken>((Class<TokenFactory.IToken>)tokenClass);
            else
                return new Terminal<TokenFactory.IToken>(token);
        }

        private static class Lamda extends Terminal<TokenFactory.IToken> {
            public Lamda() {
                super(null,null); this.name="lambda";
            }
            @Override
            public int getId(){ return 0; }
            @Override
            public int hashCode() {
                return name.hashCode();
            }
        }

        private static class Dollar extends Terminal<TokenFactory.TokenFolder.EofToken> {
            public Dollar() {
                super(TokenFactory.TokenFolder.EofToken.class,new TokenFactory.TokenFolder.EofToken());
                this.name = "dollar";
            }
            @Override
            public int getId(){ return 0; }
            @Override
            public int hashCode() {
                return name.hashCode();
            }

        }
    }

    static public class Axiom extends NoTerminal {
        public Axiom(String name) {
            super(name);
        }
    }


}

