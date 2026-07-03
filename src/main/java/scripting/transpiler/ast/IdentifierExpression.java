package scripting.transpiler.ast;

import scripting.transpiler.semantic.Resolution;

/**
 * A bare identifier reference, a local, field or parameter name.
 * Resolution happens in the semantic pass.
 */
public final class IdentifierExpression extends Expression {
    public final String name;
    /**
     * Resolution of this identifier, set by the semantic pass, null until resolved.
     * In case of a local variable or parameter, it is emitted as is.
     */
    public Resolution resolution;

    public IdentifierExpression(SourcePosition position, String name) {
        super(position);
        this.name = name;
    }
}
