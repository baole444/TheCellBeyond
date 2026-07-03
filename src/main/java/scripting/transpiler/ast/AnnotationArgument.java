package scripting.transpiler.ast;

/**
 * A single named annotation argument {@code name = value}.
 */
public final class AnnotationArgument extends AstNode {
    public final String name;
    public final Expression value;

    public AnnotationArgument(SourcePosition position, String name, Expression value) {
        super(position);
        this.name = name;
        this.value = value;
    }
}
