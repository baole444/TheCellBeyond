package components;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.DebugDraw;
import render.commands.RenderCommand;
import render.commands.TextCommand;
import render.text.*;
import scripting.API;
import utility.*;

import java.util.Objects;

@API
public class TextRenderer extends Component2D {
    private String text = "Text";
    private AssetReference assetReference = new AssetReference(Settings.FontPath.NotoSansMono);
    private float point = 12;
    private final Vector4f color = new Vector4f(1.0f);
    private String glyphRangeName = "ASCII";
    private HorizontalAlignment hAlign = HorizontalAlignment.Left;
    private VerticalAlignment vAlign = VerticalAlignment.Top;
    private transient ResourceID fontRID;
    private transient ResourceTracker fontTracker;
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

    public void onFontStatusChange(ResourceID RID, ResourceStatus status) {
        if (status != ResourceStatus.Ready) return;
        calculateTextDimensions();
        renderDirty = true;
    }

    @Override
    protected void internalStart() {
        super.internalStart();
        validateFields();
        requestLoadFont();
    }

    @Override
    protected void internalEditorStart() {
        super.internalEditorStart();
        validateFields();
        requestLoadFont();
    }

    @Override
    protected void internalUpdate(float dt) {
        if (fontRID == null) requestLoadFont();
        super.internalUpdate(dt);
    }

    @Override
    protected void internalEditorUpdate(float dt) {
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
        super.internalEditorUpdate(dt);
    }

    @Override
    protected void internalDestroy() {
        if (fontTracker == null) return;
        fontTracker.cancel();
        fontTracker = null;
    }

    public String text() {
        return text;
    }

    public void text(String text) {
        if (this.text.equals(text)) return;
        this.text = text;
        renderDirty = true;
        calculateTextDimensions();
    }

    public float point() {
        return point;
    }

    public TCBFont font() {
        return fontRID != null ? AssetManager.getFont(fontRID) : null;
    }

    public Vector4f color() {
        return color;
    }

    public void color(Vector4f color) {
        if (color == null || this.color.equals(color)) return;
        this.color.set(color);
        renderDirty = true;
    }

    public String fontPath() {
        return assetReference != null ? assetReference.canonicalPath() : "";
    }

    public void fontPath(String fontPathInput) {
        if (fontPathInput == null) return;
        AssetReference newRef = new AssetReference(fontPathInput);
        if (Objects.equals(newRef, assetReference)) return;
        if (!UnifiedPaths.exists(newRef.resolvedPath())) return;
        this.assetReference = newRef;
        requestLoadFont();
    }

    public void point(float newPoint) {
        newPoint = Math.abs(newPoint);
        if (newPoint == point) return;
        point = newPoint;
        requestLoadFont();
    }

    public boolean isTextDirty() {
        return renderDirty;
    }

    public void clearDirty() {
        renderDirty = false;
    }

    public Vector2f textDimensions() {
        return textDimensions;
    }

    public HorizontalAlignment horizontalAlignment() {
        return hAlign;
    }

    public void horizontalAlignment(HorizontalAlignment hAlign) {
        if (hAlign == null || this.hAlign == hAlign) return;
        this.hAlign = hAlign;
        renderDirty = true;
    }

    public VerticalAlignment verticalAlignment() {
        return vAlign;
    }

    public void verticalAlignment(VerticalAlignment vAlign) {
        if (vAlign == null || this.vAlign == vAlign) return;
        this.vAlign = vAlign;
        renderDirty = true;
    }

    public GlyphRange glyphRange() {
        return GlyphRange.valueOf(this.glyphRangeName);
    }

    public String glyphRangeName() {
        return glyphRangeName;
    }

    public void glyphRange(GlyphRange glyphRange) {
        if (Objects.equals(this.glyphRangeName, glyphRange.name())) return;
        glyphRangeName = glyphRange.name();
        requestLoadFont();
        renderDirty = true;
    }

    private void calculateTextDimensions() {
        TCBFont font = fontRID != null ? AssetManager.getFont(fontRID) : null;
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
        ResourceID newRID = AssetManager.loadFont(assetReference.canonicalPath(), range, point);
        if (Objects.equals(newRID, fontRID)) return;
        fontRID = newRID;
        if (fontTracker != null) fontTracker.cancel();
        fontTracker = AssetManager.track(fontRID, this::onFontStatusChange);
        TCBFont existing = AssetManager.getFont(fontRID);
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
        command.textDimension.set(textDimensions);
        command.markChanged();
        return command;
    }
}
