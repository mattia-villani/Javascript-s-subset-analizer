import java.util.*;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Created by Joe on 10/12/2016.
 */
abstract public class Symbols {
    static public final Terminal LAMBDA = new Terminal.Lamda();
    static public final Terminal DOLLAR = new Terminal.Dollar();

    static abstract public class NonActionSymbol extends Symbols {
        private Map<String,Object> state;

        public final Object get(String key){
            if ( state.containsKey(key) )
                return state.get(key);
            throw new RuntimeException("Field "+key+" of Symbol "+this+" wasn't defined. Semantic parser error");
        }
        public final <T> T get(String key, Class<T> type){
            return type.cast(get(key));
        }
        public final NonActionSymbol set(String key, Object val){
            state.put(key,val);
            return this;
        }

        protected String name = "";
        public String getName() { return name; }
        abstract public Symbols init();
    }

    static public abstract class Action extends Symbols implements Consumer<Action.Context> {
        static public class Context {
            private HashMap<String, Symbols> inner = new HashMap<>();
            public Context put(String key, Symbols val){
                inner.put(key, val);
                return this;
            }
            public Symbols get(String key){
                if ( inner.containsKey(key) ) return inner.get(key);
                throw new RuntimeException("Unpushed symbol reference "+key+". (only "+inner.keySet()+")");
            }
            public boolean containsKey( String key ){ return inner.containsKey(key); }
            @Override
            public String toString(){
                return inner.keySet().toString();
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
                    public void accept(Context context) {
                        This.accept(context);
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
            return "NTS(" + name + ")";
        }

        @Override
        public boolean equals(Object o) {
            return toString().equals(o.toString());
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
            return "TS(" + getName() + ")";
        }

        @Override
        public boolean equals(Object o) {
            return toString().equals(o.toString());
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
            public String toString() {
                return "TS(lambda)";
            }

            @Override
            public boolean equals(Object o) {
                return toString().equals(o.toString());
            }

            @Override
            public int hashCode() {
                return "LAMBDA".hashCode();
            }
        }

        private static class Dollar extends Terminal<TokenFactory.TokenFolder.EofToken> {
            public Dollar() {
                super(TokenFactory.TokenFolder.EofToken.class,new TokenFactory.TokenFolder.EofToken());this.name = "dollar";
            }

            @Override
            public String toString() {
                return "TS(dollar)";
            }

            @Override
            public boolean equals(Object o) {
                return toString().equals(o.toString());
            }

            @Override
            public int hashCode() {
                return "DOLLAR".hashCode();
            }
        }
    }

    static public class Axiom extends NoTerminal {
        public Axiom(String name) {
            super(name);
        }
    }


}

