package scripting.transpiler.sematic;

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
        ClassDeclaration classDeclaration = scriptFile.classDeclaration;
        checkReserved(classDeclaration.name, classDeclaration, "class");
        classDeclaration.fields.forEach(this::validateField);
        classDeclaration.methods.forEach(this::validateMethod);
        return errors;
    }

    private void validateField(FieldDeclaration field) {
        checkReserved(field.name, field, "field");
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
        checkReserved(method.name, method, "method");
        method.parameters.forEach(param -> checkReserved(param.name, param, "parameter"));
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
            case LocalVariableDeclaration local -> checkReserved(local.name, local, "variable");
            case ForStatement forStatement -> {
                checkReserved(forStatement.variable, forStatement, "variable");
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
}
