package render.texture;

import org.joml.Vector2f;
import org.joml.Vector2i;
import render.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * SpriteSheet facilitate the texture used by the sprites, allow register callback with Texture Management system.
 * SpriteSheet will calculate its sprites' texture coordinate and size once the texture assigned to it is ready.
 * @see TextureUnit calculate sprite as a full texture.
 */
public class SpriteSheet implements TextureStatusListener {
    private Texture texture;
    private final List<Sprite> sprites;

    private transient boolean requireCompute = false;
    private transient boolean isRegistered = false;
    private transient int lastHandleId = -1;

    private final transient int numberOfSprites;
    private final transient Vector2i spriteSize, startPosition, spriteSpacing;

    public SpriteSheet(Texture texture, int spriteWidth, int spriteHeight, int numberOfSprites, int spriteSpacing) {
        this(texture, new Vector2i(spriteWidth, spriteHeight), numberOfSprites,
                new Vector2i(spriteSpacing), new Vector2i()
        );
    }

    public SpriteSheet(Texture texture, int spriteWidth, int spriteHeight, int numberOfSprites, int horizontalSpriteSpacing, int verticalSpriteSpacing, int startPosX, int startPosY) {
        this(texture, new Vector2i(spriteWidth, spriteHeight), numberOfSprites,
                new Vector2i(horizontalSpriteSpacing, verticalSpriteSpacing),
                new Vector2i(startPosX, startPosY)
        );
    }

    public SpriteSheet(Texture texture, Vector2i spriteSize, int numberOfSprites, Vector2i spriteSpacing, Vector2i startPosition) {
        sprites = new ArrayList<>();
        this.texture = texture;
        this.spriteSize = new Vector2i(spriteSize);
        this.numberOfSprites = numberOfSprites;
        this.spriteSpacing = new Vector2i(spriteSpacing);
        this.startPosition = new Vector2i(startPosition);

        textureReadyCheck(texture);
    }

    private void textureReadyCheck(Texture texture) {
        if (texture == null) return;

        int currentId = texture.getHandleId();
        if (currentId == lastHandleId) return;
        lastHandleId = currentId;

        if (texture.isReady()) {
            requireCompute = true;
            computeSprites();
        }

        if (isRegistered) return;
        TextureStatusCallback.register(this);
        isRegistered = true;
        requireCompute = true;
    }

    private void computeSprites() {
        if (!requireCompute || texture == null || !texture.isReady()) return;

        sprites.clear();

        int instX = startPosition.x;
        int instY = texture.getHeight() - startPosition.y - spriteSize.y;
        for (int i = 0; i < numberOfSprites; i++) {
            if (instX + spriteSize.x > texture.getWidth()) {
                instX = startPosition.x;
                instY -= spriteSize.y + spriteSpacing.y;
            }

            if (instY < 0) {
                System.err.println("Error extracting sprite " + i + ". Sheet successfully extracted " + sprites.size() + " sprite(s)");
                break;
            }

            Sprite sprite = getSprite(instY, instX);
            sprites.add(sprite);

            instX += spriteSize.x + spriteSpacing.x;
        }

        requireCompute = false;
    }

    private Sprite getSprite(int instY, int instX) {
        float topY = (instY + spriteSize.y) / (float) texture.getHeight();
        float rightX = (instX + spriteSize.x) / (float) texture.getWidth();
        float leftX = instX / (float) texture.getWidth();
        float bottomY = instY / (float) texture.getHeight();

        Vector2f[] textureCoordinates = {
                new Vector2f(rightX, topY),
                new Vector2f(rightX, bottomY),
                new Vector2f(leftX, bottomY),
                new Vector2f(leftX, topY)
        };

        Sprite sprite = new Sprite();
        sprite.setTexture(this.texture);
        sprite.setTextureCoordinates(textureCoordinates);
        sprite.setWidth(spriteSize.x);
        sprite.setHeight(spriteSize.y);
        return sprite;
    }

    public Sprite spriteIndex(int index) {
        if (requireCompute) return null;

        if (index < 0 || index >= numberOfAvailableSprites()) {
            System.err.println("Sprite index " + index + " does not exist. Number of sprite in this sheet: " + numberOfAvailableSprites());
            return null;
        }

        return sprites.get(index);
    }

    public void setTexture(Texture newTexture) {
        if (texture == newTexture) return;
        if (isRegistered) {
            TextureStatusCallback.unRegister(this);
            isRegistered = false;
        }

        texture = newTexture;
        sprites.clear();

        textureReadyCheck(newTexture);
    }

    public int numberOfAvailableSprites() {
        return sprites.size();
    }

    public Vector2i getSpriteSize() {
        return spriteSize;
    }

    public Vector2i getStartPosition() {
        return startPosition;
    }

    public Vector2i getSpriteSpacing() {
        return spriteSpacing;
    }

    @Override
    public void onTextureStatusChange(int handleId, TextureHandle.Status status) {
        if (texture == null || texture.getHandleId() != handleId) return;

        switch (status) {
            case READY -> {
                if (requireCompute) computeSprites();
            }
            case DISPOSED, FAILED -> {
                if (isRegistered) {
                    TextureStatusCallback.unRegister(this);
                    isRegistered = false;
                }
                requireCompute = false;
                lastHandleId = -1;
            }
        }
    }

    public void dispose() {
        if (isRegistered) {
            TextureStatusCallback.unRegister(this);
            isRegistered = false;
        }

        sprites.clear();
    }
}
