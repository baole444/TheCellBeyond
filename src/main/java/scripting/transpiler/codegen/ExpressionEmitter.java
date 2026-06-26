package scripting.transpiler.codegen;

import scripting.transpiler.ast.*;
import scripting.transpiler.semantic.Resolution;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Render an expression node into a java source string.
 * <p>
 * Type references inside expression resolve via writer's import registry. Names resolve via {@link Resolution}.
 * Composite sub-expressions are parenthesized to preserve parsed grouping.
 */
final class ExpressionEmitter {
    private static final String FloatType = "float";
    private final EmitContext context;

    ExpressionEmitter(EmitContext context) {
        this.context = context;
    }

    String emit(Expression expression) {
        return emit(expression, null);
    }

    String emit(Expression expression, String expectedType) {
        return switch (expression) {
            case LiteralExpression literal -> literal(literal, expectedType);
            case IdentifierExpression identifier -> identifier(identifier);
            case SelfExpression _ -> "this";
            case MethodCallExpression call -> call(call);
            case ConstructorCallExpression constructor -> constructorCall(fqnOf(constructor.resolution), constructor.arguments);
            case ClassLiteralExpression classLiteral -> context.typeName(classLiteral.type) + ".class";
            case MemberAccessExpression access -> access(access);
            case IndexExpression index -> emit(index.target) + "[" + emit(index.index) + "]";
            case UnaryExpression unary -> unary(unary, expectedType);
            case BinaryExpression binary -> binary(binary, expectedType);
            case ConditionalExpression conditional -> conditional(conditional, expectedType);
            case CastExpression cast -> "((" + context.typeName(cast.type) + ") " + emit(cast.value) + ")";
            case TypeCheckExpression check -> "(" + emit(check.value) + " instanceof " + context.typeName(check.type) + ")";
            default -> throw new IllegalStateException("Unsupported expression: " + expression.getClass().getSimpleName());
        };
    }

    private String literal(LiteralExpression literal, String expectedType) {
        return switch (literal.kind) {
            case String -> JavaSourceWriter.escapeStringLiteral(decodeString(literal.text));
            case Float -> FloatType.equals(expectedType) ? literal.text + "f" : literal.text;
            case Null -> "null";
            case Integer, Boolean -> literal.text;
        };
    }

    private String identifier(IdentifierExpression identifier) {
        return switch (identifier.resolution) {
            case Resolution.APIClassResolution(String fqn) -> context.writer.importType(fqn);
            case Resolution.ProjectClassResolution(String fqn) -> context.writer.importType(fqn);
            case Resolution.APIMemberResolution(String ignored, String javaName) -> javaName;
            case Resolution.UserMemberResolution(String name) -> name;
            case null, default -> identifier.name;
        };
    }

    private String call(MethodCallExpression call) {
        if (call.resolution instanceof Resolution.ConstructorResolution(String fqn)) return constructorCall(fqn, call.arguments);
        String arguments = emitArguments(call.arguments);
        if (call.resolution instanceof Resolution.BuiltinLogResolution(String javaMethod)) {
            context.useLogger = true;
            return "Logger." + javaMethod + "(" + arguments + ")";
        }
        String receiver = call.target != null ? emit(call.target) + "." : "";
        return receiver + memberName(call.resolution, call.methodName) + "(" + arguments + ")";
    }

    private String constructorCall(String fqn, List<Expression> arguments) {
        return "new " + context.writer.importType(fqn) + "(" + emitArguments(arguments) + ")";
    }

    private String emitArguments(List<Expression> arguments) {
        return arguments.stream().map(this::emit).collect(Collectors.joining(", "));
    }

    private static String fqnOf(Resolution resolution) {
        return ((Resolution.ConstructorResolution) resolution).fqn();
    }

    private String access(MemberAccessExpression access) {
        return emit(access.target) + "." + memberName(access.resolution, access.memberName);
    }

    private String unary(UnaryExpression unary, String expectedType) {
        String operator = unary.operator == UnaryExpression.Operator.Negate ? "-" : "!";
        String propagated = unary.operator == UnaryExpression.Operator.Negate ? expectedType : null;
        return "(" + operator + emit(unary.operand, propagated) + ")";
    }

    private String binary(BinaryExpression binary, String expectedType) {
        String propagated = isArithmetic(binary.operator) ? expectedType : null;
        return "(" + emit(binary.left, propagated) + " " + operatorSymbol(binary.operator) + " " + emit(binary.right, propagated) + ")";
    }

    private String conditional(ConditionalExpression conditional, String expectedType) {
        return "(" + emit(conditional.condition) + " ? " + emit(conditional.thenValue, expectedType) + " : " + emit(conditional.elseValue, expectedType) + ")";
    }

    /**
     * Get the java name for a resolved member, or the original lexeme when it is unresolved..
     * @param resolution the member resolution
     * @param fallback the original script name for when no resolution applied
     * @return the name to emit
     */
    private static String memberName(Resolution resolution, String fallback) {
        return switch (resolution) {
            case Resolution.APIMemberResolution(String _, String javaName) -> javaName;
            case Resolution.LifecycleResolution(String javaName) -> javaName;
            case Resolution.UserMemberResolution(String name) -> name;
            case null, default -> fallback;
        };
    }

    private static boolean isArithmetic(BinaryExpression.Operator operator) {
        return switch (operator) {
            case Add, Subtract, Multiply, Divide, Modulo -> true;
            default -> false;
        };
    }

    private static String operatorSymbol(BinaryExpression.Operator operator) {
        return switch (operator) {
            case Add -> "+";
            case Subtract -> "-";
            case Multiply -> "*";
            case Divide -> "/";
            case Modulo -> "%";
            case Less -> "<";
            case Greater -> ">";
            case LessEqual -> "<=";
            case GreaterEqual -> ">=";
            case Equal -> "==";
            case NotEqual -> "!=";
            case And -> "&&";
            case Or -> "||";
        };
    }

    /**
     * Decode a string literal lexeme into its value, stripping the surrounding quotes and remove escaping base on the lexer's escape set.
     * @param lexeme the raw literal text, including the surrounding quotes
     * @return the decoded string value
     */
    private static String decodeString(String lexeme) {
        String inner = lexeme.substring(1, lexeme.length() - 1);
        StringBuilder result = new StringBuilder(inner.length());
        for (int i = 0; i < inner.length(); i++) {
            char current = inner.charAt(i);
            if (current != ('\\')) {
                result.append(current);
                continue;
            }
            i++;
            result.append(removeEscape(inner.charAt(i)));
        }
        return result.toString();
    }

    private static char removeEscape(char escaped) {
        return switch (escaped) {
            case 'b' -> '\b';
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'f' -> '\f';
            case 'r' -> '\r';
            default -> escaped;
        };
    }
}
