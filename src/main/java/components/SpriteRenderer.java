package components;

import editor.ImEditorGui;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;
import render.texture.Sprite;
import utility.TextureScale;
import utility.WorldUnit;

import java.util.Objects;

/**
 * SpriteRenderer hold a Sprite, tint color vector and dirty flag which are used by Renderer.<br>
 * The sprite dirty flag is set when Sprite, tint color or transformation updated.
 * This flag is volatile and clear by the Renderer.
 */
public class SpriteRenderer extends SpatialComponent {
    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private volatile Sprite sprite = new Sprite();
    private volatile boolean flipHorizontally = false;
    private volatile boolean flipVertically = false;

    private volatile transient boolean isSpriteDirty = true;

    @Override
    protected void additionalImGuiLogic() {
        float availX = ImGui.getContentRegionAvailX();
        ImGui.text("Sprite: ");

        if (sprite != null && sprite.getTexture() != null) {
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
            if (ImGui.button("Clear sprite")) {
                setSprite(new Sprite());
                if (gameObject != null) gameObject.setDirty(true);
            }
            ImGui.popStyleColor(3);
        }

        float previewLimitY = 160.0f;
        if (sprite == null || sprite.getTexture() == null) {
            if (ImGui.beginChild("Mock_sprite_drop_area", ImGui.getContentRegionAvailX(), previewLimitY, true)) {
                ImGui.beginDisabled();
                ImGui.textWrapped("No sprite assigned. Drag and drop a sprite from Sprite list here.");
                ImGui.endDisabled();
                ImGui.endChild();
            }
        } else {
            int textureId = sprite.getTextureID();
            Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

            Vector2f previewSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), availX, previewLimitY);
            ImGui.image(textureId, previewSize.x, previewSize.y,
                    textureCoordinates[2].x, textureCoordinates[0].y,
                    textureCoordinates[0].x, textureCoordinates[2].y
            );
        }

        if (ImGui.beginDragDropTarget()) {
            if (ImGui.isWindowHovered()) {
                ImDrawList drawList = ImGui.getWindowDrawList();
                ImVec2 min = ImGui.getItemRectMin();
                ImVec2 max = ImGui.getItemRectMax();
                drawList.addRectFilled(min, max, ImGui.colorConvertFloat4ToU32(0.2f, 0.7f, 0.2f, 0.3f));
                drawList.addRect(min, max, ImGui.colorConvertFloat4ToU32(0.2f, 0.7f, 0.2f, 0.8f), 0 , 0 , 2);
            }

            Object payload = ImGui.acceptDragDropPayload(SpriteDragDropPayload.getPayloadType());

            if (payload == null) {
                ImGui.endDragDropTarget();
                return;
            }

            Sprite dropSprite = SpriteDragDropPayload.getPayload();

            if (dropSprite != null) setSprite(dropSprite);

            if (gameObject != null) gameObject.setDirty(true);

            ImGui.endDragDropTarget();
        }

        if (ImEditorGui.colorCtrl("Color", this.color, this)) {
            this.isSpriteDirty = true;
        }

        ImGui.indent();
        ImBoolean flipHState = new ImBoolean(flipHorizontally);
        ImBoolean flipVState = new ImBoolean(flipVertically);
        String compositeID = "Flip axis##" + getUUID();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (open) {
            if (ImGui.checkbox("Horizontal##" + getUUID(), flipHState)) flipHorizontally(flipHState.get());
            if (ImGui.checkbox("Vertical##" + getUUID(), flipVState)) flipVertically(flipVState.get());
        }
        ImGui.unindent();
    }

    @Override
    protected void additionalDirtyFlagLogic() {
        isSpriteDirty = true;
    }

    /**
     * Update the dirty flag for this SpriteRenderer.
     * @param needsUpdate true to set sprite dirty
     */
    public void setSpriteDirty(boolean needsUpdate) {
        isSpriteDirty = needsUpdate;
        if (!needsUpdate && sprite != null) sprite.rendererUpdated();
    }

    /**
     * Get the tint color that is applied onto the Sprite.
     * @return tint color vector
     */
    public Vector4f getColor() {
        return this.color;
    }

    /**
     * Get the width and height of the Sprite in pixel value.
     * @return size vector of the sprite
     */
    public Vector2f getSpriteSize() {
        if (sprite == null) return new Vector2f(1.0f);

        return new Vector2f(sprite.getWidth(), sprite.getHeight());
    }

    /**
     * Get the width and height of the Sprite in world unit value.
     * This simply pass {@link SpriteRenderer#getSpriteSize()} into {@link WorldUnit#pixelToWorld(Vector2f)}
     * @return size vector of the sprite
     */
    public Vector2f getSpriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(getSpriteSize());
    }

    /**
     * Get the texture of the Sprite.
     * @return texture reference of the sprite or null if the sprite/texture is null
     */
    public Texture getTexture() {
        return sprite != null ? sprite.getTexture() : null;
    }

    /**
     * Get the texture coordinates of the Sprite.
     * @return the array of 4 UV corners in the follow order: {@code (1,1), (1,0), (0,0), (0,1)}
     */
    public Vector2f[] getTextureCoordinates() {
        return sprite != null ? sprite.getTextureCoordinates() : null;
    }

    /**
     * Set the sprite used by this SpriteRenderer.
     * This will trigger sprite dirty flag if the new sprite is different.<br>
     * Set sprite to {@code null} or {@code new Sprite()} will disable rendering of this component.
     * @param sprite the new sprite
     */
    public void setSprite(Sprite sprite) {
        if (Objects.equals(this.sprite, sprite)) return;
        this.sprite = sprite;
        isSpriteDirty = true;
    }

    /**
     * Set a new tint color for the Sprite.
     * This will trigger sprite dirty flag if the new color is different.<br>
     * Set the color to {@code (1, 1, 1, 1)} will disable tint color.
     * @param color the new color vector
     */
    public void setColor(Vector4f color) {
        if(color == null) return;
        if (Objects.equals(this.color, color)) return;
        isSpriteDirty = true;
        this.color.set(color);
    }

    /**
     * Set the horizontal flip status for the Sprite.
     * This will trigger sprite dirty flag if the state changed.<br>
     * This will not affect the original texture.
     * @param flip true to fip the sprite
     */
    public void flipHorizontally(boolean flip) {
        if (flip == flipHorizontally) return;

        flipHorizontally = flip;
        isSpriteDirty = true;
    }

    /**
     * Set the vertical flip status for the Sprite.
     * This will trigger sprite dirty flag if the state changed.<br>
     * This will not affect the original texture.
     * @param flip true to fip the sprite
     */
    public void flipVertically(boolean flip) {
        if (flip == flipVertically) return;

        flipVertically = flip;
        isSpriteDirty = true;
    }

    /**
     * Check the sprite dirty flag of this SpriteRenderer.
     * @return true if the sprite needs update
     */
    public boolean isSpriteDirty() {
        if (sprite != null && sprite.requestRendererUpdate()) isSpriteDirty = true;
        return isSpriteDirty;
    }

    /**
     * Check the horizontal flip status of this SpriteRenderer.
     * @return true if the sprite is flipped vertically
     */
    public boolean isFlipHorizontally() {
        return flipHorizontally;
    }

    /**
     * Check the vertical flip status of this SpriteRenderer.
     * @return true if the sprite is flipped vertically
     */
    public boolean isFlipVertically() {
        return flipVertically;
    }
}
