package scripting.transpiler.ast;

import scripting.transpiler.semantic.ClassRegistration;

import java.util.List;

/**
 * A {@code class Name [extends Parent]} declaration with its fields and methods.
 */
public final class ClassDeclaration extends TypeDeclaration {
    /**
     * Declared super type, or null when {@code extends} is omitted, implicitly extends {@code Object}.
     */
    public final TypeReference superType;
    public final List<FieldDeclaration> fields;
    public final List<MethodDeclaration> methods;
    /**
     * How this class registered with the engine, set by the semantic pass, null until resolved.
     */
    public ClassRegistration registration;

    public ClassDeclaration(SourcePosition position, String name, TypeReference superType, List<FieldDeclaration> fields, List<MethodDeclaration> methods) {
        super(position, name);
        this.superType = superType;
        this.fields = fields;
        this.methods = methods;
    }
}
