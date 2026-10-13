package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import utility.UnifiedPaths;

/**
 * ProjectAsset is a record of a single sprite in the project's manifest file.
 * @param category the category this import belongs to
 * @param name the name of the import single sprite
 * @param path the project relative path to the image
 * @param sizeX the width of the sprite, in pixels
 * @param sizeY the height of the sprite, in pixels
 */
public record ProjectAsset(String category, String name, String path, int sizeX, int sizeY) {
    public static final String MetadataTag = "SingleSprite";
    /**
     * Default category given to imports that is missing category.
     */
    public static final String DefaultCategory = "uncategorized";

    /**
     * Append metadata tag of this single sprite onto its unified path.
     * @return the tagged path, or null if this sprite has no usable identity
     */
    @JsonIgnore
    public String metadata() {
        if (path == null || path.isBlank() || UnifiedPaths.invalidMetadata(category) || UnifiedPaths.invalidMetadata(name)) return null;
        return UnifiedPaths.appendMetadata(UnifiedPaths.ProjectPrefix + path, MetadataTag, category, name);
    }
}
