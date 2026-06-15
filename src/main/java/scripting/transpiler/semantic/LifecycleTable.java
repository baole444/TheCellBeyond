package scripting.transpiler.semantic;

import java.util.List;
import java.util.Map;
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

    /**
     * Script hook name to its engine target.
     */
    public static final Map<String, LifecycleHook> LifeCycle = Map.of(
            "_start", new LifecycleHook("onStart", List.of()),
            "_editor_start", new LifecycleHook("onEditorStart", List.of()),
            "_ready", new LifecycleHook("onReady", List.of()),
            "_update", new LifecycleHook("onUpdate", List.of("float")),
            "_editor_update", new LifecycleHook("onEditorUpdate", List.of("float")),
            "_physic_update", new LifecycleHook("onPhysicUpdate", List.of("float")),
            "_destroy", new LifecycleHook("onDestroy", List.of()),
            "_transform_dirty", new LifecycleHook("onTransformDirty", List.of())
    );

    private LifecycleTable() {}

    /**
     * Look up a lifecycle hook by its script method name.
     * @param scriptName the {@code _snake_case} method name
     * @return the hook, or empty when the name is not a lifecycle hook
     */
    public static Optional<LifecycleHook> find(String scriptName) {
        return Optional.ofNullable(LifeCycle.get(scriptName));
    }
}
