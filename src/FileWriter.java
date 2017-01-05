import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Created by JoeAm on 05/01/2017.
 */
class FileWriter {
    private String path;

    FileWriter(String path){
        this.path = path;
    }

    void writeTokenFile(Collection<TokenFactory.IToken> tokens, GlobalTableOfSymbols tableOfSymbols) throws  IOException{
        String file = String.format("%s\ficheroTokens.txt", path);
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        for (TokenFactory.IToken token : tokens){
            writer.println(token.toFileLine(tableOfSymbols));
        }
        writer.close();
    }
}
