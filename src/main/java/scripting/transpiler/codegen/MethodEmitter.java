package scripting.transpiler.codegen;

import scripting.transpiler.ast.MethodDeclaration;
import scripting.transpiler.ast.ParameterDeclaration;
import scripting.transpiler.semantic.Resolution;

import java.util.stream.Collectors;

/**
 * Emit a method with its signature. {@code @Override} annotation is added when it resolved to an engine hook or inherited override.
 */
final class MethodEmitter {
    private final EmitContext context;

    MethodEmitter(EmitContext context) {
        this.context = context;
    }

    void emit(MethodDeclaration method) {
        if (method.resolution != null) context.writer.annotation("@Override");
        context.writer.openMethod(signature(method));
        StatementEmitter statements = new StatementEmitter(context);
        method.body.statements.forEach(statements::emit);
        context.writer.closeMethod();
    }

    private String signature(MethodDeclaration method) {
        String modifiers = visibility(method) + (method.isStatic ? " static" : "");
        String name = method.resolution != null ? overrideName(method) : method.name;
        return modifiers + " " + context.typeName(method.returnType) + " " + name + "(" + parameters(method) + ")";
    }

    private static String visibility(MethodDeclaration method) {
        if (method.resolution instanceof Resolution.LifecycleResolution) return "protected";
        return Modifiers.visibility(method.visibility);
    }

    private String parameters(MethodDeclaration method) {
        return method.parameters.stream().map(this::parameter).collect(Collectors.joining(", "));
    }

    private String parameter(ParameterDeclaration param) {
        return context.typeName(param.type) + " " + param.name;
    }

    private String overrideName(MethodDeclaration method) {
        return switch (method.resolution) {
            case Resolution.LifecycleResolution(String javaName) -> javaName;
            case Resolution.APIMemberResolution(String _, String javaName) -> javaName;
            default -> method.name;
        };
    }
}
