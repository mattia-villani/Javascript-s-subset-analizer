import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by matti on 05/10/2016.
 */
public class PL_IMPL_Main {

    public static GlobalTableOfSymbols gts = new GlobalTableOfSymbols();

    public static void main(String[] argv) throws Exception {
        if (argv.length < 2) {
            System.out.println("Requires Two Arguments: \n Arg[0]\tSource Code\n Arg[1]\tOutput Directory");
            System.exit(1);
        }
        File file = new File(argv[0]);
        String[] filenames =
                Stream.of(file.isDirectory() ? file.listFiles() : new File[]{file})
                        .map(f -> f.getAbsolutePath())
                        .toArray(i -> new String[i]);

        String outputDir = argv[1];
        FileWriter fileWriter = new FileWriter(outputDir);

        Grammar grammar = new Grammar();
        PharsingTable pharsingTable = new PharsingTable(grammar);
        fileWriter.writeParsingTable(pharsingTable);
        fileWriter.writeGramar();

        Map<String, String> errors = new HashMap<>();

        for (String filename : filenames) {
            System.out.println("///----------------------------------------------------------\\\\\\");
            System.out.println("|||\tTesting file: "+filename);
            System.out.println("\\\\\\----------------------------------------------------------///");
            Scanner scanner = new Scanner(filename);
            Parser parser = new Parser(scanner);
            parser.Parse();
            GlobalTableOfSymbols gts = null;

            LinkedList<TokenFactory.IToken> tokens = new LinkedList<>();


            try {
                gts = pharsingTable.apply(new Function<TokenFactory.ITableOfSymbols, TokenFactory.IToken>() {
                    Token token;

                    @Override
                    public TokenFactory.IToken apply(TokenFactory.ITableOfSymbols ts) {
                        Symbols.Action.Context.token = token;
                        token = parser.t;
                        if (Symbols.Action.Context.token == null) Symbols.Action.Context.token = token;
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
                String msg = "In line " + scanner.line + ", col " + scanner.col +
                        "\n\t" + Files.readAllLines(Paths.get(filename)).get(scanner.line - 1)+"\n"+e.getMessage();
                errors.put(filename, msg);
                System.err.println(msg);
                System.err.println("\n\n--StackTrace--");
                e.printStackTrace();
            } finally {
                if (Symbols.Action.Context.errors.size() > 0) {
                    System.out.flush();
                    System.err.flush();
                    System.err.println();
                    List<String> lines = Files.readAllLines(Paths.get(filename));

                    for (Symbols.Action.Context.Error e : Symbols.Action.Context.errors) {
                        String msg = "Error in line " + e.line + ", col " + e.col + " before or at \"" + e.tk + "\": " + e.reason +
                                "\n\t" + lines.get(e.line - 1);
                        System.err.println(msg);
                        if ( errors.containsKey(filename) ) errors.put(filename, errors.get(filename)+"\n"+msg);
                        else errors.put(filename, msg);
                    }
                }
            }
            fileWriter.writeTokenFile(tokens);
            if (gts != null) fileWriter.writeTableOfSymbolsFile(gts.getScopedTablesOfSymbols());
        }
        System.out.println("\n\n----- FINAL INFOS -----\n");
        for ( String filename : filenames ){
            System.out.println("-Test of file "+filename+"\n\tSuccess: "+(! errors.containsKey(filename) )+
                    ((errors.containsKey(filename)==false) ? "" : (
                        "\n\tErrors: \n\t\t"+errors.get(filename).replaceAll("\n","\n\t\t")+"\n"
                    )));
        }
    }

}
