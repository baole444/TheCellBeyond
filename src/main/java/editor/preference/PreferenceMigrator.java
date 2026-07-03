package editor.preference;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.MapType;
import utility.log.EngineLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Migrator for {@link EditorPreferences} for breaking and none breaking changes. Value of old entries are preserve via
 * mapping on none breaking changes, and explicit remap on breaking changes. Entries that are no longer exist in new version are dropped.
 * <p>
 * Migration happen incrementally at the bound of each breaking change and fast-forward between none breaking change.
 */
final class PreferenceMigrator {
    private static final EngineLog Logger = new EngineLog(PreferenceMigrator.class);
    private static final DateTimeFormatter BackupStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    /**
     * The set of version numbers that carry breaking changes (renamed keys, structure changes) that need explicit remap.
     */
    private static final Set<Integer> BreakingChanges = Set.of();
    /**
     * The limits on how many backups is saved on disk.
     */
    static final int MaxBackups = 3;
    private static final String BackupMarker = ".backup.";
    private final Path configFile;
    private final ObjectMapper mapper;
    private final MapType mapType;

    private PreferenceMigrator(Path configFile, ObjectMapper mapper) {
        this.configFile = configFile;
        this.mapper = mapper;
        this.mapType = mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
    }

    /**
     * Read the editor preference file and migrate to latest if behind.
     * @param configFile the file to migrate
     * @param mapper the YAML mapper to use
     * @return the up-to-date preferences, or default when migration failed, or file is unreadable
     */
    static EditorPreferences migrate(Path configFile, ObjectMapper mapper) {
        PreferenceMigrator migrator = new PreferenceMigrator(configFile, mapper);
        Map<String, Object> stored = migrator.read();
        if (stored == null) return new EditorPreferences();
        int fileVersion = version(stored);
        if (fileVersion >= EditorPreferences.SaveVersion) return migrator.bind(stored);
        Logger.debug(String.format("Updating editor preferences version %d to version %d...", fileVersion, EditorPreferences.SaveVersion));
        long start = System.currentTimeMillis();
        migrator.backup(fileVersion);
        Map<String, Object> migrated = migrator.migrateToLatest(stored, fileVersion);
        migrator.write(migrated);
        Logger.debug(String.format("Editor preferences finished updating. Took %dms.", System.currentTimeMillis() - start));
        return migrator.bind(migrated);
    }

    private Map<String, Object> migrateToLatest(Map<String, Object> stored, int fileVersion) {
        Map<String, Object> data = stored;
        int current = fileVersion;
        while (current < EditorPreferences.SaveVersion) {
            int nextBreaking = nextBreakingChange(current);
            if (nextBreaking == -1) {
                data = updateConfig(data, EditorPreferences.SaveVersion);
                current = EditorPreferences.SaveVersion;
                continue;
            }
            data = updateConfig(data, nextBreaking - 1);
            data = remapBreakingChange(data, nextBreaking);
            data = updateConfig(data, nextBreaking);
            current = nextBreaking;
        }
        return data;
    }

    private static int nextBreakingChange(int from) {
        for (int v = from + 1; v <= EditorPreferences.SaveVersion; v++) {
            if (BreakingChanges.contains(v)) return v;
        }
        return -1;
    }

    /**
     * Write the stored value onto a new schema mapping where the key entry still exist, otherwise the value is dropped.
     * @param data the current data to remap
     * @param target target version
     * @return the data remapped to new schema
     */
    private Map<String, Object> updateConfig(Map<String, Object> data, int target) {
        Map<String, Object> defaults = mapper.convertValue(new EditorPreferences(), mapType);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key.equals(EditorPreferences.VersionKey)) continue;
            if (defaults.containsKey(key)) defaults.put(key, entry.getValue());
        }
        defaults.put(EditorPreferences.VersionKey, target);
        return defaults;
    }

    /**
     * Currently there is no breaking change to remap yet.
     * @apiNote Change to a switch statement when there is a breaking change.
     * @param data the data to remap
     * @param version the target version
     * @return the remapped data to the targeted version
     */
    private Map<String, Object> remapBreakingChange(Map<String, Object> data, int version) {
        return data;
    }

    private static int version(Map<String, Object> stored) {
        Object raw = stored.get(EditorPreferences.VersionKey);
        return raw instanceof Number number ? number.intValue() : 0;
    }

    private Map<String, Object> read() {
        try {
            return mapper.readValue(configFile.toFile(), mapType);
        } catch (JacksonException e) {
            Logger.warning("Failed to read editor preferences: " + e.getMessage());
            return null;
        }
    }

    private void write(Map<String, Object> data) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), data);
        } catch (JacksonException e) {
            Logger.warning("Failed to write migrated editor preferences: " + e.getMessage());
        }
    }

    private EditorPreferences bind(Map<String, Object> data) {
        try {
            return mapper.convertValue(data, EditorPreferences.class);
        } catch (IllegalArgumentException e) {
            Logger.warning("Failed to bind editor preferences: " + e.getMessage());
            return new EditorPreferences();
        }
    }

    private void backup(int version) {
        try {
            String stamp = LocalDateTime.now().format(BackupStamp);
            Path backup = configFile.resolveSibling(String.format("%s.v%d.backup.%s", configFile.getFileName(), version, stamp));
            Files.copy(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
            Logger.debug("Editor preferences backup created: " + backup.getFileName());
        } catch (IOException e) {
            Logger.warning("Failed to backup editor preferences: " + e.getMessage());
        }
    }

    /**
     * Clear up timestamped backups of the given preference file, keep only the {@value MaxBackups} most recent.
     * @param configFile the preference file to look for backups
     */
    static void pruneBackups(Path configFile) {
        Path dir = configFile.getParent();
        if (dir == null) return;
        String prefix = configFile.getFileName().toString();
        try (Stream<Path> entries = Files.list(dir)) {
            List<Path> backups = entries
                    .filter(Files::isRegularFile)
                    .filter(p -> isBackup(prefix, p))
                    .sorted(Comparator.comparingLong(PreferenceMigrator::lastModified).reversed())
                    .toList();
            for (int i = MaxBackups; i < backups.size(); i++) delete(backups.get(i));
        } catch (IOException e) {
            Logger.warning("Failed to prune editor preferences backups: " + e.getMessage());
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static boolean isBackup(String prefix, Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(prefix) && name.contains(BackupMarker);
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Logger.warning("Failed to delete old preferences backup '" + path.getFileName() + "': " + e.getMessage());
        }
    }
}
