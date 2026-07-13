package project;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Migrator for the project file {@link ProjectData}. Values of old entries are preserve via mapping on none breaking changes,
 * and explicit remap on break changes. Entries that are no longer exist in new version are dropped.
 * <p>
 * Migration happen incrementally at the bound of each breaking change and fast-forward between none breaking change.
 */
final class ProjectMigrator {
    private static final EngineLog Logger = new EngineLog(ProjectMigrator.class);
    private static final DateTimeFormatter BackupStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    /**
     * The set of version numbers that carry breaking changes (renamed keys, structure changes) that need explicit remap.
     */
    private static final Set<Integer> BreakingChanges = Set.of();
    static final int MaxBackups = 3;
    private static final String BackupDirName = ".backups";
    private final Path projectFile;
    private final ObjectMapper mapper;
    private final MapType mapType;

    private ProjectMigrator(Path projectFile, ObjectMapper mapper) {
        this.projectFile = projectFile;
        this.mapper = mapper;
        this.mapType = mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
    }

    /**
     * Read the project file and migrate to latest if behind.
     * @param projectFile the file to migrate
     * @param mapper the YAML mapper to use
     * @return the up-to-date project data, or null when the file is unreadable or binding failed.
     */
    static ProjectData migrate(Path projectFile, ObjectMapper mapper) {
        ProjectMigrator migrator = new ProjectMigrator(projectFile, mapper);
        Map<String, Object> stored = migrator.read();
        if (stored == null) return null;
        int fileVersion = version(stored);
        if (fileVersion >= ProjectData.SaveVersion) {
            if (addUUID(stored)) migrator.write(stored);
            return migrator.bind(stored);
        }
        Logger.debug(String.format("Updating project file version %d to version %d...", fileVersion, ProjectData.SaveVersion));
        long start = System.currentTimeMillis();
        migrator.backup(fileVersion);
        Map<String, Object> migrated = migrator.migrateToLatest(stored, fileVersion);
        migrator.write(migrated);
        Logger.debug(String.format("Project file finished updating, took %dms", System.currentTimeMillis() - start));
        return migrator.bind(migrated);
    }

    private Map<String, Object> migrateToLatest(Map<String, Object> stored, int fileVersion) {
        Map<String, Object> data = stored;
        int current = fileVersion;
        while (current < ProjectData.SaveVersion) {
            int nextBreaking = nextBreakingChange(current);
            if (nextBreaking == -1) {
                data = updateConfig(data, ProjectData.SaveVersion);
                current = ProjectData.SaveVersion;
                continue;
            }
            data = updateConfig(data, nextBreaking - 1);
            data = remapBreakingChange(data, nextBreaking);
            data = updateConfig(data, nextBreaking);
            current = nextBreaking;
        }
        return data;
    }

    /**
     * Write the stored value onto a new schema mapping where the key entry still exist, otherwise the value is dropped.
     * @param data the current data to remap
     * @param target target version
     * @return the data remapped to new schema
     */
    private Map<String, Object> updateConfig(Map<String, Object> data, int target) {
        Map<String, Object> defaults = mapper.convertValue(new ProjectData(target, new ProjectPreference()), mapType);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key.equals(ProjectData.VersionKey)) continue;
            if (defaults.containsKey(key)) defaults.put(key, entry.getValue());
        }
        defaults.put(ProjectData.VersionKey, target);
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

    private Map<String, Object> read() {
        try {
            return mapper.readValue(projectFile.toFile(), mapType);
        } catch (JacksonException e) {
            Logger.warning("Failed to read project file: " + e.getMessage());
            return null;
        }
    }

    private void write(Map<String, Object> data) {
        try {
            mapper.writeValue(projectFile.toFile(), data);
        } catch (JacksonException e) {
            Logger.warning("Failed to write migrated project file: " + e.getMessage());
        }
    }

    private ProjectData bind(Map<String, Object> data) {
        try {
            return mapper.convertValue(data, ProjectData.class);
        } catch (IllegalArgumentException e) {
            Logger.warning("Failed to bind project data: " + e.getMessage());
            return null;
        }
    }

    private Path backupDir() {
        Path parent = projectFile.getParent();
        return parent == null ? Path.of(BackupDirName) : parent.resolve(BackupDirName);
    }

    private void backup(int version) {
        try {
            Path dir = backupDir();
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(BackupStamp);
            Path backup = dir.resolve(String.format("%s.v%d.backup.%s", projectFile.getFileName(), version, stamp));
            Files.copy(projectFile, backup, StandardCopyOption.REPLACE_EXISTING);
            Logger.debug("Project file backup created: " + backup.getFileName());
        } catch (IOException e) {
            Logger.warning("Failed to backup project file: " + e.getMessage());
        }
    }

    private static boolean addUUID(Map<String, Object> stored) {
        Object uuid = stored.get(ProjectData.UUIDKey);
        if (uuid != null && !uuid.toString().isBlank()) return false;
        stored.put(ProjectData.UUIDKey, UUID.randomUUID().toString());
        return true;
    }

    private static int version(Map<String, Object> stored) {
        Object raw = stored.get(ProjectData.VersionKey);
        return raw instanceof Number number ? number.intValue() : 0;
    }

    private static int nextBreakingChange(int from) {
        for (int v = from + 1; v <= ProjectData.SaveVersion; v++) {
            if (BreakingChanges.contains(v)) return v;
        }
        return -1;
    }
}
