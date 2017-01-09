import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by JoeAm on 05/01/2017.
 */
class FileWriter {
    private String path;

    FileWriter(String path){
        this.path = path;
        Path p = Paths.get(path);
        if (p.toFile().exists()) {
            deleteDirectory(p.toFile());
        }
        p.toFile().mkdir();

    }

    private static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
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
        boolean first = true;
        for (GlobalTableOfSymbols.ScopedTableOfSymbols tos : tableOfSymbolss){
            if (!first) writer.print("--------------------------------------------------- \n");
            else first = false;
            writer.println(tos);
        }
        writer.close();
    }

    void writeParsingTable(ParsingTable table) throws  IOException{
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

    void writeVASTParse(ParsingTable parsingTable)  throws  IOException{
        String file = Paths.get(path, "parse.txt" ).toString();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.print("Des");
        for (Production p : parsingTable.getProductionsUsed()){
            writer.print(" " + (p.id - 1));
        }
        writer.close();
    }


    void writeGrammarVast(ParsingTable parsingTable) throws  IOException{

        String file = Paths.get(path, "VASTgrammar.txt" ).toString();
        PrintWriter fwriter = new PrintWriter(file, "UTF-8");
        StringBuilder writer = new StringBuilder();
        writer.append("//// Grámatica para Vast\n" +
                "//// Gramática LL(1)\n");
        writer.append("Axioma = Program'\n\n");
        writer.append("NoTerminales = {");
        for (Symbols.NoTerminal name : parsingTable.innerSybols){
            writer.append(" " + name.getName()+"\n");
        }
        writer.append("}\n\n");
        writer.append("Terminales = {");
        for (String name : parsingTable.shortNames){
            writer.append(" " + name +"\n");
        }
        writer.append("}\n\n");

        writer.append("////La gramáica de JavaScript-PL\n");
        writer.append("Producciones = {");

        List<Production> productions = new LinkedList<>();
        productions.addAll(Production.setOfProduction);
        Collections.sort(productions, (p1, p2) -> ((Integer)p1.id).compareTo(p2.id));

        for (Production p : productions){
            writer.append("\t" + p.getSimpleString() + "\n");
        }
        writer.append("}\n");

        fwriter.print(writer.toString().replaceAll("'", "Prime"));

        fwriter.close();
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
