import java.util.Iterator;

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
        Grammar grammar = new Grammar();
        PharsingTable pharsingTable = new PharsingTable(grammar);

        Scanner scanner = new Scanner("C:\\Users\\matti\\Desktop\\test.txt");
        Parser parser = new Parser(scanner);
        parser.Parse();
        GlobalTableOfSymbols gts = new GlobalTableOfSymbols();

        pharsingTable.apply(new Iterator<TokenFactory.IToken>() {
            boolean init = false;
            Token token;
            @Override
            public boolean hasNext() {
                return !init || ( token != null && token.kind != 0 );
            }

            @Override
            public TokenFactory.IToken next(){
                init = true;
                token = parser.t;
                parser.Get();
                return TokenFactory.create(token, gts);
            }
        });
    }

}
