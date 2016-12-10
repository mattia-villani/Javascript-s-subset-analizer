import java.util.*;

/**
 * Created by matti on 22/11/2016.
 */

public abstract class Grammar {

    static public class ProductionSet{
        Map<String, Symbols> map;
        public ProductionSet(){
            map = new HashMap<String, Symbols>();
            List<Class<?>> tokens = new LinkedList<Class<?>>();

            System.out.println("Going to add to map " + Arrays.asList(TokenFactory.TokenFolder.class.getClasses()));
            tokens.addAll(Arrays.asList(TokenFactory.TokenFolder.class.getClasses()));

            System.out.println("Going to add to map " + Arrays.asList(TokenFactory.TokenFolder.WordToken.ReservedWordToken.class.getClasses()));
            tokens.addAll(Arrays.asList(TokenFactory.TokenFolder.WordToken.ReservedWordToken.class.getClasses()));

            System.out.println("Going to add to map " + Arrays.asList(TokenFactory.TokenFolder.WordToken.class.getClasses()));
            tokens.addAll(Arrays.asList(TokenFactory.TokenFolder.WordToken.class.getClasses()));

            for (Class<?> clazz : tokens)
                if (!clazz.equals(TokenFactory.TokenFolder.WordToken.class)) {
                    String str = clazz.getSimpleName().replace("Token", "").toLowerCase();
                    map.put(str, new Symbols.Terminal((Class<TokenFactory.IToken>) clazz));
                    System.out.println("Just added to map " + str);
                }

            //Program -> Sequence
            P(new Symbols.Axiom("Program"), "Sequence");
            //Sequence -> Statement Sequence | lamda
            P("Sequence", "Statement", "Sequence")
                    .or(Symbols.LAMBDA);
            //Delimiter -> Semicolon | NewLine
            P("Delimiter", "semi")
                    .or("newline");
            P("Statement", "Declaration")
                    .or("Assignment", "Delimiter")
                    .or("preinc", "id", "Delimiter")
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
            P("Assignment", "id", "assign", "Expression");
            //PreOperation -> DoubleOp id
            P("Assignment", "preinc", "id");
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
            P("FunctionDec", "function", "NullableType", "id", "openbracket", "ArgsDeclaration", "closebracket", "openbrace", "ReturnableSequence", "closebracket");
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
            P("NullableExpression", "Exp")
                    .or(Symbols.LAMBDA);
            //ArgsDeclaration -> Type id ParamDecList | lambda
            P("ArgsDeclaration", "Type", "id", "ParamDecList")
                    .or(Symbols.LAMBDA);
            //ParamDecList -> comma Type id ParamDecList | lambda
            P("ParamDecList", "comma", "Type", "id", "ParamDecList")
                    .or(Symbols.LAMBDA);
            //Value -> boolean | number | string
            P("Value", "number")
                    .or("false")
                    .or("true")
                    .or("string");
            //Type -> int | chars | bool
            P("Type", "int")
                    .or("chars")
                    .or("bool");
            //Exp -> Andexp Orexp
            P("Exp", "Andexp", "Orexp");
            //Nexp -> Term Aexp
            P("Nexp", "Term", "Aexp");
            //Aexp -> plus Nexp | minus Nexp | lambda
            P("Aexp", Symbols.LAMBDA)
                    .or("plus", "Nexp")
                    .or("minus", "Nexp");
            P("Term", "Factor", "Term'");
            P("Term'", Symbols.LAMBDA)
                    .or("mult", "Term")
                    .or("mod", "Term")
                    .or("div", "Term");
            P("Factor", "id", "Assigofunc")
                    .or("preinc", "id")
                    .or("openbracket", "Exp", "closebracket")
                    .or("not", "Exp")
                    .or("Value");
            P("Asigofunc", "assign", "Exp")
                    .or("openbracket", "Arguments", "closebracket")
                    .or(Symbols.LAMBDA);
            P("Andexp", "Bexp", "Andexp'");
            P("Andexp'", "and", "Bexp", "Andexp'")
                    .or(Symbols.LAMBDA);
            P("Orexp", "or", "Exp")
                    .or(Symbols.LAMBDA);
            P("Bexp", "Relexp", "Compexp");
            P("Relexp", "Nexp", "Relexp'");
            P("Relexp'", Symbols.LAMBDA)
                    .or("gt", "Nexp")
                    .or("lt", "Nexp")
                    .or("egt", "Nexp")
                    .or("elt", "Nexp");
            P("Compexp", Symbols.LAMBDA)
                    .or("eq", "Bexp")
                    .or("neq", "Bexp");


            System.out.println("Printng productions: "+Production.setOfProduction);
            System.out.println("Printng productionsBySymbol: "+Production.productionBySymbol);
            System.out.println("Printng productionsForSymbol: "+Production.productionsThatUseSymbol);
            for (Symbols.NoTerminal noTerminal : Production.productionBySymbol.keySet() )
                System.out.println("Debugging symbol "+noTerminal+"\n\tFirst: "+noTerminal.getFirst()+"\n\tFollow: "+noTerminal.getFollow());

        }

        Symbols lk(String str) {
            if (map.containsKey(str)) return map.get(str);
            else {
                System.out.println(str + " didn't found, adding it... ");
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
