package scripting.transpiler.ast;

/**
 * A literal value, with {@link #text} as the raw lexeme written in source, including quotes for strings.
 * Conversion to a typed value is handled by later passes.
 */
public final class LiteralExpression extends Expression {
    public enum Kind {
        Integer,
        Float,
        String,
        Boolean,
        Null
    }

    public final Kind kind;
    public final String text;

    public LiteralExpression(SourcePosition position, Kind kind, String text) {
        super(position);
        this.kind = kind;
        this.text = text;
    }
}
