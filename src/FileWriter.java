import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path p = Paths.get(path);
        if (!p.toFile().exists()) {
            p.toFile().delete();
        }
        p.toFile().mkdir();

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

    void writeSource(String path) throws IOException{
        if (Paths.get(this.path, "fuente.js" ).toFile().exists())
            Paths.get(this.path, "fuente.js" ).toFile().delete();
        Files.copy(Paths.get(path), Paths.get(this.path, "fuente.js" ) );
    }

    PrintStream getErrorPrintStream() throws FileNotFoundException{
        File file = new File(Paths.get(path, "errores.txt" ).toString());
        return new PrintStream(file);
    }


}
