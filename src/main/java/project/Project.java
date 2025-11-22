package project;

import TheCellBeyond.InputAction;
import TheCellBeyond.InputKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import physic2d.PhysicLayer;
import render.Texture;
import render.texture.SpriteSheet;
import render.texture.TextureUnit;
import utility.AssetsPool;
import utility.PathResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Project {
    private static ProjectData CurrentProject = null;
    private static String ProjectRoot = null;
    private static ProjectPreference preference = null;
    private static String _projectYmlPath = null;
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final List<String> requiredDirs = List.of("assets", "prefabs", "scenes", "sheets");
    public static final String PROJECT_VERSION = "0.1";

    public static ProjectData loadFromYaml(String path) {
        try {
            File projectFile = new File(path);

            CurrentProject = YAML_MAPPER.readValue(projectFile, ProjectData.class);
            ProjectRoot = PathResolver.toRoot(path);
            PathResolver.initialize(ProjectRoot);
            _projectYmlPath = path;

            if (CurrentProject != null) {
                if (CurrentProject.project() == null) {
                    System.err.println("Project preference is missing, generating new preference...");
                    CurrentProject = new ProjectData(CurrentProject.version(),
                            new ProjectPreference(), CurrentProject.assets(),
                            CurrentProject.sheets(), CurrentProject.scenes(),
                            currentProject().inputActions(),
                            currentProject().physicLayers()
                    );
                    save();
                }
                preference = CurrentProject.project();

                sanctionRelativePath();
                sanctionPreference();
                checkAndAddRequiredDirs();
            }

            return CurrentProject;
        } catch (IOException e) {
            System.err.println("Failed to load project file: " + e.getMessage());
            return null;
        }
    }

    public static void saveToYaml(String path) {
        try {
            if (CurrentProject != null) {
                YAML_MAPPER.writeValue(new File(path), CurrentProject);
                _projectYmlPath = path;
            }
        } catch (IOException e) {
            System.err.println("Failed to save project file: " + e.getMessage());
        }
    }

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

        ProjectData newProject = new ProjectData(PROJECT_VERSION, newPref);

        try {
            YAML_MAPPER.writeValue(potentialProject.toFile(), newProject);
        } catch (IOException e) {
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

    public static List<String> getSceneNames() {
        if (CurrentProject == null) return List.of();

        return CurrentProject.scenes().keySet().stream().toList();
    }

    public static ProjectSceneMap getScene(String key) {
        if (CurrentProject == null || CurrentProject.scenes() == null) return null;

        return CurrentProject.scenes().get(key);
    }

    public static boolean updateProjectPreference(String name, int windowWidth, int windowHeight, boolean allowResize, boolean maintainAspectRatio, float textureGlobalScale) {
        int w = Math.max(1, windowWidth);
        int h = Math.max(1, windowHeight);
        float scale = Math.max(0.01f, textureGlobalScale);

        preference = new ProjectPreference(name, w, h, allowResize, maintainAspectRatio, scale);

        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        CurrentProject = new ProjectData(CurrentProject.version(),
                preference, CurrentProject.assets(),
                CurrentProject.sheets(), CurrentProject.scenes(),
                CurrentProject.inputActions(), CurrentProject.physicLayers()
        );

        save();
        return true;
    }

    public static boolean addAsset(UUID key, ProjectAssetMap asset) {
        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        Map<UUID, ProjectAssetMap> assets = CurrentProject.assets();
        if (assets == null) {
            assets = new HashMap<>();
            CurrentProject = new ProjectData(
                    CurrentProject.version(), CurrentProject.project(), assets,
                    CurrentProject.sheets(), CurrentProject.scenes(),
                    CurrentProject.inputActions(), CurrentProject.physicLayers()
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
        if (CurrentProject == null || CurrentProject.assets() == null) {
            System.err.println("No project or assets loaded");
            return false;
        }

        if (!CurrentProject.assets().containsKey(key)) {
            System.err.println("Asset with key '" + key + "' does not exist");
            return false;
        }

        CurrentProject.assets().put(key, asset);
        save();
        return true;
    }

    public static boolean removeAsset(UUID key) {
        if (CurrentProject == null || CurrentProject.assets() == null) {
            System.err.println("No project or assets loaded");
            return false;
        }

        ProjectAssetMap removed = CurrentProject.assets().remove(key);
        if (removed == null) {
            System.err.println("Asset with key '" + key + "' does not exist");
            return false;
        }

        save();
        return true;
    }

    public static boolean addSheet(String category, String name, ProjectSheetMap sheet) {
        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        Map<String, Map<String, ProjectSheetMap>> sheets = CurrentProject.sheets();
        if (sheets == null) {
            sheets = new HashMap<>();
            CurrentProject = new ProjectData(
                    CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), sheets, CurrentProject.scenes(),
                    CurrentProject.inputActions(), CurrentProject.physicLayers()
            );
        }

        Map<String, ProjectSheetMap> categorizedSheets = sheets.computeIfAbsent(category, k -> new HashMap<>());

        if (categorizedSheets.containsKey(name)) {
            System.err.println("Sheet named '" + name + "' already exists in '" + category + "' category");
            return false;
        }

        categorizedSheets.put(name, sheet);
        save();
        return true;
    }

    public static boolean updateSheet(String category, String name, ProjectSheetMap sheet) {
        if (CurrentProject == null || CurrentProject.sheets() == null) {
            System.err.println("No project or sheets loaded");
            return false;
        }

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
        if (CurrentProject == null || CurrentProject.sheets() == null) {
            System.err.println("No project or sheets loaded");
            return false;
        }

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
        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        Map<String, ProjectSceneMap> scenes = CurrentProject.scenes();
        if (scenes == null) {
            scenes = new HashMap<>();
            CurrentProject = new ProjectData(CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), CurrentProject.sheets(), scenes,
                    CurrentProject.inputActions(), CurrentProject.physicLayers()
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
        if (CurrentProject == null || CurrentProject.scenes() == null) {
            System.err.println("No project or scenes loaded");
            return false;
        }

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
        if (CurrentProject == null || CurrentProject.scenes() == null) {
            System.err.println("No project or scenes loaded");
            return false;
        }

        ProjectSceneMap removed = CurrentProject.scenes().remove(key);
        if (removed == null) {
            System.err.println("Scene with key '" + key + "' does not exist");
            return false;
        }

        save();
        return true;
    }

    public static boolean addInputAction(String actionName, List<Set<InputKey>> keys) {
        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        if (actionName == null || actionName.isBlank() || keys == null) return false;
        String name = actionName.trim();
        if (name.isEmpty()) return false;

        Map<String, InputAction> actions = CurrentProject.inputActions();
        if (actions == null) {
            actions = new HashMap<>();
            CurrentProject = new ProjectData(CurrentProject.version(), CurrentProject.project(),
                    CurrentProject.assets(), CurrentProject.sheets(),
                    CurrentProject.scenes(), actions, CurrentProject.physicLayers()
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
        if (CurrentProject == null || CurrentProject.inputActions() == null) {
            System.err.println("No project or input actions loaded");
            return false;
        }

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
        if (CurrentProject == null || CurrentProject.inputActions() == null) {
            System.err.println("No project or input actions loaded");
            return false;
        }

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
        if (CurrentProject == null || CurrentProject.inputActions() == null) {
            System.err.println("No project or input actions loaded");
            return false;
        }

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
                    CurrentProject.inputActions(), physicLayerName);
            save();
        }

        return CurrentProject.physicLayers().layerName(layerIndex);
    }

    public static boolean updatePhysicLayerName(int layerIndex, String newName) {
        if (CurrentProject == null || CurrentProject.physicLayers() == null) {
            System.err.println("No project or physic layer names loaded");
            return false;
        }

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
                CurrentProject.inputActions(), update);

        save();
        return true;
    }

    private static void sanctionRelativePath() {
        if (CurrentProject == null) return;

        boolean modified = false;

        if (CurrentProject.assets() != null) {
            for (Map.Entry<UUID, ProjectAssetMap> entry : new HashMap<>(CurrentProject.assets()).entrySet()) {
                String path = entry.getValue().path();
                String sanctioned = fixRelativePath(path);
                if (!path.equals(sanctioned)) {
                    modified = true;
                    ProjectAssetMap fixed = new ProjectAssetMap(sanctioned, entry.getValue().sizeX(), entry.getValue().sizeY());
                    updateAsset(entry.getKey(), fixed);
                }
            }
        }

        if (CurrentProject.sheets() != null) {
            for (Map.Entry<String, Map<String, ProjectSheetMap>> category : new HashMap<>(CurrentProject.sheets()).entrySet()) {
                for (Map.Entry<String, ProjectSheetMap> sheet : new HashMap<>(category.getValue()).entrySet()) {
                    String path = sheet.getValue().path();
                    String sanctioned = fixRelativePath(path);
                    if (!path.equals(sanctioned)) {
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
            }
        }

        if (CurrentProject.scenes() != null) {
            for (Map.Entry<String, ProjectSceneMap> entry : new HashMap<>(CurrentProject.scenes()).entrySet()) {
                String path = entry.getValue().path();
                String sanctioned = fixRelativePath(path);
                if (!path.equals(sanctioned)) {
                    modified = true;
                    updateScene(entry.getKey(), new ProjectSceneMap(sanctioned));
                }
            }
        }

        if (modified) {
            System.out.println("Corrected current project's relative paths");
        }
    }

    private static void sanctionPreference() {
        if (CurrentProject == null || preference == null) return;

        int w = Math.max(1, preference.gameWindowWidth());
        int h = Math.max(1, preference.gameWindowHeight());
        float scale = Math.max(0.01f, preference.textureGlobalScale());

        preference = new ProjectPreference(preference.name(), w, h, preference.allowResize(), preference.maintainAspectRatio(), scale);

        CurrentProject = new ProjectData(CurrentProject.version(),
                preference, CurrentProject.assets(),
                CurrentProject.sheets(), CurrentProject.scenes(),
                CurrentProject.inputActions(), CurrentProject.physicLayers()
        );

        save();
    }

    public static void loadProjectData() {
        if (CurrentProject == null) return;

        if (CurrentProject.sheets() != null) {
            for (Map.Entry<String, Map<String, ProjectSheetMap>> categories : CurrentProject.sheets().entrySet()) {
                for (Map.Entry<String, ProjectSheetMap> sheets : categories.getValue().entrySet()) {
                    ProjectSheetMap sheetMap = sheets.getValue();
                    String projectPath = "project://" + sheetMap.path();

                    Texture texture = AssetsPool.loadTexture(projectPath);
                    SpriteSheet sheet = new SpriteSheet(texture, sheetMap.spriteSizeX(), sheetMap.spriteSizeY(),
                            sheetMap.numberOfSprite(), sheetMap.spriteSpacingX(), sheetMap.spriteSpacingY(),
                            sheetMap.spriteStartPosX(), sheetMap.spriteStartPosY()
                    );

                    AssetsPool.addSpriteSheet(projectPath, sheet);
                }
            }
        }

        if (CurrentProject.assets() != null) {
            for (Map.Entry<UUID, ProjectAssetMap> entry : CurrentProject.assets().entrySet()) {
                ProjectAssetMap assetMap = entry.getValue();
                String projectPath = "project://" + assetMap.path();

                Texture texture = AssetsPool.loadTexture(projectPath);
                TextureUnit unit = new TextureUnit(texture, assetMap.sizeX(), assetMap.sizeY());

                AssetsPool.addTextureUnit(projectPath, unit);
            }
        }
    }

    private static String fixRelativePath(String path) {
        if (path == null) return null;

        if (path.startsWith("/")) return "." + path;

        return path;
    }

    private static void checkAndAddRequiredDirs() {
        if (CurrentProject == null || ProjectRoot == null) return;

        for (String dir : requiredDirs) {
            Path toDir = Path.of(PathResolver.resolveToAbsolute(ProjectRoot, dir));
            if (!Files.isDirectory(toDir)) {
                try {
                    Files.createDirectories(toDir);
                } catch (IOException e) {
                    System.err.println("Cannot create missing '" + dir + "' directory for the project");
                }
            }
        }
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

    public static void clear() {
        CurrentProject = null;
        ProjectRoot = null;
        _projectYmlPath = null;
        preference = null;
    }
}
