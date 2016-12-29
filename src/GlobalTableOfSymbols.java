import javafx.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 16/10/2016.
 */
public class GlobalTableOfSymbols implements TokenFactory.ITableOfSymbols {
    LinkedList<ScopedTableOfSymbols> scopedTablesOfSymbols = new LinkedList<ScopedTableOfSymbols>();
    ScopedTableOfSymbols reserved;

    public GlobalTableOfSymbols() throws IllegalAccessException, InstantiationException {
        reserved = new ScopedTableOfSymbols(TokenFactory.TokenFolder.WordToken.ReservedWordToken.getReservedWord());
        //Add global scope
        addScope();

    }

    public Pair<Integer, Integer> queryLexema(String lexema) {
        Integer index = reserved.lookupIndexByLexema(lexema);
        if (index != null) return new Pair<Integer, Integer>(-1, index);
        for (int i = scopedTablesOfSymbols.size() - 1; i >= 0; i--) {
            if ((index = scopedTablesOfSymbols.get(i).lookupIndexByLexema(lexema)) != null)
                return new Pair(i, index);
        }
        index = scopedTablesOfSymbols.size() - 1;
        scopedTablesOfSymbols.get(index).add(new Entry(lexema));
        return new Pair<Integer, Integer>(index, scopedTablesOfSymbols.get(index).lookupIndexByLexema(lexema));
    }

    public void dropScope() {
        scopedTablesOfSymbols.remove(scopedTablesOfSymbols.size() - 1);
    }

    public void addScope() throws IllegalAccessException, InstantiationException {
        scopedTablesOfSymbols.add(new ScopedTableOfSymbols());
    }

    class ScopedTableOfSymbols {
        List<Entry> entries = new LinkedList<Entry>();
        private HashMap<String, Integer> lexemaMap = new HashMap<String, Integer>();


        public ScopedTableOfSymbols(Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken>[] initialEntries) throws InstantiationException, IllegalAccessException {
            for (Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken> e : initialEntries) add(new ReservedEntry(e));
        }

        public ScopedTableOfSymbols() {}

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

    }

    public class Entry {
        protected String lexema;

        public Entry(String lexema) {
            this.lexema = lexema;
        }

        public String getLexema() {
            return lexema;
        }
    }

    public class ReservedEntry extends Entry {

        public ReservedEntry(Class<TokenFactory.TokenFolder.WordToken.ReservedWordToken> token) throws IllegalAccessException, InstantiationException {
            super( token.newInstance().getLexema() );
        }
    }

}


