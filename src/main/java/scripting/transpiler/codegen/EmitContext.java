package scripting.transpiler.codegen;

import scripting.transpiler.TranspilerProperties;
import scripting.transpiler.ast.TypeReference;
import scripting.transpiler.semantic.Resolution;

import java.util.Map;

/**
 * Shared state for the code generation emitters, including:
 * <ul>
 *     <li>The {@link JavaSourceWriter} that is driven by the emitters.</li>
 *     <li>The name of the class being generated.</li>
 *     <li>Built in logger usage flag, which control the {@code Logger} field injection.</li>
 * </ul>
 * It also owns the type name rendering, mapping built in script types to their java spelling,
 * and routing class references through the writer's import registry.
 */
final class EmitContext {
    final JavaSourceWriter writer;
    final String className;
    /**
     * Declared field types by name, used to resolve the target type, for an assignment to a field.
     */
    final Map<String, TypeReference> fieldTypes;
    boolean useLogger;

    EmitContext(JavaSourceWriter writer, String className, Map<String, TypeReference> fieldTypes) {
        this.writer = writer;
        this.className = className;
        this.fieldTypes = fieldTypes;
    }

    /**
     * Render a type reference into its java spelling and register import for class references.
     * @param type the type reference, or null for void
     * @return the java spelling, including array brackets
     */
    String typeName(TypeReference type) {
        if (type == null) return "void";
        return baseTypeName(type) + "[]".repeat(type.arrayDepth);
    }

    private String baseTypeName(TypeReference type) {
        return switch (type.resolution) {
            case Resolution.APIClassResolution(String fqn) -> writer.importType(fqn);
            case Resolution.ProjectClassResolution(String fqn) -> writer.importType(fqn);
            case null, default -> TranspilerProperties.BuiltInToJavas.getOrDefault(type.name, type.name);
        };
    }
}
