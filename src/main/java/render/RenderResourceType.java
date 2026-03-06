package render;

import TheCellBeyond.internal.ResourceType;

public enum RenderResourceType implements ResourceType {
    Texture(16),
    FontAtlas(32);

    public final int value;

    RenderResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
