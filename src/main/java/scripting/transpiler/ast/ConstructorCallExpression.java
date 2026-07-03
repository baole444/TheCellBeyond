package scripting.transpiler.ast;

import scripting.transpiler.semantic.Resolution;

import java.util.List;

/**
 * A constructor call {@code [new] TypeName(args)}, emitting Java {@code new TypeName(args)}.
 * <p>
 * The bare {@code TypeName(args)} parses as a {@link MethodCallExpression}, recognized by the semantic pass as a constructor.
 */
public final class ConstructorCallExpression extends Expression {
    public final TypeReference type;
    public final List<Expression> arguments;
    /**
     * Resolution of the constructed type, set by the semantic pass, null until resolved.
     */
    public Resolution resolution;

    public ConstructorCallExpression(SourcePosition position, TypeReference type, List<Expression> arguments) {
        super(position);
        this.type = type;
        this.arguments = arguments;
    }
}
