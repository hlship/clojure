package clojure.lang;


/** Base class for a number of expressions that are of a literal type (boolean, number, keyword, etc.). */
abstract class LiteralExpr implements Expr {
    abstract Object val();

    public Object eval() {
        return val();
    }
}
