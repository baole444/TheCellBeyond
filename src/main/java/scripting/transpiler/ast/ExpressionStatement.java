package scripting.transpiler.ast;

/**
 * An expression used as a statement, such as a method call.
 */
public final class ExpressionStatement extends Statement {
    public final Expression expression;

    public ExpressionStatement(SourcePosition position, Expression expression) {
        super(position);
        this.expression = expression;
    }
}
