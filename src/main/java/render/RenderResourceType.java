package render;

import TheCellBeyond.internal.ResourceType;

public enum RenderResourceType implements ResourceType {
    Texture(16),
    FontAtlas(32),
    Font(48);

    public final int value;

    RenderResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
