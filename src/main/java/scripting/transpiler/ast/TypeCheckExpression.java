package scripting.transpiler.ast;

/**
 * A type check expression {@code value is Type}.
 */
public final class TypeCheckExpression extends Expression {
    public final Expression value;
    public final TypeReference type;

    public TypeCheckExpression(SourcePosition position, Expression value, TypeReference type) {
        super(position);
        this.value = value;
        this.type = type;
    }
}
