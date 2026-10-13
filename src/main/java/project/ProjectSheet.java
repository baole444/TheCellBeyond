package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import utility.UnifiedPaths;

/**
 * ProjectSheet is a record of an imported sprite sheet in the project's manifest.
 * @param category the category this import belongs to
 * @param name the name of the imported sheet
 * @param path the project relative path to the image
 * @param numberOfSprite the amount of sprites to cut from the sheet
 * @param spriteSizeX the width of each sprite on sheet, in pixels
 * @param spriteSizeY the height of each sprite on sheet, in pixels
 * @param spriteSpacingX the horizontal gap between each cut sprite, in pixels
 * @param spriteSpacingY the vertical gap between each cut sprite, in pixels
 * @param spriteStartPosX the offset to start cutting sprites on the left bound
 * @param spriteStartPosY the offset to start cutting sprites on the top bound
 * @param spriteEndPosX the offset to stop cutting sprites on the right bound
 * @param spriteEndPosY the offset to stop cutting sprites on the bottom bound
 */
public record ProjectSheet(
        String category, String name,
        String path, int numberOfSprite,
        int spriteSizeX, int spriteSizeY,
        int spriteSpacingX, int spriteSpacingY,
        int spriteStartPosX, int spriteStartPosY,
        int spriteEndPosX, int spriteEndPosY
) {
    public static final String MetadataTag = "SpriteSheet";

    /**
     * Append metadata tag of this sprite sheet onto its unified path.
     * @return the tagged path, or null if this sheet has no usable identity
     */
    @JsonIgnore
    public String metadata() {
        if (path == null || path.isBlank() || UnifiedPaths.invalidMetadata(category) || UnifiedPaths.invalidMetadata(name)) return null;
        return UnifiedPaths.appendMetadata(UnifiedPaths.ProjectPrefix + path, MetadataTag, category, name);
    }
}
