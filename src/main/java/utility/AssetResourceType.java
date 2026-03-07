package utility;

import TheCellBeyond.internal.ResourceType;

public enum AssetResourceType implements ResourceType {
    Shader(6),
    SpriteSheet(8),
    TextureUnit(10),
    Sound(12);

    private final int value;

    AssetResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
