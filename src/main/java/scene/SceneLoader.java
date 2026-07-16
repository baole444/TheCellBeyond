package scene;

import scripting.API;

/**
 * Scene loader is used by the {@link Scene} to execute logic at important point in scene life cycle.
 */
@API
public abstract class SceneLoader {
    /**
     * Called on scene finish loading resource.
     * @param scene the scene that call this method
     */
    public abstract void loadResource(Scene scene);

    /**
     * Called on scene finish starting.
     * @param scene the scene that call this method
     */
    public abstract void onSceneStarted(Scene scene);

    /**
     * Called on scene finish clean up.
     */
    public abstract void onSceneEnd();
}
