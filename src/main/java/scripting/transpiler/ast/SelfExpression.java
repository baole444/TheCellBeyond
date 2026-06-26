package scripting.transpiler.ast;

/**
 * A reference to the current instance {@code self}, equivalent of Java {@code this}.
 */
public final class SelfExpression extends Expression {
    public SelfExpression(SourcePosition position) {
        super(position);
    }
}
