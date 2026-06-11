package scripting.transpiler.ast;

/**
 * A single method parameter, {@code name : type [= default]}.
 */
public final class ParameterDeclaration extends AstNode {
    public final String name;
    public final TypeReference type;
    /**
     * Default value expression, or null when none is given.
     */
    public final Expression defaultValue;

    public ParameterDeclaration(SourcePosition position, String name, TypeReference type, Expression defaultValue) {
        super(position);
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }
}
