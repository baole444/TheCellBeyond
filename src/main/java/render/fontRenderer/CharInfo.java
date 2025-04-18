package render.fontRenderer;

import org.joml.Vector2f;

public class CharInfo {
    private final int sourceX, sourceY, width, height;

    public Vector2f[] textureCoordinates = new Vector2f[4];

    public CharInfo(int sourceX, int sourceY, int width, int height) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.width = width;
        this.height = height;
    }

    public void setTextureCoordinates(int fontWidth, int fontHeight) {
        float x0 = (float) sourceX / (float) fontWidth;
        float x1 = (float) (sourceX + width) / (float) fontWidth;

        // Start from the bottom of the glyph.
        float y0 = (float) (sourceY - height) / (float) fontHeight;
        float y1 = (float) (sourceY) / (float) fontHeight;

        textureCoordinates[0] = new Vector2f(x0, y1);
        textureCoordinates[1] = new Vector2f(x1, y0);
    }

    public int sourceX() {
        return this.sourceX;
    }

    public int sourceY() {
        return this.sourceY;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}
