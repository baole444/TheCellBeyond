package project;

import org.joml.Vector2i;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import render.texture.ImageHeader;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import utility.AssetReference;
import utility.Settings;
import utility.UnifiedPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ProjectMigratorTest {
    private static final ObjectMapper Mapper = new ObjectMapper(new YAMLFactory())
            .rebuild()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private static final String SheetImage = "sheet.png";
    private static final String EngineImage = Settings.TexturePath.EditorControls;

    @Test
    public void fillSheetRegion(@TempDir Path dir) throws IOException {
        Vector2i imageSize = copyImageInto(dir);
        ProjectData migrated = migrate(dir, """
                version: 1
                sheets:
                  object:
                    test sheet:
                      path: "sheet.png"
                      numberOfSprite: 12
                      spriteSizeX: 16
                      spriteSizeY: 16
                """);
        assertEquals(ProjectData.SaveVersion, migrated.version(), "the file should be stamped at the current version");
        ProjectSheet sheet = migrated.sheets().get("object").get("test sheet");
        assertNotNull(sheet, "the sheet should survive the migration");
        assertEquals(imageSize.x, sheet.spriteEndPosX(), "the region should span the image's width");
        assertEquals(imageSize.y, sheet.spriteEndPosY(), "the region should span the image's height");
        assertEquals(12, sheet.numberOfSprite(), "the existing cut parameters should be preserved");
        assertEquals(16, sheet.spriteSizeX());
    }

    @Test
    public void keepEmptyRegionForMissingImage(@TempDir Path dir) throws IOException {
        ProjectData migrated = migrate(dir, """
                version: 1
                sheets:
                  object:
                    gone:
                      path: "not_here.png"
                      numberOfSprite: 4
                      spriteSizeX: 8
                      spriteSizeY: 8
                """);
        ProjectSheet sheet = migrated.sheets().get("object").get("gone");
        assertNotNull(sheet, "a sheet with an unreadable image should still survive the migration");
        assertEquals(0, sheet.spriteEndPosX(), "an unmeasurable region is left empty, not guessed");
        assertEquals(0, sheet.spriteEndPosY());
    }

    @Test
    public void nameLegacyAssets(@TempDir Path dir) throws IOException {
        ProjectData migrated = migrate(dir, """
                version: 1
                assets:
                  3f2504e0-4f89-11d3-9a0c-0305e82c3301:
                    path: "assets/Main char.png"
                    sizeX: 60
                    sizeY: 60
                """);
        Map<String, ProjectAsset> categorized = migrated.assets().get(ProjectAsset.DefaultCategory);
        assertNotNull(categorized, "legacy imports should land in the default category");
        ProjectAsset asset = categorized.get("main_char_60_60");
        assertNotNull(asset, "the name should come from the file name and the size: " + categorized.keySet());
        assertEquals(ProjectAsset.DefaultCategory, asset.category(), "the record should carry its own category");
        assertEquals("main_char_60_60", asset.name(), "the record should carry its own name");
        assertEquals("assets/Main char.png", asset.path(), "the path should be preserved");
        assertEquals(60, asset.sizeX());
    }

    @Test
    public void suffixDuplicateAssetNames(@TempDir Path dir) throws IOException {
        ProjectData migrated = migrate(dir, """
                version: 1
                assets:
                  3f2504e0-4f89-11d3-9a0c-0305e82c3301:
                    path: "assets/icon.png"
                    sizeX: 16
                    sizeY: 16
                  3f2504e0-4f89-11d3-9a0c-0305e82c3302:
                    path: "assets/icon.png"
                    sizeX: 16
                    sizeY: 16
                """);
        Map<String, ProjectAsset> categorized = migrated.assets().get(ProjectAsset.DefaultCategory);
        assertEquals(2, categorized.size(), "a name collision must not merge two imports: " + categorized.keySet());
        assertNotNull(categorized.get("icon_16_16"));
        assertNotNull(categorized.get("icon_16_16_1"), "the second should be suffixed: " + categorized.keySet());
    }

    @Test
    public void nameLegacySheets(@TempDir Path dir) throws IOException {
        copyImageInto(dir);
        ProjectData migrated = migrate(dir, """
                version: 1
                sheets:
                  object:
                    test sheet:
                      path: "sheet.png"
                      numberOfSprite: 12
                      spriteSizeX: 16
                      spriteSizeY: 16
                """);
        ProjectSheet sheet = migrated.sheets().get("object").get("test sheet");
        assertEquals("object", sheet.category(), "the record should carry its own category");
        assertEquals("test sheet", sheet.name(), "the record should carry its own name");
        assertNotNull(sheet.metadata(), "a named sheet should produce a cache key");
    }

    private static Vector2i copyImageInto(Path dir) throws IOException {
        Vector2i size = ImageHeader.size(EngineImage);
        assertNotNull(size, "the test needs a readable engine image to copy");
        try (InputStream stream = UnifiedPaths.getAssetStream(new AssetReference(EngineImage).resolvedPath())) {
            Files.write(dir.resolve(SheetImage), stream.readAllBytes());
        }
        return size;
    }

    private static ProjectData migrate(Path dir, String yaml) throws IOException {
        Path projectFile = dir.resolve("_project.yml");
        Files.writeString(projectFile, yaml, StandardCharsets.UTF_8);
        ProjectData migrated = ProjectMigrator.migrate(projectFile, Mapper);
        assertNotNull(migrated, "the migration should produce bound project data");
        return migrated;
    }
}
