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
        float instX = (float) sourceX / (float) fontWidth;
        float firstX = (float) (sourceX + width) / (float) fontWidth;

        // Start from the bottom of the glyph.
        float instY = (float) sourceY - height / (float) fontHeight;
        float firstY = (float) (sourceY) / (float) fontHeight;

        textureCoordinates[0] = new Vector2f(instX, firstY);
        textureCoordinates[1] = new Vector2f(firstX, instY);
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
