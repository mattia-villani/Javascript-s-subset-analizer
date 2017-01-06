import com.sun.deploy.security.ValidationState;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;


/**
 * Created by matti on 22/11/2016.
 */


public class Grammar{
    public enum Scoop {
        Global,
        Function
    }
    TokenFactory.ITableOfSymbols curTS;
    Scoop currScoop = Scoop.Global;
    public void pushScoop(Symbols.Action.Context c){
        // todo manage table of symbols
        if ( currScoop == Scoop.Function ) c.err("Can't have nested functions" );
        else currScoop = Scoop.Function;
    }
    public void popScoop(Symbols.Action.Context c){
        // todo manage table of symbols
        if ( currScoop != Scoop.Function ) c.err("Can't un-scoop global environment" );
        else currScoop = Scoop.Global;
    }
    public Scoop getCurrScoop() {return currScoop;}
    public TokenFactory.ITableOfSymbols getTS(){return curTS;}

    public enum ATT{
        TOKEN,
        TYPE,
        VAR_TYPE,
        FUN_TYPE,
        IS_VAR_TYPE,
        INNER_TYPE,
        THERE_IS_INNER
    }
    public enum VAR_TYPES {
        INT,
        STRING,
        BOOL,
        VOID,
        INVALID
    }
    public enum TYPES {OK,ERR}
    static public class FUN_TYPES{ // todo
        final List<VAR_TYPES> argsTypes;
        final VAR_TYPES ret;
        public FUN_TYPES(){ this(new LinkedList<VAR_TYPES>(),VAR_TYPES.INVALID); }
        public FUN_TYPES(List<VAR_TYPES> list){ this( list, VAR_TYPES.INVALID ); }
        public FUN_TYPES(List<VAR_TYPES> list, VAR_TYPES ret) {
            argsTypes = list;
            this.ret = ret;
        }
        public FUN_TYPES withReturn(VAR_TYPES ret){
            return new FUN_TYPES(argsTypes, ret);
        }
        public FUN_TYPES withMoreArgs(VAR_TYPES... args){
            List<VAR_TYPES> newArgs = new LinkedList<>(Arrays.asList(args));
            newArgs.addAll(argsTypes);
            return new FUN_TYPES(newArgs, ret);
        }
        @Override
        public boolean equals(Object o){ return toString().equals(o.toString()); }
        @Override
        public String toString(){
            return "FUN:"+argsTypes.stream().map(t -> t.name()).reduce((a,b)->a+"x"+b).orElse(VAR_TYPES.VOID.name())
                    +(ret.equals(VAR_TYPES.INVALID) ? "" : ("->"+ret.name()));
        }
    }

    static public ID ID(Symbols.Action.Context c, String name){ return new ID(c,name); }
    static public ID ID(Symbols.Action.Context c){ return new ID(c,"id"); }
    static public S S(Symbols.Action.Context c, String name){ return new S(c,name); }
    static public class S {
        protected Symbols.NonActionSymbol sym;
        protected Symbols.Action.Context context;

        public S (Symbols.Action.Context c, String name) {
            sym = c.get(name);
            this.context = c;
        }
//        public <R> R Do( Function<S, R> fun ){ return fun.apply(this); }
        public void Do( Consumer<S> fun ){ fun.accept(this); }

        public S withType(Consumer<TYPES> con){ con.accept(getType()); return this; }
        public TYPES getType(){ return sym.get(ATT.TYPE,TYPES.class); }


        // REMEMBER: true == error
        public S setErr(String reason){ context.err(reason); return setERR(); }
        public S setErr(boolean err){ sym.set(ATT.TYPE, err ? TYPES.ERR : TYPES.OK ); return this; }
        public S setType(TYPES... types ){return setType(Stream.of(types)); }
        public S setType(String... ids ){return setType(Stream.of(ids).map(id->S(context,id).getType())); }
        public S setType(S... ss ){return setType(Stream.of(ss).map(s->s.getType())); }
        public S setType(Stream<TYPES> types ){return setErr(Stream.of(types).anyMatch(TYPES.ERR::equals)); }
        public S setOK(){ return setErr(false); }
        public S setERR(){ return setErr(true); }
        public S andType(String... types){ return getType().equals(TYPES.ERR) ? this: setType(types); }
        public S andType(TYPES... types){ return getType().equals(TYPES.ERR) ? this: setType(types); }

        public S setVarType(VAR_TYPES val){ sym.set(ATT.VAR_TYPE, val); return this; }
        public VAR_TYPES getVarType(){ return sym.get(ATT.VAR_TYPE, VAR_TYPES.class); }

        public S setFunType(FUN_TYPES val){ sym.set(ATT.FUN_TYPE, val); return this; }
        public FUN_TYPES getFunType(){ return sym.get(ATT.FUN_TYPE, FUN_TYPES.class); }

        public S setIsVarType(boolean isIt) { sym.set(ATT.IS_VAR_TYPE, Boolean.valueOf(isIt)); return this; }
        public boolean isVarType() { return sym.get(ATT.IS_VAR_TYPE, Boolean.class); }

        public S set(ATT att, Object val){ sym.set(att,val); return this; }
        public <T> T get(ATT att, Class<T> cast){ return sym.get(att, cast); }
    }
    static private class ID extends S{
        TokenFactory.TokenFolder.WordToken.IdToken id;

        public ID(Symbols.Action.Context c, String id) {
            super(c, id);
            this.id = c.get(id).get(ATT.TOKEN, TokenFactory.TokenFolder.WordToken.IdToken.class);
        }

        public String getLexema(){
            // todo;
            return id.getName();
        }
        @Override
        public VAR_TYPES getVarType(){
            // todo
            return VAR_TYPES.INT;
        }
        @Override
        public FUN_TYPES getFunType(){
            // todo
            return new FUN_TYPES();
        }
        @Override
        public boolean isVarType(){
            // todo
            return true;
        }
        public ID ifValid( Consumer<ID> _then, Consumer<String> _else ){
            // todo and store error
            _then.accept(this);
            return this;
        }

    }

    static public void DEC(boolean enable_declaration){
        // todo
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

        /**
         * GRAMMAR BEGINS
         */

        P(new Symbols.Axiom("Program'"), "Program",
                (A)(c,r)->r.setType("Program") );

        P("Program", "Sequence",
                (A)(c,r)->r.setType("Sequence") );

        P("Sequence", "Statement", "Sequence",
                    (A)(c,r)->r.setType("Statement", "Sequence1"))
                .or(Symbols.LAMBDA,(A)(c,r)->r.setOK() );

        //Delimiter -> Semicolon | NewLine
        P("Delimiter", "semi")
                .or("newline");

        // only state is type.
        P("Statement", "var", "Declaration", (A)(c,r)->r.setType("Declaration"))
                .or("id", "AssOrFunCall",
                        (A)(c,r)-> ID(c).ifValid(
                                    (s_id) ->
                                        S(c,"AssOrFunCall").Do(
                                                ass->{
                                                    if ( ass.isVarType() && s_id.isVarType() )
                                                        r.setErr( ! s_id.getVarType().equals(ass.getVarType()));
                                                    else if ( ! ass.isVarType() && ! s_id.isVarType() )
                                                        r.setErr( ! s_id.getFunType().equals(ass.getFunType()));
                                                    else
                                                        r.setErr(s_id.isVarType()?s_id.getLexema()+" is a variable, can't be called as a function.":s_id.getLexema()+" is a function, can't be assigned");
                                                })
                                    ,
                                    (reason) -> r.setErr("Invalid id: "+reason)
                            ))
                .or("preinc", "id", "Delimiter", (A)(c,r)->ID(c).ifValid(
                                    (id) -> {
                                        if ( id.isVarType() && id.getVarType().equals(VAR_TYPES.INT) )
                                            r.setOK().setVarType(VAR_TYPES.INT);
                                        else r.setErr("Pre inc performable only over int, but " + id.getLexema() + " is of " + id.getVarType());
                                    },
                                    (reason) -> r.setVarType(VAR_TYPES.INVALID).setErr("Invalid id: "+reason)
                                ))
                .or("Switch",(A)(c,r)->r.setType("Switch"))
                .or("Return",(A)(c,r)->r.setType("Return"))
                .or("FunctionDec", (A)(c,r)->r.setType("FunctionDec"));

        P("AssOrFunCall", "assign", "Exp",
                    (A)(c,r)-> S(c,"Exp").Do( exp ->
                                    r.setIsVarType(true)
                                    .setVarType(exp.getVarType())
                                    .setType(exp.getType())
                            ))
                .or("openbracket", "Arguments", "closebracket",
                    (A)(c,r)-> S(c,"Arguments").Do( args ->
                            r.setIsVarType(false)
                            .setFunType(args.getFunType())
                            .setType(args.getType())
                    ));

        // todo error declaring (exp reserverd word or id already in use )
        P("Declaration",
                "Type",
                (A)(c,r)->DEC(true),
                "id",
                (A)(c,r)->DEC(false),
                "Init",
                (A)(c,r)-> S(c,"Init").Do(init-> S(c,"Type").Do( type -> ID(c).ifValid(
                    (id) -> {
                        if ( init.getVarType().equals(VAR_TYPES.VOID) ) r.setType(init);
                        else if ( init.getVarType().equals(VAR_TYPES.INVALID) ) r.setERR();
                        else if ( init.getVarType().equals(id.getVarType() ) ) r.setType(init);
                        else r.setErr( "Unable to assign value "+init.getVarType()+" to variable "+id.getLexema()+" of type "+id.getVarType() );
                    },
                    (reason) -> r.setErr("Invalid identifier: "+ reason ) ))),
                "AdditionalDeclaration",
                (A)(c,r)-> r.andType("AdditionalDeclaration")
                );


        /*HERE  P("Init", "assign", "Assignable", (A)(c->c.get("Init").set("type", c.get("Assignable").get("type"))) ) // Exp, not val
                .or(Symbols.LAMBDA);
        */

        P("Init", "assign", "Assignable",
                    (A)(c,r)-> S(c,"Assignable").Do( ass -> r
                            .setVarType(ass.getVarType())
                            .setType(ass.getType()) ))
                .or(Symbols.LAMBDA, (A)(c,r)-> r
                        .setOK()
                        .setVarType(VAR_TYPES.VOID) );


        P("Assignable", "Value", (A)(c,r)->S(c,"Value").Do( val ->
                        r.setVarType(val.getVarType()).setType(val)
                    ))
                .or("id", "AssOrFunCall", (A)(c,r)->S(c,"AssOrFunCall").Do(ass->ID(c).ifValid( id-> {
                             if ( id.isVarType() ){
                                 if ( ass.isVarType() ) r.setVarType(ass.getVarType()).setType(ass);
                                 else r.setVarType(VAR_TYPES.INVALID).setErr(id.getLexema()+" a value, can't be called.");
                             }else
                                 if ( ass.isVarType() ) r.setVarType(VAR_TYPES.INVALID).setErr(id.getLexema()+" is a function, it can't be assigned.");
                                 else r.setVarType(ass.getFunType().ret).setType(ass);
                         }
                         ,
                         reason -> r.setERR().setVarType(VAR_TYPES.INVALID)
                        )));

        //AdditionalDeclaration -> Comma Type id Init AdditionalDeclaration | Delimiter
        P("AdditionalDeclaration", "comma", "Declaration", "AdditionalDeclaration", (A)(c,r)->r.setType("Declaration","AdditionalDeclaration1"))
                .or("Delimiter", (A)(c,r)->r.setOK());
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
        P("Arguments", "Exp", "Paramlist", (A)(c,r)->S(c,"Exp").Do(exp->S(c,"Paramlist").Do(lst ->
                    r.setType(exp,lst).setFunType(lst.getFunType().withMoreArgs(exp.getVarType()))
                )))
                .or(Symbols.Terminal.LAMBDA,(A)(c,r)->r.setOK().setFunType(new FUN_TYPES()));

        P("Paramlist", "comma", "Arguments", (A)(c,r)->r.setType("Arguments").setFunType(S(c,"Arguments").getFunType() ))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().setFunType(new FUN_TYPES()));


        P("FunctionDec", "function", "NullableType", "id", "openbracket", "ArgsDeclaration", "closebracket", "openbrace", "Sequence", "closebrace");

        P("NullableType", "Type", (A)(c,r)->r.setVarType(S(c,"Type").getVarType()).setOK())
                .or(Symbols.LAMBDA, (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK());

        P("Return", "return", "NullableExp", "Delimiter", (A)(c,r)->S(c,"NullableExp").Do(nulexp->{
                if ( getCurrScoop().equals(Scoop.Global) == false )
                    r.setErr("Can't use return statement here, it has to be used inside a function declaration");
                else r
                        .setVarType(nulexp.getVarType())
                        .setType(nulexp);
            }));

        P("NullableExp", "Exp", (A)(c,r)->S(c,"Exp").Do(exp->
                    r.setVarType(exp.getVarType()).setType(exp.getType())))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK());

        // todo
        //ArgsDeclaration -> Type id ParamDecList | lambda
        P("ArgsDeclaration", "Type", "id", "ParamDecList")
                .or(Symbols.LAMBDA);
        //ParamDecList -> comma Type id ParamDecList | lambda
        P("ParamDecList", "comma", "Type", "id", "ParamDecList")
                .or(Symbols.LAMBDA);


        P("Value", "number", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.INT))
                .or("false", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.BOOL))
                .or("true", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.BOOL))
                .or("string", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.STRING));

        P("Type", "int", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.INT) )
                .or("chars", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.STRING))
                .or("bool", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.BOOL)) ;

        //Exp -> Andexp Orexp todo correct this
        P("Exp", "Andexp", "Orexp", (A)(c,r)->r.setOK().setVarType(VAR_TYPES.INT));
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

        A Relexp2Nexp = (A)(c,r)->S(c,"Nexp").Do(nexp-> {
            if ( nexp.getVarType().equals(VAR_TYPES.INT ))
                r.setVarType(VAR_TYPES.BOOL).setType(nexp.getType());
            else if ( nexp.getVarType().equals(VAR_TYPES.INVALID) )
                r.setVarType(VAR_TYPES.INVALID).setERR();
            else r.setErr("In relational comparision (<,>,<=,>=), the two members have to be int, " +
                        "but the second member is of type " + nexp.getVarType());
        });

        P("Relexp'", Symbols.LAMBDA, (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK())
                .or("gt", "Nexp",Relexp2Nexp)
                .or("lt", "Nexp",Relexp2Nexp)
                .or("egt", "Nexp",Relexp2Nexp)
                .or("elt", "Nexp",Relexp2Nexp);

        A Compexp2Bexp = (A)(c,r)->S(c,"Bexp").Do(b -> r
                        .setVarType(VAR_TYPES.BOOL)
                        .set(ATT.INNER_TYPE,b.getVarType())
                        .set(ATT.THERE_IS_INNER, Boolean.TRUE)
                        .setType(b.getType()));

        P("Compexp", Symbols.LAMBDA, (A)(c,r)->r
                                .set(ATT.THERE_IS_INNER, Boolean.FALSE)
                                .setVarType( VAR_TYPES.VOID )
                                .setOK()
                            )
                .or("eq", "Bexp",Compexp2Bexp)
                .or("neq", "Bexp",Compexp2Bexp);

        /**
         * END OF GRAMMAR!!!!
         */

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
                                public void accept(Context context, S root) {
                                    System.out.print("\tFIREING Action with Context: "+context);
                                    a.accept(context, root);
                                    System.out.println(" ---->>>> "+context);
                                }
                            }).apply((A)(seq[i]));
            else sims[i] = (Symbols) seq[i];
        return new P_fact(gen, sims);
    }

    private P_fact P(String gen, Object... seq) {
        return P(lkNT(gen), seq);
    }

    private interface A extends BiConsumer<Symbols.Action.Context,S> {}

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
