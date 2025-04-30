package components;

import editor.ImEditorGui;
import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.text.AsyncFontManager;
import render.text.FontManager;
import render.text.GlyphRange;
import render.text.TCBFont;
import utility.Settings;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class TextComponent extends Component {
    private String text;
    private String fontPath;
    private int fontSize;
    private Vector4f color;
    private boolean isDirty = true;
    private String glyphRangeName = "ASCII";
    private boolean isProjectAsset = false;

    private transient TCBFont font;
    private transient Vector2f textDimensions = new Vector2f();
    private transient Vector2f worldPosition = null;
    private transient CompletableFuture<TCBFont> fontLoadingFuture = null;
    private transient boolean fontRequested = false;

    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum VerticalAlignment {
        TOP, MIDDLE, BOTTOM
    }

    private HorizontalAlignment hAlign = HorizontalAlignment.LEFT;
    private VerticalAlignment vAlign = VerticalAlignment.TOP;

    public TextComponent() {
        this.text = "";
        this.fontPath = Settings.PATH.CONSOLA;
        this.fontSize = 16;
        this.color = new Vector4f(1, 1, 1, 1);
    }

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
    }

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color, boolean isProjectAsset) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
        this.isProjectAsset = isProjectAsset;
    }

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color, GlyphRange glyphRange) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
        this.glyphRangeName = glyphRange.name();
    }

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color, GlyphRange glyphRange, boolean isProjectAsset) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
        this.glyphRangeName = glyphRange.name();
        this.isProjectAsset = isProjectAsset;
    }

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color, Vector2f position) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
        this.worldPosition = position;
    }

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color, Vector2f position, GlyphRange glyphRange) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
        this.glyphRangeName = glyphRange.name();
        this.worldPosition = position;
    }

    public Vector2f getWorldPosition() {
        if (worldPosition != null) {
            return worldPosition;
        }

        if (gameObject != null) {
            return gameObject.transform.position;
        }

        return new Vector2f(0.0f, 0.0f);
    }

    public void setWorldPosition(Vector2f position) {
        this.worldPosition = position;
        this.isDirty = true;
    }

    public boolean isDirectRendering() {
        return worldPosition != null;
    }

    private void requestLoadFont() {
        if (fontRequested) {
            return;
        }

        fontRequested = true;

        try {
            GlyphRange range = GlyphRange.valueOf(glyphRangeName);

            font = FontManager.get().getFont(fontPath, fontSize, range);

            if (!FontManager.get().isFontLoaded(fontPath, fontSize, range) && !FontManager.get().isFontLoading(fontPath, fontSize, range)) {
                fontLoadingFuture = FontManager.get().loadFontAsync(fontPath, fontSize, isProjectAsset, range, loadedFont -> {
                    font = loadedFont;
                    calculateTextDimensions();
                    isDirty = true;
                });
            }
        } catch (Exception e) {
            AsyncFontManager.LOGGER.warning("Error requesting font: " + e.getMessage());
            fontRequested = false;
        }
    }

    private void calculateTextDimensions() {
        if (font == null || text.isEmpty()) {
            textDimensions.set(0, 0);
            return;
        }

        float scaledFontSie = font.getFontSize() * Settings.WORLD_SCALE_FACTOR;

        float width = 0;
        float height = scaledFontSie;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                height += scaledFontSie;
                continue;
            }

            width += font.getCharInfo(c).advance() * Settings.WORLD_SCALE_FACTOR;
        }

        textDimensions.set(width, height);
    }

    @Override
    public void start() {
        requestLoadFont();
        calculateTextDimensions();
    }

    @Override
    public void update(float dt) {
        if (!fontRequested) {
            requestLoadFont();
        }
    }

    @Override
    public void imgui() {
        String textInput = ImEditorGui.inputTextWithIME("Text", text, 1024);
        if (!textInput.equals(text)) {
            this.text = textInput;
            this.isDirty = true;
            calculateTextDimensions();
        }

        String fontPathInput = ImEditorGui.inputText("Font Path", fontPath);
        if (!fontPathInput.equals(fontPath)) {
            this.fontPath = fontPathInput;
            requestLoadFont();
        }

        int fontSizeInput = ImEditorGui.dragIntCtrl("Font Size", fontSize);
        if (fontSizeInput != fontSize) {
            this.fontSize = Math.abs(fontSizeInput);
            requestLoadFont();
        }

        if (ImGui.beginCombo("Glyph Range", glyphRangeName)) {
            for (GlyphRange range : GlyphRange.values()) {
                if (ImGui.selectable(range.getDescription(), range.name().equals(glyphRangeName))) {
                    glyphRangeName = range.name();
                    fontRequested = false;
                    requestLoadFont();
                    this.isDirty = true;
                }
            }

            ImGui.endCombo();
        }

        if (ImEditorGui.colorCtrl("Color", color)) {
            this.isDirty = true;
        }

        if (ImGui.beginCombo("Horizontal Alignment", hAlign.toString())) {
            for (HorizontalAlignment align : HorizontalAlignment.values()) {
                if (ImGui.selectable(align.toString(), align == hAlign)) {
                    hAlign = align;
                    this.isDirty = true;
                }
            }

            ImGui.endCombo();
        }

        if (ImGui.beginCombo("Vertical Alignment", vAlign.toString())) {
            for (VerticalAlignment align : VerticalAlignment.values()) {
                if (ImGui.selectable(align.toString(), align == vAlign)) {
                    vAlign = align;
                    this.isDirty = true;
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
            this.isDirty = true;
            calculateTextDimensions();
        }
    }

    public TCBFont getFont() {
        if (font == null && !fontRequested) {
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
            this.isDirty = true;
        }
    }

    public boolean isProjectAsset() {
        return isProjectAsset;
    }

    public void setProjectAsset(boolean isProjectAsset) {
        this.isProjectAsset = isProjectAsset;
    }

    public String getFontPath() {
        return fontPath;
    }

    public void setFontPath(String fontPath) {
        this.fontPath = fontPath;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        this.isDirty = false;
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
            this.isDirty = true;
        }
    }

    public VerticalAlignment getVerticalAlignment() {
        return vAlign;
    }

    public void setVerticalAlignment(VerticalAlignment vAlign) {
        if (this.vAlign != vAlign) {
            this.vAlign = vAlign;
            this.isDirty = true;
        }
    }

    public GlyphRange getGlyphRange() {
        return GlyphRange.valueOf(this.glyphRangeName);
    }

    public void setGlyphRange(GlyphRange glyphRange) {
        if (!Objects.equals(this.glyphRangeName, glyphRange.name())) {
            this.glyphRangeName = glyphRange.name();
            fontRequested = false;
            requestLoadFont();
            this.isDirty = true;
        }
    }
}
