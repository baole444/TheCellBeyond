package scripting.transpiler.semantic;

import scripting.transpiler.TranspilerProperties;

import java.util.List;
import java.util.Optional;

/**
 * Table of lifecycle hooks, contains the resolution of special snake case aliases mapping to their java method counterparts.
 * This table is shared by the semantic pass and code generation.
 */
public final class LifecycleTable {
    /**
     * A lifecycle hook target.
     * @param javaName the engine method to override
     * @param parameterTypes simple type names of the hook's parameters, in order
     */
    public record LifecycleHook(String javaName, List<String> parameterTypes) {}

    private LifecycleTable() {}

    /**
     * Look up a lifecycle hook by its script method name.
     * @param scriptName the {@code _snake_case} method name
     * @return the hook, or empty when the name is not a lifecycle hook
     */
    public static Optional<LifecycleHook> find(String scriptName) {
        return Optional.ofNullable(TranspilerProperties.LifeCycle.get(scriptName));
    }
}
