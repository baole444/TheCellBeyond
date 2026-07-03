package scripting.transpiler.ast;

import java.util.List;

/**
 * An annotation applied to a field, such as {@code @export}, or {@code @export(label = "Name"}.
 * Validation of the name happens in the semantic pass.
 */
public final class Annotation extends AstNode {
    public final String name;
    public List<AnnotationArgument> arguments;

    public Annotation(SourcePosition position, String name, List<AnnotationArgument> arguments) {
        super(position);
        this.name = name;
        this.arguments = arguments;
    }
}
