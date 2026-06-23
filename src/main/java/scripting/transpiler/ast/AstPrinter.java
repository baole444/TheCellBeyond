package scripting.transpiler.ast;

import java.util.List;

public final class AstPrinter {
    private final StringBuilder out = new StringBuilder();

    private AstPrinter() {}

    public static String print(AstNode node) {
        AstPrinter printer = new AstPrinter();
        printer.render(node, 0);
        return printer.out.toString();
    }

    private void render(AstNode node, int depth) {
        line(depth, describe(node), node);
        for (AstNode child : children(node)) render(child, depth + 1);
    }

    private void line(int depth, String text, AstNode node) {
        out.repeat("  ", depth).append(text).append("  @").append(node.position).append('\n');
    }

    private static String describe(AstNode node) {
        return switch (node) {
            case ScriptFile _ -> "ScriptFile";
            case ClassDeclaration n -> "ClassDeclaration " + n.name + (n.superType == null ? ""  : " extends " + typeName(n.superType));
            case EnumDeclaration n -> "EnumDeclaration " + n.name;
            case EnumConstant n -> "EnumConstant " + n.name;
            case FieldDeclaration n -> "FieldDeclaration " + (n.isConst ? "const " : "var ") + n.visibility + (n.isStatic ? " static" : "") + " " + n.name + " : " + typeName(n.type);
            case MethodDeclaration n -> "MethodDeclaration " + n.visibility + (n.isStatic ? " static" : "") + " " + n.name + "() -> " + (n.returnType == null ? "void" : typeName(n.returnType));
            case ParameterDeclaration n -> "ParameterDeclaration " + n.name + " : " + typeName(n.type);
            case TypeReference n -> "TypeReference " + typeName(n);
            case Annotation n -> "Annotation @" + n.name;
            case AnnotationArgument n -> "AnnotationArgument " + n.name;
            case Block _ -> "Block";
            case LocalVariableDeclaration n -> "LocalVariableDeclaration " + (n.isConst ? "const " : "var ") + n.name + " : " + typeName(n.type);
            case ReturnStatement _ -> "ReturnStatement";
            case BreakStatement _ -> "BreakStatement";
            case ContinueStatement _ -> "ContinueStatement";
            case PassStatement _ -> "PassStatement";
            case ExpressionStatement _ -> "ExpressionStatement";
            case AssignmentStatement n -> "AssignmentStatement " + n.operator;
            case IfStatement _ -> "IfStatement";
            case ElifClause _ -> "ElifClause";
            case WhileStatement _ -> "WhileStatement";
            case ForStatement n -> "ForStatement " + n.variable + " in";
            case LiteralExpression n -> "LiteralExpression " + n.kind + " " + n.text;
            case IdentifierExpression n -> "IdentifierExpression " + n.name;
            case MemberAccessExpression n -> "MemberAccessExpression ." + n.memberName;
            case MethodCallExpression n -> "MethodCallExpression " + (n.target == null ? "" : ".") + n.methodName + "()";
            case IndexExpression _ -> "IndexExpression";
            case UnaryExpression n -> "UnaryExpression " + n.operator;
            case BinaryExpression n -> "BinaryExpression " + n.operator;
            case TypeCheckExpression n -> "TypeCheckExpression is " + typeName(n.type);
            case CastExpression n -> "CastExpression as " + typeName(n.type);
            case ConditionalExpression _ -> "ConditionalExpression";
            default -> node.getClass().getSimpleName();
        };
    }

    private static List<AstNode> children(AstNode node) {
        return switch (node) {
            case ScriptFile n -> List.of(n.typeDeclaration);
            case ClassDeclaration n -> concat(nullable(n.superType), n.fields, n.methods);
            case EnumDeclaration n -> concat(n.constants, n.fields);
            case EnumConstant n -> List.copyOf(n.arguments);
            case FieldDeclaration n -> concat(n.annotations, List.of(n.type), nullable(n.initializer));
            case MethodDeclaration n -> concat(n.parameters, optionalType(n.returnType), List.of(n.body));
            case ParameterDeclaration n -> concat(List.of(n.type), nullable(n.defaultValue));
            case Annotation n -> List.copyOf(n.arguments);
            case AnnotationArgument n -> List.of(n.value);
            case Block n -> List.copyOf(n.statements);
            case LocalVariableDeclaration n -> concat(List.of(n.type), nullable(n.initializer));
            case ReturnStatement n -> nullable(n.value);
            case ExpressionStatement n -> List.of(n.expression);
            case AssignmentStatement n -> List.of(n.target, n.value);
            case IfStatement n -> concat(List.of(n.condition, n.thenBlock), n.elifClauses, nullable(n.elseBlock));
            case ElifClause n -> List.of(n.condition, n.block);
            case WhileStatement n -> List.of(n.condition, n.body);
            case ForStatement n -> List.of(n.iterable, n.body);
            case MemberAccessExpression n -> List.of(n.target);
            case MethodCallExpression n -> concat(nullable(n.target), n.arguments, List.of());
            case IndexExpression n -> List.of(n.target, n.index);
            case UnaryExpression n -> List.of(n.operand);
            case BinaryExpression n -> List.of(n.left, n.right);
            case TypeCheckExpression n -> List.of(n.value);
            case CastExpression n -> List.of(n.value);
            case ConditionalExpression n -> List.of(n.condition, n.thenValue, n.elseValue);
            default -> List.of();
        };
    }

    private static String typeName(TypeReference type) {
        return type.name + "[]".repeat(type.arrayDepth);
    }

    private static List<AstNode> nullable(AstNode node) {
        return node == null ? List.of() : List.of(node);
    }

    private static List<AstNode> optionalType(TypeReference type) {
        return type == null ? List.of() : List.of(type);
    }

    @SafeVarargs
    private static List<AstNode> concat(List<? extends AstNode>... groups) {
        List<AstNode> all = new java.util.ArrayList<>();
        for (List<? extends AstNode> group : groups) all.addAll(group);
        return all;
    }
}
