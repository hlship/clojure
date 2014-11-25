package clojure.lang;

import clojure.asm.commons.GeneratorAdapter;

public class MethodParamExpr implements Expr, MaybePrimitiveExpr {
	final Class c;

	public MethodParamExpr(Class c){
		this.c = c;
	}

	public Object eval() {
		throw Util.runtimeException("Can't eval");
	}

	public void emit(EvaluationContext context, ObjExpr objx, GeneratorAdapter gen){
		throw Util.runtimeException("Can't emit");
	}

	public boolean hasJavaClass() {
		return c != null;
	}

	public Class getJavaClass() {
		return c;
	}

	public boolean canEmitPrimitive(){
		return Util.isPrimitive(c);
	}

	public void emitUnboxed(EvaluationContext context, ObjExpr objx, GeneratorAdapter gen){
		throw Util.runtimeException("Can't emit");
	}
}
