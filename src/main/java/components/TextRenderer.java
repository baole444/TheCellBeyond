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
import render.text.*;
import utility.*;

import java.util.Objects;

public class TextRenderer extends Component2D implements ResourceStatusListener {
    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum VerticalAlignment {
        TOP, MIDDLE, BOTTOM
    }

    private String text = "Text";
    private AssetReference assetReference = new AssetReference(Settings.FontPath.NotoSansMono);
    private float point = 12;
    private final Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean isTextDirty = true;
    private String glyphRangeName = "ASCII";
    private HorizontalAlignment hAlign = HorizontalAlignment.LEFT;
    private VerticalAlignment vAlign = VerticalAlignment.TOP;
    private transient TCBFont font;
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
        TCBFont loaded = AssetManager.get().getFont(RID);
        if (loaded != null) applyFont(loaded);
    }

    @Override
    protected void onStart() {
        ResourceStatusCallback.register(this);
        requestLoadFont();
    }

    @Override
    protected void onEditorStart() {
        ResourceStatusCallback.register(this);
        requestLoadFont();
    }

    @Override
    protected void onUpdate(float dt) {
        if (font == null || fontRID == null) requestLoadFont();
        super.onUpdate(dt);
    }

    @Override
    protected void onEditorUpdate(float dt) {
        Vector2f pos = new Vector2f(getEffectiveTransform().position);
        DebugDraw.addLine2(pos, new Vector2f(pos).add(textDimensions.x, 0), new Vector4f(0.8f, 0.2f, 0.2f, 1.0f), 1);
        super.onEditorUpdate(dt);
    }

    @Override
    protected void onTransformDirty() {
        isTextDirty = true;
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
        if (EditorWidget.colorCtrl("Color", color, this)) isTextDirty = true;
        ImGui.text("Alignment");
        ImGui.indent();
        if (ImGui.beginCombo("Horizontal", hAlign.toString())) {
            for (HorizontalAlignment align : HorizontalAlignment.values()) {
                if (!ImGui.selectable(align.toString(), align == hAlign)) continue;
                hAlign = align;
                isTextDirty = true;
            }
            ImGui.endCombo();
        }
        if (ImGui.beginCombo("Vertical", vAlign.toString())) {
            for (VerticalAlignment align : VerticalAlignment.values()) {
                if (ImGui.selectable(align.toString(), align == vAlign)) continue;
                vAlign = align;
                isTextDirty = true;
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
        this.isTextDirty = true;
        calculateTextDimensions();
    }

    public float getPoint() {
        return point;
    }

    public TCBFont getFont() {
        if (font == null) requestLoadFont();
        return font;
    }

    public Vector4f getColor() {
        return color;
    }

    public void setColor(Vector4f color) {
        if (this.color.equals(color)) return;
        this.color.set(color);
        isTextDirty = true;
    }

    public String getFontPath() {
        return assetReference != null ? assetReference.canonicalPath() : "";
    }

    public void setFontPath(String fontPathInput) {
        if  (fontPathInput == null) return;
        AssetReference newRef = new AssetReference(fontPathInput);
        if (Objects.equals(newRef, assetReference)) return;
        UnifiedPaths resolver = UnifiedPaths.get();
        if (!resolver.exists(newRef.resolvedPath())) return;
        this.assetReference = newRef;
        requestLoadFont();
    }

    public boolean isTextDirty() {
        return isTextDirty;
    }

    public void clearDirty() {
        isTextDirty = false;
    }

    public Vector2f getTextDimensions() {
        return textDimensions;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return hAlign;
    }

    public void setHorizontalAlignment(HorizontalAlignment hAlign) {
        if (this.hAlign == hAlign) return;
        this.hAlign = hAlign;
        isTextDirty = true;
    }

    public VerticalAlignment getVerticalAlignment() {
        return vAlign;
    }

    public void setVerticalAlignment(VerticalAlignment vAlign) {
        if (this.vAlign == vAlign) return;
        this.vAlign = vAlign;
        isTextDirty = true;
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
        isTextDirty = true;
    }

    private void calculateTextDimensions() {
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
        if (existing != null && existing.loaded()) applyFont(existing);
    }

    private void applyFont(TCBFont loadedFont) {
        font = loadedFont;
        calculateTextDimensions();
        isTextDirty = true;
    }
}
