import java.util.*;
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

    public void apply(Iterator<TokenFactory.IToken> it){
        Stack<Symbols> workingStack = new Stack<>();
        Symbols.Terminal currentToken ;

        workingStack.push(Symbols.DOLLAR);
        workingStack.push(axiom);
        prompt(null,workingStack,"initial state");

        do {
            TokenFactory.IToken actualToken = it.hasNext() ? it.next() : null;
            currentToken = actualToken == null ? Symbols.DOLLAR : new Symbols.Terminal(actualToken.getClass());

            while ( workingStack.peek() instanceof Symbols.NoTerminal ){
                Symbols.NoTerminal currentStackHead = (Symbols.NoTerminal)workingStack.peek();
                Production production = table.get(currentStackHead).get(currentToken);
                if ( production == null ) throw new RuntimeException("No Production defined for pair ("+currentStackHead+","+currentToken+")");
                Collection<Symbols> rev = production.getReversed();
                workingStack.pop();
                workingStack.addAll( rev );
                prompt(actualToken, workingStack, "Applied rule "+production+"\n\t\t");
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
