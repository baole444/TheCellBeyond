package scripting.transpiler.codegen;

import scripting.transpiler.ast.Visibility;

/**
 * Render declaration modifiers shared by members.
 */
final class Modifiers {
    private Modifiers() {}

    static String visibility(Visibility visibility) {
        return switch (visibility) {
            case Public -> "public";
            case Private -> "private";
            case Protected -> "protected";
        };
    }
}
