package clojure.lang;

/**
 * Base class for expressions that have no type: monitor entry and exit, and throws expressions.
 */
abstract class UntypedExpr implements Expr {

    public Class getJavaClass() {
        throw new IllegalArgumentException("Has no Java class");
    }

    public boolean hasJavaClass() {
        return false;
    }
}
