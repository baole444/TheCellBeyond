package scripting.transpiler.ast;

/**
 * Root node of a parsed {@code .tcbs} file, carrying its top level type.
 */
public final class ScriptFile extends AstNode {
    public final TypeDeclaration typeDeclaration;

    public ScriptFile(SourcePosition position, TypeDeclaration typeDeclaration) {
        super(position);
        this.typeDeclaration = typeDeclaration;
    }
}
