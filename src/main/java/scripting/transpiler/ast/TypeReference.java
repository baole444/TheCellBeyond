package scripting.transpiler.ast;

import scripting.transpiler.semantic.Resolution;

/**
 * A reference to a type by simple name. Resolution to fully qualified name happens in semantic pass.
 */
public final class TypeReference extends AstNode {
    public final String name;
    /**
     * {@code int} = 0, {@code int[]} = 1, {@code int[][]} = 2.
     */
    public final int arrayDepth;
    /**
     * Resolution of this type, set by the semantic pass, null if is a primitive type or until resolved.
     */
    public Resolution resolution;

    public TypeReference(SourcePosition position, String name, int arrayDepth) {
        super(position);
        this.name = name;
        this.arrayDepth = arrayDepth;
    }
}
