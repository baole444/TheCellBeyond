package editor.template;

import components.Component2D;
import components.TextRenderer;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import render.text.GlyphRange;
import render.text.HorizontalAlignment;
import render.text.VerticalAlignment;

import java.util.Objects;
import java.util.UUID;

/**
 * Template for {@link TextRenderer}'s editor UI.
 */
final class TextRendererTemplate implements IComponentTemplate<TextRenderer> {
    private static final TextRendererTemplate instance = new TextRendererTemplate();
    private TextRendererTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(TextRenderer component) {
        ImGui.spacing();
        UUID uuid = component.getUUID();
        boolean openText = ImGui.collapsingHeader("TextRenderer##TextRenderer_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openText) return;
        String textInput = EditorWidget.inputTextWithIME("Text", component.text(), 1024, component);
        if (!Objects.equals(component.text(), textInput)) component.text(textInput);
        String currentPath = component.fontPath();
        String fontPathInput = EditorWidget.inputText("Font Path", currentPath, component);
        if (!Objects.equals(fontPathInput, currentPath)) component.fontPath(fontPathInput);
        component.point(EditorWidget.dragFloatCtrl("Font Size", component.point(), component));
        if (EditorWidget.colorCtrl("Color", component.color(), component)) component.renderDirty(true);
        ImGui.text("Alignment");
        ImGui.indent();
        HorizontalAlignment hAlign = component.horizontalAlignment();
        if (ImGui.beginCombo("Horizontal", hAlign == null ? "Select one..." : hAlign.toString())) {
            for (HorizontalAlignment align : HorizontalAlignment.values()) {
                if (!ImGui.selectable(align.toString(), align == hAlign)) continue;
                component.horizontalAlignment(align);
            }
            ImGui.endCombo();
        }
        VerticalAlignment vAlign = component.verticalAlignment();
        if (ImGui.beginCombo("Vertical", vAlign == null ? "Select one..." : vAlign.toString())) {
            for (VerticalAlignment align : VerticalAlignment.values()) {
                if (!ImGui.selectable(align.toString(), align == vAlign)) continue;
                component.verticalAlignment(align);
            }
            ImGui.endCombo();
        }
        ImGui.unindent();
        String glyphRangeName = component.glyphRangeName();
        if (ImGui.beginCombo("Glyph Range", glyphRangeName)) {
            for (GlyphRange range : GlyphRange.values()) {
                if (ImGui.selectable(range.getDescription(), range.name().equals(glyphRangeName))) component.glyphRange(range);
            }
            ImGui.endCombo();
        }
    }

    /**
     * Render the content of {@link #editorUI(TextRenderer)} and call {@link Component2DTemplate#render(Component2D)}.
     * @param textRenderer the context component
     */
    static void render(TextRenderer textRenderer) {
        if (textRenderer == null) return;
        instance.editorUI(textRenderer);
        Component2DTemplate.render(textRenderer);
    }
}
