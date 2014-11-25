package clojure.lang;

import clojure.asm.commons.GeneratorAdapter;
import clojure.asm.commons.Method;

class DefExpr implements Expr {
	public final Var var;
	public final Expr init;
	public final Expr meta;
	public final boolean initProvided;
	public final boolean isDynamic;
	public final String source;
	public final int line;
	public final int column;
	final static Method bindRootMethod = Method.getMethod("void bindRoot(Object)");
	final static Method setMetaMethod = Method.getMethod("void setMeta(clojure.lang.IPersistentMap)");
	final static Method setDynamicMethod = Method.getMethod("clojure.lang.Var setDynamic(boolean)");

	public DefExpr(String source, int line, int column, Var var, Expr init, Expr meta, boolean initProvided, boolean isDynamic){
		this.source = source;
		this.line = line;
		this.column = column;
		this.var = var;
		this.init = init;
		this.meta = meta;
		this.isDynamic = isDynamic;
		this.initProvided = initProvided;
	}

    public Object eval() {
		try
			{
			if(initProvided)
				{
//			if(init instanceof FnExpr && ((FnExpr) init).closes.count()==0)
//				var.bindRoot(new FnLoaderThunk((FnExpr) init,var));
//			else
				var.bindRoot(init.eval());
				}
			if(meta != null)
				{
                IPersistentMap metaMap = (IPersistentMap) meta.eval();
				var.setMeta(metaMap);
				}
			return var.setDynamic(isDynamic);
			}
		catch(Throwable e)
			{
			if(!(e instanceof Compiler.CompilerException))
				throw new Compiler.CompilerException(source, line, column, e);
			else
				throw (Compiler.CompilerException) e;
			}
	}

	public void emit(EvaluationContext context, ObjExpr objx, GeneratorAdapter gen){
		objx.emitVar(gen, var);
		if(isDynamic)
			{
			gen.push(true);
			gen.invokeVirtual(Compiler.VAR_TYPE, setDynamicMethod);
			}
		if(meta != null)
			{
                gen.dup();
                meta.emit(EvaluationContext.EXPRESSION, objx, gen);
                gen.checkCast(Compiler.IPERSISTENTMAP_TYPE);
                gen.invokeVirtual(Compiler.VAR_TYPE, setMetaMethod);
			}
		if(initProvided)
			{
			gen.dup();
			if(init instanceof FnExpr)
				{
				((FnExpr)init).emitForDefn(objx, gen);
				}
			else
				init.emit(EvaluationContext.EXPRESSION, objx, gen);
			gen.invokeVirtual(Compiler.VAR_TYPE, bindRootMethod);
			}

		if(context == EvaluationContext.STATEMENT)
			gen.pop();
	}

	public boolean hasJavaClass(){
		return true;
	}

	public Class getJavaClass(){
		return Var.class;
	}

	static class Parser implements IParser{
		public Expr parse(EvaluationContext context, Object form) {
			//(def x) or (def x initexpr) or (def x "docstring" initexpr)
			String docstring = null;
			if(RT.count(form) == 4 && (RT.third(form) instanceof String)) {
				docstring = (String) RT.third(form);
				form = RT.list(RT.first(form), RT.second(form), RT.fourth(form));
			}
			if(RT.count(form) > 3)
				throw Util.runtimeException("Too many arguments to def");
			else if(RT.count(form) < 2)
				throw Util.runtimeException("Too few arguments to def");
			else if(!(RT.second(form) instanceof Symbol))
					throw Util.runtimeException("First argument to def must be a Symbol");
			Symbol sym = (Symbol) RT.second(form);
			Var v = Compiler.lookupVar(sym, true);
			if(v == null)
				throw Util.runtimeException("Can't refer to qualified var that doesn't exist");
			if(!v.ns.equals(Compiler.currentNS()))
				{
				if(sym.ns == null)
					{
					v = Compiler.currentNS().intern(sym);
					Compiler.registerVar(v);
					}
//					throw Util.runtimeException("Name conflict, can't def " + sym + " because namespace: " + currentNS().name +
//					                    " refers to:" + v);
				else
					throw Util.runtimeException("Can't create defs outside of current ns");
				}
			IPersistentMap mm = sym.meta();
			boolean isDynamic = RT.booleanCast(RT.get(mm, Compiler.dynamicKey));
			if(isDynamic)
			   v.setDynamic();
            if(!isDynamic && sym.name.startsWith("*") && sym.name.endsWith("*") && sym.name.length() > 2)
                {
                RT.errPrintWriter().format("Warning: %1$s not declared dynamic and thus is not dynamically rebindable, "
                                          +"but its name suggests otherwise. Please either indicate ^:dynamic %1$s or change the name. (%2$s:%3$d)\n",
                                           sym, Compiler.SOURCE_PATH.get(), Compiler.LINE.get());
                }
			if(RT.booleanCast(RT.get(mm, Compiler.arglistsKey)))
				{
				IPersistentMap vm = v.meta();
				//vm = (IPersistentMap) RT.assoc(vm,staticKey,RT.T);
				//drop quote
				vm = (IPersistentMap) RT.assoc(vm, Compiler.arglistsKey,RT.second(mm.valAt(Compiler.arglistsKey)));
				v.setMeta(vm);
				}
            Object source_path = Compiler.SOURCE_PATH.get();
            source_path = source_path == null ? "NO_SOURCE_FILE" : source_path;
            mm = (IPersistentMap) RT.assoc(mm, RT.LINE_KEY, Compiler.LINE.get()).assoc(RT.COLUMN_KEY, Compiler.COLUMN.get()).assoc(RT.FILE_KEY, source_path);
			if (docstring != null)
			  mm = (IPersistentMap) RT.assoc(mm, RT.DOC_KEY, docstring);
//			mm = mm.without(RT.DOC_KEY)
//					.without(Keyword.intern(null, "arglists"))
//					.without(RT.FILE_KEY)
//					.without(RT.LINE_KEY)
//					.without(RT.COLUMN_KEY)
//					.without(Keyword.intern(null, "ns"))
//					.without(Keyword.intern(null, "name"))
//					.without(Keyword.intern(null, "added"))
//					.without(Keyword.intern(null, "static"));
            mm = (IPersistentMap) Compiler.elideMeta(mm);
			Expr meta = mm.count()==0 ? null: Compiler.analyze(context == EvaluationContext.EVAL ? context : EvaluationContext.EXPRESSION, mm);
			return new DefExpr((String) Compiler.SOURCE.deref(), Compiler.lineDeref(), Compiler.columnDeref(),
			                   v, Compiler.analyze(context == EvaluationContext.EVAL ? context : EvaluationContext.EXPRESSION, RT.third(form), v.sym.name),
			                   meta, RT.count(form) == 3, isDynamic);
		}
	}
}
