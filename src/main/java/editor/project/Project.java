package editor.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import render.Texture;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.PathResolver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
    private static ProjectData CurrentProject = null;
    private static String ProjectRoot = null;
    private static ProjectPreference preference = null;
    private static String _projectYmlPath = null;
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

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
                            CurrentProject.sheets(), CurrentProject.scenes()
                    );
                    save();
                }
                preference = CurrentProject.project();

                sanctionRelativePath();
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

    public static List<String> getSceneNames() {
        if (CurrentProject == null) return List.of();

        return CurrentProject.scenes().keySet().stream().toList();
    }

    public static ProjectSceneMap getScene(String key) {
        if (CurrentProject == null || CurrentProject.scenes() == null) return null;

        return CurrentProject.scenes().get(key);
    }

    public static boolean updateProjectPreference(String name, int windowWidth, int windowHeight, boolean allowResize, boolean maintainAspectRatio) {
        preference = new ProjectPreference(name, windowWidth, windowHeight, allowResize, maintainAspectRatio);

        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        CurrentProject = new ProjectData(CurrentProject.version(),
                preference, CurrentProject.assets(),
                CurrentProject.sheets(), CurrentProject.scenes()
        );

        save();
        return true;
    }

    public static boolean addAsset(String key, ProjectAssetMap asset) {
        if (CurrentProject == null) {
            System.err.println("No project loaded");
            return false;
        }

        Map<String, ProjectAssetMap> assets = CurrentProject.assets();
        if (assets == null) {
            assets = new HashMap<>();
            CurrentProject = new ProjectData(
                    CurrentProject.version(), CurrentProject.project(), assets,
                    CurrentProject.sheets(), CurrentProject.scenes()
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

    public static boolean updateAsset(String key, ProjectAssetMap asset) {
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

    public static boolean removeAsset(String key) {
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
                    CurrentProject.assets(), sheets, CurrentProject.scenes()
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
                    CurrentProject.assets(), CurrentProject.sheets(), scenes
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

    private static void sanctionRelativePath() {
        if (CurrentProject == null) return;

        boolean modified = false;

        if (CurrentProject.assets() != null) {
            for (Map.Entry<String, ProjectAssetMap> entry : new HashMap<>(CurrentProject.assets()).entrySet()) {
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
            for (Map.Entry<String, ProjectAssetMap> entry : CurrentProject.assets().entrySet()) {
                ProjectAssetMap assetMap = entry.getValue();
                String projectPath = "project://" + assetMap.path();

                Texture texture = AssetsPool.loadTexture(projectPath);
                SpriteSheet sheet = new SpriteSheet(texture, assetMap.sizeX(), assetMap.sizeY(), 1, 0);

                AssetsPool.addSpriteSheet(projectPath, sheet);
            }
        }
    }

    private static String fixRelativePath(String path) {
        if (path == null) return null;

        if (path.startsWith("/")) return "." + path;

        return path;
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
}
