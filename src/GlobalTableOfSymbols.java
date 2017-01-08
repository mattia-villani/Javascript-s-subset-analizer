import javafx.util.Pair;

import java.util.*;

/**
 * Created by Joe on 16/10/2016.
 */
public class GlobalTableOfSymbols implements TokenFactory.ITableOfSymbols {
    public static GlobalTableOfSymbols globalTableOfSymbols = new GlobalTableOfSymbols();

    public enum EDITING {
        FORBIDDEN,
        VAR,
        FUN
    }
    public static EDITING editing = EDITING.FORBIDDEN;

    LinkedList<ScopedTableOfSymbols> scopedTablesOfSymbols = new LinkedList<ScopedTableOfSymbols>();
    ScopedTableOfSymbols reserved;
    final ScopedTableOfSymbols principal;
    private Stack<Integer> currentScope = new Stack<>();

    public GlobalTableOfSymbols() {
        try {
            reserved = new ScopedTableOfSymbols(TokenFactory.TokenFolder.WordToken.ReservedWordToken.getReservedWord());
            //Add global scope
            addScope("TABLA PRINCIPAL");
            principal = getCurrentTOS();

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    public ScopedTableOfSymbols getCurrentTOS() {
        return scopedTablesOfSymbols.get(currentScope.peek() - 1);
    }


    public LinkedList<ScopedTableOfSymbols> getScopedTablesOfSymbols() {
        return scopedTablesOfSymbols;
    }

    public boolean hasEntry(String lexema){
        return getEntry(lexema).getKey() != null;
    }

    public Pair<ScopedTableOfSymbols, Entry> getEntry(String lexema) {
   //     System.err.println("\n\nJust search lexema " + lexema + ". Current TOS " + getCurrentTOS().lexemaMap.keySet() + " ;; " + getCurrentTOS().entries.stream().map(e -> e.getLexema()).reduce((a, b) -> a + " " + b));
        Optional<Entry> e = getCurrentTOS().entries.stream().filter(x -> x.lexema.equals(lexema)).findFirst();
        Entry er;
        if (e.isPresent()) {
            er = e.orElse(null);
            return new Pair<>(getCurrentTOS(), er);
        }
        else { //If not in current scope look in global
            e = principal.entries.stream().filter(x -> x.lexema.equals(lexema)).findFirst();
            er = e.orElse(null);
            return new Pair<>(principal, er);
        }

    }

    private Pair<Integer, Integer> toIntegerIndexing(Pair<ScopedTableOfSymbols, Entry> loc) {
        return new Pair<>(loc.getKey().index, loc.getKey().entries.indexOf(loc.getValue()));
    }

    public Pair<Integer, Integer> queryLexema(String lexema) {
        Integer index = reserved.lookupIndexByLexema(lexema);
        if (index != null) return new Pair<>(-1, index);

        //try and find lexema
        Pair<ScopedTableOfSymbols, Entry> entryPair = getEntry(lexema);
        Pair<Integer, Integer> indexedEntry = toIntegerIndexing(entryPair);

        if (indexedEntry.getValue() != -1) {
            //if lexema found
            return editing.equals(EDITING.FORBIDDEN)==false
                    ? null
                    : indexedEntry;
//            return indexedEntry;
        } else
            //if lexema not found
            if (!editing.equals(EDITING.FORBIDDEN)) {

                //add lexema to the table of symbols
                ScopedTableOfSymbols stos = getCurrentTOS();
                stos.add(EDITING.VAR.equals(editing) ? new Entry(lexema) : new FunctionEntry(lexema));
    //            System.err.println("\n\nJust added lexema " + lexema + ". Current TOS " + getCurrentTOS().lexemaMap.keySet() + " ;; " + getCurrentTOS().entries.stream().map(e -> e.getLexema()).reduce((a, b) -> a + " " + b));

                //get its index
                return new Pair<>(currentScope.peek(), getCurrentTOS().lookupIndexByLexema(lexema));
            } else return null;
    }

    public boolean currentScopeIsGlobal() {
        return currentScope.size() == 1;
    }

    public void dropScope() {
        if (currentScope.size() == 1) throw new RuntimeException("Cannot unscope global environment");
        currentScope.pop();
    }

    public int addScope(String name) {
        int index = scopedTablesOfSymbols.size() + 1;
        scopedTablesOfSymbols.add(new ScopedTableOfSymbols(name, index));
        currentScope.push(index);
        return index;
    }

    class ScopedTableOfSymbols {
        String name;
        int index;
        List<Entry> entries = new LinkedList<Entry>();
        private HashMap<String, Integer> lexemaMap = new HashMap<>();


        public ScopedTableOfSymbols(Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken>[] initialEntries) throws InstantiationException, IllegalAccessException {
            for (Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken> e : initialEntries)
                add(new ReservedEntry(e));
        }

        public ScopedTableOfSymbols(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public void add(Entry e) {
            if (lexemaMap.containsKey(e.getLexema()))
                throw (new RuntimeException("Symbol already in table (" + e.getLexema() + ")"));
            lexemaMap.put(e.getLexema(), entries.size());
            entries.add(e);
        }

        public Integer lookupIndexByLexema(String lexema) {
            if (lexemaMap.keySet().contains(lexema)) {
                int index = lexemaMap.get(lexema);
                return index;
            } else {
                return null;
            }

        }

        public Entry lookUpEntryByIndex(Integer index) {
            return entries.get(index);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s #%d\n", name, index));
            for (Entry e : entries)
                builder.append(e + "\n");
            return builder.toString();
        }

    }

    public enum VarType {INT, CAD, BOOL, RES, VOID}

    int[] sizeofType = new int[]{4, 4, 1, 0, 0};

    static String getTypeAsString(VarType type) {
        return type.equals(VarType.INT) ? "INT"
                : type.equals(VarType.CAD) ? "CAD"
                : type.equals(VarType.BOOL) ? "BOOL"
                : type.equals(VarType.RES) ? "RES"
                : type.equals(VarType.VOID) ? "VOID"
                : null;
    }

    int memoryLocation = 0;

    public class Entry {
        protected VarType varType;
        protected String lexema;
        protected int offset;
        protected int size;


        public Entry(String lexema) {
            this.lexema = lexema;
        }

        public void setEntryVals(VarType varType) {
            this.varType = varType;
            this.size = sizeofType[varType.ordinal()];
            this.offset = memoryLocation;
            memoryLocation += size;
        }

        public String getLexema() {
            return lexema;
        }

        public VarType getVarType() {
            return varType;
        }

        public String getTypeAsString() {
            return GlobalTableOfSymbols.getTypeAsString(varType);
        }

        @Override
        public String toString() {
            if (varType == null)
                return String.format("* LEXEMA : '%s'\nNOT SET", lexema);
            else
                return String.format("* LEXEMA : '%s'\nATRIBUTOS :\n + tipo : %s\n + desplazamiento : %d", lexema, getTypeAsString(), offset);
        }

        @Override
        public boolean equals(Object obj) {
            Entry other = (Entry) obj;
            return this.lexema.equals(other.lexema);
        }
    }

    public class FunctionEntry extends Entry {
        List<VarType> paramTypes = new LinkedList<>();
        int tableindex;
        int numparams;

        public FunctionEntry(String lexema) {
            super(lexema);
        }

        public void setScope(){
            tableindex = currentScope.peek();
        }

        public List<VarType> getParamTypes() {
            return paramTypes;
        }

        public void addParamtype(VarType type) {
            paramTypes.add(type);
            numparams++;
        }


        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("(Esto es una función)\n%s\n", super.toString()));
            builder.append(String.format(" + parametros : %d\n", numparams));
            builder.append(String.format(" + idtabla : %d\n", tableindex));
            int i = 1;
            for (VarType type : paramTypes)
                builder.append(String.format(" + tipoparam%d : %s\n", i++, GlobalTableOfSymbols.getTypeAsString(type)));
            return builder.toString();
        }
    }

    public class ReservedEntry extends Entry {

        public ReservedEntry(Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken> token) throws IllegalAccessException, InstantiationException {
            super(token.newInstance().getLexema());
            super.setEntryVals(VarType.RES);
        }

        @Override
        public String toString() {
            return String.format("* ENTRADA RESERVADA : '%s'\nATRIBUTOS :\n + tipo : %s\n + desplazamiento : %d", lexema, getTypeAsString(), offset);
        }
    }

}


