package scripting.transpiler.ast;

/**
 * Base for the top level type declaration in a {@code .tcbs} file, wither a {@link ClassDeclaration} or an {@link EnumDeclaration}.
 */
public abstract sealed class TypeDeclaration extends AstNode permits ClassDeclaration, EnumDeclaration {
    public final String name;

    protected TypeDeclaration(SourcePosition position, String name) {
        super(position);
        this.name = name;
    }
}
