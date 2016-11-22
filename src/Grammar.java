import javafx.util.Pair;

import java.util.*;

/**
 * Created by matti on 22/11/2016.
 */

public abstract class Grammar {

    abstract static public class Symbols{
        abstract public Set<Terminal> getFirst();
        abstract public Set<Terminal> getFollow();
        static public class NoTerminal extends Symbols{
            private String name ;
            public NoTerminal(String name){ this.name = name; }
            @Override
            public String toString(){ return "NTS("+name+")"; }
            @Override
            public boolean equals(Object o){ return toString().equals(o.toString()); }
            @Override
            public int hashCode(){ return name.hashCode(); }
            @Override
            public Set<Terminal> getFirst(){
                Set<Terminal> set = new HashSet<Terminal>();
                for ( Production production : Production.productionBySymbol.get(this) )
                    set.addAll(production.getFirstSymbol().getFirst());
                return set; // if it is LL it is not going to loop
            }
            @Override
            public Set<Terminal> getFollow(){
                Set<Terminal> set = new HashSet<Terminal>();
                if ( this instanceof Axiom && ! Production.productionsThatUseSymbol.containsKey(this)) return set;
                for ( Production production : Production.productionsThatUseSymbol.get(this) ){
                    Symbols[] symbols = production.generated;
                    for ( int i=0; i<symbols.length; i++ )
                        if ( symbols[i].equals(this) ) {
                            boolean lambdaFound = true, metThisSymbol = false;
                            for (int j = 1 + i
                                 ; lambdaFound && j < symbols.length && (metThisSymbol=symbols[j].equals(this)) == false
                                    ; j++) {
                                Set<Terminal> firsts = symbols[j].getFirst();
                                if (firsts.contains(Terminal.LAMBDA)) {
                                    lambdaFound = true;
                                    firsts.remove(Terminal.LAMBDA);
                                } else lambdaFound = false;
                                set.addAll(firsts);
                            }
                            if ( lambdaFound && !metThisSymbol && production.generating.equals(this)==false ) set.addAll(production.generating.getFollow());
                        }
                }
                return set;
            }
        }
        static public class Terminal<T extends TokenFactory.IToken> extends Symbols{
            public static class Lamda extends Terminal<TokenFactory.IToken>{
                public Lamda(){ super(null); }
                @Override
                public String toString(){ return "TS(lambda)"; }
                @Override
                public boolean equals(Object o){ return toString().equals(o.toString()); }
                @Override
                public int hashCode(){ return "LAMBDA".hashCode(); }
            }
            public static class Dollar extends Terminal<TokenFactory.IToken>{
                public Dollar(){ super(null); }
                @Override
                public String toString(){ return "TS(dollar)"; }
                @Override
                public boolean equals(Object o){ return toString().equals(o.toString()); }
                @Override
                public int hashCode(){ return "DOLLAR".hashCode(); }
            }


            private Set<Terminal> thisSet;
            private Class<T> tokenClass;
            public Terminal(Class<T> tc) {
                tokenClass = tc;
                thisSet = new HashSet<Terminal>();
                thisSet.add(this);
            }
            @Override
            public Set<Terminal> getFirst(){ return thisSet; }
            @Override
            public Set<Terminal> getFollow(){ return new HashSet<Terminal>(); }
            @Override
            public String toString(){ return "TS("+tokenClass.getSimpleName()+")"; }
            @Override
            public boolean equals(Object o){ return toString().equals(o.toString()); }
            @Override
            public int hashCode(){ return tokenClass.getSimpleName().hashCode(); }
        }
        static public final Terminal LAMBDA = new Terminal.Lamda();
        static public final Terminal DOLLAR = new Terminal.Dollar();

        static public class Axiom extends NoTerminal{
            public Axiom(String name){
                super(name);
            }
            @Override
            public Set<Terminal> getFollow(){
                Set<Terminal> set = super.getFollow();
                set.add(Symbols.DOLLAR);
                return set;
            }
        }
    }

    static public class Production{
        static public Set<Production> setOfProduction = new HashSet<Production>();
        static public Map<Symbols.NoTerminal, List<Production>> productionBySymbol = new HashMap<Symbols.NoTerminal, List<Production>>();
        static public Map<Symbols.NoTerminal, List<Production>> productionsThatUseSymbol = new HashMap<Symbols.NoTerminal, List<Production>>();


        public final Symbols.NoTerminal generating ;
        public final Symbols[] generated;
        public Production(Symbols.NoTerminal generating, Symbols... generated){
            if ( generating == null || generated == null || generated.length<1 )
                throw new RuntimeException("Badly formatted production");
            this.generated = generated;
            this.generating = generating;
            setOfProduction.add(this);
            List<Production> list = productionBySymbol.get(generating);
            if (list == null ) list = new LinkedList<Production>();
            list.add(this);
            productionBySymbol.put(generating, list);
            for ( Symbols x : generated)
                if (x instanceof Symbols.NoTerminal){
                    List<Production> l = productionsThatUseSymbol.get(x);
                    if (l == null ) l = new LinkedList<Production>();
                    l.add(this);
                    productionsThatUseSymbol.put((Symbols.NoTerminal)x, l);
                }
        }
        public Symbols getFirstSymbol() { return generated[0]; }
        public Set<Symbols.Terminal> getFirstSet(){
            Set<Symbols.Terminal> set = new HashSet<Symbols.Terminal>();
            boolean thereIsLambda = true;
            int i=0;
            do {
                Symbols symbols = generated[i];
                set.addAll(symbols.getFirst());
                if ( set.contains(Symbols.LAMBDA )){
                    thereIsLambda=true;
                    set.remove(Symbols.LAMBDA);
                }else thereIsLambda = false;
                i++;
            }while( i<generated.length && thereIsLambda );
            if ( thereIsLambda )
                set.addAll(generating.getFollow());
            return set;
        }

        static public Set<Symbols.Terminal> first(Symbols.NoTerminal symbol){
            Set<Symbols.Terminal> set = new HashSet<Symbols.Terminal>();
            if ( productionBySymbol.containsKey(symbol) )
                for ( Production production : productionBySymbol.get(symbol) ){
                    Symbols fs = production.getFirstSymbol();
                    set.addAll( fs.getFirst() );
                }
            else throw new RuntimeException("No production for symbol "+symbol);
            return set;
        }
        @Override
        public String toString(){
            String res = generating+"-> ";
            for ( Symbols s : generated )
                res+=s+" ";
            return res;
        }
    }


    static public class ProductionSet{


        public ProductionSet(){
            Symbols.NoTerminal S = new Symbols.Axiom("S");
            Symbols.NoTerminal A = new Symbols.NoTerminal("A");
            Symbols.NoTerminal B = new Symbols.NoTerminal("B");
            Symbols.Terminal<TokenFactory.TokenFolder.NumberToken> a
                    = new Symbols.Terminal<TokenFactory.TokenFolder.NumberToken>(TokenFactory.TokenFolder.NumberToken.class);
            Symbols.Terminal<TokenFactory.TokenFolder.SemiToken> b
                    = new Symbols.Terminal<TokenFactory.TokenFolder.SemiToken>(TokenFactory.TokenFolder.SemiToken.class);
            Symbols.Terminal<TokenFactory.TokenFolder.ColonToken> c
                    = new Symbols.Terminal<TokenFactory.TokenFolder.ColonToken>(TokenFactory.TokenFolder.ColonToken.class);


            new Production(S, a,A,B,b);
            new Production(A, a,A,c);
            new Production(A, Symbols.LAMBDA);
            new Production(B, b,B);
            new Production(B, c);

            System.out.println("Printng productions: "+Production.setOfProduction);
            System.out.println("Printng productionsBySymbol: "+Production.productionBySymbol);
            System.out.println("Printng productionsForSymbol: "+Production.productionsThatUseSymbol);
            for (Symbols.NoTerminal noTerminal : Production.productionBySymbol.keySet() )
                System.out.println("Debugging symbol "+noTerminal+"\n\tFirst: "+noTerminal.getFirst()+"\n\tFollow: "+noTerminal.getFollow());

        }
    }

    static public class PharsingTable{
        public final Map<Symbols.NoTerminal, Map<Symbols.Terminal, Production>> table
                = new HashMap<Symbols.NoTerminal, Map<Symbols.Terminal, Production>>();
        public PharsingTable(ProductionSet productionSet){
            for (Map.Entry<Symbols.NoTerminal, List<Production>> item: Production.productionBySymbol.entrySet()){
                Symbols.NoTerminal noTerminal = item.getKey();
                List<Production> productionList = item.getValue();
                if ( !table.containsKey(noTerminal)) table.put(noTerminal, new HashMap<Symbols.Terminal, Production>());
                Map<Symbols.Terminal, Production> row = table.get(noTerminal);
                for (Production production : productionList )
                    for (Symbols.Terminal terminal : production.getFirstSet() )
                        if ( row.containsKey(terminal) ) throw new RuntimeException("This is not LL(1)");
                        else row.put(terminal, production);
            }
        }
        @Override
        public String toString(){
            return table.toString();
        }
    }

}
