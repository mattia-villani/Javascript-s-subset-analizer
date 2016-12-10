import java.util.HashSet;
import java.util.Set;

/**
 * Created by Joe on 10/12/2016.
 */
abstract public class Symbols {
    static public final Terminal LAMBDA = new Terminal.Lamda();
    static public final Terminal DOLLAR = new Terminal.Dollar();

    abstract public Set<Terminal> getFirst();

    abstract public Set<Terminal> getFollow();

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

        @Override
        public Set<Terminal> getFirst() {
            Set<Terminal> set = new HashSet<Terminal>();
            for (Production production : Production.productionBySymbol.get(this))
                try {
                    set.addAll(production.getFirstSymbol().getFirst());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            return set; // if it is LL it is not going to loop
        }

        @Override
        public Set<Terminal> getFollow() {
            Set<Terminal> set = new HashSet<Terminal>();
            if (this instanceof Axiom && !Production.productionsThatUseSymbol.containsKey(this)) return set;
            for (Production production : Production.productionsThatUseSymbol.get(this)) {
                Symbols[] symbols = production.generated;
                for (int i = 0; i < symbols.length; i++)
                    if (symbols[i].equals(this)) {
                        boolean lambdaFound = true, metThisSymbol = false;
                        for (int j = 1 + i
                             ; lambdaFound && j < symbols.length && (metThisSymbol = symbols[j].equals(this)) == false
                                ; j++) {
                            Set<Terminal> firsts = symbols[j].getFirst();
                            if (firsts.contains(Terminal.LAMBDA)) {
                                lambdaFound = true;
                                firsts.remove(Terminal.LAMBDA);
                            } else lambdaFound = false;
                            set.addAll(firsts);
                        }
                        if (lambdaFound && !metThisSymbol && production.generating.equals(this) == false)
                            set.addAll(production.generating.getFollow());
                    }
            }
            return set;
        }
    }

    static public class Terminal<T extends TokenFactory.IToken> extends Symbols {
        private Set<Terminal> thisSet;
        private Class<T> tokenClass;


        public Terminal(Class<T> tc) {
            tokenClass = tc;
            thisSet = new HashSet<Terminal>();
            thisSet.add(this);
        }

        @Override
        public Set<Terminal> getFirst() {
            return thisSet;
        }

        @Override
        public Set<Terminal> getFollow() {
            return new HashSet<Terminal>();
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

        public static class Lamda extends Terminal<TokenFactory.IToken> {
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

        public static class Dollar extends Terminal<TokenFactory.IToken> {
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

        @Override
        public Set<Terminal> getFollow() {
            Set<Terminal> set = super.getFollow();
            set.add(Symbols.DOLLAR);
            return set;
        }
    }
}

