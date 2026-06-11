package scripting.transpiler.ast;

/**
 * A {@code while cond:} loop.
 */
public final class WhileStatement extends Statement {
    public final Expression condition;
    public final Block body;

    public WhileStatement(SourcePosition position, Expression condition, Block body) {
        super(position);
        this.condition = condition;
        this.body = body;
    }
}
