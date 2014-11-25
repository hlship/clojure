package clojure.lang;

import clojure.asm.commons.GeneratorAdapter;

/**
 * Defines an interface for assignable types, which include instance and static fields (for Java classes and instances), but
 * also {@link clojure.lang.Var}'s: both root values and thread-local bindings.
 */
interface AssignableExpr {
    Object evalAssign(Expr val);

    void emitAssign(EvaluationContext context, ObjExpr objx, GeneratorAdapter gen, Expr val);
}
