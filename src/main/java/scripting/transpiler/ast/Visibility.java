package scripting.transpiler.ast;

/**
 * Declared visibility of a field or method. {@link #Public} is the default when no keyword is given.
 */
public enum Visibility {
    Public,
    Private,
    Protected,
}
