import javafx.util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * Created by matti on 22/11/2016.
 */

public class Grammar{


    public enum ATT{TOKEN, TYPE, VAR_TYPE, FUN_TYPE, IS_VAR_TYPE, IDS_LIST, ENTRY, ID, RETURN }
    public enum VAR_TYPES { INT, STRING, BOOL, VOID, INVALID }
    public enum TYPES {OK,ERR}

    public static Optional<Pair<VAR_TYPES,List<String>>> cheatted = Optional.empty();

    static public class FUN_TYPES{
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
        public FUN_TYPES withMoreArgs(List<VAR_TYPES> args){
            return withMoreArgs( args.stream().toArray(i->new VAR_TYPES[i]) );
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
        public final TokenFactory.TokenFolder.WordToken.IdToken id;
        public ID(Symbols.Action.Context c, String id) {
            super(c, id);
            this.id = c.get(id).get(ATT.TOKEN, TokenFactory.TokenFolder.WordToken.IdToken.class);
            Pair i = GlobalTableOfSymbols.globalTableOfSymbols.queryLexema(getLexema());
        }
        public String getLexema(){
            return id.lexema;
        }

        @Override
        public VAR_TYPES getVarType(){
            String lex = getLexema();
            return cheatted
                    .filter(p->p.getValue().contains(lex))
                    .map(t->cheatted.get().getKey())
                    .orElseGet(()->{
                        GlobalTableOfSymbols.VarType v =
                        Optional.ofNullable(GlobalTableOfSymbols.globalTableOfSymbols.getEntry(lex))
                                .map(t->t.getValue().getVarType()).orElseThrow( () -> new RuntimeException("Unable to get varType for id "+getLexema()+". Temp var has stored "+cheatted) );
                        return TypeConverter.TOStoFUN(v);
                    });
        }
        @Override
        public FUN_TYPES getFunType(){
            GlobalTableOfSymbols.FunctionEntry e = (GlobalTableOfSymbols.FunctionEntry) GlobalTableOfSymbols.globalTableOfSymbols.getEntry(getLexema()).getValue();
            List<VAR_TYPES> vars = new LinkedList<>();
            for (GlobalTableOfSymbols.VarType v : e.paramTypes  ){
                vars.add( TypeConverter.TOStoFUN(v));
            }
            return new FUN_TYPES(vars, TypeConverter.TOStoFUN(e.getVarType()));
        }
        @Override
        public ID setFunType(FUN_TYPES fun){
            if (isVarType()) throw new RuntimeException("Attempt to set function values "+fun+" on a variable "+getLexema());
            GlobalTableOfSymbols.FunctionEntry e = (GlobalTableOfSymbols.FunctionEntry) GlobalTableOfSymbols.globalTableOfSymbols.getEntry(getLexema()).getValue();
            for (VAR_TYPES v : fun.argsTypes  ){
                e.addParamtype( TypeConverter.FUNtoTOS(v));
            }
            e.setEntryVals( TypeConverter.FUNtoTOS(fun.ret));
            return this;
        }
        @Override
        public ID setVarType(VAR_TYPES type){
            if (!isVarType()) throw new RuntimeException("Attempt to set variable values "+type+" on a variable "+getLexema());
            GlobalTableOfSymbols.Entry e = GlobalTableOfSymbols.globalTableOfSymbols.getEntry(getLexema()).getValue();
            e.setEntryVals(TypeConverter.FUNtoTOS(type));
            return this;
        }
        @Override
        public boolean isVarType(){
            GlobalTableOfSymbols.Entry entry = GlobalTableOfSymbols.globalTableOfSymbols.getEntry(this.getLexema()).getValue();
            if ( entry == null ) throw new RuntimeException("Unexpceted missing entry "+getLexema());
            return entry instanceof GlobalTableOfSymbols.FunctionEntry == false;
        }
        public ID ifValid( Consumer<ID> _then, Consumer<String> _else ){
            if ( id.isInvalid() )
                _else.accept((GlobalTableOfSymbols.editing.equals(GlobalTableOfSymbols.EDITING.FORBIDDEN)?"Unkwon":"Already declared ")+id.lexema );
            else _then.accept(this);
            return this;
        }

    }

    static public void PUSH_SCOPE(String name){
        GlobalTableOfSymbols.globalTableOfSymbols.addScope(name);
    }
    static public void POP_SCOPE(){
        GlobalTableOfSymbols.globalTableOfSymbols.dropScope();
    }

    static public void DEC(GlobalTableOfSymbols.EDITING edit){
        GlobalTableOfSymbols.editing = edit;
    }

    Map<String, Symbols> map;
    public Grammar(){
        cheatted = Optional.empty();
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

        P("Program", "Delimiter", "Sequence",
                (A)(c,r)->r.setType("Sequence") );

            P("Sequence", "Statement", "Delimiter", "Sequence",
                    (A)(c,r)->r
                            .setType("Statement", "Sequence1")
                            .set(ATT.RETURN, retMerg(
                                    S(c,"Statement").get(ATT.RETURN,FUN_TYPES.class),
                                    S(c, "Sequence1").get(ATT.RETURN,FUN_TYPES.class))))
                .or(Symbols.LAMBDA,(A)(c,r)->r.setOK().set(ATT.RETURN,null) );

        //Delimiter -> Semicolon | NewLine
        P("Delimiter", "semi", "DelimiterOrLambda")
                .or("newline", "DelimiterOrLambda");
        P("DelimiterOrLambda", "Delimiter")
                .or(Symbols.LAMBDA);

        Function<String, A> verifyAssigmentOrFunctionCall = str -> (A)(c,r)-> ID(c).ifValid(
                (s_id) ->
                        S(c,str).Do(
                                ass->{
                                    r.setNullRet();
                                    if ( ass.isVarType() && ass.getVarType().equals(VAR_TYPES.VOID) )
                                        r.setType(ass).setVarType(s_id.getVarType());
                                    else if ( ass.isVarType() && s_id.isVarType() )
                                        r.setErr( ! s_id.getVarType().equals(ass.getVarType()) && ! ass.getVarType().equals(VAR_TYPES.VOID) )
                                                .setVarType(s_id.getVarType());
                                    else if ( ! ass.isVarType() && ! s_id.isVarType() )
                                        r.setErr( ! s_id.getFunType().equals(ass.getFunType())).setVarType( s_id.getFunType().ret );
                                    else
                                        r.setErr(s_id.isVarType()?s_id.getLexema()+" is a variable, can't be called as a function.":s_id.getLexema()+" is a function, can't be assigned").setVarType(VAR_TYPES.INVALID);

                                })
                ,
                (reason) -> r.setErr("Invalid id: "+reason).setNullRet().setVarType(VAR_TYPES.INVALID)
        );

        // only state is type.
        P("Statement",
                    (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.VAR),
                    "var","Type",
                    (A)(c,r)->cheatted=Optional.of(new Pair<>(S(c,"Type").getVarType(), new LinkedList<String>())),
                    "Declaration",
                    (A)(c,r)->cheatted=Optional.empty(),
                    (A)(c,r)->S(c,"Declaration").Do( dec -> S(c,"Type").Do( type -> {
                                r.setType(dec).setNullRet();
                                List<ID> list = (List<ID>)dec.get(ATT.IDS_LIST, List.class);
                                Collections.reverse(list);
                                for ( ID id : list )
                                    id.setVarType(type.getVarType());
                                VAR_TYPES inits = dec.getFunType().returnArgsTypeIfAllEquals_elseINVALID();
                                if ( inits.equals(VAR_TYPES.VOID) == false && inits.equals(type.getVarType()) == false )
                                    r.setErr("Unable to perform assigment. \n\t\tVariables declared as "+type.getVarType()+", but at least one of the initializations are of different type:"+
                                                 dec.getFunType().toString()
                                                         .replace("FUN:","")
                                                         .replace(type.getVarType().toString(),"")
                                                         .replace("xx","x")
                                                         .replace("x"," ").trim().replace(" ", ", "));
                            })),
                    (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.FORBIDDEN))
                .or("id", "AssOrFunCall", verifyAssigmentOrFunctionCall.apply("AssOrFunCall") )
                .or("Preinc", (A)(c,r)->S(c,"Preinc").Do(pr->
                                r.setVarType( pr.getVarType() )
                                .setType( pr ) ) )
                .or("Switch",(A)(c,r)->r.setType("Switch").set(ATT.RETURN, S(c,"Switch").get(ATT.RETURN,FUN_TYPES.class)))
                .or("Return",(A)(c,r)->r.setType("Return").set(ATT.RETURN, new FUN_TYPES( Arrays.asList( S(c,"Return").getVarType() ))))
                .or("FunctionDec", (A)(c,r)->r.setType("FunctionDec").setNullRet());

        P("Preinc", "preinc", "id", (A)(c,r)->ID(c).ifValid(
                    (id) -> {
                        if ( id.isVarType() && id.getVarType().equals(VAR_TYPES.INT) )
                            r.setOK().setVarType(VAR_TYPES.INT);
                        else r.setErr("Pre inc performable only over int, but " + id.getLexema() + " is of " + id.getVarType());
                    },
                    (reason) -> r.setVarType(VAR_TYPES.INVALID).setErr("Invalid id: "+reason)
                ).Do( ( id  ) -> r.setNullRet() ));

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
                            .setVarType(args.getFunType().ret)
                            .setType(args.getType())
                    ));

        P("Declaration",
                "id",
                (A)(c,r)-> {
                    DEC(GlobalTableOfSymbols.EDITING.FORBIDDEN);
                    ID(c).ifValid( id-> cheatted.ifPresent(p -> p.getValue().add(id.getLexema())), reason->{} );
                },
                "Init",
                (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.VAR),
                "AdditionalDeclaration",
                (A)(c,r)-> S(c,"Init").Do( init -> S(c,"AdditionalDeclaration").Do( addDec -> ID(c).ifValid(
                    (id) -> {
                        List<ID> ids = new LinkedList<>(addDec.get(ATT.IDS_LIST, List.class));
                        ids.add(id);
                        if ( init.getVarType().equals(VAR_TYPES.VOID) )
                            r.setType(init,addDec).set(ATT.IDS_LIST,ids).setFunType(addDec.getFunType());
                        else if ( init.getVarType().equals(VAR_TYPES.INVALID) )
                            r.setERR().set(ATT.IDS_LIST, ids).setFunType(addDec.getFunType());
                        else
                            r.setType(init,addDec).set(ATT.IDS_LIST, ids).setFunType(addDec.getFunType().withMoreArgs(init.getVarType()));
                    },
                    (reason) -> r.setErr("Invalid identifier: "+ reason )
                                .setFunType(addDec.getFunType())
                                .set(ATT.IDS_LIST, addDec.get(ATT.IDS_LIST,List.class))
                    )))
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

        P("AdditionalDeclaration", "comma", "Declaration", "AdditionalDeclaration",
                    (A)(c,r)->S(c,"Declaration").Do(dec-> S(c,"AdditionalDeclaration1").Do(addDec->{
                        List<ID> ids = new LinkedList<>(addDec.get(ATT.IDS_LIST,List.class));
                        ids.addAll( dec.get(ATT.IDS_LIST, List.class) );
                        r.setType("Declaration","AdditionalDeclaration1")
                                .set(ATT.IDS_LIST, ids)
                                .setFunType(
                                        addDec.getFunType()
                                                .withMoreArgs(dec.getFunType().argsTypes)
                                );
                    })))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().set(ATT.IDS_LIST,new LinkedList<ID>()).setFunType(new FUN_TYPES()));

        P("Switch", "switch",
                "openbracket",
                "Exp",
                "closebracket",
                "openbrace",
                "Cases", (A)(c,r)->S(c,"Exp").Do(exp->S(c,"Cases").Do(cases->{
                    VAR_TYPES tps = cases.getFunType().returnArgsTypeIfAllEquals_elseINVALID();
                    r.set(ATT.RETURN, cases.get(ATT.RETURN,FUN_TYPES.class));
                    if ( cases.getFunType().argsTypes.isEmpty() )
                       r.setErr("At least one case statement have to be listed for switch statement");
                    else if ( tps.equals(VAR_TYPES.INVALID) ) r.setERR();
                    else if ( tps.equals(exp.getVarType()) ) r.setType(cases, exp);
                    else r.setErr("Case value "+tps+" incompatible with switch guard "+exp.getVarType() );
                })), "closebrace");

        P("Cases", "Case", "Cases", (A)(c,r)->S(c,"Case").Do(cas->S(c,"Cases1").Do(cass->{
                    r.set(ATT.RETURN, retMerg( cas.get(ATT.RETURN,FUN_TYPES.class), cass.get(ATT.RETURN, FUN_TYPES.class)));
                    VAR_TYPES tps = cass.getFunType().returnArgsTypeIfAllEquals_elseINVALID();
                    if ( tps.equals(VAR_TYPES.INVALID) )
                        r.setERR().setFunType(cass.getFunType());
                    else if ( ! tps.equals(VAR_TYPES.VOID) && ! cas.getVarType().equals(tps) )
                        r.setErr("In case statement, all case have to have condition of the same type. "+tps+" and "+cas.getVarType()+" were met")
                        .setFunType(new FUN_TYPES(Arrays.asList(VAR_TYPES.INVALID)));
                    else r.setType(cas, cass).setFunType(cass.getFunType().withMoreArgs(cas.getVarType()));
                })))
                .or(Symbols.LAMBDA, (A)(c,r)->
                        r.setOK()
                                .setVarType(VAR_TYPES.VOID)
                                .set(ATT.RETURN,new FUN_TYPES())
                                .setFunType(new FUN_TYPES()));

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


        P("FunctionDec", (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.FUN),
                "function",
                "NullableType",
                "id",
                (A)(c,r)-> ID(c).ifValid(
                        i -> {
                            GlobalTableOfSymbols.FunctionEntry fe =
                                    (GlobalTableOfSymbols.FunctionEntry) GlobalTableOfSymbols.globalTableOfSymbols.getEntry(ID(c).getLexema()).getValue();
                            r.set(ATT.ENTRY, fe)
                                    .set(ATT.ID, i).setOK();
                            fe.setScope();
                        },
                        reason -> r
                                .setErr("Can't use id "+ID(c).getLexema()+" as funcName: "+reason)
                                .set(ATT.ENTRY, null)
                                .set(ATT.ID, null)
                ),
                (A)(c,r)->PUSH_SCOPE(ID(c).getLexema()),
                (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.VAR),
                "openbracket",
                "ArgsDeclaration",
                (A)(c,r)->DEC(GlobalTableOfSymbols.EDITING.FORBIDDEN),
                (A)(c,r)-> S(c,"ArgsDeclaration").Do( args -> {
                    r.andType(args.getType()).setIsVarType(false);
                    Optional.ofNullable(r.get(ATT.ENTRY, GlobalTableOfSymbols.FunctionEntry.class))
                            .ifPresent( e -> {
                                FUN_TYPES fun = args.getFunType().withReturn(S(c, "NullableType").getVarType());
                                for (VAR_TYPES v : fun.argsTypes) {
                                    e.addParamtype(TypeConverter.FUNtoTOS(v));
                                }
                                e.setEntryVals(TypeConverter.FUNtoTOS(fun.ret));
                            });
                }),
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
                    POP_SCOPE();
                }),
                "closebrace"
        );

        P("NullableType", "Type", (A)(c,r)->r.setVarType(S(c,"Type").getVarType()).setOK())
                .or(Symbols.LAMBDA, (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK());

        P("Return", "return", "NullableExp", (A)(c,r)->S(c,"NullableExp").Do(nulexp->{
                if ( GlobalTableOfSymbols.globalTableOfSymbols.currentScopeIsGlobal() )
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
                    (_id)-> { _id.setVarType(type.getVarType()); System.out.println("\n\nValid arg "+_id.id+"\n\n"); },
                    (re)->r.setErr("Can't user "+((ID)id)+" as arg name. probably already in use") );
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
        P("Exp", "Andexp", "Orexp", (A)(c,r)->S(c,"Andexp").Do(and->S(c,"Orexp").Do(or->{
            if (or.getVarType().equals(VAR_TYPES.INVALID) || and.getVarType().equals(VAR_TYPES.INVALID))
                r.setERR().setVarType(VAR_TYPES.INVALID);
            else {
                if ( or.getVarType().equals(VAR_TYPES.VOID) )
                    r.setVarType(and.getVarType()).setType(and);
                else if ( and.getVarType().equals(VAR_TYPES.BOOL) )
                    r.setVarType(VAR_TYPES.BOOL).setType(and,or);
                else r.setVarType(VAR_TYPES.INVALID).setErr("Boolean arithmetic can be used with booleans only, but "+and.getVarType()+" was incountred");
            }
        })));

        A firstRoundLambda = (A)(c,r)->r.setVarType(VAR_TYPES.VOID).setOK();
        BiFunction<VAR_TYPES,String,A> firstRoundCheck = (wishedType,unit) -> (c,r) ->S(c,unit).Do( term-> {
            if ( term.getVarType().equals(VAR_TYPES.INVALID) )
                r.setERR().setVarType(VAR_TYPES.INVALID);
            else if ( wishedType != null && term.getVarType().equals(wishedType) == false )
                r.setErr("Operation performable only over "+wishedType+", but the second memeber is of type "+term.getVarType())
                        .setVarType(VAR_TYPES.INVALID);
            else r.setType(term).setVarType(term.getVarType());
        });
        BiFunction<VAR_TYPES,VAR_TYPES, BiFunction<String,String, A>> secondRoundCheck =
                (wishedType, returnType) -> (comp,unit) -> (A)(c, r)->S(c,comp).Do(fact->S(c,unit).Do(term->{
                    if ( term.getVarType().equals(VAR_TYPES.INVALID) || fact.getVarType().equals(VAR_TYPES.INVALID))
                        r.setERR().setVarType(VAR_TYPES.INVALID);
                    else if ( term.getVarType().equals(VAR_TYPES.VOID) )
                        r.setType(fact).setVarType(fact.getVarType());
                    else if ( fact.getVarType().equals(term.getVarType()) == false )
                        r.setVarType(VAR_TYPES.INVALID).setErr("Operation performable only between members of the same types, but got "+fact.getVarType()+" and "+term.getVarType());
                    else if ( wishedType == null || fact.getVarType().equals(wishedType) )
                        r.setType(fact,term).setVarType(returnType==null ? fact.getVarType() : returnType);
                    else
                        r.setVarType(VAR_TYPES.INVALID).setErr("Operation performable only over "+wishedType+", but the first memeber is of type "+term.getVarType());
                }));


        P("Andexp", "Bexp", "Andexp'", secondRoundCheck.apply(VAR_TYPES.BOOL,null).apply("Bexp","Andexp'"));
        P("Andexp'", "and", "Bexp", "Andexp'",
                    secondRoundCheck.apply(VAR_TYPES.BOOL, null).apply("Bexp","Andexp'1"))
                .or(Symbols.LAMBDA, firstRoundLambda);

        P("Bexp", "Relexp", "Compexp", secondRoundCheck.apply(null,VAR_TYPES.BOOL).apply("Relexp","Compexp"));
        P("Relexp", "Nexp", "Relexp'",secondRoundCheck.apply(VAR_TYPES.INT,VAR_TYPES.BOOL).apply("Nexp","Relexp'"));
        P("Orexp", "or", "Exp", firstRoundCheck.apply(VAR_TYPES.BOOL, "Exp"))
                .or(Symbols.LAMBDA,firstRoundLambda);


        P("Nexp", "Term", "Aexp",secondRoundCheck.apply(VAR_TYPES.INT,null).apply("Term","Aexp"));
        //Aexp -> plus Nexp | minus Nexp | lambda
        P("Aexp", Symbols.LAMBDA,firstRoundLambda)
                .or("plus", "Nexp", firstRoundCheck.apply(VAR_TYPES.INT,"Nexp"))
                .or("minus", "Nexp", firstRoundCheck.apply(VAR_TYPES.INT,"Nexp"));

        P("Term", "Factor", "Term'",secondRoundCheck.apply(VAR_TYPES.INT,null).apply("Factor","Term'"));

        P("Term'", Symbols.LAMBDA, firstRoundLambda)
                .or("mult", "Term",firstRoundCheck.apply(VAR_TYPES.INT,"Term"))
                .or("mod", "Term",firstRoundCheck.apply(VAR_TYPES.INT,"Term"))
                .or("div", "Term",firstRoundCheck.apply(VAR_TYPES.INT,"Term"));

        P("Factor", "id", "AssOrFunCallOrLambda", verifyAssigmentOrFunctionCall.apply("AssOrFunCallOrLambda") )
                .or("Preinc",(A)(c,r)->S(c,"Preinc").Do(pre->r.setType(pre).setVarType(pre.getVarType())))
                .or("openbracket", "Exp", "closebracket",(A)(c,r)->S(c,"Exp").Do(exp->
                    r.setType(exp.getType()).setVarType(exp.getVarType())))
                .or("not", "Exp", (A)(c,r)->S(c,"Exp").Do(exp->{
                    if ( exp.getVarType().equals(VAR_TYPES.INVALID) )
                        r.setERR().setVarType(VAR_TYPES.INVALID);
                    else if ( exp.getVarType().equals(VAR_TYPES.BOOL) == false )
                        r.setErr("Can't apply not boolean operator to value of type "+exp.getVarType());
                    else
                        r.setType(exp).setVarType(exp.getVarType());
                }))
                .or("Value", (A)(c,r)->r.setOK().setVarType(S(c,"Value").getVarType()));

        P("AssOrFunCallOrLambda", "AssOrFunCall", (A)(c,r)->S(c,"AssOrFunCall").Do(ass->{
                        r   .setType(ass)
                            .setIsVarType(ass.isVarType());
                        if ( ass.isVarType() ) r.setVarType( ass.getVarType() );
                        else r.setFunType( ass.getFunType() );
                    }))
                .or(Symbols.LAMBDA, (A)(c,r)->r.setOK().setVarType(VAR_TYPES.VOID).setIsVarType(true));

        P("Relexp'", Symbols.LAMBDA, firstRoundLambda )
                .or("gt", "Nexp",firstRoundCheck.apply(VAR_TYPES.INT,"Nexp"))
                .or("lt", "Nexp",firstRoundCheck.apply(VAR_TYPES.INT,"Nexp"))
                .or("egt", "Nexp",firstRoundCheck.apply(VAR_TYPES.INT,"Nexp"))
                .or("elt", "Nexp",firstRoundCheck.apply(VAR_TYPES.INT,"Nexp"));

        P("Compexp", Symbols.LAMBDA, firstRoundLambda)
                .or("eq", "Bexp",firstRoundCheck.apply(null, "Bexp"))
                .or("neq", "Bexp",firstRoundCheck.apply(null, "Bexp"));

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
        static VAR_TYPES TOStoFUN(GlobalTableOfSymbols.VarType v){
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

        static GlobalTableOfSymbols.VarType FUNtoTOS(VAR_TYPES v){
            switch (v){
                case STRING:
                    return GlobalTableOfSymbols.VarType.CAD;
                case INT:
                    return GlobalTableOfSymbols.VarType.INT;
                case BOOL:
                    return GlobalTableOfSymbols.VarType.BOOL;
                case VOID:
                    return GlobalTableOfSymbols.VarType.VOID;
                default:
                    return null;
            }
        }
    }
}
