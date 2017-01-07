import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * Created by matti on 22/11/2016.
 */

public class Grammar{

    public enum ATT{TOKEN, TYPE, VAR_TYPE, FUN_TYPE, IS_VAR_TYPE, INNER_TYPE, THERE_IS_INNER, RETURN }
    public enum VAR_TYPES { INT, STRING, BOOL, VOID, INVALID }
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
        public VAR_TYPES returnArgsTypeIfAllEquals_elseINVALID(){
            return argsTypes.stream().reduce((a,b)->a.equals(b)?a:VAR_TYPES.INVALID).orElse(VAR_TYPES.VOID);
        }
    }

    static FUN_TYPES retMerg ( FUN_TYPES f1, FUN_TYPES f2 ){
        if ( f1 == null ) return f2;
        if ( f2 == null ) return f1;
        List<VAR_TYPES> args = new LinkedList<>(f1.argsTypes);
        args.addAll(f2.argsTypes);
        return new FUN_TYPES(args);
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
        public S setType(Stream<TYPES> types ){return setErr(types.anyMatch(TYPES.ERR::equals)); }
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

        public S setNullRet(){ sym.set(ATT.RETURN,null); return this; }
    }
    static private class ID extends S{
        TokenFactory.TokenFolder.WordToken.IdToken id;
        public ID(Symbols.Action.Context c, String id) {
            super(c, id);
            this.id = c.get(id).get(ATT.TOKEN, TokenFactory.TokenFolder.WordToken.IdToken.class);
            PL_IMPL_Main.gts.queryLexema(getLexema());
        }
        public String getLexema(){
            return id.lexema;
        }

        @Override
        public VAR_TYPES getVarType(){
            String lex = getLexema();
            GlobalTableOfSymbols.varType v =
                    Optional.ofNullable(PL_IMPL_Main.gts.getEntry(lex))
                            .map(t->t.getType()).orElseThrow( () -> new RuntimeException("Unable to get varType") );
            return TypeConverter.TOStoFUN(v);
        }
        @Override
        public FUN_TYPES getFunType(){
            GlobalTableOfSymbols.FunctionEntry e = (GlobalTableOfSymbols.FunctionEntry) PL_IMPL_Main.gts.getEntry(getLexema());
            List<VAR_TYPES> vars = new LinkedList<>();
            for (GlobalTableOfSymbols.varType v : e.paramTypes  ){
                vars.add( TypeConverter.TOStoFUN(v));
            }
            return new FUN_TYPES(vars, TypeConverter.TOStoFUN(e.getType()));
        }
        @Override
        public ID setFunType(FUN_TYPES fun){
            if (isVarType()) throw new RuntimeException("Attempt to set function values "+fun+" on a variable "+getLexema());
            GlobalTableOfSymbols.FunctionEntry e = (GlobalTableOfSymbols.FunctionEntry) PL_IMPL_Main.gts.getEntry(getLexema());
            for (VAR_TYPES v : fun.argsTypes  ){
                e.addParamtype( TypeConverter.FUNtoTOS(v));
            }
            e.setEntryVals( TypeConverter.FUNtoTOS(fun.ret));
            return this;
        }
        @Override
        public ID setVarType(VAR_TYPES type){
            if (!isVarType()) throw new RuntimeException("Attempt to set variable values "+type+" on a variable "+getLexema());
            GlobalTableOfSymbols.Entry e = PL_IMPL_Main.gts.getEntry(getLexema());
            e.setEntryVals(TypeConverter.FUNtoTOS(type));
            return this;
        }
        @Override
        public boolean isVarType(){
            GlobalTableOfSymbols.Entry entry = PL_IMPL_Main.gts.getEntry(this.getLexema());
            if ( entry == null ) throw new RuntimeException("Unexpceted missing entry "+getLexema());
            return entry instanceof GlobalTableOfSymbols.FunctionEntry == false;
        }
        public ID ifValid( Consumer<ID> _then, Consumer<String> _else ){
            if ( id.isInvalid() )
                _else.accept(" unknown "+id.lexema );
            else _then.accept(this);
            return this;
        }

    }

    static public void PUSH_SCOOP(String name){
        PL_IMPL_Main.gts.addScope(name);
    }
    static public void POP_SCOOP(){
        PL_IMPL_Main.gts.dropScope();
    }

    static public void DEC(GlobalTableOfSymbols.EDITING edit){
        GlobalTableOfSymbols.editing = edit;
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
                    (A)(c,r)->r
                            .setType("Statement", "Sequence1")
                            .set(ATT.RETURN, retMerg(
                                    S(c,"Statement").get(ATT.RETURN,FUN_TYPES.class),
                                    S(c, "Sequence1").get(ATT.RETURN,FUN_TYPES.class))))
                .or(Symbols.LAMBDA,(A)(c,r)->r.setOK().set(ATT.RETURN,null) );

        //Delimiter -> Semicolon | NewLine
        P("Delimiter", "semi")
                .or("newline");

        // only state is type.
        P("Statement",
                    (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.VAR),
                    "var",
                    "Declaration",
                    (A)(c,r)->r.setType("Declaration").setNullRet(),
                    (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.FORBITTEN))
                .or("id", "AssOrFunCall",
                        (A)(c,r)-> ID(c).ifValid(
                                    (s_id) ->
                                        S(c,"AssOrFunCall").Do(
                                                ass->{
                                                    r.setNullRet();
                                                    if ( ass.isVarType() && s_id.isVarType() )
                                                        r.setErr( ! s_id.getVarType().equals(ass.getVarType()));
                                                    else if ( ! ass.isVarType() && ! s_id.isVarType() )
                                                        r.setErr( ! s_id.getFunType().equals(ass.getFunType()));
                                                    else
                                                        r.setErr(s_id.isVarType()?s_id.getLexema()+" is a variable, can't be called as a function.":s_id.getLexema()+" is a function, can't be assigned");
                                                })
                                    ,
                                    (reason) -> r.setErr("Invalid id: "+reason).setNullRet()
                            ))
                .or("preinc", "id", "Delimiter", (A)(c,r)->ID(c).ifValid(
                                    (id) -> {
                                        if ( id.isVarType() && id.getVarType().equals(VAR_TYPES.INT) )
                                            r.setOK().setVarType(VAR_TYPES.INT);
                                        else r.setErr("Pre inc performable only over int, but " + id.getLexema() + " is of " + id.getVarType());
                                    },
                                    (reason) -> r.setVarType(VAR_TYPES.INVALID).setErr("Invalid id: "+reason)
                                ).Do( ( id  ) -> r.setNullRet() ))
                .or("Switch",(A)(c,r)->r.setType("Switch").set(ATT.RETURN, S(c,"Switch")))
                .or("Return",(A)(c,r)->r.setType("Return").set(ATT.RETURN, new FUN_TYPES( Arrays.asList( S(c,"Return").getVarType() ))))
                .or("FunctionDec", (A)(c,r)->r.setType("FunctionDec").setNullRet());

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
                "id",
                (A)(c,r)->ID(c).setVarType(S(c,"Type").getVarType()),
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

        P("Init", "assign", "Exp",
                (A)(c,r)-> S(c,"Exp").Do( exp ->
                        r.setIsVarType(true)
                                .setVarType(exp.getVarType())
                                .setType(exp.getType())
                ))
                .or(Symbols.LAMBDA, (A)(c,r)-> r
                        .setOK()
                        .setVarType(VAR_TYPES.VOID) );

        P("AdditionalDeclaration", "comma", "Declaration", "AdditionalDeclaration", (A)(c,r)->r.setType("Declaration","AdditionalDeclaration1"))
                .or("Delimiter", (A)(c,r)->r.setOK());

        P("Switch", "switch",
                "openbracket",
                "Exp",
                "closebracket",
                "openbrace",
                "Case",
                "Cases",
                "closebrace");

        P("Cases", "Case", "Cases", (A)(c,r)->S(c,"Case").Do(cas->S(c,"Cases").Do(cass->{
                    r.set(ATT.RETURN, retMerg( cas.get(ATT.RETURN,FUN_TYPES.class), cass.get(ATT.RETURN, FUN_TYPES.class)));
                    VAR_TYPES tps = cass.getFunType().returnArgsTypeIfAllEquals_elseINVALID();
                    if ( tps.equals(VAR_TYPES.INVALID) )
                        r.setERR().setFunType(cass.getFunType());
                    else if ( ! tps.equals(VAR_TYPES.VOID) && ! cas.getVarType().equals(cass.getVarType()) )
                        r.setErr("In case statement, all case have to have condition of the same type. "+tps+" and "+cas.getVarType()+" were met")
                        .setFunType(new FUN_TYPES(Arrays.asList(VAR_TYPES.INVALID)));
                    else r.setType(cas, cass).setFunType(cass.getFunType().withMoreArgs(cas.getVarType()));
                })))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().setVarType(VAR_TYPES.VOID));

        P("Case", "case", "Value", "colon", "Sequence", "Break",
                (A)(c,r)->S(c,"Value").Do(val->S(c,"Sequence").Do(seq->
                    r.setType(val,seq).setVarType(val.getVarType()).set(ATT.RETURN, seq.get(ATT.RETURN, FUN_TYPES.class))
                )));

        P("Break", "break", "Delimiter")
                .or(Symbols.LAMBDA);


        //Arguments -> Paramlist | lambda
        P("Arguments", "Exp", "Paramlist", (A)(c,r)->S(c,"Exp").Do(exp->S(c,"Paramlist").Do(lst ->
                    r.setType(exp,lst).setFunType(lst.getFunType().withMoreArgs(exp.getVarType()))
                )))
                .or(Symbols.Terminal.LAMBDA,(A)(c,r)->r.setOK().setFunType(new FUN_TYPES()));

        P("Paramlist", "comma", "Arguments", (A)(c,r)->r.setType("Arguments").setFunType(S(c,"Arguments").getFunType() ))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().setFunType(new FUN_TYPES()));


        P("FunctionDec",(A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.FUN),
                "function",
                "NullableType",
                "id",
                (A)(c,r)->{
                    DEC(GlobalTableOfSymbols.EDITING.VAR);
                    PUSH_SCOOP(ID(c).getLexema());
                },
                "openbracket",
                "ArgsDeclaration",
                (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.FORBITTEN),
                (A)(c,r)-> S(c,"ArgsDeclaration").Do( args -> ID(c).ifValid(
                        id->id
                                .setIsVarType(false)
                                .setFunType( args.getFunType().withReturn(S(c,"NullableType").getVarType()))
                                .setType(args)
                        ,reason->r.setErr("Invalid function id: "+reason)
                )),
                "closebracket", "openbrace",
                "Sequence",
                (A)(c,r)->S(c,"Sequence").Do(seq->{
                    VAR_TYPES ret = S(c,"NullableType").getVarType();
                    VAR_TYPES retVals =
                            Optional.ofNullable(seq.get(ATT.RETURN, FUN_TYPES.class))
                                    .orElse(new FUN_TYPES())
                                    .returnArgsTypeIfAllEquals_elseINVALID();
                    if ( retVals.equals(VAR_TYPES.INVALID) )
                        r.setErr("The returned valued are not consistent, they all have to be of type "+ret+
                                ", but "+seq.get(ATT.RETURN, FUN_TYPES.class).toString()+" were met" );
                    else if ( retVals.equals(ret) == false )
                        r.setErr("Function body must return "+ret+", but "+retVals+" is returned");
                    else r.andType(seq.getType());
                    POP_SCOOP();
                }),
                "closebrace"
        );

        P("NullableType", "Type", (A)(c,r)->r.setVarType(S(c,"Type").getVarType()).setOK())
                .or(Symbols.LAMBDA, (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK());

        P("Return", "return", "NullableExp", "Delimiter", (A)(c,r)->S(c,"NullableExp").Do(nulexp->{
                if ( PL_IMPL_Main.gts.currentScopeIsGlobal() )
                    r.setErr("Can't use return statement here, it has to be used inside a function declaration")
                        .setVarType(VAR_TYPES.INVALID);
                else r
                        .setVarType(nulexp.getVarType())
                        .setType(nulexp);
            }));

        P("NullableExp", "Exp", (A)(c,r)->S(c,"Exp").Do(exp->
                    r.setVarType(exp.getVarType()).setType(exp.getType())))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK());

        Function<String,A> rightParrAssigm = paramListName -> (c,r)-> ID(c).Do( id-> {
            S type = S(c,"Type");
            r.setType(paramListName);
            ((ID)id).ifValid(
                    (_id)-> _id.setVarType(type.getVarType()),
                    (re)->r.setErr("Can't user "+((ID)id).getLexema()+" as arg name. probably already in use") );
            r.setFunType( S(c,paramListName).getFunType().withMoreArgs(type.getVarType()));
        });
        P("ArgsDeclaration", "Type", "id","ParamDecList", rightParrAssigm.apply("ParamDecList"))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().setFunType(new FUN_TYPES()));
        //ParamDecList -> comma Type id ParamDecList | lambda
        P("ParamDecList", "comma", "Type", "id", "ParamDecList",rightParrAssigm.apply("ParamDecList1"))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().setFunType(new FUN_TYPES()));


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

    Set<Symbols.Terminal> getTerminals(){
        Set<Symbols.Terminal> t = new HashSet<>();
        for (Object s : map.values().stream().filter(x -> x instanceof Symbols.Terminal).toArray()){
            t.add((Symbols.Terminal) s);
        }
       return t;
    }

    Set<Symbols.NoTerminal> getNoTerminals() {
        Set<Symbols.NoTerminal> t = new HashSet<>();
        for (Object s : map.values().stream().filter(x -> x instanceof Symbols.NoTerminal).toArray()){
            t.add((Symbols.NoTerminal) s);
        }
        return t;
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

    private static class TypeConverter{
        static VAR_TYPES TOStoFUN(GlobalTableOfSymbols.varType v){
            switch (v){
                case CAD:
                    return VAR_TYPES.STRING;
                case INT:
                    return VAR_TYPES.INT;
                case BOOL:
                    return VAR_TYPES.BOOL;
                case VOID:
                    return VAR_TYPES.VOID;
                default:
                    return null;
            }
        }

        static GlobalTableOfSymbols.varType FUNtoTOS(VAR_TYPES v){
            switch (v){
                case STRING:
                    return GlobalTableOfSymbols.varType.CAD;
                case INT:
                    return GlobalTableOfSymbols.varType.INT;
                case BOOL:
                    return GlobalTableOfSymbols.varType.BOOL;
                case VOID:
                    return GlobalTableOfSymbols.varType.VOID;
                default:
                    return null;
            }
        }
    }
}
