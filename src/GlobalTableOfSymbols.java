import javafx.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.ToIntFunction;

/**
 * Created by Joe on 16/10/2016.
 */
public class GlobalTableOfSymbols implements TokenFactory.ITableOfSymbols {
    public enum EDITING {
        FORBITTEN,
        VAR,
        FUN
    };
    public static EDITING editing = EDITING.FORBITTEN;

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
            throw new RuntimeException( e.getMessage() );
        }

    }

    public ScopedTableOfSymbols getCurrentTOS () {
        return scopedTablesOfSymbols.get(currentScope.peek()-1);
    }

    public void addEntryToScope(Entry e){
        getCurrentTOS().add(e);
    }

    public LinkedList<ScopedTableOfSymbols> getScopedTablesOfSymbols() {
        return scopedTablesOfSymbols;
    }
//problem

    public Entry getEntry(String lexema){
        // todo Not only in the currentTOS, but also in the global one if it is not present in current (accessing a global value from a function)
        System.err.println("\n\nJust search lexema "+lexema+". Current TOS "+getCurrentTOS().lexemaMap.keySet()+" ;; "+getCurrentTOS().entries.stream().map(e->e.getLexema()).reduce((a,b)->a+" "+b));
        Optional<Entry> e = getCurrentTOS().entries.stream().filter( x -> x.lexema.equals(lexema)).findFirst();
       return e.orElse(null);
    }

    public Entry getEntry(Pair<Integer,Integer> loc){
        return scopedTablesOfSymbols.get(loc.getKey()-1).entries.get(loc.getValue());
    }


    public Pair<Integer, Integer> queryLexema(String lexema) {
        Integer index = reserved.lookupIndexByLexema(lexema);
        if (index != null) return new Pair<>(-1, index);

        Entry entry  = new Entry(lexema);

        final int UNFOUND = -1;
        ToIntFunction<Entry> lookup = e -> {
          int ret = UNFOUND;
          ret = getCurrentTOS().entries.indexOf(e);
          if ( ret == -1 && getCurrentTOS() != principal )
              ret = principal.entries.indexOf(e);
          return ret;
        };

        if (lookup.applyAsInt(entry)!=UNFOUND){
            return editing.equals(EDITING.FORBITTEN)
                    ? null
                    : new Pair(currentScope.peek(), lookup.applyAsInt(entry));
        } else if ( ! editing.equals(EDITING.FORBITTEN) ){
            getCurrentTOS().add(EDITING.VAR.equals(editing) ? entry: new FunctionEntry(lexema));
            System.err.println("\n\nJust added lexema "+lexema+". Current TOS "+getCurrentTOS().lexemaMap.keySet()+" ;; "+getCurrentTOS().entries.stream().map(e->e.getLexema()).reduce((a,b)->a+" "+b));
            return new Pair<>(currentScope.peek(), getCurrentTOS().lookupIndexByLexema(lexema));
        } else return null;//throw new RuntimeException("HERE "+editing+" "+lexema);
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
        scopedTablesOfSymbols.add(new ScopedTableOfSymbols(name, index ));
        currentScope.push(index);
        return index;
    }

    class ScopedTableOfSymbols {
        String name;
        int index;
        List<Entry> entries = new LinkedList<Entry>();
        private HashMap<String, Integer> lexemaMap = new HashMap<String, Integer>();


        public ScopedTableOfSymbols(Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken>[] initialEntries) throws InstantiationException, IllegalAccessException {
            for (Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken> e : initialEntries) add(new ReservedEntry(e));
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
            return lexemaMap.get(lexema);
        }

        public Entry lookUpEntryByIndex(Integer index) {
            return entries.get(index);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s #%d", name, index));
            for (Entry e : entries){
                builder.append(e + "\n");
            }
            return builder.toString();
        }

    }

    public enum varType { INT, CAD, BOOL, RES, VOID }
    int[] sizeofType = new int[]{ 4, 4, 1, 0, 0};

    static String getTypeAsString(varType type) { return type.equals(varType.INT) ? "INT"
            : type.equals(varType.CAD) ? "CAD"
            : type.equals(varType.BOOL) ? "BOOL"
            : type.equals(varType.RES) ? "RES"
            : type.equals(varType.VOID) ? "VOID"
            : null; };

    int memoryLocation = 0;

    public class Entry {
        protected varType type;
        protected String lexema;
        protected int offset;
        protected int size;


        public Entry(String lexema) {
            this.lexema = lexema;
        }

        public void setEntryVals(varType type){
            this.type = type;
            this.size = sizeofType[type.ordinal()];
            this.offset = memoryLocation;
            memoryLocation += size;
        }

        public String getLexema() {
            return lexema;
        }
        public varType getType() {
            return type;
        }
        public String getTypeAsString(){
            return GlobalTableOfSymbols.getTypeAsString(type);
        }

        @Override
        public String toString() {
            if ( type == null )
                return String.format("* LEXEMA : '%s'\nUNSETTED", lexema );
            else
                return String.format("* LEXEMA : '%s'\nATRIBUTOS :\n + tipo : %s\n + desplazamiento : %d", lexema, getTypeAsString(), offset );
        }

        @Override
        public boolean equals(Object obj){
            Entry other = (Entry) obj;
            return this.lexema.equals(other.lexema);
        }
    }

    public class FunctionEntry extends Entry{
        List<varType> paramTypes = new LinkedList<>();
        int tableindex;
        int numparams;

        public FunctionEntry(String lexema){
            super(lexema);
        }

        public List<varType> getParamTypes(){
            return paramTypes;
        }

        public void addParamtype(varType type){
            paramTypes.add(type);
            numparams++;
        }



        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("(Esto es una función)\n%s\n", super.toString()));
            builder.append(String.format(" + parametros : %d\n",numparams));
            builder.append(String.format(" + idtabla : %d\n", tableindex));
            int i = 1;
            for (varType type : paramTypes)
                builder.append(String.format(" + tipoparam%d : %s\n", i++, GlobalTableOfSymbols.getTypeAsString(type)));
            return builder.toString();
        }
    }

    public class ReservedEntry extends Entry {

        public ReservedEntry(Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken> token) throws IllegalAccessException, InstantiationException {
            super( token.newInstance().getLexema() );
            super.setEntryVals( varType.RES );
        }

        @Override
        public String toString() {
            return String.format("* ENTRADA RESERVADA : '%s'\nATRIBUTOS :\n + tipo : %s\n + desplazamiento : %d", lexema, getTypeAsString(), offset );
        }
    }

}


