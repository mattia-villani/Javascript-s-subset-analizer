import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

/**
 * Created by JoeAm on 05/01/2017.
 */
class FileWriter {
    private String path;

    FileWriter(String path){
        this.path = path;
    }

    void writeTokenFile(Collection<TokenFactory.IToken> tokens) throws  IOException{
        String file = Paths.get(path, "ficheroTokens.txt" ).toString();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        for (TokenFactory.IToken token : tokens){
            writer.println(token);
        }
        writer.close();
    }

    void writeTableOfSymbolsFile(Collection<GlobalTableOfSymbols.ScopedTableOfSymbols> tableOfSymbolss )throws  IOException {
        String file = Paths.get(path, "ficheroTS.txt" ).toString();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        for (GlobalTableOfSymbols.ScopedTableOfSymbols tos : tableOfSymbolss){
            writer.println(tos);
        }
        writer.close();
    }

    void writeParsingTable(PharsingTable table) throws  IOException{
        String file = Paths.get(path, "ficheroTablaParsing.csv" ).toString();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.print(table.toCSV());
        writer.close();
    }

    void writeGramar() throws  IOException{
        String file = Paths.get(path, "grammar.txt" ).toString();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        for (Symbols.NoTerminal nt : Production.productionBySymbol.keySet()){
           List<Production > productions = Production.productionBySymbol.get(nt);
           Production first = productions.get(0);
           writer.write(first.getSimpleString());
           writer.write("\n");
           for (int i = 1; i< productions.size(); i++) {
               Production subsequent = productions.get(i);
               writer.write(subsequent.getSimpleString(true));
               writer.write("\n");
           }
           writer.write("\n");
        }
        writer.close();
    }


}
