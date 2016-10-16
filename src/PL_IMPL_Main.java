
/**
 * Created by matti on 05/10/2016.
 */
public class PL_IMPL_Main {

    public static void main(String[] argv){
        Scanner scanner = new Scanner("C:\\Users\\Joe\\Desktop\\test.txt");
        Parser parser = new Parser(scanner);
        parser.Parse();
        Token token = parser.t;
        GlobalTableOfSymbols gts = new GlobalTableOfSymbols();
        while (token != null && token.kind != 0) {
            System.out.println(TokenFactory.create(token, gts));
            parser.Get();
            token = parser.t;
        }

    }

}
