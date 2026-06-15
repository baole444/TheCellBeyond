package scripting.transpiler.sematic;

/**
 * ClassRegistration enums allow tracking the lineage that script classes extending from.
 * This control the emission for {@code @RegisterGameObject} and {@code @RegisterComponent} annotation during code generation.
 */
public enum ClassRegistration {
    /**
     * Lineage reaches {@link TheCellBeyond.GameObject}, emit {@code @RegisterGameObject}.
     */
    GameObject,
    /**
     * Lineage reaches {@link components.Component}, emit {@code @RegisterComponent}.
     */
    Component,
    /**
     * A normal class, often created for data or use as helper class, no registration annotation.
     */
    None
}

