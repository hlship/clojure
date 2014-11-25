package clojure.lang;

import clojure.asm.commons.GeneratorAdapter;

/**
 * Common interface for a large number of different possible expressions as read by the reader, representing
 * the basic special forms and common data types (vector, map, set, etc.).
 */
interface Expr {

    /**
     * May be invoked on a totally constant expression to provide its constant value.
     */
    Object eval();

    /**
     * Generates code for evaluating the expression.
     *
     * @param context - how the result of the expression is to be used
     * @param objx    - ???
     * @param gen     - adapter that is used to generate part of a method's implementation
     */
    void emit(EvaluationContext context, ObjExpr objx, GeneratorAdapter gen);

    /**
     * Returns true if the expression has a known type.
     */
    boolean hasJavaClass();

    /**
     * Returns the type of the expression, if known ({@link #hasJavaClass()} returns true).
     * Throws an exception if invoked when no type is known.
     */
    Class getJavaClass();
}
