import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by matti on 05/10/2016.
 */
public class PL_IMPL_Main {
    static int prev_line;
    static boolean closingDelimiterPut;

    private static boolean verbose;

    public static void main(String[] argv) throws Exception {
        File file;
        String outputDirRoot;
        if (argv.length < 2) {
            System.out.println("\n No arguments specified, using test files and writing to Desktop\\Resultados\n\n");
            file = new File("Resources/tests");
            outputDirRoot = System.getProperty("user.home") + "/Desktop/Resultados";
        } else {
            file = new File(argv[0]);
            outputDirRoot = argv[1];
        }

        String[] filenames =
                Stream.of(file.isDirectory() ? file.listFiles() : new File[]{file})
                        .map(f -> f.getAbsolutePath())
                        .toArray(i -> new String[i]);


        FileWriter fileWriter = new FileWriter(outputDirRoot);

        Grammar grammar = new Grammar();
        ParsingTable parsingTable = new ParsingTable(grammar);
        fileWriter.writeParsingTable(parsingTable);
        fileWriter.writeGrammarVast(parsingTable);
        fileWriter.writeGramar();

        Map<String, String> errors = new HashMap<>();

        System.out.println("Productions:");
        Production.setOfProduction.stream()
                .sorted(Comparator.comparingInt(a -> a.id))
                .map(p->"\t("+p.id+") -- "+p)
                .forEach(System.out::println);

        for (String filename : filenames) {
            closingDelimiterPut = false;
            String dir = outputDirRoot + "\\" + Paths.get(filename).getFileName().toString().replaceFirst("[.][^.]+$", "");
            (new File(dir)).mkdir();
            fileWriter = new FileWriter(dir);
            fileWriter.writeSource(filename);
            System.out.println("///----------------------------------------------------------\\\\\\");
            System.out.println("|||\tTesting file: "+filename);
            System.out.println("\\\\\\----------------------------------------------------------///");
            Scanner scanner = new Scanner(filename);
            Parser parser = new Parser(scanner);
            parser.Parse();
            GlobalTableOfSymbols gts = new GlobalTableOfSymbols();
            GlobalTableOfSymbols.globalTableOfSymbols = gts;

            LinkedList<TokenFactory.IToken> tokens = new LinkedList<>();
            Symbols.Action.Context.errors = new LinkedList<>();
            TokenFactory.TokenFolder.NewlineToken nlt = new TokenFactory.TokenFolder.NewlineToken();
            PL_IMPL_Main.prev_line = -1;

            try {
                gts = parsingTable.apply(new Function<TokenFactory.ITableOfSymbols, TokenFactory.IToken>() {
                    Token token;

                    @Override
                    public TokenFactory.IToken apply(TokenFactory.ITableOfSymbols ts) {
                        Symbols.Action.Context.token = token;
                        token = parser.t;
                        if (Symbols.Action.Context.token == null) Symbols.Action.Context.token = token;
                        while ( prev_line < token.line ) {
                            prev_line++;
                            return nlt;
                        }
                        if ( token.kind == 0 && closingDelimiterPut == false ){
                            closingDelimiterPut = true;
                            return nlt;
                        }
                        parser.Get();
                        TokenFactory.IToken tk = TokenFactory.create(token, ts);
                        if (token.kind != 0) tokens.add(tk);
                        return tk;
                    }
                });
            } catch (RuntimeException e) {
                System.out.flush();
                System.err.flush();
                System.err.println();
                String msg = "TERMINAL ERROR In line " + scanner.line + ", col " + scanner.col +
                        "\n\t" + Files.readAllLines(Paths.get(filename))
                                    .get(Math.min(
                                                scanner.line,
                                                Files.readAllLines(Paths.get(filename)).size()
                                        ) - 1)+"\n"+e.getMessage();
                errors.put(filename, msg);
                PrintStream p = fileWriter.getErrorPrintStream();
                p.println(msg);
                p.println("\n\n--StackTrace--");
                e.printStackTrace(p);
                p.close();
            } finally {
                if (Symbols.Action.Context.errors.size() > 0) {
                    System.out.flush();
                    System.err.flush();
                    System.err.println();
                    List<String> lines = Files.readAllLines(Paths.get(filename));
                    for (Symbols.Action.Context.Error e : Symbols.Action.Context.errors) {
                        String msg = "Error in line " + e.line + ", col " + e.col + " before or at \"" + e.tk + "\": " + e.reason +
                                "\n\t" + lines.get(Math.min(e.line,lines.size()) - 1);
                        System.err.println(msg);
                        if ( errors.containsKey(filename) ) errors.put(filename, errors.get(filename)+"\n"+msg);
                        else errors.put(filename, msg);
                    }
                    PrintStream ps = fileWriter.getErrorPrintStream();
                    ps.println("-Test of file "+filename+"\n\tSuccess: "+(! errors.containsKey(filename) )+
                            ((errors.containsKey(filename)==false) ? "" : (
                                    "\n\tErrors: \n\t\t"+errors.get(filename).replaceAll("\n","\n\t\t")+"\n"
                            )));
                    ps.close();
                }
            }
            fileWriter.writeTokenFile(tokens);
            fileWriter.writeVASTParse(parsingTable);
            if (gts != null) fileWriter.writeTableOfSymbolsFile(gts.getScopedTablesOfSymbols());
        }
        if (errors.size() != 0){
            System.out.println("These files produced errors which can be found in their output directory:");
            for ( String filename : filenames ){
                if (verbose) System.out.println("-Test "+filename);
                if (errors.containsKey(filename)) {
                    System.out.println("\t\t"+Paths.get(filename).getFileName());
                    if (verbose) System.out.println("\tSuccess: false\n\t"+errors.get(filename).replace("\n","\n\t")+"\n");
                }if (verbose) System.out.println("\tSuccess: true\n");
            }
        }
    }

}
