package render.texture;

import project.Project;
import project.ProjectSheet;
import scripting.API;
import utility.AssetManager;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpriteSheets is a collection of static methods for resolving sprite sheets imported from the project manifest, based on their categories and names.
 * <p>
 * Additionally, sheets can also be registered and queried via script dynamically during runtime, using the sheets' name and category.
 * </p>
 * All sheets imported from project manifest are permanent entries on registered sheets.
 * @see SingleSprites Resolve imported single sprites
 */
@API
public final class SpriteSheets {
    private static final EngineLog Logger = new EngineLog(SpriteSheets.class);
    private static final Map<String, SpriteSheet> runtimeSheets = new ConcurrentHashMap<>();

    private SpriteSheets() {}

    /**
     * Get a registered sprite sheet.
     * @param category the category of the sheet
     * @param name the name of the sheet
     * @return the registered sheet, or null if none was registered under the given category and name
     */
    public static SpriteSheet get(String category, String name) {
        SpriteSheet runtime = runtimeSheets.get(key(category, name));
        if (runtime != null) return runtime;
        ProjectSheet imported = imported(category, name);
        return imported == null ? null : AssetManager.getSpriteSheet(imported.metadata());
    }

    /**
     * Check if a sprite sheet is registered or not.
     * @param category the category of the sheet
     * @param name the name of the sheet
     * @return true if registered
     */
    public static boolean has(String category, String name) {
        return get(category, name) != null;
    }

    /**
     * Register a sprite sheet built during runtime under the given name and category.
     * <p>
     * If the name collided with a sheet of the same category on the project manifest, registration is rejected.
     * @param category the category for the sheet
     * @param name the name for the sheet
     * @param sheet the sheet to register with
     * @return true if registered successfully, or false if invalid category / name or name already taken
     * @apiNote Sprite sheets registered like this only last during runtime, or until an explicit call on {@link #clear()} or {@link #unregister(String, String)} happened.
     */
    public static boolean register(String category, String name, SpriteSheet sheet) {
        if (sheet == null || UnifiedPaths.invalidMetadata(category) || UnifiedPaths.invalidMetadata(name)) return false;
        if (imported(category, name) != null) {
            Logger.warning(String.format("Cannot register sprite sheet '%s/%s': existing import of the same name already registered", category, name));
            return false;
        }
        if (runtimeSheets.putIfAbsent(key(category, name), sheet) == null) return true;
        Logger.warning(String.format("Sprite sheet named '%s' already registered in '%s' category", name, category));
        return false;
    }

    /**
     * Forget a sprite sheet that was registered at runtime. Imports from project manifest are not affected.
     * @param category the category of the sheet
     * @param name the name of the sheet
     * @return true if removed successfully
     */
    public static boolean unregister(String category, String name) {
        return runtimeSheets.remove(key(category, name)) != null;
    }

    /**
     * Forget all sprite sheets that was registered at runtime. Imports from project manifest are not affected.
     */
    public static void clear() {
        runtimeSheets.clear();
    }

    private static ProjectSheet imported(String category, String name) {
        if (!Project.loaded()) return null;
        Map<String, ProjectSheet> categorized = Project.currentProject().sheets().get(category);
        return categorized == null ? null : categorized.get(name);
    }

    private static String key(String category, String name) {
        return category + "/" + name;
    }
}
