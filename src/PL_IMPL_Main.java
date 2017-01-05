import java.util.Iterator;
import java.util.function.Function;

/**
 * Created by matti on 05/10/2016.
 */
public class PL_IMPL_Main {

    public static void main(String[] argv) throws Exception {
/*
        GlobalTableOfSymbols gts = new GlobalTableOfSymbols();
        while (token != null && token.kind != 0) {
            System.out.println(TokenFactory.create(token, gts));
            parser.Get();
            token = parser.t;
        }
  */
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
        GlobalTableOfSymbols gts = new GlobalTableOfSymbols();

        pharsingTable.apply(new Function<TokenFactory.ITableOfSymbols, TokenFactory.IToken>() {
            Token token;
            @Override
            public TokenFactory.IToken apply(TokenFactory.ITableOfSymbols ts){
                token = parser.t;
                parser.Get();
                return TokenFactory.create(token, ts);
            }
        });

        //fileWriter.writeTokenFile(, gts);

    }

}
