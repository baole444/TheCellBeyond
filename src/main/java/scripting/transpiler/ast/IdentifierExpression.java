package scripting.transpiler.ast;

/**
 * A bare identifier reference, a local, field or parameter name.
 * Resolution happens in the semantic pass.
 */
public final class IdentifierExpression extends Expression {
    public final String name;

    public IdentifierExpression(SourcePosition position, String name) {
        super(position);
        this.name = name;
    }
}
