package scripting.transpiler.ast;

/**
 * A {@code return [expr]} statement.
 */
public final class ReturnStatement extends Statement {
    /**
     * Returned expression, or null for a bare {@code return}.
     */
    public final Expression value;

    public ReturnStatement(SourcePosition position, Expression value) {
        super(position);
        this.value = value;
    }
}
