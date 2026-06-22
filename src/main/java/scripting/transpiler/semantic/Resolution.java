package scripting.transpiler.semantic;

/**
 * The semantic resolution attached to a reference in the AST.
 * It allows code generation to emit the correct java name, base on the type of symbol the reference denoted.
 */
public sealed interface Resolution {
    /**
     * A lifecycle hook method. Generation emits {@link @Override} and the engine method name.
     * @param javaName the engine method overridden, such as {@code onPhysicUpdate}
     */
    record LifecycleResolution(String javaName) implements Resolution {}
    /**
     * A user defined name of current class member. Unlike API members, these name are emitted as is.
     * @param name the name to emit verbatim
     */
    record UserMemberResolution(String name) implements Resolution {}
    /**
     * A reference to a script class in the project.
     * @param fqn the fully qualified name, such as {@code scripts.Player}
     */
    record ProjectClassResolution(String fqn) implements Resolution {}
    /**
     * A reference to an API class.
     * @param fqn the fully qualified name of the API's type
     */
    record APIClassResolution(String fqn) implements Resolution {}
    /**
     * A method or field of an API type.
     * @param receiverClassFQN the fully qualified name of the type
     * @param javaName the original java name of the member
     */
    record APIMemberResolution(String receiverClassFQN, String javaName) implements Resolution {}
    /**
     * A language logging built in, such as {@code print} or {@code print_error}.
     * @param javaMethod the {@code EngineLog} instance method to call,
     * one of {@code debug}, {@code info}, {@code warning}, {@code error}.
     */
    record BuiltinLogResolution(String javaMethod) implements Resolution {}
    /**
     * The {@code range{...}} built in used as a {@code for} loop iterable.
     * Generate to an indexed for loop in java.
     * <p>
     * The argument count, 1 to 3, selected the start, end and step. This is read from the call's arguments at generation.
     */
    record BuiltinRangeResolution() implements Resolution {}
}
