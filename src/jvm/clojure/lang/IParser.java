package clojure.lang;

/**
 * Parsers are triggered by specific special forms or by other logic. It's all quite recursive.
 */
interface IParser {
    /**
     * Parses the form returning an expression that can be used for evaluation or code generation.
     * @param context - identifies how the expression to be parsed will itself be used
     * @param form - the form to parse into an expression
     * @return
     */
    Expr parse(EvaluationContext context, Object form);
}
