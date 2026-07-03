package scripting.transpiler.ast;

/**
 * A local {@code var} or {@code const} declaration inside a method body.
 */
public final class LocalVariableDeclaration extends Statement {
    public final boolean isConst;
    public final String name;
    /**
     * Declared type, or null when the type clause is omitted. The semantic pass infers the type from initializer,
     * otherwise it stay null and code generation fallback to {@code var}.
     */
    public TypeReference type;
    /**
     * Initializer expression, or null when a {@code var} omits {@code = expr}.
     */
    public final Expression initializer;

    public LocalVariableDeclaration(SourcePosition position, boolean isConst, String name, TypeReference type, Expression initializer) {
        super(position);
        this.isConst = isConst;
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }
}
