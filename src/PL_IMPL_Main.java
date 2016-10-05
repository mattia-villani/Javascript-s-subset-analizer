
/**
 * Created by matti on 05/10/2016.
 */
public class PL_IMPL_Main {

    public static void main(String[] argv){
        Scanner scanner = new Scanner("C:\\Users\\matti\\Desktop\\Prova.txt");
        Parser parser = new Parser(scanner);
        parser.Parse();
        Token token = parser.la;
        System.out.println(token.val);
    }

}
