package clojure.lang;

/**
 * Identifies how the evaluation of a particular form will be utilized.
 */
public enum EvaluationContext {
    /**
     * The value is ignored (only evaluated for side-effects).
     */
    STATEMENT,

    /**
     * The value is required (most likely, to supply an argument to a function invocation).
     */
    EXPRESSION,

    /**
     * The value is to be returned from the enclosing recur frame.
     */
    RETURN,

    /** Still working out what this exactly means. */
    EVAL
}
