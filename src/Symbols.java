import java.util.HashSet;
import java.util.Set;

/**
 * Created by Joe on 10/12/2016.
 */
abstract public class Symbols {

    static public final Terminal LAMBDA = new Terminal.Lamda();
    static public final Terminal DOLLAR = new Terminal.Dollar();


    static public abstract class Action extends Symbols implements Runnable {
    }

    static public class NoTerminal extends Symbols {
        private String name;

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

    }

    static public class Terminal<T extends TokenFactory.IToken> extends Symbols {
        private Set<Terminal> thisSet;

        public final Class<? extends TokenFactory.IToken> tokenClass ;
        public Terminal(Class<T> tc) {
            tokenClass = tc;
            thisSet = new HashSet<Terminal>();
            thisSet.add(this);
        }


        @Override
        public String toString() {
            return "TS(" + tokenClass.getSimpleName() + ")";
        }

        @Override
        public boolean equals(Object o) {
            return toString().equals(o.toString());
        }

        @Override
        public int hashCode() {
            return tokenClass.getSimpleName().hashCode();
        }

        private static class Lamda extends Terminal<TokenFactory.IToken> {
            public Lamda() {
                super(null);
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

        private static class Dollar extends Terminal<TokenFactory.IToken> {
            public Dollar() {
                super(null);
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

