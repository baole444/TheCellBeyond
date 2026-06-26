package scripting.transpiler.semantic;

import scripting.transpiler.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Run the structure validation rules, such as annotation placement, lifecycle hook signatures,
 * along with incorrect reserved java keywords usages as user identifiers.
 */
public final class Validator {
    private static final String ExportAnnotation = "export";
    private static final String VoidType = "void";
    private static final String CLassRole = "class";
    private static final String EnumRole = "enum";
    private static final String EnumConstantRole = "enum constant";
    private static final String FieldRole = "field";
    private static final String MethodRole = "method";
    private static final String SignalRole = "signal";
    private static final String ParameterRole = "parameter";
    private static final String VariableRole = "variable";
    private final List<SemanticError> errors = new ArrayList<>();
    private static final Set<String> JavaKeywords = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
    );

    /**
     * Validate the script in place
     * @param scriptFile the parsed and resolved script
     * @return the validation errors, empty when the script is correct
     */
    public List<SemanticError> validate(ScriptFile scriptFile) {
        if (scriptFile.typeDeclaration instanceof EnumDeclaration enumDeclaration) {
            validateEnum(enumDeclaration);
            return errors;
        }
        ClassDeclaration classDeclaration = (ClassDeclaration) scriptFile.typeDeclaration;
        checkReserved(classDeclaration.name, classDeclaration, "class");
        classDeclaration.fields.forEach(this::validateField);
        classDeclaration.methods.forEach(this::validateMethod);
        classDeclaration.signals.forEach(this::validateSignal);
        return errors;
    }

    private void validateSignal(SignalDeclaration signal) {
        checkReserved(signal.name, signal, SignalRole);
        for (ParameterDeclaration param : signal.parameters) {
            checkReserved(param.name, param, ParameterRole);
            if (param.type.arrayDepth != 0) error(param.type, String.format("Signal parameter '%s' has unsupported array type", param.name));
        }
    }

    private void validateEnum(EnumDeclaration enumDeclaration) {
        checkReserved(enumDeclaration.name, enumDeclaration, EnumRole);
        enumDeclaration.fields.forEach(field -> checkReserved(field.name, field, FieldRole));
        enumDeclaration.constants.forEach(constant -> validateConstant(constant, enumDeclaration.fields));
    }

    private void validateConstant(EnumConstant constant, List<FieldDeclaration> fields) {
        checkReserved(constant.name, constant, EnumConstantRole);
        if (constant.arguments.size() != fields.size()) {
            error(constant, String.format("Enum constant '%s' expects %d argument(s), found %d", constant.name, fields.size(), constant.arguments.size()));
            return;
        }
        for (int i = 0; i < fields.size(); i++) validateConstantArgument(constant.arguments.get(i), fields.get(i));
    }

    private void validateConstantArgument(Expression argument, FieldDeclaration field) {
        if (!(argument instanceof LiteralExpression literal)) return;
        if (literalMatchesType(literal, field.type)) return;
        error(argument, String.format("Enum field '%s' is of type '%s', incompatible with the given literal", field.name, field.type.name));
    }

    private void validateField(FieldDeclaration field) {
        checkReserved(field.name, field, FieldRole);
        field.annotations.forEach(a -> validateAnnotation(a, field));
    }

    private void validateAnnotation(Annotation annotation, FieldDeclaration field) {
        if (!annotation.name.equals(ExportAnnotation)) {
            error(annotation, "Unknown annotation '@" + annotation.name + "'");
            return;
        }
        if (field.isConst) error(annotation, "@export is not allowed on a 'const', final field");
    }

    private void validateMethod(MethodDeclaration method) {
        checkReserved(method.name, method, MethodRole);
        method.parameters.forEach(param -> checkReserved(param.name, param, ParameterRole));
        LifecycleTable.find(method.name).ifPresent(hook -> validateLifecycle(method, hook));
        validateBlock(method.body);
    }

    private void validateIf(IfStatement ifStatement) {
        validateBlock(ifStatement.thenBlock);
        ifStatement.elifClauses.forEach(clause -> validateBlock(clause.block));
        if (ifStatement.elseBlock != null) validateBlock(ifStatement.elseBlock);
    }

    private void validateBlock(Block block) {
        block.statements.forEach(this::validateStatement);
    }

    private void validateStatement(Statement statement) {
        switch (statement) {
            case LocalVariableDeclaration local -> checkReserved(local.name, local, VariableRole);
            case ForStatement forStatement -> {
                checkReserved(forStatement.variable, forStatement, VariableRole);
                validateBlock(forStatement.body);
            }
            case WhileStatement whileStatement -> validateBlock(whileStatement.body);
            case IfStatement ifStatement -> validateIf(ifStatement);
            default -> {}
        }
    }

    private void validateLifecycle(MethodDeclaration method, LifecycleTable.LifecycleHook hook) {
        if (method.returnType != null && !method.returnType.name.equals(VoidType)) error(method.returnType, "Lifecycle hook '" + method.name + "' must return void");
        List<String> expected = hook.parameterTypes();
        if (method.parameters.size() != expected.size()) {
            error(method, String.format("Lifecycle hook '%s' expect %d parameter(s), found %d", method.name, expected.size(), method.parameters.size()));
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (matchingType(method.parameters.get(i).type, expected.get(i))) continue;
            error(method.parameters.get(i), String.format("Lifecycle hook '%s' parameter %d must be of type '%s'", method.name, i + 1, expected.get(i)));
        }
    }

    private void checkReserved(String name, AstNode node, String role) {
        if (!JavaKeywords.contains(name)) return;
        error(node, String.format("'%s' is a reserved java keyword and cannot be used as a %s name", name, role));
    }

    private void error(AstNode node, String message) {
        SourcePosition position = node.position;
        errors.add(new SemanticError(position.file(), position.line(), position.column(), message));
    }

    private static boolean matchingType(TypeReference current, String expected) {
        return current.arrayDepth == 0 && current.name.equals(expected);
    }

    /**
     * Attempt to check a literal argument against a primitive or {@code String} field type.
     * None built in field types pass through unchecked.
     * @param literal the literal argument
     * @param type the declared field type
     * @return true when the literal is compatible or when the type is not checked
     */
    private static boolean literalMatchesType(LiteralExpression literal, TypeReference type) {
        if (type.arrayDepth != 0) return true;
        return switch (type.name) {
            case "int" -> literal.kind == LiteralExpression.Kind.Integer;
            case "float" -> literal.kind == LiteralExpression.Kind.Integer || literal.kind == LiteralExpression.Kind.Float;
            case "bool" -> literal.kind == LiteralExpression.Kind.Boolean;
            case "String" -> literal.kind == LiteralExpression.Kind.String || literal.kind == LiteralExpression.Kind.Null;
            default -> true;
        };
    }
}
