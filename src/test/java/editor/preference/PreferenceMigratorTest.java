package editor.preference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.MapType;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class PreferenceMigratorTest {
    private static final ObjectMapper Mapper = new ObjectMapper(new YAMLFactory())
            .rebuild()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    public void versionlessFileGainsCleanBuildDefaultAndPreservesValues(@TempDir Path dir) throws IOException {
        Path config = write(dir, """
                autoSaveOnExit: true
                autoSaveOnChangeScene: false
                showGridLine: true
                """);
        EditorPreferences result = PreferenceMigrator.migrate(config, Mapper);
        assertTrue(result.autoSaveOnExit(), "user value preserved");
        assertFalse(result.autoSaveOnChangeScene());
        assertTrue(result.showGridLine(), "user value preserved");
        assertTrue(result.cleanBuildScripts(), "new field takes its current default");
    }

    @Test
    public void migrationStampsVersionAndBacksUpOnDisk(@TempDir Path dir) throws IOException {
        Path config = write(dir, """
                autoSaveOnExit: false
                autoSaveOnChangeScene: false
                showGridLine: false
                """);
        PreferenceMigrator.migrate(config, Mapper);
        Map<String, Object> onDisk = read(config);
        assertEquals(EditorPreferences.SaveVersion, ((Number) onDisk.get(EditorPreferences.VersionKey)).intValue());
        assertEquals(Boolean.TRUE, onDisk.get("cleanBuildScripts"));
        assertEquals(1, backups(dir), "a timestamped backup of the old file is kept");
    }

    @Test
    public void upToDateFileIsNotOverwritten(@TempDir Path dir) throws IOException {
        Path config = write(dir, """
                autoSaveOnExit: false
                autoSaveOnChangeScene: false
                showGridLine: false
                cleanBuildScripts: false
                version: 1
                """);
        EditorPreferences result = PreferenceMigrator.migrate(config, Mapper);
        assertFalse(result.cleanBuildScripts(), "an at-version value is respected, not reset to default");
        assertEquals(0, backups(dir), "no backup is taken when nothing migrates");
    }

    @Test
    public void emptyFileFallsBackToDefaults(@TempDir Path dir) throws IOException {
        Path config = write(dir, "");
        EditorPreferences result = PreferenceMigrator.migrate(config, Mapper);
        assertEquals(new EditorPreferences(), result);
    }

    @Test
    public void pruneKeepsOnlyMostRecentBackups(@TempDir Path dir) throws IOException {
        Path config = write(dir, "version: 1");
        int total = PreferenceMigrator.MaxBackups + 3;
        for (int i = 0; i < total; i++) {
            Path backup = dir.resolve(String.format("configs.v0.backup.2026-06-28_08-%02d-00", i));
            Files.writeString(backup, "old", StandardCharsets.UTF_8);
            Files.setLastModifiedTime(backup, FileTime.fromMillis(1_000L * (i + 1)));
        }
        PreferenceMigrator.pruneBackups(config);
        assertEquals(PreferenceMigrator.MaxBackups, backups(dir), "only the most recent backups are kept");
        assertTrue(Files.exists(config), "the live config file is never pruned");
        for (int i = total - PreferenceMigrator.MaxBackups; i < total; i++) {
            Path kept = dir.resolve(String.format("configs.v0.backup.2026-06-28_08-%02d-00", i));
            assertTrue(Files.exists(kept), "the most recent backup is retained: " + kept.getFileName());
        }
    }

    private static Path write(Path dir, String content) throws IOException {
        Path config = dir.resolve("configs");
        Files.writeString(config, content, StandardCharsets.UTF_8);
        return config;
    }

    private static Map<String, Object> read(Path config) {
        MapType mapType = Mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
        return Mapper.readValue(config.toFile(), mapType);
    }

    private static long backups(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(p -> p.getFileName().toString().contains(".backup.")).count();
        }
    }
}
