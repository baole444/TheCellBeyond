package scripting.transpiler.ast;

/**
 * A {@code for variable in iterable:} loop.
 */
public final class ForStatement extends Statement {
    public final String variable;
    public final Expression iterable;
    public final Block body;

    public ForStatement(SourcePosition position, String variable, Expression iterable, Block body) {
        super(position);
        this.variable = variable;
        this.iterable = iterable;
        this.body = body;
    }
}
