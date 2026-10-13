package editor.template;

import components.Component2D;
import components.SpriteRenderer;
import editor.EditorColors;
import editor.EditorWidget;
import editor.payload.SpriteDragDropPayload;
import imgui.ImGui;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.texture.Sprite;
import utility.TextureScale;

import java.util.UUID;

/**
 * Template for {@link SpriteRenderer}'s editor UI.
 */
final class SpriteRendererTemplate implements IComponentTemplate<SpriteRenderer> {
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
        if (ImGui.getContentRegionAvailX() <= 0.0f || ImGui.getContentRegionAvailY() <= 0.0f) return;
        ImGui.spacing();
        UUID uuid = component.getUUID();
        boolean openSprite = ImGui.collapsingHeader("SpriteRenderer##SpriteRenderer_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openSprite) return;
        float availX = ImGui.getContentRegionAvailX();
        ImGui.text("Sprite: ");
        Sprite sprite = component.sprite();
        if (sprite != null && sprite.hasTexture()) {
            ImGui.sameLine();
            EditorColors.RedButton.create(() -> {
                if (ImGui.button("Clear sprite##SpriteRenderer_ClearSprite_" + uuid)) component.sprite(null);
            });
        }
        float previewLimitY = 160.0f;
        if (sprite == null || !sprite.hasTexture()) {
            if (ImGui.beginChild("##SpriteRender_DropTarget_Region_" + component.getUUID(), ImGui.getContentRegionAvailX(), previewLimitY, ImGuiChildFlags.Borders)) {
                ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.InstructionHighLight);
                ImGui.textWrapped("No sprite assigned. Drag and drop a sprite from Sprite list here.");
                ImGui.popStyleColor(1);
            }
            ImGui.endChild();
        } else {
            int textureId = sprite.textureID();
            Vector2f[] textureCoordinates = sprite.textureCoordinates();
            Vector2f previewSize = TextureScale.calculateFitDimension(sprite.width(), sprite.height(), availX, previewLimitY);
            ImGui.image(textureId, previewSize.x, previewSize.y,
                    textureCoordinates[2].x, textureCoordinates[0].y,
                    textureCoordinates[0].x, textureCoordinates[2].y
            );
        }
        acceptDragDrop(component);
        Vector4f color = new Vector4f(component.color());
        if (EditorWidget.colorCtrl("Color", color, component)) component.color(color);
        ImBoolean flipHState = new ImBoolean(component.flipHorizontally());
        ImBoolean flipVState = new ImBoolean(component.flipVertically());
        String compositeID = "Flip axis##SpriteRenderer_FlipAxis_" + uuid;
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (!open) return;
        if (ImGui.checkbox("Horizontal##SpriteRenderer_FlipHorizontal_Checkbox_" + uuid, flipHState)) component.flipHorizontally(flipHState.get());
        if (ImGui.checkbox("Vertical##SpriteRenderer_FlipVertical_Checkbox_" + uuid, flipVState)) component.flipVertically(flipVState.get());
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
     * Render the content of {@link #editorUI(SpriteRenderer)} and call {@link Component2DTemplate#render(Component2D)}.
     * @param spriteRenderer the context component
     */
    static void render(SpriteRenderer spriteRenderer) {
        if (spriteRenderer == null) return;
        instance.editorUI(spriteRenderer);
        Component2DTemplate.render(spriteRenderer);
    }
}
