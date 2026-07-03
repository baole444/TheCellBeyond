package scripting.transpiler.ast;

import java.util.List;

/**
 * A {@code signal name(params)} class member, shortcut for {@code public final Signal} instance field.
 * The signal's constructor carries the boxed contract types derived from the parameter types.
 */
public final class SignalDeclaration extends AstNode {
    public final String name;
    public final List<ParameterDeclaration> parameters;

    public SignalDeclaration(SourcePosition position, String name, List<ParameterDeclaration> parameters) {
        super(position);
        this.name = name;
        this.parameters = parameters;
    }
}
