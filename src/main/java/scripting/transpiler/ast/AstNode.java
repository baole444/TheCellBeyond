package scripting.transpiler.ast;

/**
 * AstNode is the base class for all TCBScript AST node.
 * <p>
 * It carries the source position from the start, allow mapping generated Java errors back to script source.
 */
public abstract class AstNode {
    public final SourcePosition position;

    protected AstNode(SourcePosition position) {
        this.position = position;
    }
}
