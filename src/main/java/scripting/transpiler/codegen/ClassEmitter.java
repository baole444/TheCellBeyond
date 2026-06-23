package scripting.transpiler.codegen;

import scripting.RegisterComponent;
import scripting.RegisterGameObject;
import scripting.transpiler.ast.ClassDeclaration;
import scripting.transpiler.ast.TypeReference;
import scripting.transpiler.semantic.ClassRegistration;
import utility.log.EngineLog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Drive the {@link JavaSourceWriter} to render the entire class including: registration annotation,
 * optional Logger field, and the class's methods.
 * <p>
 * The result is a complete java compilation unit in the flat {@code scripts} package.
 */
public final class ClassEmitter {
    private static final String Package = "scripts";
    private static final String EngineLogType = EngineLog.class.getName();
    private static final String RegisterGameObjectType = RegisterGameObject.class.getName();
    private static final String RegisterComponentType = RegisterComponent.class.getName();

    /**
     * Render a resolved class declaration into java source.
     * @param classDeclaration the class to emit, resolved by the semantic pass
     * @param sourceName the originating script file, used in the generated file header
     * @return the rendered Java source
     */
    public static String emit(ClassDeclaration classDeclaration, String sourceName) {
        EmitContext context = new EmitContext(new JavaSourceWriter(Package), classDeclaration.name, fieldTypes(classDeclaration));
        context.useLogger = CheckLogger.inUse(classDeclaration);
        context.writer.fileComment(String.format("// generated from %s - edit if you know what you are doing", sourceName));
        registration(context, classDeclaration.registration);
        context.writer.openType(declarationLine(context, classDeclaration));
        body(context, classDeclaration);
        context.writer.closeType();
        return context.writer.render();
    }

    private static Map<String, TypeReference> fieldTypes(ClassDeclaration classDeclaration) {
        Map<String, TypeReference> types = new LinkedHashMap<>();
        classDeclaration.fields.forEach(field -> types.put(field.name, field.type));
        return types;
    }

    private static void registration(EmitContext context, ClassRegistration registration) {
        switch (registration) {
            case GameObject -> context.writer.annotation("@" + context.writer.importType(RegisterGameObjectType));
            case Component -> context.writer.annotation("@" + context.writer.importType(RegisterComponentType));
            case null, default -> {}
        }
    }

    private static String declarationLine(EmitContext context, ClassDeclaration classDeclaration) {
        String line = "public class " + classDeclaration.name;
        if (classDeclaration.superType == null) return line;
        return line + " extends " + context.typeName(classDeclaration.superType);
    }

    private static void body(EmitContext context, ClassDeclaration classDeclaration) {
        FieldEmitter fields = new FieldEmitter(context);
        MethodEmitter methods = new MethodEmitter(context);
        boolean first = true;
        if (context.useLogger) {
            logger(context);
            first = false;
        }
        for (var field : classDeclaration.fields) {
            if (!first) context.writer.blankLine();
            fields.emit(field);
            first = false;
        }
        for (var method : classDeclaration.methods) {
            if (!first) context.writer.blankLine();
            methods.emit(method);
            first = false;
        }
    }

    private static void logger(EmitContext context) {
        String type = context.writer.importType(EngineLogType);
        context.writer.field("private static final " + type + " Logger = new " + type + "(" + context.className + ".class)");
    }
}
