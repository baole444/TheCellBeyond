package scripting.transpiler.ast;

import scripting.transpiler.semantic.Resolution;

import java.util.List;

/**
 * A {@code func} declaration. A {@code _snake_name} is just a name, lifecycle mapping happens in the semantic pass.
 */
public final class MethodDeclaration extends AstNode {
    public final Visibility visibility;
    public final boolean isStatic;
    public final String name;
    public final List<ParameterDeclaration> parameters;
    /**
     * Declared return type, or null for an absent {@code -> type}, equivalent to {@code void}.
     */
    public final TypeReference returnType;
    public final Block body;
    /**
     * Resolution of life cycle method when overriding an engine hook, set by the semantic pass, null for plain user defined method.
     */
    public Resolution resolution;

    public MethodDeclaration(SourcePosition position, Visibility visibility, boolean isStatic, String name, List<ParameterDeclaration> parameters, TypeReference returnType, Block body) {
        super(position);
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
}
