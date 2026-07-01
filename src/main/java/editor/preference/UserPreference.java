package editor.preference;

import scripting.builder.jdk.JDKInstallation;
import scripting.builder.jdk.JDKManager;
import scripting.builder.jdk.download.JDKDownloader;
import scripting.builder.jdk.download.JDKProvider;
import tools.jackson.core.JacksonException;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class UserPreference {
    private static final EngineLog Logger = new EngineLog(UserPreference.class);
    private static final String Application = "TheCellBeyond";
    private static Path ConfigDir = null;
    private static final String RecentProjectFile = "recent_project";
    private static final String EditorPreferenceFile = "configs";
    private static final String JDKRegistryFile = "jdk_registry";
    private static final String JDKInstallDir = "jdks";
    private static final String EditorLayoutFile = "layout.ini";
    private static final ObjectMapper YAMLMapper = new ObjectMapper(new YAMLFactory()).rebuild().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final HashMap<UUID, RecentProject> recentProjects = new HashMap<>();
    private static EditorPreferences editorPreferences = new EditorPreferences();
    private static JDKRegistry jdkRegistry = new JDKRegistry();

    static {
        loadConfigDirectory();
        loadEditorPreferences();
        loadRecentProjects();
        loadJDKRegistry();
        scanForJDKs();
        cleanupEditorPreferenceBackups();
    }

    private UserPreference() {}

    /**
     * Get the currently cached entries of JDK installations.
     * @return the current JDK registry
     */
    public static JDKRegistry jdkRegistry() {
        return jdkRegistry;
    }

    /**
     * Update the currently cached entries of JDK installation and save to disk.
     * @param registry the new registry to cache with
     */
    public static void updateJDKRegistry(JDKRegistry registry) {
        if (registry == null) return;
        jdkRegistry = registry;
        saveJDKRegistry();
    }

    /**
     * Get the path to the Editor's layout config file.
     * @return the path to the layout.ini file
     */
    public static String editorLayoutFilepath() {
        if (ConfigDir == null) return EditorLayoutFile;
        return ConfigDir.resolve(EditorLayoutFile).toString();
    }

    /**
     * Get the directory where the downloaded JDKs are stored and scanned from.
     * @return the managed JDK install directory, or null when there is no config file
     */
    public static Path jdkInstallDir() {
        if (ConfigDir == null) return null;
        return ConfigDir.resolve(JDKInstallDir);
    }

    /**
     * Run the scan for JDK installation again.
     * @return a future completing when the scan finished
     */
    public static CompletableFuture<Void> rescanJDKs() {
        return JDKManager.rescan();
    }

    /**
     * Get the current selected JDK installation detail record.
     * @return the currently selected {@link JDKInstallation}
     */
    public static JDKInstallation selectedJDK() {
        String selectedHome = jdkRegistry.selection();
        if (JDKRegistry.JavaHomeSelection.equals(selectedHome)) return JDKManager.javaHome();
        Path target = Path.of(selectedHome).toAbsolutePath().normalize();
        JDKInstallation match = findByHome(JDKManager.userAdded(), target);
        return match != null ? match :  findByHome(JDKManager.detected(), target);
    }

    /**
     * Get the currently selected JDK installation home directory.
     * @return the path to the JDK home, or null if none selected
     */
    public static Path selectedJDKHome() {
        JDKInstallation jdk = selectedJDK();
        return jdk != null && jdk.valid() ? jdk.home() : null;
    }

    /**
     * Save the user selected JDK to the given path to the JDK's home directory.
     * @param home the JDK home directory to save
     */
    public static void selectJDK(Path home) {
        if (home == null) return;
        updateJDKRegistry(new JDKRegistry(jdkRegistry.jdks(), home.toAbsolutePath().normalize().toString()));
    }

    /**
     * Save the user selected JDK to the given aboslute home path.
     * @param home the JDK home directory to save, use {@value JDKRegistry#JavaHomeSelection} for java home
     */
    public static void selectJDK(String home) {
        if (home == null || home.isBlank()) return;
        updateJDKRegistry(new JDKRegistry(jdkRegistry.jdks(), home));
    }

    /**
     * Inspect the given directory for a usable JDK to register and select it.
     * Invalid JDK will not be registered or selected.
     * @param directory the directory to add
     * @return a future of the inspection result
     */
    public static CompletableFuture<JDKInstallation> addJDK(Path directory) {
        return JDKManager.add(directory).thenApply(jdk -> {
            if (!jdk.valid()) return jdk;
            String home = jdk.home().toAbsolutePath().normalize().toString();
            List<String> paths = new ArrayList<>(jdkRegistry.jdks());
            if (!paths.contains(home)) paths.add(home);
            updateJDKRegistry(new JDKRegistry(paths, home));
            return jdk;
        });
    }

    public static CompletableFuture<JDKInstallation> downloadJDK(int version, JDKProvider provider) {
        Path dir = jdkInstallDir();
        if (dir == null) return CompletableFuture.completedFuture(null);
        return JDKDownloader.download(dir, version, provider).thenCompose(home -> home == null ? CompletableFuture.completedFuture(null) : addJDK(home));
    }

    /**
     * Remove a JDK installation that was added by the user. In the case of removing the selected installation,
     * the system will fall back to JAVA_HOME if available.
     * @param home the home path to remove
     */
    public static void removeJDK(String home) {
        if (home == null) return;
        JDKManager.remove(Path.of(home));
        List<String> paths = new ArrayList<>(jdkRegistry.jdks());
        if (!paths.remove(home)) return;
        String selection = home.equals(jdkRegistry.selection()) ? JDKRegistry.JavaHomeSelection : jdkRegistry.selection();
        updateJDKRegistry(new JDKRegistry(paths, selection));
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

    /**
     * Update information for a recently opened project entries with the same path as the given entry.
     * @param recentProject the entry's new information record to update with
     */
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

    /**
     * Reload the recently opened project entries from disk
     * @return the map of the refreshed entries
     */
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

    public static EditorPreferences preferences() {
        return editorPreferences;
    }

    public static void updatePreferences(EditorPreferences newPreferences) {
        if (newPreferences == null) return;
        editorPreferences = newPreferences;
        savePreferences();
    }

    /**
     * Reload the editor preferences from disk.
     * @return the refreshed preference record
     */
    public static EditorPreferences reloadPreferences() {
        loadEditorPreferences();
        return preferences();
    }

    private static void scanForJDKs() {
        List<Path> paths = new ArrayList<>();
        jdkRegistry.jdks().forEach(p -> paths.add(Path.of(p)));
        JDKManager.scan(paths);
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

    private static void loadJDKRegistry() {
        if (ConfigDir == null) return;
        Path file = ConfigDir.resolve(JDKRegistryFile);
        if (!Files.exists(file)) return;
        try {
            jdkRegistry = YAMLMapper.readValue(file.toFile(), JDKRegistry.class);
        } catch (JacksonException | IllegalArgumentException e) {
            Logger.warning("Failed to load JDK registry: " + e.getMessage());
        }
    }

    private static JDKInstallation findByHome(List<JDKInstallation> jdks, Path normalizedHome) {
        for (JDKInstallation jdk : jdks) {
            Path home = jdk.home();
            if (home != null && home.toAbsolutePath().normalize().equals(normalizedHome)) return jdk;
        }
        return null;
    }

    private static void saveJDKRegistry() {
        if (ConfigDir == null) return;
        Path file = ConfigDir.resolve(JDKRegistryFile);
        try {
            YAMLMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), jdkRegistry);
        } catch (JacksonException e) {
            Logger.warning("Failed to save JDK registry: " + e.getMessage());
        }
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
}
