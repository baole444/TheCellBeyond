package TheCellBeyond.internal;

import scene.Scene;
import scene.SceneLoader;

/**
 * The EngineHost interface allows defining driver to operate the engine.
 * A host can be registered via {@link Engine#host(EngineHost)} before the window runs.
 * </p>
 * By default, all hooks in the host have no behaviour or tooling, and it always accepts application's close request.
 */
public interface EngineHost {
    /**
     * Called once after window and its GL context is created, before rendering started.
     * @param windowPtr the pointer of the created window
     */
    default void initialize(long windowPtr) {}

    /**
     * Setup and load the host's designated project.
     * <p>
     * Called once after renderer is ready.
     */
    default void boot() {}

    /**
     * Called every frame after the scene has been rendered.
     * @param dt the frame delta time
     * @param scene the current scene, or null if there is none
     */
    default void frame(float dt, Scene scene) {}

    /**
     * Called when the window receive a close request.
     * @return true to let the window close
     */
    default boolean closeRequested() {
        return true;
    }

    /**
     * Called once while the window is shutting down, before it is destroyed.
     */
    default void dispose() {}

    /**
     * Create the scene loader used for edit mode.
     * @return a new editor scene loader, or null if the host has no edit mode
     */
    default SceneLoader editSceneLoader() {
        return null;
    }
}
