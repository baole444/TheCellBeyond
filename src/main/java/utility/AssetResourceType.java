package utility;

import TheCellBeyond.internal.ResourceType;

public enum AssetResourceType implements ResourceType {
    SpriteSheet(2),
    TextureUnit(4);

    private final int value;

    AssetResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
