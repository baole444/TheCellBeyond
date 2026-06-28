package editor.preference;

import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.MapType;
import tools.jackson.dataformat.yaml.YAMLFactory;
import utility.log.EngineLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class UserPreference {
    private static final EngineLog Logger = new EngineLog(UserPreference.class);
    private static final String Application = "TheCellBeyond";
    private static Path ConfigDir = null;
    private static final String RecentProjectFile = "recent_project";
    private static final String EditorPreferenceFile = "configs";
    private static final String EditorLayoutFile = "layout.ini";
    private static final ObjectMapper YAMLMapper = new ObjectMapper(new YAMLFactory()).rebuild().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final HashMap<UUID, RecentProject> recentProjects = new HashMap<>();
    private static EditorPreferences editorPreferences = new EditorPreferences();

    static {
        loadConfigDirectory();
        loadEditorPreferences();
        loadRecentProjects();
        cleanupEditorPreferenceBackups();
    }

    private static void loadConfigDirectory() {
        String OS = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        String dataDir;
        if (OS.contains("win")) dataDir = System.getenv("APPDATA");
        else if (OS.contains("mac")) dataDir = home + File.separator + "Library" + File.separator + "Application Support";
        else dataDir = home + File.separator + ".config";
        Path tcbDir = Path.of(dataDir, Application);
        try {
            Files.createDirectories(tcbDir);
        } catch (IOException e) {
            Logger.warning("Failed to create config preference directory: " + e.getMessage());
            tcbDir = Path.of(System.getProperty("user.dir"), "config");
            try {
                Files.createDirectories(tcbDir);
            } catch (IOException fallbackE) {
                Logger.warning("Cannot write to fallback config directory: " + fallbackE.getMessage());
                tcbDir = null;
            }
        }
        ConfigDir = tcbDir;
    }

    private static void loadRecentProjects() {
        if (ConfigDir == null) return;
        Path recents = ConfigDir.resolve(RecentProjectFile);
        if (!Files.exists(recents)) return;
        try {
            MapType mapType = YAMLMapper.getTypeFactory().constructMapType(HashMap.class, UUID.class, RecentProject.class);
            recentProjects.clear();
            recentProjects.putAll(YAMLMapper.readValue(recents.toFile(), mapType));
        } catch (JacksonIOException e) {
            Logger.warning("Failed to load recent projects: " + e.getMessage());
        }
    }

    private static void loadEditorPreferences() {
        if (ConfigDir == null) return;
        Path config = ConfigDir.resolve(EditorPreferenceFile);
        if (!Files.exists(config)) return;
        editorPreferences = PreferenceMigrator.migrate(config, YAMLMapper);
    }

    private static void cleanupEditorPreferenceBackups() {
        if (ConfigDir == null) return;
        PreferenceMigrator.pruneBackups(ConfigDir.resolve(EditorPreferenceFile));
    }

    public static String editorLayoutFilepath() {
        if (ConfigDir == null) return EditorLayoutFile;
        return ConfigDir.resolve(EditorLayoutFile).toString();
    }

    public static HashMap<UUID, RecentProject> recentProjects() {
        return new HashMap<>(recentProjects);
    }

    public static RecentProject recentProject(String path) {
        if (path == null) return null;
        for (RecentProject project : recentProjects.values()) {
            if (project.path().equals(path)) return project;
        }
        return null;
    }

    public static void updateRecentProject(RecentProject recentProject) {
        if (recentProject == null || recentProject.path() == null) return;
        for (Map.Entry<UUID, RecentProject> entry : recentProjects.entrySet()) {
            RecentProject project = entry.getValue();
            if (!project.path().equals(recentProject.path())) continue;
            entry.setValue(recentProject);
            saveRecentProjects();
            break;
        }
    }

    public static HashMap<UUID, RecentProject> reloadRecentProject() {
        loadRecentProjects();
        return recentProjects();
    }

    public static void addRecentProject(RecentProject recentProject) {
        if (recentProject == null) return;
        recentProjects.put(UUID.randomUUID(), recentProject);
        saveRecentProjects();
    }

    public static void updateRecentProject(UUID uuid, RecentProject recentProject) {
        if (uuid == null || recentProject == null) return;
        recentProjects.put(uuid, recentProject);
        saveRecentProjects();
    }

    public static void removeRecentProject(UUID uuid) {
        if (uuid == null) return;
        recentProjects.remove(uuid);
        saveRecentProjects();
    }

    private static void saveRecentProjects() {
        if (ConfigDir == null) return;
        Path recents = ConfigDir.resolve(RecentProjectFile);
        try {
            YAMLMapper.writerWithDefaultPrettyPrinter().writeValue(recents.toFile(), recentProjects);
        } catch (JacksonIOException e) {
            Logger.warning("Failed to save recent projects: " + e.getMessage());
        }
    }

    public static EditorPreferences preferences() {
        return editorPreferences;
    }

    public static void updatePreferences(EditorPreferences newPreferences) {
        if (newPreferences == null) return;
        editorPreferences = newPreferences;
        savePreferences();
    }

    public static EditorPreferences reloadPreferences() {
        loadEditorPreferences();
        return preferences();
    }

    private static void savePreferences() {
        if (ConfigDir == null) return;
        Path config = ConfigDir.resolve(EditorPreferenceFile);
        try {
            MapType mapType = YAMLMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
            Map<String, Object> data = YAMLMapper.convertValue(editorPreferences, mapType);
            data.put(EditorPreferences.VersionKey, EditorPreferences.SaveVersion);
            YAMLMapper.writerWithDefaultPrettyPrinter().writeValue(config.toFile(), data);
        } catch (JacksonIOException | IllegalArgumentException e) {
            Logger.warning("Failed to save editor preferences: " + e.getMessage());
        }
    }
}
