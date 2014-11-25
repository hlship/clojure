package clojure.lang;

import clojure.asm.ClassVisitor;
import clojure.asm.Opcodes;
import clojure.asm.Type;
import clojure.asm.commons.GeneratorAdapter;
import clojure.asm.commons.Method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;

// Formatting is still quite funky from when this was an inner class of Compiler

/**
 * A form that defines a function (possibly varadic).
 */
class FnExpr extends ObjExpr{
    static final int MAX_POSITIONAL_ARITY = 20;
    //if there is a variadic overload (there can only be one) it is stored here
	Compiler.FnMethod variadicMethod = null;
	IPersistentCollection methods;
	private boolean hasMeta;
	//	String superName = null;

	FnExpr(Symbol tag){
		super(tag);
	}

	boolean supportsMeta(){
		return hasMeta;
	}

	public Class getJavaClass() {
		return tag != null ? Compiler.HostExpr.tagToClass(tag) : AFunction.class;
	}

	protected void emitMethods(ClassVisitor cv){
		//override of invoke/doInvoke for each method
		for(ISeq s = RT.seq(methods); s != null; s = s.next())
			{
			Compiler.ObjMethod method = (Compiler.ObjMethod) s.first();
			method.emit(this, cv);
			}

		if(isVariadic())
			{
			GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
			                                            Method.getMethod("int getRequiredArity()"),
			                                            null,
			                                            null,
			                                            cv);
			gen.visitCode();
			gen.push(variadicMethod.reqParms.count());
			gen.returnValue();
			gen.endMethod();
			}
	}

	static Expr parse(EvaluationContext context, ISeq form, String name) {
		ISeq origForm = form;
		FnExpr fn = new FnExpr(Compiler.tagOf(form));
		fn.src = form;
		Compiler.ObjMethod enclosingMethod = (Compiler.ObjMethod) Compiler.METHOD.deref();
		if(((IMeta) form.first()).meta() != null)
			{
			fn.onceOnly = RT.booleanCast(RT.get(RT.meta(form.first()), Keyword.intern(null, "once")));
			}

		String basename = (enclosingMethod != null ?
		                  enclosingMethod.objx.name
		                  : (Compiler.munge(Compiler.currentNS().name.name))) + "$";

		Symbol nm = null;

		if(RT.second(form) instanceof Symbol) {
			nm = (Symbol) RT.second(form);
			name = nm.name + "__" + RT.nextID();
		} else {
			if(name == null)
				name = "fn__" + RT.nextID();
			else if (enclosingMethod != null)
				name += "__" + RT.nextID();
		}

		String simpleName = Compiler.munge(name).replace(".", "_DOT_");

		fn.name = basename + simpleName;
		fn.internalName = fn.name.replace('.', '/');
		fn.objtype = Type.getObjectType(fn.internalName);
		ArrayList<String> prims = new ArrayList();
		try
			{
			Var.pushThreadBindings(
					RT.mapUniqueKeys(Compiler.CONSTANTS, PersistentVector.EMPTY,
					       Compiler.CONSTANT_IDS, new IdentityHashMap(),
					       Compiler.KEYWORDS, PersistentHashMap.EMPTY,
					       Compiler.VARS, PersistentHashMap.EMPTY,
					       Compiler.KEYWORD_CALLSITES, PersistentVector.EMPTY,
					       Compiler.PROTOCOL_CALLSITES, PersistentVector.EMPTY,
					       Compiler.VAR_CALLSITES, Compiler.emptyVarCallSites(),
                                               Compiler.NO_RECUR, null
					));

			//arglist might be preceded by symbol naming this fn
			if(nm != null)
				{
				fn.thisName = nm.name;
				fn.isStatic = false;
				form = RT.cons(Compiler.FN, RT.next(RT.next(form)));
				}

			//now (fn [args] body...) or (fn ([args] body...) ([args2] body2...) ...)
			//turn former into latter
			if(RT.second(form) instanceof IPersistentVector)
				form = RT.list(Compiler.FN, RT.next(form));
			fn.line = Compiler.lineDeref();
			fn.column = Compiler.columnDeref();
			Compiler.FnMethod[] methodArray = new Compiler.FnMethod[MAX_POSITIONAL_ARITY + 1];
			Compiler.FnMethod variadicMethod = null;
			for(ISeq s = RT.next(form); s != null; s = RT.next(s))
				{
				Compiler.FnMethod f = Compiler.FnMethod.parse(fn, (ISeq) RT.first(s), fn.isStatic);
				if(f.isVariadic())
					{
					if(variadicMethod == null)
						variadicMethod = f;
					else
						throw Util.runtimeException("Can't have more than 1 variadic overload");
					}
				else if(methodArray[f.reqParms.count()] == null)
					methodArray[f.reqParms.count()] = f;
				else
					throw Util.runtimeException("Can't have 2 overloads with same arity");
				if(f.prim != null)
					prims.add(f.prim);
				}
			if(variadicMethod != null)
				{
				for(int i = variadicMethod.reqParms.count() + 1; i <= MAX_POSITIONAL_ARITY; i++)
					if(methodArray[i] != null)
						throw Util.runtimeException(
								"Can't have fixed arity function with more params than variadic function");
				}

			if(fn.isStatic && fn.closes.count() > 0)
				throw new IllegalArgumentException("static fns can't be closures");
			IPersistentCollection methods = null;
			for(int i = 0; i < methodArray.length; i++)
				if(methodArray[i] != null)
					methods = RT.conj(methods, methodArray[i]);
			if(variadicMethod != null)
				methods = RT.conj(methods, variadicMethod);

			fn.methods = methods;
			fn.variadicMethod = variadicMethod;
			fn.keywords = (IPersistentMap) Compiler.KEYWORDS.deref();
			fn.vars = (IPersistentMap) Compiler.VARS.deref();
			fn.constants = (PersistentVector) Compiler.CONSTANTS.deref();
			fn.keywordCallsites = (IPersistentVector) Compiler.KEYWORD_CALLSITES.deref();
			fn.protocolCallsites = (IPersistentVector) Compiler.PROTOCOL_CALLSITES.deref();
			fn.varCallsites = (IPersistentSet) Compiler.VAR_CALLSITES.deref();

			fn.constantsID = RT.nextID();
			}
		finally
			{
			Var.popThreadBindings();
			}
		IPersistentMap fmeta = RT.meta(origForm);
		if(fmeta != null)
			fmeta = fmeta.without(RT.LINE_KEY).without(RT.COLUMN_KEY).without(RT.FILE_KEY);

		fn.hasMeta = RT.count(fmeta) > 0;

		try
			{
			fn.compile(fn.isVariadic() ? "clojure/lang/RestFn" : "clojure/lang/AFunction",
			           (prims.size() == 0)?
			            null
						:prims.toArray(new String[prims.size()])
            );
			}
		catch(IOException e)
			{
			throw Util.sneakyThrow(e);
			}
		fn.getCompiledClass();

		if(fn.supportsMeta())
			{
			return new Compiler.MetaExpr(fn, Compiler.MapExpr
					.parse(context == EvaluationContext.EVAL ? context : EvaluationContext.EXPRESSION, fmeta));
			}
		else
			return fn;
	}

	boolean isVariadic(){
		return variadicMethod != null;
	}

	public final IPersistentCollection methods(){
		return methods;
	}

	public void emitForDefn(ObjExpr objx, GeneratorAdapter gen){
			emit(EvaluationContext.EXPRESSION,objx,gen);
	}
}
