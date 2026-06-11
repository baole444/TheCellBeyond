package scripting.transpiler.ast;

import java.util.List;

/**
 * A method call {@code foo()} or {@code obj.foo()}.
 */
public final class MethodCallExpression extends Expression {
    /**
     * The targeting expression, null for unqualified calls, such as implicit {@code this} or inherited call.
     */
    public final Expression target;
    public final String methodName;
    public final List<Expression> arguments;

    public MethodCallExpression(SourcePosition position, Expression target, String methodName, List<Expression> arguments) {
        super(position);
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments;
    }
}
