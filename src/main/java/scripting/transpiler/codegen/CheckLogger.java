package scripting.transpiler.codegen;


import scripting.transpiler.ast.*;
import scripting.transpiler.semantic.Resolution;

/**
 * Scan a class for logging built in usage. It looks for {@link Resolution.BuiltinLogResolution} on method calls,
 * which were attached by the semantic pass.
 */
final class CheckLogger {
    private CheckLogger() {}

    static boolean inUse(ClassDeclaration classDeclaration) {
        return classDeclaration.methods.stream().anyMatch(method -> inBlock(method.body));
    }

    private static boolean inBlock(Block block) {
        return block.statements.stream().anyMatch(CheckLogger::inStatement);
    }

    private static boolean inStatement(Statement statement) {
        return switch (statement) {
            case LocalVariableDeclaration local -> local.initializer != null && inExpression(local.initializer);
            case AssignmentStatement assign -> inExpression(assign.target) || inExpression(assign.value);
            case ExpressionStatement expression -> inExpression(expression.expression);
            case ReturnStatement returnStatement -> returnStatement.value != null && inExpression(returnStatement.value);
            case IfStatement ifStatement -> inIf(ifStatement);
            case WhileStatement whileStatement -> inExpression(whileStatement.condition) || inBlock(whileStatement.body);
            default -> false;
        };
    }

    private static boolean inIf(IfStatement ifStatement) {
        if (inExpression(ifStatement.condition) || inBlock(ifStatement.thenBlock)) return true;
        if (ifStatement.elifClauses.stream().anyMatch(clause -> inExpression(clause.condition) || inBlock(clause.block))) return true;
        return ifStatement.elseBlock != null && inBlock(ifStatement.elseBlock);
    }

    private static boolean inExpression(Expression expression) {
        return switch (expression) {
            case MethodCallExpression call -> call.resolution instanceof Resolution.BuiltinLogResolution || (call.target != null && inExpression(call.target)) || call.arguments.stream().anyMatch(CheckLogger::inExpression);
            case MemberAccessExpression access -> inExpression(access.target);
            case BinaryExpression binary -> inExpression(binary.left) || inExpression(binary.right);
            case UnaryExpression unary -> inExpression(unary.operand);
            case ConditionalExpression conditional -> inExpression(conditional.condition) || inExpression(conditional.thenValue) || inExpression(conditional.elseValue);
            case IndexExpression index -> inExpression(index.target) || inExpression(index.index);
            case CastExpression cast -> inExpression(cast.value);
            case TypeCheckExpression check -> inExpression(check.value);
            default -> false;
        };
    }
}
