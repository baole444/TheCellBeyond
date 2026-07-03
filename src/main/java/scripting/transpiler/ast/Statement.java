package scripting.transpiler.ast;

/**
 * Statement is the base class for all statement nodes.
 */
public abstract class Statement extends AstNode {
    protected Statement(SourcePosition position) {
        super(position);
    }
}
