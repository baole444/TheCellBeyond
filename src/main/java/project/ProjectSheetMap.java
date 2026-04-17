package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import utility.UnifiedPaths;

public record ProjectSheetMap(
        String path, int numberOfSprite,
        int spriteSizeX, int spriteSizeY,
        int spriteSpacingX, int spriteSpacingY,
        int spriteStartPosX, int spriteStartPosY
) {
    public static final String metadataTag = "SpriteSheet";

    @JsonIgnore
    public String metadata(String category, String name) {
        if (path == null || path.isBlank() || UnifiedPaths.invalidMetadata(category) || UnifiedPaths.invalidMetadata(name)) return null;
        return UnifiedPaths.appendMetadata(UnifiedPaths.ProjectPrefix + path, metadataTag, category, name);
    }
}
