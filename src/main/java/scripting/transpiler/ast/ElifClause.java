package scripting.transpiler.ast;

/**
 * A single {@code elif cond:} clause of an {@link IfStatement}.
 */
public final class ElifClause extends AstNode {
    public final Expression condition;
    public final Block block;

    public ElifClause(SourcePosition position, Expression condition, Block block) {
        super(position);
        this.condition = condition;
        this.block = block;
    }
}
