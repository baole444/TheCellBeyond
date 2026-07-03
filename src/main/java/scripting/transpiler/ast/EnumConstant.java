package scripting.transpiler.ast;

import java.util.List;

/**
 * An Enum constant, a name with its positional constructor arguments, empty for a bare constant.
 */
public final class EnumConstant extends AstNode {
    public final String name;
    public final List<Expression> arguments;

    public EnumConstant(SourcePosition position, String name, List<Expression> arguments) {
        super(position);
        this.name = name;
        this.arguments = arguments;
    }
}
