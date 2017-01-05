import com.sun.deploy.security.ValidationState;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * Created by matti on 22/11/2016.
 */


public class Grammar{
    static private class U{ // support to short the actions
        static public String TYPE = "type",
            OK_TYPE = "ok_type",
            ERR_TYPE = "err_type";

        static public String ARG_COUNT = "arg_count";

        static public String VAR_TYPE = "var_type",
            INT_VAR_TYPE = "int",
            STRING_VAR_TYPE = "string",
            BOOL_VAR_TYPE = "bool";


        static void IF(boolean guard, Runnable _then, Runnable _else){
            if ( guard ) _then.run();
            else _else.run();
        }
        static Runnable ERR(String messg){
            return ()-> Symbols.Action.Context.err(messg);
        }
        static void SET_TYPE(Symbols.Action.Context c, String assigned, String... types){
            boolean ok = true;
            for ( int i=0; i<types.length && ok; i++ )
                ok = ok && c.get(types[i]).get(TYPE,String.class).equals(OK_TYPE);
            c.get(assigned).set(TYPE, ok?OK_TYPE:ERR_TYPE );
        }
    }


    Map<String, Symbols> map;
    public Grammar(){
        map = new HashMap<String, Symbols>();
        List<Class<?>> tokens = new LinkedList<Class<?>>();

        System.out.println("Going to add to map " + Arrays.asList(TokenFactory.TokenFolder.class.getClasses()));
        tokens.addAll(Arrays.asList(TokenFactory.TokenFolder.class.getClasses()));

        System.out.println("Going to add to map " + Arrays.asList(TokenFactory.TokenFolder.WordToken.ReservedWordToken.class.getClasses()));
        tokens.addAll(Arrays.asList(TokenFactory.TokenFolder.WordToken.ReservedWordToken.class.getClasses()));

        System.out.println("Going to add to map " + Arrays.asList(TokenFactory.TokenFolder.WordToken.class.getClasses()));
        tokens.addAll(Arrays.asList(TokenFactory.TokenFolder.WordToken.class.getClasses()));

        for (Class<?> clazz : tokens)
            if (!clazz.equals(TokenFactory.TokenFolder.WordToken.class)) {
                String str = clazz.getSimpleName().replace("Token", "").toLowerCase();
                map.put(str, new Symbols.Terminal((Class<TokenFactory.IToken>) clazz)/*{
                    @Override
                    public int getId(){ return 0; }

                }*/);
                System.out.println("Just added to map " + str);
            }

        P(new Symbols.Axiom("Program'"), "Program",
                (A)c->U.SET_TYPE(c,"Program'","Program") );

        P("Program", "Sequence",
                (A)c->U.SET_TYPE(c,"Program","Sequence") );

        P("Sequence", "Statement", "Sequence",
                    (A)c->U.SET_TYPE(c,"Sequence", "Statement", "Sequence1"))
                .or(Symbols.LAMBDA,(A)c->U.SET_TYPE(c,"Sequence") );

        //Delimiter -> Semicolon | NewLine
        P("Delimiter", "semi")
                .or("newline");

        P("Statement", "Declaration", (A)c->U.SET_TYPE(c,"Statement","Declaration"))
                .or("id", "AssOrFunCall", (A)c->{
                        Symbols.NonActionSymbol assOrFunCall = c.get("AssOrFunCall");
                        TokenFactory.TokenFolder.WordToken.IdToken tk = c.get("id").get("token", TokenFactory.TokenFolder.WordToken.IdToken.class);
                        boolean error = assOrFunCall.get(U.TYPE).equals(U.ERR_TYPE);
                        // TODO  if ( tk.getEntry().getType() matches with the one of AssOrFunCall ) OK else ERROR;
                        c.get("Statement").set(U.TYPE, error ? U.ERR_TYPE : U.OK_TYPE );
                    })
                .or("preinc", "id", "Delimiter",(A)c->{
                        Symbols.NonActionSymbol id = c.get("id");
                        TokenFactory.TokenFolder.WordToken.IdToken tk = id.get("token", TokenFactory.TokenFolder.WordToken.IdToken.class);
                        if( id.get(U.VAR_TYPE).equals(U.INT_VAR_TYPE) )
                            U.SET_TYPE(c,"Statement");
                        else {
                            c.get("Statement").set(U.TYPE, U.ERR_TYPE);                             //TODO | put lexema
                            c.err("Pre-inc operation can be performed only over integer, but "+id.getName()+" is "+id.get(U.VAR_TYPE));
                        }
                    })
                .or("Switch",(A)c->U.SET_TYPE(c,"Statement"))
                .or("Return",(A)c->U.SET_TYPE(c,"Statement"))
                .or("FunctionDec", (A)c->U.SET_TYPE(c,"Statement","FunctionDec"));

        P("AssOrFunCall", "assign", "Exp",
                    (A)c->{
                        Symbols.NonActionSymbol assOrFunCall = c.get("AssOrFunCall");
                        Symbols.NonActionSymbol exp = c.get("Exp");
                        assOrFunCall.set(U.TYPE, exp.get(U.TYPE));
                        assOrFunCall.set(U.VAR_TYPE, exp.get(U.VAR_TYPE));
                    })
                .or("openbracket", "Arguments", "closebracket",
                    (A)c->{
                        Symbols.NonActionSymbol assOrFunCall = c.get("AssOrFunCall");
                        Symbols.NonActionSymbol args = c.get("Arguments");
                        assOrFunCall.set(U.TYPE, args.get(U.TYPE));
                        assOrFunCall.set(U.ARG_COUNT, args.get(U.ARG_COUNT));
                    });

        //Declaration -> var Type id Init AdditionalDeclaration
        P("Declaration", "var", "Type", "id", "Init", "AdditionalDeclaration");
        //Init -> = Exp | lambda
/*HERE*/P("Init", "assign", "Assignable", (A)(c->c.get("Init").set("type", c.get("Assignable").get("type"))) ) // Exp, not val
                .or(Symbols.LAMBDA);
        P("Assignable", "Value", (A)(c->c.get("Assignable").set("type", c.get("Value").get("type")) ))
                .or("id", "AssOrFunCall", (A)(c->{c.get("Assignable").set("type", "bool");c.err("Reason!");} ));
        //AdditionalDeclaration -> Comma Type id Init AdditionalDeclaration | Delimiter
        P("AdditionalDeclaration", "comma", "Type", "id", "Init", "AdditionalDeclaration")
                .or("Delimiter");
        //Switch -> switch openbracket Exp closebracket openbrace Case Cases closebrace
        P("Switch", "switch", "openbracket", "Exp", "closebracket", "openbrace", "Case", "Cases", "closebrace");
        //Cases -> Case Cases | Lambda
        P("Cases", "Case", "Cases")
                .or(Symbols.LAMBDA);
        //Case -> case Value colon Sequence Break
        P("Case", "case", "Value", "colon", "Sequence", "Break");
        //Break -> break Delimiter | lamda
        P("Break", "break", "Delimiter")
                .or(Symbols.LAMBDA);
        //Arguments -> Paramlist | lambda
        P("Arguments", "Exp", "Paramlist")
                .or(Symbols.Terminal.LAMBDA);
        //Paramlist -> Exp comma Paramlist | Exp
        P("Paramlist", "comma", "Exp", "Paramlist")
                .or(Symbols.Terminal.LAMBDA);
        //Functiondeclaration -> function NullableType id openbracket ArgsDeclaration closebracket openbrace Sequence closebracket
        P("FunctionDec", "function", "NullableType", "id", "openbracket", "ArgsDeclaration", "closebracket", "openbrace", "Sequence", "closebrace");
        //NullableType -> Type | Lambda
        P("NullableType", "Type")
                .or(Symbols.LAMBDA);
        //Return -> return NullableExp | lambda
        P("Return", "return", "NullableExp", "Delimiter");
        //NullableExp -> Exp | lambda
        P("NullableExp", "Exp")
                .or(Symbols.LAMBDA);
        //ArgsDeclaration -> Type id ParamDecList | lambda
        P("ArgsDeclaration", "Type", "id", "ParamDecList")
                .or(Symbols.LAMBDA);
        //ParamDecList -> comma Type id ParamDecList | lambda
        P("ParamDecList", "comma", "Type", "id", "ParamDecList")
                .or(Symbols.LAMBDA);
        //Value -> boolean | number | string
        P("Value", "number", (A)(c->c.get("Value").set("type", "int")))
                .or("false", (A)(c->c.get("Value").set("type", "bool")))
                .or("true", (A)(c->c.get("Value").set("type", "bool")))
                .or("string", (A)(c->c.get("Value").set("type", "string")));
        //Type -> int | chars | bool
        P("Type", "int", (A)(c->c.get("Type").set("type", c.get("int").get("token", TokenFactory.TokenFolder.WordToken.ReservedWordToken.IntToken.class).getLexema())))
                .or("chars", (A)(c->c.get("Type").set("type", "string")))
                .or("bool", (A)(c->c.get("Type").set("type", "bool"))) ;
        //Exp -> Andexp Orexp
        P("Exp", "Andexp", "Orexp");
        //Nexp -> Term Aexp
        P("Nexp", "Term", "Aexp");
        //Aexp -> plus Nexp | minus Nexp | lambda
        P("Aexp", Symbols.LAMBDA)
                .or("plus", "Nexp")
                .or("minus", "Nexp");
        P("Term", "Factor", "Term'");
        P("Term'", Symbols.LAMBDA)
                .or("mult", "Term")
                .or("mod", "Term")
                .or("div", "Term");
        P("Factor", "id", "Assigofunc")
                .or("preinc", "id")
                .or("openbracket", "Exp", "closebracket")
                .or("not", "Exp")
                .or("Value");
        P("Assigofunc", "assign", "Exp")
                .or("openbracket", "Arguments", "closebracket")
                .or(Symbols.LAMBDA);
        P("Andexp", "Bexp", "Andexp'");
        P("Andexp'", "and", "Bexp", "Andexp'")
                .or(Symbols.LAMBDA);
        P("Orexp", "or", "Exp")
                .or(Symbols.LAMBDA);
        P("Bexp", "Relexp", "Compexp");
        P("Relexp", "Nexp", "Relexp'");
        P("Relexp'", Symbols.LAMBDA)
                .or("gt", "Nexp")
                .or("lt", "Nexp")
                .or("egt", "Nexp")
                .or("elt", "Nexp");
        P("Compexp", Symbols.LAMBDA)
                .or("eq", "Bexp")
                .or("neq", "Bexp");

        System.out.println(" -- Sys deubg -- ");
        for (String symName : map.keySet())
            try {
                Symbols sym = map.get(symName);
                System.out.println(">>" + symName + ": " + sym);
                System.out.println("\tfirst ( " + symName + " ) = " + Production.getFirstSetOf(sym));
                if (sym instanceof Symbols.NoTerminal)
                    System.out.println("\tfollow( " + symName + " ) = " + Production.getFollowSetOf((Symbols.NoTerminal) sym));
            } catch (Exception e) {
                System.out.println("... Failed with " + symName);
                e.printStackTrace();
                break;
            }
        System.out.println(" -- Sys deubg ended -- ");
    }

    Symbols lk(String str) {
        if (map.containsKey(str)) return map.get(str);
        else {
            System.out.println(str + " didn't found, adding it... ");
            Symbols.NoTerminal symb = new Symbols.NoTerminal(str){
                @Override
                public int getId(){ return 0; }
            };
            map.put(str, symb);
            return symb;
        }
    }

    Symbols.NoTerminal lkNT(String str) {
        Symbols s = lk(str);
        if (s instanceof Symbols.NoTerminal) return (Symbols.NoTerminal) s;
        throw new RuntimeException("GENERATING SYMBOL " + str + " is not non terminal");
    }

    ;

    private P_fact P(Symbols.NoTerminal gen, Object... seq) {
        Symbols[] sims = new Symbols[seq.length];
        for (int i = 0; i < seq.length; i++)
            if (seq[i] instanceof String) sims[i] = lk((String) seq[i]);
            else if ( seq[i] instanceof A )
                sims[i] = ((Function<A, Symbols.Action>)
                        a -> new Symbols.Action() {
                                @Override
                                public void accept(Context context) {
                                    System.out.print("\tFIREING Action with Context: "+context);
                                    a.accept(context);
                                    System.out.println(" ---->>>> "+context);
                                }
                            }).apply((A)(seq[i]));
            else sims[i] = (Symbols) seq[i];
        return new P_fact(gen, sims);
    }

    private P_fact P(String gen, Object... seq) {
        return P(lkNT(gen), seq);
    }

    private interface A extends Consumer<Symbols.Action.Context> {}

    private class P_fact {
        Symbols.NoTerminal gen;

        public P_fact(Symbols.NoTerminal gen, Symbols... seq) {
            this.gen = gen;
            new Production(gen, seq);
        }

        public P_fact or(Symbols... seq) { // useless
            return new P_fact(gen, seq);
        }

        public P_fact or(Object... seq) {
            return P(gen, seq);
        }
    }
}
