package scripting.transpiler.ast;

/**
 * An assignment (@code target op value}.
 * The target's validity as an lvalue is checked in the semantic pass, not the grammar.
 */
public final class AssignmentStatement extends Statement {
    public enum Operator {
        Assign,
        AddAssign,
        SubtractAssign,
        MultiplyAssign,
        DivideAssign,
        ModuloAssign
    }

    public final Expression target;
    public final Operator operator;
    public final Expression value;

    public AssignmentStatement(SourcePosition position, Expression target, Operator operator, Expression value) {
        super(position);
        this.target = target;
        this.operator = operator;
        this.value = value;
    }
}
