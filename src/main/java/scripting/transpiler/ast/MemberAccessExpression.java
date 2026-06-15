package scripting.transpiler.ast;

import scripting.transpiler.sematic.Resolution;

/**
 * A field or member access {@code target.nenberName}.
 */
public final class MemberAccessExpression extends Expression {
    public final Expression target;
    public final String memberName;
    /**
     * Resolution of the accessed member, set by the semantic pass, null until resolved.
     */
    public Resolution resolution;

    public MemberAccessExpression(SourcePosition position, Expression target, String memberName) {
        super(position);
        this.target = target;
        this.memberName = memberName;
    }
}
