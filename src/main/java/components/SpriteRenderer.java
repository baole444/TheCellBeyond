package components;

import editor.ImEditorGui;
import editor.payload.SpriteDragDropPayload;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.Texture;
import render.texture.Sprite;
import utility.TextureScale;
import utility.WorldUnit;

/**
 * A class dedicated to rendering a sprite, and it's life cycle.
 */
public class SpriteRenderer extends SpatialComponent {
    private final Vector4f color = new Vector4f(1, 1, 1 , 1);
    private Sprite sprite = new Sprite();

    private transient boolean isSpriteDirty = true;

    @Override
    protected void additionalImGuiLogic() {
        float sizeLimit = Math.min(ImGui.getContentRegionAvailX(), 160);
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

        if (sprite == null || sprite.getTexture() == null) {
            ImGui.beginChild("Mock_sprite_drop_area", sizeLimit, sizeLimit, true);
            ImGui.beginDisabled();
            ImGui.textWrapped("No sprite assigned. Drag and drop a sprite from Sprite list here.");
            ImGui.endDisabled();
            ImGui.endChild();
        } else {
            int textureId = sprite.getTextureID();
            Vector2f[] textureCoordinates = sprite.getTextureCoordinates();

            Vector2f previewSize = TextureScale.calculateFitDimension(sprite.getWidth(), sprite.getHeight(), sizeLimit, sizeLimit);
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
    }

    @Override
    protected void additionalDirtyFlagLogic() {
        if (!isSpriteDirty) isSpriteDirty = true;
    }

    @Override
    protected void additionalUpdateLogic(float dt) {
        if (!isSpriteDirty && sprite != null && sprite.getTextureCoordinates() != null) {
            isSpriteDirty = true;
        }
    }

    public void setSpriteDirty(boolean needsUpdate) {
        this.isSpriteDirty = needsUpdate;
    }

    public Vector4f getColor() {
        return this.color;
    }

    public Vector2f getSpriteSize() {
        if (sprite == null) return new Vector2f(1, 1);

        return new Vector2f(sprite.getWidth(), sprite.getHeight());
    }

    public Vector2f getSpriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(getSpriteSize());
    }

    public Texture getTexture() {
        return sprite != null ? sprite.getTexture() : null;
    }

    public Vector2f[] getTextureCoordinates() {
        return sprite != null ? sprite.getTextureCoordinates() : null;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
        isSpriteDirty = true;
    }

    public void setColor(Vector4f color) {
        if(!this.color.equals(color)) {
            isSpriteDirty = true;
            this.color.set(color);
        }
    }

    public boolean isSpriteDirty() {
        return isSpriteDirty;
    }

    public void setTexture(Texture texture) {
        this.sprite.setTexture(texture);
        isSpriteDirty = true;
    }
}
