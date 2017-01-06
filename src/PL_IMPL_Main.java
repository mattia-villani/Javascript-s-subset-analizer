import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by matti on 05/10/2016.
 */
public class PL_IMPL_Main {

    public static GlobalTableOfSymbols gts = new GlobalTableOfSymbols();

    public static void main(String[] argv) throws Exception {
        if (argv.length < 2){
            System.out.println("Requires Two Arguments: \n Arg[0]\tSource Code\n Arg[1]\tOutput Directory");
            System.exit(1);
        }
        String filename = argv[0];
        String outputDir = argv[1];
        FileWriter fileWriter = new FileWriter(outputDir);

        Grammar grammar = new Grammar();
        PharsingTable pharsingTable = new PharsingTable(grammar);

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
                    if (Symbols.Action.Context.token==null) Symbols.Action.Context.token = token;
                    parser.Get();
                    TokenFactory.IToken tk = TokenFactory.create(token, ts);
                    if ( token.kind != 0 )tokens.add(tk);
                    return tk;
                }
            });
        }catch (RuntimeException e){
            System.out.flush();
            System.err.flush();
            System.err.println();
            System.err.println("In line "+scanner.line+", col "+scanner.col+
                    "\n\t"+ Files.readAllLines(Paths.get(filename)).get(scanner.line-1));
            System.err.println(e.getMessage()+"\n\n--StackTrace--");
            e.printStackTrace();
        }finally {
            if (Symbols.Action.Context.errors.size()>0) {
                System.out.flush();
                System.err.flush();
                System.err.println();
                List<String> lines =  Files.readAllLines(Paths.get(filename));

                for (Symbols.Action.Context.Error e : Symbols.Action.Context.errors)
                    System.err.println("Error in line "+e.line+", col "+e.col+" before or at \""+e.tk+"\": "+e.reason+
                            "\n\t"+ lines.get(e.line-1));
            }
        }
        fileWriter.writeTokenFile(tokens);
        if ( gts != null ) fileWriter.writeTableOfSymbolsFile(gts.getScopedTablesOfSymbols());
    }

}
