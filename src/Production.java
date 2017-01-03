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

    public Collection<Symbols> getReversed(){
        List<Symbols> list = new LinkedList<>(Arrays.asList(generated));
        Collections.reverse(list);
        return list;
    }

    /**
     * @param symbol
     * @return the first set.
     */
    static Set<Symbols.Terminal> getFirstSetOf(Symbols symbol) {
        return getFirstSetOf(new Symbols[]{symbol});
    }

    static Set<Symbols.Terminal> getFirstSetOf(Symbols[] symbols) {
        return getFirstSetOf(symbols, new HashMap<Symbols, Set<Symbols.Terminal>>(), new HashSet<Symbols>());
    }

    static Set<Symbols.Terminal> getFirstSetOf(Symbols[] symbols, Map<Symbols, Set<Symbols.Terminal>> alreadySpottedFirstSets, Set<Symbols> recursivnessAvoidance) {
        Set<Symbols.Terminal> firsts = new HashSet<Symbols.Terminal>();
        Iterator<Symbols> it = Arrays.asList(symbols).iterator();

        while (it.hasNext()) {
            Symbols sym = it.next();

            if (sym instanceof Symbols.Terminal) {
                firsts.remove(Symbols.LAMBDA);
                firsts.add((Symbols.Terminal) sym);
                break;
            }

            if (recursivnessAvoidance.contains(sym)) throw new RuntimeException("Not LL for " + sym);
            else recursivnessAvoidance.add(sym);

            Set<Symbols.Terminal> fSetOfSym = alreadySpottedFirstSets.get(sym);
            if (fSetOfSym == null) {
                fSetOfSym = new HashSet<Symbols.Terminal>();
                List<Production> productionList = productionBySymbol.get(sym);
                for (Production production : productionList)
                    if (production.getFirstSymbol().equals(Symbols.Terminal.LAMBDA))
                        fSetOfSym.add(Symbols.Terminal.LAMBDA);
                    else
                        fSetOfSym.addAll(production.getFirstSetOf(production.generated, alreadySpottedFirstSets, recursivnessAvoidance));

                alreadySpottedFirstSets.put(sym, fSetOfSym);
            }

            recursivnessAvoidance.remove(sym);

            firsts.addAll(fSetOfSym);
            if (fSetOfSym.contains(Symbols.LAMBDA) == false) {
                firsts.remove(Symbols.LAMBDA);
                break;
            }
        }


        return firsts;
    }

    /**
     * @param symbol
     * @return the follow set.
     */
    static Set<Symbols.Terminal> getFollowSetOf(Symbols.NoTerminal symbol) {
        return getFollowSetOf(symbol, new HashMap<Symbols.NoTerminal, Set<Symbols.Terminal>>(), new HashSet<Production>());
    }

    static Set<Symbols.Terminal> getFollowSetOf(Symbols.NoTerminal symbol, Map<Symbols.NoTerminal, Set<Symbols.Terminal>> memory, Set<Production> doNotUse) {
        Set<Symbols.Terminal> set = new HashSet<Symbols.Terminal>();
        if (symbol instanceof Symbols.Axiom) {
            set.add(Symbols.DOLLAR);
            memory.put(symbol, set);
            return set;
        }
        for (Production production : productionsThatUseSymbol.get(symbol))
            if (doNotUse.contains(production)) {
            } else {
                doNotUse.add(production);
                for (int i = 0; i < production.generated.length; i++)
                    if (production.generated[i].equals(symbol)) {
                        Symbols[] leftGenerated =
                                Arrays.copyOfRange(production.generated, i + 1, production.generated.length);
                        Set<Symbols.Terminal> firsts =
                                Production.getFirstSetOf(leftGenerated.length != 0 ? leftGenerated : new Symbols[]{Symbols.LAMBDA});
//                        System.out.println("_________" + symbol + "_________FIRST OF " + Arrays.asList(leftGenerated) + " : " + firsts);
                        set.addAll(firsts);
                        if (firsts.contains(Symbols.LAMBDA) && production.generating.equals(symbol) == false) {
                            Set<Symbols.Terminal> follow;
                            if (memory.containsKey(production.generating)) {
                                follow = memory.get(production.generating);
                            } else {
                                follow = getFollowSetOf(production.generating, memory, doNotUse);
                            }
                            set.addAll(follow);
                        }
                    }
                doNotUse.remove(production);
            }
        set.remove(Symbols.LAMBDA);
        memory.put(symbol, set);
        return set;
    }

    public Symbols getFirstSymbol() {
        return generated[0];
    }

    @Override
    public String toString() {
        String res = generating + "-> ";
        for (Symbols s : generated)
            res += s + " ";
        return res;
    }
}
