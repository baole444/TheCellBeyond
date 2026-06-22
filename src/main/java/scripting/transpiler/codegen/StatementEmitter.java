package scripting.transpiler.codegen;

import scripting.transpiler.ast.*;
import scripting.transpiler.semantic.Resolution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walk a method body and emit each statement as java source lines through the writer.
 * <p>
 * Nested blocks open/close with indent and dedent. A {@code pass} emits nothing.
 */
final class StatementEmitter {
    private final EmitContext context;
    private final ExpressionEmitter expressions;
    private final Map<String, TypeReference> scope = new HashMap<>();

    StatementEmitter(EmitContext context, List<ParameterDeclaration> params) {
        this.context = context;
        this.expressions = new ExpressionEmitter(context);
        params.forEach(p -> scope.put(p.name, p.type));
    }

    void emit(Statement statement) {
        switch (statement) {
            case LocalVariableDeclaration local -> local(local);
            case AssignmentStatement assign -> context.writer.line(assignment(assign) + ";");
            case ExpressionStatement expression -> context.writer.line(expressions.emit(expression.expression) + ";");
            case ReturnStatement re -> context.writer.line(returnStatement(re));
            case IfStatement ifStatement -> ifStatement(ifStatement);
            case WhileStatement whileStatement -> whileStatement(whileStatement);
            case ForStatement forStatement -> forStatement(forStatement);
            case BreakStatement ignored -> context.writer.line("break;");
            case ContinueStatement ignored -> context.writer.line("continue;");
            case PassStatement ignored -> {}
            default -> throw new IllegalStateException("Unsupported statement: " + statement.getClass().getSimpleName());
        }
    }

    private void block(Block block) {
        context.writer.indent();
        block.statements.forEach(this::emit);
        context.writer.dedent();
    }

    private void local(LocalVariableDeclaration local) {
        String prefix = local.isConst ? "final " : "";
        String initializer = local.initializer == null ? "" : " = " + expressions.emit(local.initializer, local.type.name);
        context.writer.line(prefix + context.typeName(local.type) + " " + local.name + initializer + ";");
        scope.put(local.name, local.type);
    }

    private String assignment(AssignmentStatement assign) {
        return expressions.emit(assign.target) + " " + assignmentOperator(assign.operator) + " " + expressions.emit(assign.value, targetType(assign.target));
    }

    private String returnStatement(ReturnStatement returnStatement) {
        return returnStatement.value == null ? "return;" : "return " + expressions.emit(returnStatement.value) + ";";
    }

    /**
     * Get the declared type name of an assignment target when it is a bare identifier of a known local, parameter, or field.
     * @param target the assignment target expression
     * @return the target type name, or null when not statically known
     */
    private String targetType(Expression target) {
        if (!(target instanceof IdentifierExpression identifier)) return null;
        TypeReference type = scope.containsKey(identifier.name) ? scope.get(identifier.name) : context.fieldTypes.get(identifier.name);
        return type != null && type.arrayDepth == 0 ? type.name : null;
    }

    private void ifStatement(IfStatement ifStatement) {
        context.writer.line("if (" + expressions.emit(ifStatement.condition) + ") {");
        block(ifStatement.thenBlock);
        for (ElifClause clause : ifStatement.elifClauses) {
            context.writer.line("} else if (" + expressions.emit(clause.condition) + ") {");
            block(clause.block);
        }
        if (ifStatement.elseBlock != null) {
            context.writer.line("} else {");
            block(ifStatement.elseBlock);
        }
        context.writer.line("}");
    }

    private void whileStatement(WhileStatement whileStatement) {
        context.writer.line("while (" + expressions.emit(whileStatement.condition) + ") {");
        block(whileStatement.body);
        context.writer.line("}");
    }

    private void forStatement(ForStatement forStatement) {
        if (forStatement.iterable instanceof MethodCallExpression call && call.resolution instanceof Resolution.BuiltinRangeResolution) {
            context.writer.line(rangeHeader(forStatement.variable, call.arguments));
            block(forStatement.body);
            context.writer.line("}");
            return;
        }
        context.writer.line("for (var " + forStatement.variable + " : " + expressions.emit(forStatement.iterable) + ") {");
        block(forStatement.body);
        context.writer.line("}");
    }

    /**
     * Convert a {@code rnage(...)} iterable to an ascending indexed {@code for} header.
     * <p>
     * The argument count select the bounds:
     * <ul>
     *     <li>range(end)</li>
     *     <li>range(start, end)</li>
     *     <li>range(start, end, step</li>
     * </ul>
     * This currently assume no negative steps.
     * @param variable the loop index
     * @param args the range arguments
     * @return the for loop header
     */
    private String rangeHeader(String variable, List<Expression> args) {
        String start = args.size() >= 2 ? expressions.emit(args.getFirst()) : "0";
        String end = expressions.emit(args.get(args.size() >= 2 ? 1 : 0));
        String step = args.size() == 3 ? expressions.emit(args.get(2)) : "1";
        return "for (int " + variable + " = " + start + "; " + variable + " < " + end + "; " + variable + " += " + step + ") {";
    }

    private static String assignmentOperator(AssignmentStatement.Operator operator) {
        return switch (operator) {
            case Assign -> "=";
            case AddAssign -> "+=";
            case SubtractAssign -> "-=";
            case MultiplyAssign -> "*=";
            case DivideAssign -> "/=";
            case ModuloAssign -> "%=";
        };
    }
}
