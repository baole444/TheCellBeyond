package components;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import TheCellBeyond.internal.ResourceStatusListener;
import editor.EditorWidget;
import imgui.ImGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.DebugDraw;
import render.commands.RenderCommand;
import render.commands.TextCommand;
import render.text.*;
import utility.*;

import java.util.Objects;

public class TextRenderer extends Component2D implements ResourceStatusListener {
    private String text = "Text";
    private AssetReference assetReference = new AssetReference(Settings.FontPath.NotoSansMono);
    private float point = 12;
    private final Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private String glyphRangeName = "ASCII";
    private HorizontalAlignment hAlign = HorizontalAlignment.Left;
    private VerticalAlignment vAlign = VerticalAlignment.Top;
    private transient ResourceID fontRID;
    private final transient Vector2f textDimensions = new Vector2f();

    public TextRenderer() {
        String name = TextRenderer.class.getSimpleName();
        super(name);
    }

    public TextRenderer(String text, String fontPath, float point, Vector4f color, GlyphRange glyphRange) {
        this.text = text;
        this.assetReference = new AssetReference(fontPath);
        this.point = point;
        if (color != null) this.color.set(color);
        this.glyphRangeName = glyphRange.name();
    }

    @Override
    public void onResourceStatusChange(ResourceID RID, ResourceStatus status) {
        if (status != ResourceStatus.READY || !RID.equals(fontRID)) return;
        calculateTextDimensions();
        renderDirty = true;
    }

    @Override
    protected void onStart() {
        validateFields();
        ResourceStatusCallback.register(this);
        requestLoadFont();
    }

    @Override
    protected void onEditorStart() {
        validateFields();
        ResourceStatusCallback.register(this);
        requestLoadFont();
    }

    @Override
    protected void onUpdate(float dt) {
        if (fontRID == null) requestLoadFont();
        super.onUpdate(dt);
    }

    @Override
    protected void onEditorUpdate(float dt) {
        float xOffset = 0;
        if (hAlign != null) {
            switch (hAlign) {
                case Centre -> xOffset = -textDimensions.x / 2.0f;
                case Right -> xOffset = -textDimensions.x;
            }
        }
        float yOffset = 0;
        if (vAlign != null) {
            switch (vAlign) {
                case Middle -> yOffset = -textDimensions.y / 2.0f;
                case Bottom -> yOffset = -textDimensions.y;
            }
        }
        Vector2f start = new Vector2f(effectiveTransform().position).add(xOffset, yOffset);
        DebugDraw.addLine2(start, new Vector2f(start).add(textDimensions.x, 0), new Vector4f(0.8f, 0.2f, 0.2f, 1.0f), 1);
        super.onEditorUpdate(dt);
    }

    @Override
    protected void onTransformDirty() {
        renderDirty = true;
    }

    @Override
    protected void additionalImGuiLogic() {
        String textInput = EditorWidget.inputTextWithIME("Text", text, 1024, this);
        setText(textInput);
        String currentPath = assetReference != null ? assetReference.canonicalPath() : "";
        String fontPathInput = EditorWidget.inputText("Font Path", currentPath, this);
        if (!fontPathInput.equals(currentPath)) {
            UnifiedPaths resolver = UnifiedPaths.get();
            AssetReference newRef = new AssetReference(fontPathInput);
            if (resolver.exists(newRef.resolvedPath())) {
                this.assetReference = newRef;
                requestLoadFont();
            }
        }
        float fontSizeInput = EditorWidget.dragFloatCtrl("Font Size", point, this);
        if (fontSizeInput != point) {
            this.point = Math.abs(fontSizeInput);
            requestLoadFont();
        }
        if (EditorWidget.colorCtrl("Color", color, this)) renderDirty = true;
        ImGui.text("Alignment");
        ImGui.indent();
        if (ImGui.beginCombo("Horizontal", hAlign == null ? "Select one..." : hAlign.toString())) {
            for (HorizontalAlignment align : HorizontalAlignment.values()) {
                if (!ImGui.selectable(align.toString(), align == hAlign)) continue;
                hAlign = align;
                renderDirty = true;
            }
            ImGui.endCombo();
        }
        if (ImGui.beginCombo("Vertical", vAlign == null ? "Select one..." : vAlign.toString())) {
            for (VerticalAlignment align : VerticalAlignment.values()) {
                if (ImGui.selectable(align.toString(), align == vAlign)) continue;
                vAlign = align;
                renderDirty = true;
            }
            ImGui.endCombo();
        }
        ImGui.unindent();
        if (ImGui.beginCombo("Glyph Range", glyphRangeName)) {
            for (GlyphRange range : GlyphRange.values()) {
                if (ImGui.selectable(range.getDescription(), range.name().equals(glyphRangeName))) setGlyphRange(range);
            }
            ImGui.endCombo();
        }
    }

    @Override
    protected void onDestroy() {
        ResourceStatusCallback.unregister(this);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (this.text.equals(text)) return;
        this.text = text;
        renderDirty = true;
        calculateTextDimensions();
    }

    public float getPoint() {
        return point;
    }

    public TCBFont getFont() {
        return fontRID != null ? AssetManager.get().getFont(fontRID) : null;
    }

    public Vector4f getColor() {
        return color;
    }

    public void setColor(Vector4f color) {
        if (this.color.equals(color)) return;
        this.color.set(color);
        renderDirty = true;
    }

    public String getFontPath() {
        return assetReference != null ? assetReference.canonicalPath() : "";
    }

    public void setFontPath(String fontPathInput) {
        if (fontPathInput == null) return;
        AssetReference newRef = new AssetReference(fontPathInput);
        if (Objects.equals(newRef, assetReference)) return;
        UnifiedPaths resolver = UnifiedPaths.get();
        if (!resolver.exists(newRef.resolvedPath())) return;
        this.assetReference = newRef;
        requestLoadFont();
    }

    public boolean isTextDirty() {
        return renderDirty;
    }

    public void clearDirty() {
        renderDirty = false;
    }

    public Vector2f getTextDimensions() {
        return textDimensions;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return hAlign;
    }

    public void setHorizontalAlignment(HorizontalAlignment hAlign) {
        if (hAlign == null || this.hAlign == hAlign) return;
        this.hAlign = hAlign;
        renderDirty = true;
    }

    public VerticalAlignment getVerticalAlignment() {
        return vAlign;
    }

    public void setVerticalAlignment(VerticalAlignment vAlign) {
        if (vAlign == null || this.vAlign == vAlign) return;
        this.vAlign = vAlign;
        renderDirty = true;
    }

    public GlyphRange getGlyphRange() {
        return GlyphRange.valueOf(this.glyphRangeName);
    }

    public String getGlyphRangeName() {
        return glyphRangeName;
    }

    public void setGlyphRange(GlyphRange glyphRange) {
        if (Objects.equals(this.glyphRangeName, glyphRange.name())) return;
        glyphRangeName = glyphRange.name();
        requestLoadFont();
        renderDirty = true;
    }

    private void calculateTextDimensions() {
        TCBFont font = fontRID != null ? AssetManager.get().getFont(fontRID) : null;
        if (font == null || text.isEmpty()) {
            textDimensions.zero();
            return;
        }
        float scaledFontSize = WorldUnit.pixelToWorld(font.fontSizePixels());
        float lineWidth = 0;
        float maxLineWidth = 0;
        float lineCount = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lineCount++;
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
                lineWidth = 0;
                continue;
            }
            CharMetric charMetric = font.charMetric(c);
            if (charMetric != null) lineWidth += WorldUnit.pixelToWorld(charMetric.advance());
        }
        maxLineWidth = Math.max(maxLineWidth, lineWidth);
        float height = scaledFontSize * lineCount;
        float width = lineCount > 1 ? maxLineWidth : lineWidth;
        textDimensions.set(width, height);
    }

    private void requestLoadFont() {
        GlyphRange range = GlyphRange.valueOf(glyphRangeName);
        ResourceID newRID = AssetManager.get().loadFont(assetReference.canonicalPath(), range, point);
        if (Objects.equals(newRID, fontRID)) return;
        fontRID = newRID;
        TCBFont existing = AssetManager.get().getFont(fontRID);
        if (existing == null || !existing.loaded()) return;
        calculateTextDimensions();
        renderDirty = true;
    }

    //TODO: need proper null safe resolution later
    private void validateFields() {
        if (text == null) text = "";
        if (hAlign == null) hAlign = HorizontalAlignment.Left;
        if (vAlign == null) vAlign = VerticalAlignment.Top;
    }

    @Override
    public RenderCommand buildRenderCommand() {
        TextCommand command = TextCommand.acquire();
        command.submitterID = gameObject != null ? gameObject.getUID() : 0;
        command.text = text;
        command.fontRID = fontRID;
        command.points = point;
        command.horizontalAlignment = hAlign == null ? HorizontalAlignment.Left : hAlign;
        command.verticalAlignment = vAlign == null ? VerticalAlignment.Top : vAlign;
        command.modulate.set(color);
        RenderCommand renderCommand = super.buildRenderCommand();
        if (renderCommand == null) return command;
        renderCommand.next = command;
        return renderCommand;
    }
}
