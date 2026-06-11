package scripting.transpiler.ast;

/**
 * Expression is the base class for all expression nodes.
 */
public abstract class Expression extends AstNode {
    protected Expression(SourcePosition position) {
        super(position);
    }
}
