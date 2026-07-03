package scripting.transpiler.ast;

/**
 * A cast expression {@code value as Type}.
 */
public final class CastExpression extends Expression {
    public final Expression value;
    public final TypeReference type;

    public CastExpression(SourcePosition position, Expression value, TypeReference type) {
        super(position);
        this.value = value;
        this.type = type;
    }
}
