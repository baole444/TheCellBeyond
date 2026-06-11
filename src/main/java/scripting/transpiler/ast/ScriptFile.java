package scripting.transpiler.ast;

/**
 * Root node of a parsed {@code .tcbs} file.
 */
public final class ScriptFile extends AstNode {
    public final ClassDeclaration classDeclaration;

    public ScriptFile(SourcePosition position, ClassDeclaration classDeclaration) {
        super(position);
        this.classDeclaration = classDeclaration;
    }
}
