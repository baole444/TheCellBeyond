package scripting.transpiler.ast;

/**
 * A binary expression. Logical spellings are normalized as follows:
 * <ul>
 *     <li>{@code and}/{@code &&} -> {@link Operator#And}</li>
 *     <li>{@code or}/{@code ||} -> {@link Operator#Or}</li>
 * </ul>
 */
public final class BinaryExpression extends Expression {
    public enum Operator {
        Add,
        Subtract,
        Multiply,
        Divide,
        Modulo,
        Less,
        Greater,
        LessEqual,
        GreaterEqual,
        Equal,
        NotEqual,
        And,
        Or
    }

    public final Operator operator;
    public final Expression left;
    public final Expression right;

    public BinaryExpression(SourcePosition position, Operator operator, Expression left, Expression right) {
        super(position);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }
}
