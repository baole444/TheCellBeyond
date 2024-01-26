package components;

import org.joml.Vector2f;
import render.Texture;

import java.util.ArrayList;
import java.util.List;

public class SpriteSheet {
    private Texture texture;
    private List<Sprite> sprites;

    public SpriteSheet(Texture texture, int spsWidth, int spsHeight, int countSprite, int spacing) {
        this.sprites = new ArrayList<>();

        this.texture = texture;

        int instX = 0;
        int instY = texture.loadHeight() - spsHeight;
        for (int i = 0; i < countSprite; i++) {
            float topY = (instY + spsHeight) / (float)texture.loadHeight();
            float rightX = (instX + spsWidth) / (float)texture.loadWidth();
            float leftX = instX / (float)texture.loadWidth();
            float bottomY = instY / (float)texture.loadHeight();

            Vector2f[] texCoord = {
                    new Vector2f(rightX, topY),
                    new Vector2f(rightX, bottomY),
                    new Vector2f(leftX, bottomY),
                    new Vector2f(leftX, topY)
            };

            Sprite sprite = new Sprite();
            sprite.setTex(this.texture);
            sprite.setTexCrd(texCoord);
            sprite.setWidth(spsWidth);
            sprite.setHeight(spsHeight);
            this.sprites.add(sprite);
            instX += spsWidth + spacing;
            if (instX >= texture.loadWidth()) {
                instX = 0;
                instY -= spsHeight + spacing;
            }
        }
    }

    public Sprite spriteIndex(int index) {
        return this.sprites.get(index);
    }

    public int size() {
        return sprites.size();
    }
}
