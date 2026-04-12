package utility;

import TheCellBeyond.internal.ResourceType;

/**
 * AssetResourceType enums are unclassified type of resource managed by AssetManager.
 */
public enum AssetResourceType implements ResourceType {
    /**
     * Sprite sheet resource.
     */
    SpriteSheet(2),
    /**
     * Texture unit resource.
     */
    TextureUnit(4);

    /**
     * Numeric ID for the resource type.
     */
    public final int value;

    AssetResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
