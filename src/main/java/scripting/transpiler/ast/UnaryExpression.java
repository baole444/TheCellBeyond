package scripting.transpiler.ast;

/**
 * A prefix unary expression, with both the {@code not} keyword and {@code !} mapped to {@link Operator#Not}.
 */
public final class UnaryExpression extends Expression {
    public enum Operator {
        Negate,
        Not
    }

    public final Operator operator;
    public final Expression operand;

    public UnaryExpression(SourcePosition position, Operator operator, Expression operand) {
        super(position);
        this.operator = operator;
        this.operand = operand;
    }
}
