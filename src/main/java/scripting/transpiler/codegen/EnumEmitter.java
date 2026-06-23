package scripting.transpiler.codegen;

import scripting.transpiler.ast.EnumConstant;
import scripting.transpiler.ast.EnumDeclaration;
import scripting.transpiler.ast.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Drive the {@link JavaSourceWriter} to render the entire enum class.
 * <p>
 * When constants carry positional arguments, there will be trailing field section to emit {@code public final} fields,
 * with a synthesized constructor that assign them in order.
 * A bare enum has no fields and no constructor.
 */
public final class EnumEmitter {
    private static final String Package = "scripts";
    private final EmitContext context;
    private final ExpressionEmitter expressions;
    private final EnumDeclaration enumDeclaration;

    private EnumEmitter(EnumDeclaration enumDeclaration) {
        context = new EmitContext(new JavaSourceWriter(Package), enumDeclaration.name, fieldTypes(enumDeclaration));
        expressions = new ExpressionEmitter(context);
        this.enumDeclaration = enumDeclaration;
    }

    /**
     * Render a resolved enum declaration into java source.
     * @param enumDeclaration the enum to emit, resolved by the semantic pass
     * @param sourceName the originating script file, used in the generated file header
     * @return the rendered Java source
     */
    public static String emit(EnumDeclaration enumDeclaration, String sourceName) {
        return new EnumEmitter(enumDeclaration).render(sourceName);
    }

    private String render(String sourceName) {
        context.writer.fileComment(String.format("// generated from %s - edit if you know what you are doing", sourceName));
        context.writer.openType("public enum " + enumDeclaration.name);
        int last = enumDeclaration.constants.size() - 1;
        boolean hasFields = !enumDeclaration.fields.isEmpty();
        for (int i = 0; i <= last; i++) {
            String terminator = i < last ? "," : (hasFields ? ";" : "");
            context.writer.line(constant(enumDeclaration.constants.get(i)) + terminator);
        }
        if (!enumDeclaration.fields.isEmpty()) {
            context.writer.blankLine();
            fields();
            context.writer.blankLine();
            constructor();
        }
        context.writer.closeType();
        return context.writer.render();
    }

    private String constant(EnumConstant constant) {
        if (constant.arguments.isEmpty()) return constant.name;
        String arguments = IntStream.range(0, constant.arguments.size())
                .mapToObj(i -> expressions.emit(constant.arguments.get(i), enumDeclaration.fields.get(i).type.name))
                .collect(Collectors.joining(", "));
        return constant.name + "(" + arguments + ")";
    }

    private void fields() {
        enumDeclaration.fields.forEach(field -> context.writer.field("public final " + context.typeName(field.type) + " " + field.name));
    }

    private void constructor() {
        String parameters = enumDeclaration.fields.stream().map(field -> context.typeName(field.type) + " " + field.name).collect(Collectors.joining(", "));
        context.writer.openMethod(enumDeclaration.name + "(" + parameters + ")");
        enumDeclaration.fields.forEach(field -> context.writer.line("this." + field.name + " = " + field.name + ";"));
        context.writer.closeMethod();
    }

    private static Map<String, TypeReference> fieldTypes(EnumDeclaration enumDeclaration) {
        Map<String, TypeReference> types = new LinkedHashMap<>();
        enumDeclaration.fields.forEach(field -> types.put(field.name, field.type));
        return types;
    }
}
