package render.texture;

import project.Project;
import project.ProjectAsset;
import scripting.API;
import utility.AssetManager;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SingleSprites is a collection of static methods for resolving single sprites imported from the project manifest, based on their categories and names.
 * <p>
 * Additionally, sprites can also be registered and queried via script dynamically during runtime, using the sprites' name and category.
 * </p>
 * All sprites imported from project manifest are permanent entries on registered single sprites.
 * @see SpriteSheets Resolve imported sprite sheets
 */
@API
public final class SingleSprites {
    private static final EngineLog Logger = new EngineLog(SingleSprites.class);
    private static final Map<String, SingleSprite> runtimeSprites = new ConcurrentHashMap<>();

    private SingleSprites() {}

    /**
     * Get a registered single sprite.
     * @param category the category of the sprite
     * @param name the name of the sprite
     * @return the registered sprite, or null if none was registered under the given category and name
     */
    public static SingleSprite get(String category, String name) {
        SingleSprite runtime = runtimeSprites.get(key(category, name));
        if (runtime != null) return runtime;
        ProjectAsset imported = imported(category, name);
        return imported == null ? null : AssetManager.getSingleSprite(imported.metadata());
    }

    /**
     * Check if a single sprite is registered or not.
     * @param category the category of the sprite
     * @param name the name of the sprite
     * @return true if registered
     */
    public static boolean has(String category, String name) {
        return get(category, name) != null;
    }

    /**
     * Register a single sprite built during runtime under the given name and category.
     * <p>
     * If the name collided with a sprite of the same category on the project manifest, registration is rejected.
     * @param category the category for the sprite
     * @param name the name for the sprite
     * @param singleSprite the sprite to register with
     * @return true if registered successfully, or false if invalid category / name or name already taken
     * @apiNote Single sprites registered like this only last during runtime, or until an explicit call on {@link #clear()} or {@link #unregister(String, String)} happened.
     */
    public static boolean register(String category, String name, SingleSprite singleSprite) {
        if (singleSprite == null || UnifiedPaths.invalidMetadata(category) || UnifiedPaths.invalidMetadata(name)) return false;
        if (imported(category, name) != null) {
            Logger.warning(String.format("Cannot register single sprite '%s/%s': existing import of the same name already registered", category, name));
            return false;
        }
        if (runtimeSprites.putIfAbsent(key(category, name), singleSprite) == null) return true;
        Logger.warning(String.format("Single sprite named '%s' already registered in '%s' category", name, category));
        return false;
    }

    /**
     * Forget a single sprite that was registered at runtime. Imports from project manifest are not affected.
     * @param category the category of the sprite
     * @param name the name of the sprite
     * @return true if removed successfully
     */
    public static boolean unregister(String category, String name) {
        return runtimeSprites.remove(key(category, name)) != null;
    }

    /**
     * Forget all single sprites that was registered at runtime. Imports from project manifest are not affected.
     */
    public static void clear() {
        runtimeSprites.clear();
    }

    private static ProjectAsset imported(String category, String name) {
        if (!Project.loaded()) return null;
        Map<String, ProjectAsset> categorized = Project.currentProject().assets().get(category);
        return categorized == null ? null : categorized.get(name);
    }

    private static String key(String category, String name) {
        return category + "/" + name;
    }
}
