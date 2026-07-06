package scripting.transpiler.codegen;

import scripting.transpiler.TranspilerProperties;
import scripting.transpiler.ast.Annotation;
import scripting.transpiler.ast.AnnotationArgument;
import scripting.transpiler.ast.FieldDeclaration;

import java.util.stream.Collectors;

/**
 * Emit a field declaration, with {@code @Export} annotation when required. The mapping are as follows:
 * <ul>
 *     <li>{@code const} -> {@code final}, an instance's constant.</li>
 *     <li>{@code var} -> pass through, an instance's field.</li>
 *     <li>{@code static} -> {@code static}, can be combined with {@code const} and {@code var} to create static constant and variable.</li>
 * </ul>
 */
final class FieldEmitter {
    private final EmitContext context;
    private final ExpressionEmitter expressions;

    FieldEmitter(EmitContext context) {
        this.context = context;
        expressions = new ExpressionEmitter(context);
    }

    void emit(FieldDeclaration field) {
        field.annotations.stream().filter(annotation -> annotation.name.equals(TranspilerProperties.ExportAnnotation)).forEach(this::exportAnnotation);
        context.writer.field(declarationLine(field));
    }

    private void exportAnnotation(Annotation annotation) {
        String name = context.writer.importType(TranspilerProperties.ExportFQN);
        if (annotation.arguments == null || annotation.arguments.isEmpty()) {
            context.writer.annotation("@" + name);
            return;
        }
        String arguments = annotation.arguments.stream().map(this::argument).collect(Collectors.joining(", "));
        context.writer.annotation("@" + name + "(" + arguments + ")");
    }

    private String argument(AnnotationArgument argument) {
        return argument.name + " = " + expressions.emit(argument.value);
    }

    private String declarationLine(FieldDeclaration field) {
        StringBuilder line = new StringBuilder(Modifiers.visibility(field.visibility));
        if (field.isStatic) line.append(" static");
        if (field.isConst) line.append(" final");
        line.append(' ').append(context.typeName(field.type)).append(' ').append(field.name);
        if (field.initializer != null) line.append(" = ").append(expressions.emit(field.initializer, field.type.name));
        return line.toString();
    }
}
