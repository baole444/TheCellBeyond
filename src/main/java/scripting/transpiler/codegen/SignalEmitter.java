package scripting.transpiler.codegen;

import scripting.transpiler.ast.ParameterDeclaration;
import scripting.transpiler.ast.SignalDeclaration;
import scripting.transpiler.ast.TypeReference;
import signal.Signal;

import java.util.stream.Collectors;

/**
 * Emit a {@code signal} member as a {@code public final Signal} instance field,
 * with its constructor carries the boxed contract types (primitives) or object type class.
 */
final class SignalEmitter {
    private static final String SignalType = Signal.class.getName();
    private final EmitContext context;

    SignalEmitter(EmitContext context) {
        this.context = context;
    }

    void emit(SignalDeclaration signal) {
        String type = context.writer.importType(SignalType);
        String contract = signal.parameters.stream().map(this::contractType).collect(Collectors.joining(", "));
        context.writer.field(String.format("public final %s %s = new %s(%s)", type, signal.name, type, contract));
    }

    private String contractType(ParameterDeclaration parameter) {
        return boxed(parameter.type) + ".class";
    }

    private String boxed(TypeReference type) {
        return switch (type.name) {
            case "int" -> "Integer";
            case "float" -> "Float";
            case "bool" -> "Boolean";
            default -> context.typeName(type);
        };
    }
}
