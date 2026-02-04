package editor.template;

import components.SpriteRenderer;
import editor.EditorColors;
import editor.ImEditorGui;
import editor.payload.SpriteDragDropPayload;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.texture.Sprite;
import utility.TextureScale;

import java.util.UUID;

/**
 * Template for {@link SpriteRenderer}'s editor UI.
 */
class SpriteRendererTemplate implements ComponentTemplate<SpriteRenderer> {
    private static final SpriteRendererTemplate instance = new SpriteRendererTemplate();
    private SpriteRendererTemplate() {}

    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(SpriteRenderer component) {
        float availX = ImGui.getContentRegionAvailX();
        ImGui.text("Sprite: ");

        UUID uuid = component.getUUID();
        Sprite sprite = component.sprite();
        if (sprite != null && sprite.getTexture() != null) {
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.2f, 0.2f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.3f, 0.3f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.2f, 0.2f, 1.0f);
            if (ImGui.button("Clear sprite##SpriteRenderer_ClearSprite_" + uuid)) component.sprite(null);
            ImGui.popStyleColor(3);
        }

        float previewLimitY = 160.0f;
        if (sprite == null || sprite.getTexture() == null) {
            if (ImGui.beginChild("##SpriteRender_DropTarget_Region_" + component.getUUID(), ImGui.getContentRegionAvailX(), previewLimitY, true)) {
                ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.InstructionHighLight);
                ImGui.textWrapped("No sprite assigned. Drag and drop a sprite from Sprite list here.");
                ImGui.popStyleColor(1);
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
        acceptDragDrop(component);

        Vector4f color = new Vector4f(component.color());
        if (ImEditorGui.colorCtrl("Color", color, component)) {
            component.color(color);
        }

        ImGui.indent();
        ImBoolean flipHState = new ImBoolean(component.flipHorizontally());
        ImBoolean flipVState = new ImBoolean(component.flipVertically());
        String compositeID = "Flip axis##SpriteRenderer_FlipAxis_" + uuid;
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (open) {
            if (ImGui.checkbox("Horizontal##SpriteRenderer_FlipHorizontal_Checkbox_" + uuid, flipHState)) component.flipHorizontally(flipHState.get());
            if (ImGui.checkbox("Vertical##SpriteRenderer_FlipVertical_Checkbox_" + uuid, flipVState)) component.flipVertically(flipVState.get());
        }
        ImGui.unindent();
    }

    private static void acceptDragDrop(SpriteRenderer component) {
        if (!ImGui.beginDragDropTarget()) return;
        Object payload = ImGui.acceptDragDropPayload(SpriteDragDropPayload.getPayloadType());
        if (payload == null) {
            ImGui.endDragDropTarget();
            return;
        }
        Sprite dropSprite = SpriteDragDropPayload.getPayload();
        if (dropSprite != null) {
            component.sprite(dropSprite);
            SpriteDragDropPayload.clearPayload();
        }
        ImGui.endDragDropTarget();
    }

    /**
     * Render the content of {@link #editorUI(SpriteRenderer)}
     * @param spriteRenderer the context component
     */
    static void render(SpriteRenderer spriteRenderer) {
        if (spriteRenderer == null) return;
        instance.editorUI(spriteRenderer);
    }
}
