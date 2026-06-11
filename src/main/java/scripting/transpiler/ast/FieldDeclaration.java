package scripting.transpiler.ast;

import java.util.List;

/**
 * A field declaration of {@code [annotations] [visibility] [static] var name : type [= expr]},
 * or {@code [annotations] [visibility] const Name : type = expr}.
 */
public final class FieldDeclaration extends AstNode {
    public final List<Annotation> annotations;
    public final Visibility visibility;
    public final boolean isStatic;
    public final boolean isConst;
    public final String name;
    public final TypeReference type;
    /**
     * Initializer expression, or null when a {@code var} omits {@code = expr}.
     */
    public final Expression initializer;

    public FieldDeclaration(SourcePosition position, List<Annotation> annotations, Visibility visibility, boolean isStatic, boolean isConst, String name, TypeReference type, Expression initializer) {
        super(position);
        this.annotations = annotations;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.isConst = isConst;
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }
}
