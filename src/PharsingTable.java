import jdk.nashorn.internal.objects.Global;

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

        GlobalTableOfSymbols tableOfSymbol = new GlobalTableOfSymbols();

        Stack<Symbols> P = new Stack<>();
        Stack<Symbols> aux = new Stack<>();
        Symbols.Terminal a = ip_get.apply(tableOfSymbol);
        Symbols X,Aux;

        P.push(Symbols.DOLLAR);
        P.push(axiom.init());
        aux.push(null);
        prompt(null,P,"initial state");

        do {
            X = P.peek();
            Aux = aux.peek();

            if ( X instanceof Symbols.Terminal ) {
                if ( X.equals(a) ) {
                    P.pop();
                    aux.push(X);
                    a = ip_get.apply(tableOfSymbol);
                    prompt( a.token, P, "Poped "+X);
                }else throw new RuntimeException("Exprected "+X+", but got "+a+": syntax error");
            }else if ( X instanceof Symbols.NoTerminal ){
                Production production = table.get(X).get(a);
                if ( production != null ) {
                    P.pop();
                    aux.push(X);
                    P.addAll(production.getReversed((Symbols.NoTerminal)X));
                    prompt( a.token, P, "Applied "+production);
                }else throw new RuntimeException("No Production defined for pair M[" +X+ "," + a + "]: Syntax error");
            }else if ( X instanceof Symbols.Action) {
                P.pop();
                Symbols.Action action = (Symbols.Action)X;
                action.accept(action.context);
                prompt( a.token, P, "Applied action with Context "+action.context);
            }else throw new RuntimeException("Unrecognized symbol "+X);
//            System.out.println("-->>loop X("+X+"), Aux("+Aux+")");
        }while ( !P.empty() && (P.peek()!=Symbols.DOLLAR || Aux!= axiom));

        return tableOfSymbol;
    }

    public void applySyntaxOnly(Iterator<TokenFactory.IToken> it){
        Stack<Symbols> workingStack = new Stack<>();
        Symbols.Terminal currentToken ;

        workingStack.push(Symbols.DOLLAR);
        workingStack.push(axiom);
        prompt(null,workingStack,"initial state");

        do {
            TokenFactory.IToken actualToken = it.hasNext() ? it.next() : null;
            currentToken = actualToken == null ? Symbols.DOLLAR : new Symbols.Terminal(actualToken.getClass());

            while ( workingStack.peek() instanceof Symbols.Terminal == false ) {
                while (workingStack.peek() instanceof Symbols.NoTerminal) {
                    Symbols.NoTerminal currentStackHead = (Symbols.NoTerminal) workingStack.peek();
                    Production production = table.get(currentStackHead).get(currentToken);
                    if (production == null)
                        throw new RuntimeException("No Production defined for pair (" + currentStackHead + "," + currentToken + ")");
                    Collection<Symbols> rev = production.getReversed(null); // TO FIX
                    workingStack.pop();
                    workingStack.addAll(rev);
                    prompt(actualToken, workingStack, "Applied rule " + production + "\n\t\t");
                }
                while (workingStack.peek() instanceof Symbols.Action) {
                    Symbols.Action action = (Symbols.Action)workingStack.pop();
                    action.accept( action.context );
                }
            }

            if ( currentToken.equals(workingStack.peek()) ) workingStack.pop();
            else throw new RuntimeException("No matching tokens at the top of the stack ("+workingStack.peek()+","+currentToken+")");
            prompt(actualToken,workingStack, "pop of "+currentToken );
        }while ( currentToken != Symbols.DOLLAR );

        if ( workingStack.empty() == false ) throw new RuntimeException("Stack wasn't consumed. "+workingStack);
    }

    public void prompt(TokenFactory.IToken token, Stack<Symbols> stack, String msg){
        System.out.println( token+" ::: "+msg + " >> -- >> "+stack);
    }
    @Override
    public String toString(){
        return table.toString();
    }
}
