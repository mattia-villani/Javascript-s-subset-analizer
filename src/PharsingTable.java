import jdk.nashorn.internal.objects.Global;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by matti on 03/01/2017.
 */
public class PharsingTable{
    public final Map<Symbols.NoTerminal, Map<Symbols.Terminal, Production>> table
            = new HashMap<>();
    protected List<Class<? extends TokenFactory.IToken>> tokens;
    protected List<Symbols.NoTerminal> innerSybols;
    protected Symbols.Axiom axiom;

    protected List<String> shortNames;

    public PharsingTable(Grammar grammar){
        tokens = Arrays.asList( (Class<? extends TokenFactory.IToken>[]) TokenFactory.TokenFolder.class.getClasses() );
        innerSybols = new LinkedList<>(Production.productionBySymbol.keySet());

        axiom = innerSybols.stream()
                .filter( e -> e instanceof Symbols.Axiom )
                .map( Symbols.Axiom.class::cast )
                .reduce( (a,b) ->{
                    if ( a!=null && b!=null ) throw new RuntimeException("Two axioms "+a+", "+b);
                    else return a==null ? b : a;
                }).orElseThrow(()->new RuntimeException("At least one axiom has to be defined"));

        shortNames = Stream.of( (Class<?>[])tokens.toArray() )
                .map(c -> c.getSimpleName()).collect(Collectors.toList());

        System.out.println("Token list: "+shortNames );
        System.out.println("NoTerminal symbols list: "+innerSybols);
        System.out.println("Axiom :" + axiom);

        Map<Symbols.Terminal, Production> row ;

        for (Symbols.NoTerminal symbol: innerSybols){

            List<Production> productionList = Production.productionBySymbol.get(symbol);
            table.put( symbol, row = new HashMap<>());

            for (Production production : productionList ) {
                Set<Symbols.Terminal> set = Production.getFirstSetOf(production.generated);
                if (set.contains(Symbols.LAMBDA)){
                    set.addAll(Production.getFollowSetOf(production.generating));
                    set.remove(Symbols.LAMBDA);
                }
                for ( Symbols.Terminal terminal : set )
                    if (row.containsKey(terminal))
                        throw new RuntimeException("This is not LL(1)for symbol "+terminal+": " + row.get(terminal) + " clashing with " + production);
                    else row.put(terminal, production);
            }
        }
    }


    public GlobalTableOfSymbols apply(Function<TokenFactory.ITableOfSymbols, TokenFactory.IToken> tokenGetter ){
        Function<TokenFactory.ITableOfSymbols,Symbols.Terminal> ip_get = (tableOfSymbol) -> {
            TokenFactory.IToken actualToken = tokenGetter.apply(tableOfSymbol);
            return actualToken instanceof TokenFactory.TokenFolder.EofToken
                    ? Symbols.DOLLAR : new Symbols.Terminal(actualToken);
        };

        System.out.println("Productions:");
        Production.setOfProduction.stream()
                .sorted(Comparator.comparingInt(a -> a.id))
                .map(p->"\t("+p.id+") -- "+p)
                .forEach(System.out::println);

        GlobalTableOfSymbols tableOfSymbol = new GlobalTableOfSymbols();

        Stack<Symbols> P = new Stack<>();
        Stack<Symbols> aux = new Stack<>();
        Symbols.Terminal a = ip_get.apply(tableOfSymbol);
        Symbols X,Aux;

        Symbols.NonActionSymbol AXIOM = (Symbols.NonActionSymbol)axiom.init();
        P.push(Symbols.DOLLAR);
        P.push(AXIOM);
        aux.push(null);
        prompt(null,P,"initial state");

        do {
            X = P.peek();
            Aux = aux.peek();

            if ( X instanceof Symbols.Terminal ) {
                if ( X.equals(a) ) {
                    P.pop();
                    aux.push(X);
                    ((Symbols.Terminal)X).set(Grammar.ATT.TOKEN,a.token);
                    a = ip_get.apply(tableOfSymbol);
                    prompt( a.token, P, "Poped "+X);
                }else throw new RuntimeException("Exprected "+X+", but got "+a+": syntax error");
            }else if ( X instanceof Symbols.NoTerminal ){
                Production production = table.get(X).get(a);
                if ( production != null ) {
                    P.pop();
                    aux.push(X);
                    P.addAll(production.getReversed((Symbols.NoTerminal)X));
                    prompt( a.token, P, "Applied ("+production.id+"):"+production);
                }else throw new RuntimeException("Syntax error: Expected "+X+", but got "+a+"\n\tNo Production defined for pair M[" +X+ "," + a + "]");
            }else if ( X instanceof Symbols.Action) {
                P.pop();
                Symbols.Action action = (Symbols.Action)X;
                action.accept(action.context, new Grammar.S(action.context, action.context.productionRoot.getName()) );
                prompt( a.token, P, "Applied action with Context "+action.context);
            }else throw new RuntimeException("Unrecognized symbol "+X);
            //System.out.println("-->>loop X("+X+"), Aux("+Aux+"). aux:"+aux);
        }while ( !P.empty() && (P.peek()!=Symbols.DOLLAR || Aux!= axiom));

        Grammar.TYPES end = AXIOM.get(Grammar.ATT.TYPE, Grammar.TYPES.class);
        (end.equals(Grammar.TYPES.ERR)?System.err:System.out).println("\n\n\tComputation ended with code "+end+"\n");

        return tableOfSymbol;
    }


    public void prompt(TokenFactory.IToken token, Stack<Symbols> stack, String msg){
        System.out.println( token+" ::: "+msg + " >> -- >> "+stack);
    }
    @Override
    public String toString(){
        return table.toString();
    }
}
