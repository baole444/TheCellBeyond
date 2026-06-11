package scripting.transpiler.ast;

/**
 * A {@code pass} no operation statement, used to fill empty block.
 */
public final class PassStatement extends Statement {
    public PassStatement(SourcePosition position) {
        super(position);
    }
}
