import java.util.*;

/**
 * Created by matti on 22/11/2016.
 */

public abstract class Grammar {

    abstract static public class Symbols{
        static public final Terminal LAMBDA = new Terminal.Lamda();
        static public final Terminal DOLLAR = new Terminal.Dollar();

        abstract public Set<Terminal> getFirst();

        abstract public Set<Terminal> getFollow();

        static public abstract class Action extends Symbols implements Runnable {
        }

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
        }

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

        static public Set<Symbols.Terminal> first(Symbols.NoTerminal symbol) {
            Set<Symbols.Terminal> set = new HashSet<Symbols.Terminal>();
            if (productionBySymbol.containsKey(symbol))
                for (Production production : productionBySymbol.get(symbol)) {
                    Symbols fs = production.getFirstSymbol();
                    set.addAll(fs.getFirst());
                }
            else throw new RuntimeException("No production for symbol " + symbol);
            return set;
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

        @Override
        public String toString(){
            String res = generating+"-> ";
            for ( Symbols s : generated )
                res+=s+" ";
            return res;
        }
    }


    static public class ProductionSet{
        Map<String, Symbols> map;
        public ProductionSet(){
            map = new HashMap<String, Symbols>();
            for (Class<?> clazz : TokenFactory.TokenFolder.class.getClasses())
                map.put(clazz.getName().replace("Token", ""), new Symbols.Terminal((Class<TokenFactory.IToken>) clazz));

            //Program -> Sequence
            P(new Symbols.Axiom("Program"), "Sequence");
            //Sequence -> Statement Sequence | lamda
            P("Sequence", "Statement", "Sequence")
                    .or(Symbols.LAMBDA);
            //Delimiter -> Semicolon | NewLine
            P("Delimiter", "Semi")
                    .or("NewLine");
            //Statement -> Declaration | Assignment Delimiter | PreOperation Delimiter | Switch
            //              | FunctionCall Delimiter | FunctionDec
            P("Statement", "Declaration")
                    .or("Assignment", "Delimiter")
                    .or("PreOperation Delimiter")
                    .or("Switch")
                    .or("FunctionCall", "Delimiter")
                    .or("FunctionDec");
            //Declaration -> var Type id Init AdditionalDeclaration
            P("Declaration", "var", "Type", "id", "Init", "AdditionalDeclaration");
            //Init -> = Expression | lambda
            P("Init", "Expression")
                    .or(Symbols.LAMBDA);
            //AdditionalDeclaration -> Comma Type id Init AdditionalDeclaration | Delimiter
            P("AdditionalDeclaration", "comma", "Type", "id", "Init", "AdditionalDeclaration")
                    .or("Delimiter");
            //Assignment -> id equals Expression
            P("Assignment", "id", "equals", "Expression");
            //PreOperation -> DoubleOp id
            P("Assignment", "DoubleOp", "id");
            //Switch -> switch openbracket Expression closebracket openbrace Case Cases closebrace
            P("Switch", "switch", "openbracket", "Expression", "closebracket", "openbrace", "Case", "Cases", "closebrace");
            //Cases -> Case Cases | Lambda
            P("Cases", "Case", "Cases")
                    .or(Symbols.LAMBDA);
            //Case -> case Value colon Sequence Break
            P("Case", "case", "Value", "colon", "Sequence", "Break");
            //Break -> break Delimiter | lamda
            P("Break", "break", "Delimiter")
                    .or(Symbols.LAMBDA);
            //FunctionCall -> id openbracket Arguments closebracket
            P("FunctionCall", "id", "openbracket", "Arguments", "closebracket");
            //Arguments -> Paramlist | lambda
            P("Arguments", "Paramlist")
                    .or(Symbols.Terminal.LAMBDA);
            //Paramlist -> Expression comma Paramlist | Expression
            P("Paramlist", "Expression", "comma", "Paramlist")
                    .or("Expression");
            //Functiondeclaration -> function NullableType id openbracket ArgsDeclaration closebracket openbrace ReturnableSequence closebracket
            P("FunctionDeclaration", "function", "NullableType", "id", "openbracket", "ArgsDeclaration", "closebracket", "openbrace", "ReturnableSequence", "closebracket");
            //NullableType -> Type | Lambda
            P("NullableType", "Type")
                    .or(Symbols.LAMBDA);
            //ReturnableSequence -> Sequence AfterReturn
            P("ReturnableSequence", "Sequence", "AfterReturn");
            //AfterReturn -> Return ReturnableSequence | lambda
            P("AfterReturn", "Return", "ReturnableSequence")
                    .or(Symbols.LAMBDA);
            //Return -> return NullableExpression | lambda
            P("Return", "return", "NullableExpression")
                    .or(Symbols.LAMBDA);
            //NullableExpression -> Expression | lambda
            P("NullableExpression", "Expression")
                    .or(Symbols.LAMBDA);
            //ArgsDeclaration -> Type id ParamDecList | lambda
            P("ArgsDeclaration", "Type", "id", "ParamDecList")
                    .or(Symbols.LAMBDA);
            //ParamDecList -> comma Type id ParamDecList | lambda
            P("ParamDecList", "comma", "Type", "id", "ParamDecList")
                    .or(Symbols.LAMBDA);
            //Value -> boolean | number | string
            P("Value", "boolean")
                    .or("number")
                    .or("string");
            //Type -> int | chars | bool
            P("Type", "int")
                    .or("chars")
                    .or("bool");
            //Expression ->

            //RawExpression -> id | Value | PreOperation | Assignment | ( Expression ) | not Expression
            P("RawExpression", "id")
                    .or("Value")
                    .or("PreOperation")
                    .or("Assignment")
                    .or("openbracket", "Expression", "closebracket")
                    .or("not", "Expression");
            //
            //Expresision -> RawExpression OrOperator;
            //OrOperator -> or Expression | AndOperator;
            //AndOperator -> and Expression | ComparisonOperator;
            //ComparisonOperator -> comparison Expression | RelationalOperator;
            //RelationalOperator -> relational Expression | SumOperator;
            //SumOperator -> sumOrMinus Expression | MultiplyOperator;
            //MutiplyOperator -> mulOrDivOrMod Expression | lambda;


            System.out.println("Printng productions: "+Production.setOfProduction);
            System.out.println("Printng productionsBySymbol: "+Production.productionBySymbol);
            System.out.println("Printng productionsForSymbol: "+Production.productionsThatUseSymbol);
            for (Symbols.NoTerminal noTerminal : Production.productionBySymbol.keySet() )
                System.out.println("Debugging symbol "+noTerminal+"\n\tFirst: "+noTerminal.getFirst()+"\n\tFollow: "+noTerminal.getFollow());

        }

        Symbols lk(String str) {
            if (map.containsKey(str)) return map.get(str);
            else {
                Symbols.NoTerminal symb = new Symbols.NoTerminal(str);
                map.put(str, symb);
                return symb;
            }
        }

        Symbols.NoTerminal lkNT(String str) {
            Symbols s = lk(str);
            if (s instanceof Symbols.NoTerminal) return (Symbols.NoTerminal) s;
            throw new RuntimeException("GENERATING SYMBOL " + str + " is not non terminal");
        }

        ;

        private P_fact P(Symbols.NoTerminal gen, Object... seq) {
            Symbols[] sims = new Symbols[seq.length];
            for (int i = 0; i < seq.length; i++)
                if (seq[i] instanceof String) sims[i] = lk((String) seq[i]);
                else sims[i] = (Symbols) seq[i];
            return new P_fact(gen, sims);
        }

        private P_fact P(String gen, Object... seq) {
            return P(lkNT(gen), seq);
        }

        private P_fact P(String single) {
            String[] strs = single.split(" ");
            return P(strs[0], Arrays.asList(strs).subList(1, strs.length));
        }

        static abstract private class Action extends Symbols.Action {
        }

        private class P_fact {
            Symbols.NoTerminal gen;

            public P_fact(Symbols.NoTerminal gen, Symbols... seq) {
                this.gen = gen;
                new Production(gen, seq);
            }

            public P_fact or(Symbols... seq) { // useless
                return new P_fact(gen, seq);
            }

            public P_fact or(Object... seq) {
                return P(gen, seq);
            }
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
