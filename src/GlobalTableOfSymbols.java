import javafx.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by Joe on 16/10/2016.
 */
public class GlobalTableOfSymbols implements TokenFactory.ITableOfSymbols {
    LinkedList<ScopedTableOfSymbols> scopedTablesOfSymbols = new LinkedList<ScopedTableOfSymbols>();
    ScopedTableOfSymbols reserved;
    private Stack<Integer> currentScope = new Stack<>();

    public GlobalTableOfSymbols() {
        try {
            reserved = new ScopedTableOfSymbols(TokenFactory.TokenFolder.WordToken.ReservedWordToken.getReservedWord());
            //Add global scope
            addScope("TABLA PRINCIPAL");

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
       Optional<Entry> e = getCurrentTOS().entries.stream().filter( x -> x.lexema.equals(lexema)).findFirst();
       return e.orElse(null);
    }

    public Entry getEntry(Pair<Integer,Integer> loc){
        return scopedTablesOfSymbols.get(loc.getKey()-1).entries.get(loc.getValue());
    }


    public Pair<Integer, Integer> queryLexema(String lexema, varType type) {
        Integer index = reserved.lookupIndexByLexema(lexema);
        if (index != null) return new Pair<>(-1, index);

        Entry entry  = new Entry(lexema, type);

        if (getCurrentTOS().entries.contains(entry)){
            return new Pair(currentScope.peek(), getCurrentTOS().entries.indexOf(entry));
        } else {
            getCurrentTOS().add(entry);
            return new Pair<>(currentScope.peek(), getCurrentTOS().lookupIndexByLexema(lexema));
        }
    }

    public boolean currentScopeIsGlobal() {
        return currentScope.size() == 1;
    }

    public void dropScope() {
        if (currentScope.size() == 1) throw new RuntimeException("Cannot unscope global environment");
        currentScope.pop();
    }

    public int addScope(String name) throws IllegalAccessException, InstantiationException {
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


        public Entry(String lexema, varType type) {
            this.lexema = lexema;
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
            return String.format("* LEXEMA : '%s'\nATRIBUTOS :\n + tipo : %s\n + desplazamiento : %d", lexema, getTypeAsString(), offset );
        }

        @Override
        public boolean equals(Object obj){
            Entry other = (Entry) obj;
            return this.lexema.equals(other.lexema);

        }
    }

    public class FunctionEntry extends Entry{
        varType[] paramTypes;
        int tableindex;
        int numparams;

        public FunctionEntry(String lexema, varType type, varType[] paramTypes, int tableindex){
            super(lexema, type);
            this.paramTypes = paramTypes;
            this.numparams = paramTypes.length;
            this.tableindex = tableindex;
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
            super( token.newInstance().getLexema(), varType.RES );
        }

        @Override
        public String toString() {
            return String.format("* ENTRADA RESERVADA : '%s'\nATRIBUTOS :\n + tipo : %s\n + desplazamiento : %d", lexema, getTypeAsString(), offset );
        }
    }

}


