package utility;

import TheCellBeyond.internal.ResourceType;

public enum AssetResourceType implements ResourceType {
    SpriteSheet(8),
    TextureUnit(10);

    private final int value;

    AssetResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
