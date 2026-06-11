package scripting.transpiler.ast;

import java.util.List;

/**
 * A {@code class Name [extends Parent]} declaration with its fields and methods.
 */
public final class ClassDeclaration extends AstNode {
    public final String name;
    /**
     * Declared super type, or null when {@code extends} is omitted, implicitly extends {@code Object}.
     */
    public final TypeReference superType;
    public final List<FieldDeclaration> fields;
    public final List<MethodDeclaration> methods;

    public ClassDeclaration(SourcePosition position, String name, TypeReference superType, List<FieldDeclaration> fields, List<MethodDeclaration> methods) {
        super(position);
        this.name = name;
        this.superType = superType;
        this.fields = fields;
        this.methods = methods;
    }
}
