package render.texture;

import org.joml.Vector2f;
import render.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * A class dedicated to handle sprite sheet details.
 * Handle the separation of the sheet into individual sprites and indexing them.
 */
public class SpriteSheet implements TextureStatusListener {
    private final Texture texture;
    private final List<Sprite> sprites;
    private transient boolean requireCompute = false;
    private final transient int spsWidth, spsHeight, countSprite, spacing;

    public SpriteSheet(Texture texture, int spsWidth, int spsHeight, int countSprite, int spacing) {
        this.sprites = new ArrayList<>();

        this.texture = texture;

        this.spsWidth = spsWidth;
        this.spsHeight = spsHeight;
        this.countSprite = countSprite;
        this.spacing = spacing;

        if (!texture.isReady()) {
            TextureStatusCallback.register(this);
            requireCompute = true;
            return;
        }

        computeSprites();
    }

    private void computeSprites() {
        if (!requireCompute || !texture.isReady()) return;

        int instX = 0;
        int instY = texture.getHeight() - spsHeight;
        for (int i = 0; i < countSprite; i++) {
            Sprite sprite = getSprite(instY, instX);
            this.sprites.add(sprite);
            instX += spsWidth + spacing;
            if (instX >= texture.getWidth()) {
                instX = 0;
                instY -= spsHeight + spacing;
            }
        }

        requireCompute = false;

        // SpriteSheet in most cases a long-live object, unregister might not be needed.
        // Currently, there is no method that requires re-compute the SpriteSheet so leave it here for now.
        // If in the future, there are mechanics that update the SpriteSheet during runtime,
        // then move unregistering to clean up code or no unregister at all.
        TextureStatusCallback.unRegister(this);
    }

    private Sprite getSprite(int instY, int instX) {
        float topY = (instY + spsHeight) / (float)texture.getHeight();
        float rightX = (instX + spsWidth) / (float)texture.getWidth();
        float leftX = instX / (float)texture.getWidth();
        float bottomY = instY / (float)texture.getHeight();

        Vector2f[] textureCoordinates = {
                new Vector2f(rightX, topY),
                new Vector2f(rightX, bottomY),
                new Vector2f(leftX, bottomY),
                new Vector2f(leftX, topY)
        };

        Sprite sprite = new Sprite();
        sprite.setTexture(this.texture);
        sprite.setTextureCoordinates(textureCoordinates);
        sprite.setWidth(spsWidth);
        sprite.setHeight(spsHeight);
        return sprite;
    }

    public Sprite spriteIndex(int index) {
        if (requireCompute) return null;

        return this.sprites.get(index);
    }

    public int size() {
        return sprites.size();
    }

    @Override
    public void onTextureStatusChange(int handleId, TextureHandle.Status status) {
        if (texture.getHandleId() == handleId && status.equals(TextureHandle.Status.READY)) computeSprites();
    }
}
