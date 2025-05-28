package threading.states;

import components.SpriteRender;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class SpriteRenderState {
    private Vector4f color;
    private String texturePath;
    private Vector2f[] textCoords;

    public SpriteRenderState(SpriteRender spriteRender) {
        this.color = new Vector4f(spriteRender.getColor());

        if (spriteRender.getTexture() != null) {
            this.texturePath = spriteRender.getTexture().getFilePath();
            this.textCoords = new Vector2f[4];
            Vector2f[] originalCoords = spriteRender.getTextureCoordinates();
            for (int i = 0; i < 4; i++) {
                this.textCoords[i] = new Vector2f(originalCoords[i]);
            }
        }
    }

    public Vector4f getColor() {
        return color;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public Vector2f[] getTextCoords() {
        return textCoords;
    }
}
