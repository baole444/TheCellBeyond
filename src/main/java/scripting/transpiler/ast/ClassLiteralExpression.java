package scripting.transpiler.ast;

/**
 * A class literal {@code TypeName.class}, emitting the Java class literal.
 */
public final class ClassLiteralExpression extends Expression {
    public final TypeReference type;

    public ClassLiteralExpression(SourcePosition position, TypeReference type) {
        super(position);
        this.type = type;
    }
}
