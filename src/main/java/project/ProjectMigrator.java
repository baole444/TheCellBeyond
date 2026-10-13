package project;

import org.joml.Vector2i;
import render.texture.ImageHeader;
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
    private static final Set<Integer> BreakingChanges = Set.of(2);
    private static final String BackupDirName = ".backups";
    private static final String SheetsKey = "sheets";
    private static final String AssetsKey = "assets";
    private static final String AssetCategoryKey = "category";
    private static final String AssetNameKey = "name";
    private static final String AssetSizeXKey = "sizeX";
    private static final String AssetSizeYKey = "sizeY";
    private static final String SheetPathKey = "path";
    private static final String SheetEndXKey = "spriteEndPosX";
    private static final String SheetEndYKey = "spriteEndPosY";
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
     * Apply the structure for breaking changes introduced by a version.
     * @param data the data to remap
     * @param version the target version
     * @return the remapped data to the targeted version
     */
    private Map<String, Object> remapBreakingChange(Map<String, Object> data, int version) {
        return switch (version) {
            case 2 -> updateToV2(data);
            default -> data;
        };
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

    private String resolveAgainstProject(String relativePath) {
        Path parent = projectFile.getParent();
        return parent == null ? relativePath : parent.resolve(relativePath).toString();
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

    private Map<String, Object> updateToV2(Map<String, Object> data) {
        writeSheetRegions(data);
        renameAssets(data);
        return data;
    }

    /**
     * Rewrite all the cut region of imported sheets.
     * @param data the data to remap
     */
    private void writeSheetRegions(Map<String, Object> data) {
        if (!(data.get(SheetsKey) instanceof Map<?, ?> categories)) return;
        Map<String, Object> migratedCategories = new LinkedHashMap<>();
        for (Map.Entry<?, ?> category : categories.entrySet()) {
            if (!(category.getValue() instanceof Map<?, ?> named)) continue;
            Map<String, Object> migratedNamed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> sheet : named.entrySet()) {
                migratedNamed.put(String.valueOf(sheet.getKey()), withCutRegion(sheet.getValue(), String.valueOf(category.getKey()), String.valueOf(sheet.getKey())));
            }
            migratedCategories.put(String.valueOf(category.getKey()), migratedNamed);
        }
        data.put(SheetsKey, migratedCategories);
    }

    /**
     * Rewrite the cut region of the sheet.
     * @param sheet the stored sheet entry
     * @param category the sheet's category
     * @param name the name of the sheet
     * @return the entry with rewritten cut region
     */
    private Object withCutRegion(Object sheet, String category, String name) {
        if (!(sheet instanceof Map<?, ?> stored)) return sheet;
        String sheetName = String.format("%s/%s", category, name);
        Map<String, Object> entry = new LinkedHashMap<>();
        stored.forEach((k, v) -> entry.put(String.valueOf(k), v));
        entry.put(AssetCategoryKey, category);
        entry.put(AssetNameKey, name);
        Object path = entry.get(SheetPathKey);
        Vector2i size = path != null ? ImageHeader.sizeOfFile(resolveAgainstProject(path.toString())) : null;
        if (size == null) Logger.warning(String.format("Cannot read image file at %s for sheet '%s'", path, sheetName));
        else {
            entry.put(SheetEndXKey, size.x);
            entry.put(SheetEndYKey, size.y);
        }
        return entry;
    }

    /**
     * Move all single sprite imports from UUID key to categories and names.
     * @param data the data to remap
     */
    private void renameAssets(Map<String, Object> data) {
        if (!(data.get(AssetsKey) instanceof Map<?, ?> stored)) return;
        Map<String, Object> named = new LinkedHashMap<>();
        Set<String> taken = new HashSet<>();
        for (Map.Entry<?, ?> asset : stored.entrySet()) {
            if (!(asset.getValue() instanceof Map<?, ?> oldEntry)) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            oldEntry.forEach((k, v) -> entry.put(String.valueOf(k), v));
            Object path = entry.get(SheetPathKey);
            String base = path == null ? "sprite" : path.toString();
            String fileName = base.substring(base.lastIndexOf('/') + 1);
            int ext = fileName.lastIndexOf('.');
            if (ext > 0) fileName = fileName.substring(0, ext);
            String cleaned = fileName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
            base = cleaned.isBlank() ? "sprite" : cleaned;
            String name = String.format("%s_%s_%s", base, entry.getOrDefault(AssetSizeXKey, 0), entry.getOrDefault(AssetSizeYKey, 0));
            int suffix = 1;
            String newName = name;
            while (!taken.add(newName)) {
                newName = name + "_" + suffix;
                suffix++;
            }
            entry.put(AssetCategoryKey, ProjectAsset.DefaultCategory);
            entry.put(AssetNameKey, newName);
            named.put(newName, entry);
        }
        Map<String, Object> categories = new LinkedHashMap<>();
        if (!named.isEmpty()) categories.put(ProjectAsset.DefaultCategory, named);
        data.put(AssetsKey, categories);
    }
}
