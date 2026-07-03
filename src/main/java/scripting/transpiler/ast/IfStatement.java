package scripting.transpiler.ast;

import java.util.List;

/**
 * An {@code if} statement with optional {@code elif} clauses and an optional {@code else} block.
 */
public final class IfStatement extends Statement {
    public final Expression condition;
    public final Block thenBlock;
    public final List<ElifClause> elifClauses;
    /**
     * The {@code else} block, or null when absent.
     */
    public final Block elseBlock;

    public IfStatement(SourcePosition position, Expression condition, Block thenBlock, List<ElifClause> elifClauses, Block elseBlock) {
        super(position);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elifClauses = elifClauses;
        this.elseBlock = elseBlock;
    }
}
