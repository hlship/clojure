package clojure.lang;

import clojure.asm.commons.GeneratorAdapter;

/**
 * An extension of {@link clojure.lang.Expr}, for expressions that may be able to evaluate to a primitive
 * (unboxed, non-wrapper) value such as int (rather than Integer).
 */
interface MaybePrimitiveExpr extends Expr {
    boolean canEmitPrimitive();

    void emitUnboxed(EvaluationContext context, ObjExpr objx, GeneratorAdapter gen);
}
