package scripting.transpiler.ast;

/**
 * A field or member access {@code target.nenberName}.
 */
public final class MemberAccessExpression extends Expression {
    public final Expression target;
    public final String memberName;

    public MemberAccessExpression(SourcePosition position, Expression target, String memberName) {
        super(position);
        this.target = target;
        this.memberName = memberName;
    }
}
