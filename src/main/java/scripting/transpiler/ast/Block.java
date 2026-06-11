package scripting.transpiler.ast;

import java.util.List;

/**
 * An ordered list of statements. Inline single statement suites are normalized into a single element block.
 */
public final class Block extends AstNode {
    public final List<Statement> statements;

    public Block(SourcePosition position, List<Statement> statements) {
        super(position);
        this.statements = statements;
    }
}
