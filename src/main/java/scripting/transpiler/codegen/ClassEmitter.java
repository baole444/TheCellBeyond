package scripting.transpiler.codegen;

import scripting.transpiler.TranspilerProperties;
import scripting.transpiler.ast.*;
import scripting.transpiler.semantic.ClassRegistration;

/**
 * Drive the {@link JavaSourceWriter} to render the entire class including: registration annotation,
 * optional Logger field, and the class's methods.
 * <p>
 * The result is a complete java compilation unit in the flat {@code scripts} package. {@code extends Object} is normalized and remove.
 */
public final class ClassEmitter {

    /**
     * Render a resolved class declaration into java source.
     * @param classDeclaration the class to emit, resolved by the semantic pass
     * @param sourceName the originating script file, used in the generated file header
     * @return the rendered Java source
     */
    public static String emit(ClassDeclaration classDeclaration, String sourceName) {
        EmitContext context = new EmitContext(new JavaSourceWriter(TranspilerProperties.ScriptPackage), classDeclaration.name);
        context.useLogger = CheckLogger.inUse(classDeclaration);
        context.writer.fileComment(String.format("// generated from %s - edit if you know what you are doing", sourceName));
        registration(context, classDeclaration.registration);
        context.writer.openType(declarationLine(context, classDeclaration));
        body(context, classDeclaration);
        context.writer.closeType();
        return context.writer.render();
    }

    private static void registration(EmitContext context, ClassRegistration registration) {
        switch (registration) {
            case GameObject -> context.writer.annotation("@" + context.writer.importType(TranspilerProperties.RegisterGameObjectFQN));
            case Component -> context.writer.annotation("@" + context.writer.importType(TranspilerProperties.RegisterComponentFQN));
            case null, default -> {}
        }
    }

    private static String declarationLine(EmitContext context, ClassDeclaration classDeclaration) {
        String line = "public class " + classDeclaration.name;
        TypeReference superType = classDeclaration.superType;
        if (superType == null || superType.name.equals(TranspilerProperties.ObjectType)) return line;
        return line + " extends " + context.typeName(superType);
    }

    private static void body(EmitContext context, ClassDeclaration classDeclaration) {
        FieldEmitter fields = new FieldEmitter(context);
        SignalEmitter signals = new SignalEmitter(context);
        MethodEmitter methods = new MethodEmitter(context);
        boolean first = true;
        if (context.useLogger) {
            logger(context);
            first = false;
        }
        for (FieldDeclaration field : classDeclaration.fields) {
            if (!first) context.writer.blankLine();
            fields.emit(field);
            first = false;
        }
        for (SignalDeclaration signal : classDeclaration.signals) {
            if (!first) context.writer.blankLine();
            signals.emit(signal);
            first = false;
        }
        for (MethodDeclaration method : classDeclaration.methods) {
            if (!first) context.writer.blankLine();
            methods.emit(method);
            first = false;
        }
    }

    private static void logger(EmitContext context) {
        String type = context.writer.importType(TranspilerProperties.EngineLogFQN);
        context.writer.field(String.format("private static final %s Logger = new %s(%s.class)", type, type, context.className));
    }
}
