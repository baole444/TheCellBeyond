package render;

public enum RenderResourceType {
    Texture(16),
    FontAtlas(32);

    public final int value;

    RenderResourceType(int value) {
        this.value = value;
    }
}
