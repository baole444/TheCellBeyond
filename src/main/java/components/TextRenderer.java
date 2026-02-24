package components;

import editor.EditorWidget;
import imgui.ImGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.DebugDraw;
import render.text.*;
import utility.AssetReference;
import utility.UnifiedPaths;
import utility.Settings;
import utility.WorldUnit;

import java.util.Objects;

public class TextRenderer extends Component2D implements FontStatusCallback {
    private String text = "Text";
    private AssetReference assetReference = new AssetReference(Settings.FontPath.NotoSansMono);
    private float point = 12;
    private final Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean isTextDirty = true;
    private String glyphRangeName = "ASCII";

    private transient TCBFont font;
    private final transient Vector2f textDimensions = new Vector2f();
    private transient FontRequest currentRequest = null;
    private transient boolean pendingRequest = false;

    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum VerticalAlignment {
        TOP, MIDDLE, BOTTOM
    }

    private HorizontalAlignment hAlign = HorizontalAlignment.LEFT;
    private VerticalAlignment vAlign = VerticalAlignment.TOP;

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

    public TextRenderer(String text, String fontPath, float point, Vector4f color, Vector2f position, GlyphRange glyphRange) {
        this.text = text;
        this.assetReference = new AssetReference(fontPath);
        this.point = point;
        if (color != null) this.color.set(color);
        this.glyphRangeName = glyphRange.name();
        globalPosition(position);
    }

    private void requestLoadFont() {
        if (pendingRequest) {
            return;
        }

        GlyphRange range = GlyphRange.valueOf(glyphRangeName);
        FontRequest request = new FontRequest(assetReference, point, range);

        if (currentRequest == null || !currentRequest.equals(request)) {
            currentRequest = request;
            pendingRequest = true;
            FontManager.get().requestFont(request, this);
        }
    }

    @Override
    public void onFontReady(TCBFont loadedFont, FontRequest request) {
        if (currentRequest != null && currentRequest.equals(request)) {
            this.font = loadedFont;
            this.pendingRequest = false;
            calculateTextDimensions();
            this.isTextDirty = true;
        }
    }

    private void calculateTextDimensions() {
        if (font == null || text.isEmpty()) {
            textDimensions.set(0, 0);
            return;
        }

        float scaledFontSize = WorldUnit.pixelToWorld(font.getFontSizePixel());

        float width = 0;
        float height;
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

            float advance = WorldUnit.pixelToWorld(font.getCharInfo(c).advance());
            lineWidth += advance;
            width += advance;
        }

        maxLineWidth = Math.max(maxLineWidth, lineWidth);
        height = scaledFontSize * lineCount;

        if (lineCount > 1) {
            width = maxLineWidth;
        }

        textDimensions.set(width, height);
    }

    @Override
    protected void onStart() {
        requestLoadFont();
    }

    @Override
    protected void onEditorStart() {
        requestLoadFont();
    }

    @Override
    protected void onUpdate(float dt) {
        if (font == null
                || !assetReference.equals(currentRequest.fontAsset())
                || point != currentRequest.point()
                || !glyphRangeName.equals(currentRequest.glyphRange().name())
        ) requestLoadFont();
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
                this.pendingRequest = false;
                requestLoadFont();
            }
        }

        float fontSizeInput = EditorWidget.dragFloatCtrl("Font Size", point, this);
        if (fontSizeInput != point) {
            this.point = Math.abs(fontSizeInput);
            this.pendingRequest = false;
            requestLoadFont();
        }

        if (EditorWidget.colorCtrl("Color", color, this)) {
            this.isTextDirty = true;
        }

        ImGui.text("Alignment");
        ImGui.indent();
        if (ImGui.beginCombo("Horizontal", hAlign.toString())) {
            for (HorizontalAlignment align : HorizontalAlignment.values()) {
                if (ImGui.selectable(align.toString(), align == hAlign)) {
                    hAlign = align;
                    this.isTextDirty = true;
                }
            }

            ImGui.endCombo();
        }

        if (ImGui.beginCombo("Vertical", vAlign.toString())) {
            for (VerticalAlignment align : VerticalAlignment.values()) {
                if (ImGui.selectable(align.toString(), align == vAlign)) {
                    vAlign = align;
                    this.isTextDirty = true;
                }
            }

            ImGui.endCombo();
        }

        ImGui.unindent();

        if (ImGui.beginCombo("Glyph Range", glyphRangeName)) {
            for (GlyphRange range : GlyphRange.values()) {
                if (ImGui.selectable(range.getDescription(), range.name().equals(glyphRangeName))) {
                    setGlyphRange(range);
                }
            }

            ImGui.endCombo();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            this.isTextDirty = true;
            calculateTextDimensions();
        }
    }

    public float getPoint() {
        return point;
    }

    public TCBFont getFont() {
        if (font == null) {
            requestLoadFont();
        }

        return font;
    }

    public Vector4f getColor() {
        return color;
    }

    public void setColor(Vector4f color) {
        if (!this.color.equals(color)) {
            this.color.set(color);
            this.isTextDirty = true;
        }
    }

    public String getFontPath() {
        return assetReference != null ? assetReference.canonicalPath() : "";
    }

    public void setFontPath(String fontPathInput) {
        if  (fontPathInput == null) return;

        AssetReference newRef = new AssetReference(fontPathInput);

        if (!Objects.equals(newRef, assetReference)) {
            UnifiedPaths resolver = UnifiedPaths.get();
            if (resolver.exists(newRef.resolvedPath())) {
                this.assetReference = newRef;
                this.pendingRequest = false;
                requestLoadFont();
            }
        }
    }

    public boolean isTextDirty() {
        return isTextDirty;
    }

    public void clearDirty() {
        this.isTextDirty = false;
    }

    public Vector2f getTextDimensions() {
        return textDimensions;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return hAlign;
    }

    public void setHorizontalAlignment(HorizontalAlignment hAlign) {
        if (this.hAlign != hAlign) {
            this.hAlign = hAlign;
            this.isTextDirty = true;
        }
    }

    public VerticalAlignment getVerticalAlignment() {
        return vAlign;
    }

    public void setVerticalAlignment(VerticalAlignment vAlign) {
        if (this.vAlign != vAlign) {
            this.vAlign = vAlign;
            this.isTextDirty = true;
        }
    }

    public GlyphRange getGlyphRange() {
        return GlyphRange.valueOf(this.glyphRangeName);
    }

    public String getGlyphRangeName() {
        return glyphRangeName;
    }

    public void setGlyphRange(GlyphRange glyphRange) {
        if (!Objects.equals(this.glyphRangeName, glyphRange.name())) {
            this.glyphRangeName = glyphRange.name();
            this.pendingRequest = false;
            requestLoadFont();
            this.isTextDirty = true;
        }
    }
}
