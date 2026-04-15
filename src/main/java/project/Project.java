package project;

import TheCellBeyond.InputAction;
import TheCellBeyond.InputKey;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import eventviewer.event.ProjectEvent;
import physic2d.PhysicLayer;
import render.Texture;
import render.texture.SpriteSheet;
import render.texture.TextureUnit;
import scripting.ScriptLoader;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import utility.AssetManager;
import utility.UnifiedPaths;
import utility.prefabrication.PrefabManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Project stores user's project data structure, resource import and paths to scene files.
 */
public class Project {
    private static ProjectData CurrentProject = null;
    private static String ProjectRoot = null;
    private static ProjectPreference preference = null;
    private static String _projectYmlPath = null;
    private static final ObjectMapper YAMLMapper = new ObjectMapper(new YAMLFactory()).rebuild().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
    private static final List<String> requiredDirs = List.of("assets", "prefabs", "scenes", "sheets", "scripts", "scripts-src");
    public static final String ProjectVersion = "1.2";

    /**
     * Check if there is a project with valid path loaded or not.
     * @return true if current project and project root not null
     */
    public static boolean loaded() {
        return CurrentProject != null && ProjectRoot != null;
    }

    public static void loadFromYaml(String path) {
        try {
            File projectFile = new File(path);
            CurrentProject = YAMLMapper.readValue(projectFile, ProjectData.class);
            ProjectRoot = UnifiedPaths.toRoot(path);
            UnifiedPaths.initialize(ProjectRoot);
            _projectYmlPath = path;
            if (CurrentProject == null) {
                System.err.println("Cannot load project file!");
                return;
            }
            preference = CurrentProject.project();
            sanctionRelativePath();
            checkAndAddRequiredDirs();
            loadScripts();
            EngineEventCallback.emit(CurrentProject, new EditorEvent(EditorEvent.Type.ProjectLoaded));
        } catch (JacksonIOException e) {
            System.err.println("Failed to load project file: " + e.getMessage());
        }
    }

    public static void saveToYaml(String path) {
        try {
            if (CurrentProject == null) return;
            YAMLMapper.writeValue(new File(path), CurrentProject);
            _projectYmlPath = path;
        } catch (JacksonIOException e) {
            System.err.println("Failed to save project file: " + e.getMessage());
        }
    }

    /**
     * Save the current project to disk at its original path.
     */
    public static void save() {
        if (_projectYmlPath == null) {
            System.err.println("No project path stored for auto-save");
            return;
        }
        saveToYaml(_projectYmlPath);
    }

    public static boolean createNewProject(Path newProjectRoot, ProjectPreference preference) {
        if (newProjectRoot == null) return false;
        String warning = "It is ill-advised to create multiple projects in same root directory.\n" +
                "If nested project is needed, consider create it in a sub-directory of the parent project.";
        if (ProjectRoot != null && newProjectRoot.equals(Path.of(ProjectRoot))) {
            System.err.println(warning);
            return false;
        }
        Path potentialProject = newProjectRoot.resolve("_project.yml");
        if (potentialProject.toFile().exists()) {
            System.err.println(warning);
            return false;
        }
        ProjectPreference newPref = preference;
        if (newPref == null) newPref = new ProjectPreference();
        ProjectData newProject = new ProjectData(ProjectVersion, newPref);
        try {
            YAMLMapper.writeValue(potentialProject.toFile(), newProject);
        } catch (JacksonIOException e) {
            System.err.println("Failed to create new project");
            return false;
        }
        for (String dir : requiredDirs) {
            Path toDir = newProjectRoot.resolve(dir);
            try {
                Files.createDirectories(toDir);
            } catch (IOException e) {
                System.err.println("Cannot create '" + dir + "' directory for the project");
            }
        }
        return true;
    }

    public static boolean updateProjectPreference(String name, int windowWidth, int windowHeight, boolean allowResize, boolean maintainAspectRatio, float textureGlobalScale, ClearColor clearColor, int physicFrameRate, RenderingSetting renderingSetting) {
        if (noProjectLoaded()) return false;
        ProjectPreference oldPref = preference;
        preference = new ProjectPreference(name, windowWidth, windowHeight, allowResize, maintainAspectRatio, textureGlobalScale, clearColor, physicFrameRate, renderingSetting);
        CurrentProject = new ProjectData(CurrentProject.version(),
                preference, CurrentProject.assets(),
                CurrentProject.sheets(), CurrentProject.scenes(),
                CurrentProject.inputActions(), CurrentProject.physicLayers(),
                CurrentProject.scriptScanDirs()
        );
        save();
        if (oldPref.renderingSetting().targetFrameRate() != preference.renderingSetting().targetFrameRate()) EngineEventCallback.emit(new ProjectEvent(ProjectEvent.Type.TargetFrameRateChanged));
        if (oldPref.renderingSetting().vsyncMode() != preference.renderingSetting().vsyncMode()) EngineEventCallback.emit(new ProjectEvent(ProjectEvent.Type.VsyncModeChanged));
        return true;
    }

    public static boolean addAsset(UUID key, ProjectAssetMap asset) {
        if (noProjectLoaded()) return false;

        Map<UUID, ProjectAssetMap> assets = CurrentProject.assets();
        if (assets == null) {
            assets = new HashMap<>();
            CurrentProject = new ProjectData(
                    CurrentProject.version(), CurrentProject.project(), assets,
                    CurrentProject.sheets(), CurrentProject.scenes(),
                    CurrentProject.inputActions(), CurrentProject.physicLayers(),
                    CurrentProject.scriptScanDirs()
            );
        }
        if (assets.containsKey(key)) {
            System.err.println("Asset with key '" + key + "' already exists");
            return false;
        }
        assets.put(key, asset);
        save();
        return true;
    }

    public static boolean updateAsset(UUID key, ProjectAssetMap asset) {
        if (noProjectLoaded()) return false;
        if (!CurrentProject.assets().containsKey(key)) {
            System.err.println("Asset with key '" + key + "' does not exist");
            return false;
        }
        CurrentProject.assets().put(key, asset);
        save();
        return true;
    }

    public static boolean removeAsset(UUID key) {
        if (noProjectLoaded()) return false;
        ProjectAssetMap removed = CurrentProject.assets().remove(key);
        if (removed == null) {
            System.err.println("Asset with key '" + key + "' does not exist");
            return false;
        }
        save();
        return true;
    }

    public static boolean addSheet(String category, String name, ProjectSheetMap sheet) {
        if (noProjectLoaded()) return false;
        Map<String, Map<String, ProjectSheetMap>> sheets = CurrentProject.sheets();
        if (sheets == null) {
            sheets = new HashMap<>();
            CurrentProject = new ProjectData(
                    CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), sheets, CurrentProject.scenes(),
                    CurrentProject.inputActions(), CurrentProject.physicLayers(),
                    CurrentProject.scriptScanDirs()
            );
        }
        Map<String, ProjectSheetMap> categorizedSheets = sheets.computeIfAbsent(category, _ -> new HashMap<>());
        if (categorizedSheets.containsKey(name)) {
            System.err.println("Sheet named '" + name + "' already exists in '" + category + "' category");
            return false;
        }
        categorizedSheets.put(name, sheet);
        save();
        return true;
    }

    public static boolean updateSheet(String category, String name, ProjectSheetMap sheet) {
        if (noProjectLoaded()) return false;
        Map<String, ProjectSheetMap> categorizedSheets = CurrentProject.sheets().get(category);
        if (categorizedSheets == null || !categorizedSheets.containsKey(name)) {
            System.err.println("Sheet named '" + name + "' does not exist in '" + category + "' category");
            return false;
        }
        categorizedSheets.put(name, sheet);
        save();
        return true;
    }

    public static boolean removeSheet(String category, String name) {
        if (noProjectLoaded()) return false;
        Map<String, ProjectSheetMap> categorizedSheets = CurrentProject.sheets().get(category);
        if (categorizedSheets == null) {
            System.err.println("Category '" + category + "' does not exist");
            return false;
        }
        ProjectSheetMap removed = categorizedSheets.remove(name);
        if (removed == null) {
            System.err.println("Sheet named '" + name + "' does not exist in '" + category + "' category");
            return false;
        }
        if (categorizedSheets.isEmpty()) CurrentProject.sheets().remove(category);
        save();
        return true;
    }

    public static boolean addScene(String key, ProjectSceneMap scene) {
        if (noProjectLoaded()) return false;
        Map<String, ProjectSceneMap> scenes = CurrentProject.scenes();
        if (scenes == null) {
            scenes = new HashMap<>();
            CurrentProject = new ProjectData(CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), CurrentProject.sheets(), scenes,
                    CurrentProject.inputActions(), CurrentProject.physicLayers(),
                    CurrentProject.scriptScanDirs()
            );
        }
        if (scenes.containsKey(key)) {
            System.err.println("Scene with key '" + key + "' already exists");
            return false;
        }
        scenes.put(key, scene);
        save();
        return true;
    }

    public static boolean updateScene(String key, ProjectSceneMap scene) {
        if (noProjectLoaded()) return false;
        if (key == null) return false;
        if (!CurrentProject.scenes().containsKey(key)) {
            System.err.println("Scene with key '" + key + "' does not exist");
            return false;
        }
        CurrentProject.scenes().put(key, scene);
        save();
        return true;
    }

    public static boolean removeScene(String key) {
        if (noProjectLoaded()) return false;
        ProjectSceneMap removed = CurrentProject.scenes().remove(key);
        if (removed == null) {
            System.err.println("Scene with key '" + key + "' does not exist");
            return false;
        }
        save();
        return true;
    }

    public static boolean addInputAction(String actionName, List<Set<InputKey>> keys) {
        if (noProjectLoaded()) return false;
        if (actionName == null || actionName.isBlank() || keys == null) return false;
        String name = actionName.trim();
        if (name.isEmpty()) return false;
        Map<String, InputAction> actions = CurrentProject.inputActions();
        if (actions == null) {
            actions = new HashMap<>();
            CurrentProject = new ProjectData(CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), CurrentProject.sheets(),
                    CurrentProject.scenes(), actions, CurrentProject.physicLayers(),
                    CurrentProject.scriptScanDirs()
            );
        }
        if (actions.containsKey(name)) {
            System.err.println("Input action with named '" + name + "' already exists");
            return false;
        }
        InputAction action = new InputAction(name, new ArrayList<>(keys));
        actions.put(name, action);
        save();
        return true;
    }

    public static boolean updateInputActionKey(String actionName, List<Set<InputKey>> keys) {
        if (noProjectLoaded()) return false;
        if (actionName == null || keys == null) return false;
        Map<String, InputAction> actions = CurrentProject.inputActions();
        InputAction action = actions.get(actionName);
        if (action == null) {
            System.err.println("Input action with name '" + actionName + "' does not exist");
            return false;
        }
        InputAction update = new InputAction(action.name(), keys);
        actions.put(actionName, update);
        save();
        return true;
    }

    public static boolean updateInputActionName(String oldActionName, String newActionName) {
        if (noProjectLoaded()) return false;
        if (oldActionName == null || newActionName == null || oldActionName.isBlank() || newActionName.isBlank()) return false;
        String oldName = oldActionName.trim();
        String newName = newActionName.trim();
        Map<String, InputAction> actions = CurrentProject.inputActions();
        if (!actions.containsKey(oldName) || actions.containsKey(newName)) return false;
        InputAction oldAction = actions.remove(oldName);
        if (oldAction == null) return false;
        InputAction newAction = new InputAction(newName, oldAction.keys());
        actions.put(newName, newAction);
        save();
        return true;
    }

    public static boolean removeInputAction(String actionName) {
        if (noProjectLoaded()) return false;
        if (actionName == null || actionName.isBlank()) return false;
        String name = actionName.trim();
        if (name.isEmpty()) return false;
        InputAction removed = CurrentProject.inputActions().remove(name);
        if (removed == null) {
            System.err.println("Input action with name '" + actionName + "' does not exist");
            return false;
        }
        save();
        return true;
    }

    public static String getPhysicLayerName(int layerIndex) {
        if (CurrentProject == null) return "Layer " + layerIndex;
        PhysicLayerName physicLayerName = CurrentProject.physicLayers();
        if (physicLayerName == null) {
            physicLayerName = new PhysicLayerName();
            CurrentProject = new ProjectData(CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), CurrentProject.sheets(), CurrentProject.scenes(),
                    CurrentProject.inputActions(), physicLayerName,
                    CurrentProject.scriptScanDirs()
            );
            save();
        }
        return CurrentProject.physicLayers().layerName(layerIndex);
    }

    public static boolean updatePhysicLayerName(int layerIndex, String newName) {
        if (noProjectLoaded()) return false;
        if (!PhysicLayer.isLayerIndexValid(layerIndex)) {
            System.err.println("Invalid layer index: " + layerIndex);
            return false;
        }
        if (newName == null || newName.isBlank()) return false;
        String name = newName.trim();
        if (name.isEmpty()) return false;
        PhysicLayerName update = CurrentProject.physicLayers().updateLayerName(layerIndex, name);
        CurrentProject = new ProjectData(CurrentProject.version(), CurrentProject.project(), CurrentProject.assets(),
                CurrentProject.sheets(), CurrentProject.scenes(),
                CurrentProject.inputActions(), update,
                CurrentProject.scriptScanDirs()
        );
        save();
        return true;
    }

    public static boolean addScriptScanDir(String relativeDir) {
        if (noProjectLoaded()) return false;
        if (relativeDir == null || relativeDir.isBlank()) return false;
        relativeDir = relativeDir.trim();
        List<String> dirs = CurrentProject.scriptScanDirs();
        if (dirs.contains(relativeDir)) {
            System.err.println("Script scan directory '" + relativeDir + "' already exists");
            return false;
        }
        dirs.add(relativeDir);
        save();
        loadScripts();
        return true;
    }

    public static boolean removeScriptScanDir(String relativeDir) {
        if (noProjectLoaded()) return false;
        if (relativeDir == null || relativeDir.isBlank()) return false;
        boolean removed = CurrentProject.scriptScanDirs().remove(relativeDir.trim());
        if (!removed) {
            System.err.println("Script scan directory '" + relativeDir + "' does not exist");
            return false;
        }
        save();
        loadScripts();
        return true;
    }

    private static void loadScripts() {
        if (CurrentProject == null || ProjectRoot == null) return;
        List<Path> resolvedPaths = CurrentProject.scriptScanDirs().stream()
                .map(dir -> Path.of(UnifiedPaths.resolveToAbsolute(ProjectRoot, dir)))
                .toList();
        ScriptLoader.load(resolvedPaths);
    }

    private static boolean noProjectLoaded() {
        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return true;
        }
        return false;
    }

    private static void sanctionRelativePath() {
        if (CurrentProject == null) return;
        boolean modified = false;
        for (Map.Entry<UUID, ProjectAssetMap> entry : new HashMap<>(CurrentProject.assets()).entrySet()) {
            String path = entry.getValue().path();
            String sanctioned = fixRelativePath(path);
            if (path.equals(sanctioned)) continue;
            modified = true;
            ProjectAssetMap fixed = new ProjectAssetMap(sanctioned, entry.getValue().sizeX(), entry.getValue().sizeY());
            updateAsset(entry.getKey(), fixed);
        }
        for (Map.Entry<String, Map<String, ProjectSheetMap>> category : new HashMap<>(CurrentProject.sheets()).entrySet()) {
            for (Map.Entry<String, ProjectSheetMap> sheet : new HashMap<>(category.getValue()).entrySet()) {
                String path = sheet.getValue().path();
                String sanctioned = fixRelativePath(path);
                if (path.equals(sanctioned)) return;
                modified = true;
                ProjectSheetMap current = sheet.getValue();
                ProjectSheetMap fixed = new ProjectSheetMap(sanctioned, current.numberOfSprite(),
                        current.spriteSizeX(), current.spriteSizeY(),
                        current.spriteSpacingX(), current.spriteSpacingY(),
                        current.spriteStartPosX(), current.spriteStartPosY()
                );
                updateSheet(category.getKey(), sheet.getKey(), fixed);
            }
        }
        for (Map.Entry<String, ProjectSceneMap> entry : new HashMap<>(CurrentProject.scenes()).entrySet()) {
            String path = entry.getValue().path();
            String sanctioned = fixRelativePath(path);
            if (!path.equals(sanctioned)) {
                modified = true;
                updateScene(entry.getKey(), new ProjectSceneMap(sanctioned));
            }
        }
        if (modified) System.out.println("Corrected current project's relative paths");
    }

    /**
     * Check and request to load declared resource from the project manifest.
     * The engine will skip loaded resource.
     */
    public static void loadProjectData() {
        if (CurrentProject == null) return;
        for (Map.Entry<String, Map<String, ProjectSheetMap>> categories : CurrentProject.sheets().entrySet()) {
            for (Map.Entry<String, ProjectSheetMap> sheets : categories.getValue().entrySet()) {
                ProjectSheetMap sheetMap = sheets.getValue();
                String projectPath = UnifiedPaths.ProjectPrefix + sheetMap.path();
                Texture texture = AssetManager.getTexture(AssetManager.loadTexture(projectPath));
                SpriteSheet sheet = new SpriteSheet(texture, sheetMap.spriteSizeX(), sheetMap.spriteSizeY(),
                        sheetMap.numberOfSprite(), sheetMap.spriteSpacingX(), sheetMap.spriteSpacingY(),
                        sheetMap.spriteStartPosX(), sheetMap.spriteStartPosY()
                );
                AssetManager.addSpriteSheet(projectPath, sheet);
            }
        }
        for (Map.Entry<UUID, ProjectAssetMap> entry : CurrentProject.assets().entrySet()) {
            ProjectAssetMap assetMap = entry.getValue();
            String projectPath = UnifiedPaths.ProjectPrefix + assetMap.path();
            Texture texture = AssetManager.getTexture(AssetManager.loadTexture(projectPath));
            TextureUnit unit = new TextureUnit(texture, assetMap.sizeX(), assetMap.sizeY());
            AssetManager.addTextureUnit(projectPath, unit);
        }
        PrefabManager.loadAllPrefabs();
    }

    private static String fixRelativePath(String path) {
        if (path == null) return null;
        if (path.startsWith("/")) return "." + path;
        return path;
    }

    private static void checkAndAddRequiredDirs() {
        if (CurrentProject == null || ProjectRoot == null) return;
        for (String dir : requiredDirs) {
            Path toDir = Path.of(UnifiedPaths.resolveToAbsolute(ProjectRoot, dir));
            if (Files.isDirectory(toDir)) continue;
            try {
                Files.createDirectories(toDir);
            } catch (IOException e) {
                System.err.println("Cannot create missing '" + dir + "' directory for the project");
            }
        }
    }

    public static List<String> getSceneNames() {
        if (CurrentProject == null) return List.of();
        return CurrentProject.scenes().keySet().stream().toList();
    }

    public static ProjectSceneMap getScene(String key) {
        if (CurrentProject == null) return null;
        return CurrentProject.scenes().get(key);
    }

    public static ProjectData currentProject() {
        return CurrentProject;
    }

    public static String projectRoot() {
        return ProjectRoot;
    }

    public static ProjectPreference preference() {
        return preference;
    }

    public static float getGameAspectRatio() {
        if (preference == null) return (float) 640 / 480;
        return (float) preference.gameWindowWidth() / preference.gameWindowHeight();
    }

    public static String projectYMLPath() {
        return _projectYmlPath;
    }

    public static List<String> scriptScanDirs() {
        if (CurrentProject == null) return List.of();
        return CurrentProject.scriptScanDirs();
    }

    public static void clear() {
        ScriptLoader.unload();
        CurrentProject = null;
        ProjectRoot = null;
        _projectYmlPath = null;
        preference = null;
    }
}
