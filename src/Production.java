import java.util.*;

/**
 * Created by Joe on 10/12/2016.
 */
public class Production {
    static public Set<Production> setOfProduction = new HashSet<Production>();
    static public Map<Symbols.NoTerminal, List<Production>> productionBySymbol = new HashMap<Symbols.NoTerminal, List<Production>>();
    static public Map<Symbols.NoTerminal, List<Production>> productionsThatUseSymbol = new HashMap<Symbols.NoTerminal, List<Production>>();


    public final Symbols.NoTerminal generating;
    public final Symbols[] generated;

    public Production(Symbols.NoTerminal generating, Symbols... generated) {
        if (generating == null || generated == null || generated.length < 1)
            throw new RuntimeException("Badly formatted production");
        this.generated = generated;
        this.generating = generating;
        setOfProduction.add(this);
        List<Production> list = productionBySymbol.get(generating);
        if (list == null) list = new LinkedList<Production>();
        list.add(this);
        productionBySymbol.put(generating, list);
        for (Symbols x : generated)
            if (x instanceof Symbols.NoTerminal) {
                List<Production> l = productionsThatUseSymbol.get(x);
                if (l == null) l = new LinkedList<Production>();
                l.add(this);
                productionsThatUseSymbol.put((Symbols.NoTerminal) x, l);
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

    public Symbols getFirstSymbol() {
        return generated[0];
    }

    public Set<Symbols.Terminal> getFirstSet() {
        Set<Symbols.Terminal> set = new HashSet<Symbols.Terminal>();
        boolean thereIsLambda = true;
        int i = 0;
        do {
            Symbols symbols = generated[i];
            set.addAll(symbols.getFirst());
            if (set.contains(Symbols.LAMBDA)) {
                thereIsLambda = true;
                set.remove(Symbols.LAMBDA);
            } else thereIsLambda = false;
            i++;
        } while (i < generated.length && thereIsLambda);
        if (thereIsLambda)
            set.addAll(generating.getFollow());
        return set;
    }

    @Override
    public String toString() {
        String res = generating + "-> ";
        for (Symbols s : generated)
            res += s + " ";
        return res;
    }
}
