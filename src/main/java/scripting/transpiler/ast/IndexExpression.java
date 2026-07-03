package scripting.transpiler.ast;

/**
 * An indexed access {@code target[index]}.
 */
public final class IndexExpression extends Expression {
    public final Expression target;
    public final Expression index;

    public IndexExpression(SourcePosition position, Expression target, Expression index) {
        super(position);
        this.target = target;
        this.index = index;
    }
}
