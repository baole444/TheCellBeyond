package scripting.transpiler.ast;

/**
 * A conditional, ternary expression {@code condition ? thenValue : elseValue}.
 */
public final class ConditionalExpression extends Expression {
    public final Expression condition;
    public final Expression thenValue;
    public final Expression elseValue;

    public ConditionalExpression(SourcePosition position, Expression condition, Expression thenValue, Expression elseValue) {
        super(position);
        this.condition = condition;
        this.thenValue = thenValue;
        this.elseValue = elseValue;
    }
}
