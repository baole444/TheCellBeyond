package editor.preference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class UserPreference {
    private static final String APPLICATION = "TheCellBeyond";
    private static Path CONFIG_DIR = null;
    private static final String RECENT_PROJECT_FILE = "recent_project";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final HashMap<UUID, RecentProject> recentProjects = new HashMap<>();

    static {
        loadConfigDirectory();
        loadRecentProjects();
    }

    private static void loadConfigDirectory() {
        String OS = System.getProperty("os.name");

        String dataDir = OS.contains("win") ? System.getenv("APPDATA") :
                System.getProperty("user.home") + File.separator + ".config";

        Path tcbDir = Path.of(dataDir, APPLICATION);

        try {
            Files.createDirectories(tcbDir);
        } catch (IOException e) {
            System.err.println("Failed to create config preference directory: " + e.getMessage());
            tcbDir = Path.of(System.getProperty("user.dir"), "config");
            try {
                Files.createDirectories(tcbDir);
            } catch (IOException fallbackE) {
                System.err.println("Cannot write to fallback config directory: " + fallbackE.getMessage());
                tcbDir = null;
            }
        }

        CONFIG_DIR = tcbDir;
    }

    private static void loadRecentProjects() {
        if (CONFIG_DIR == null) return;

        Path recents = CONFIG_DIR.resolve(RECENT_PROJECT_FILE);
        if (Files.exists(recents)) {
            try {
                MapType mapType = YAML_MAPPER.getTypeFactory().constructMapType(HashMap.class, UUID.class, RecentProject.class);
                recentProjects.clear();
                recentProjects.putAll(YAML_MAPPER.readValue(recents.toFile(), mapType));
            } catch (IOException e) {
                System.err.println("Failed to load recent projects");
            }
        }
    }

    public static HashMap<UUID, RecentProject> recentProjects() {
        return new HashMap<>(recentProjects);
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

    private static void saveRecentProjects() {
        if (CONFIG_DIR == null) return;

        Path recents = CONFIG_DIR.resolve(RECENT_PROJECT_FILE);

        try {
            YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(recents.toFile(), recentProjects);
        } catch (IOException e) {
            System.err.println("Failed to save recent projects");
        }
    }
}
