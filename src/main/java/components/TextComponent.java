package components;

import editor.ImEditorGui;
import imgui.ImGui;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.text.*;
import utility.PathResolver;
import utility.Settings;

import java.io.File;
import java.util.Objects;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;

public class TextComponent extends SpatialComponent implements FontStatusCallback {
    private String text;
    private String fontPath;
    private int fontSize;
    private final Vector4f color;
    private boolean isDirty = true;
    private String glyphRangeName = "ASCII";
    private boolean isProjectAsset = false;

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

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
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

    public TextComponent(String text, String fontPath, int fontSize, Vector4f color, Vector2f position, GlyphRange glyphRange) {
        this.text = text;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.color = color;
        this.glyphRangeName = glyphRange.name();
        setWorldPosition(position);
    }

    private void requestLoadFont() {
        if (pendingRequest) {
            return;
        }

        GlyphRange range = GlyphRange.valueOf(glyphRangeName);

        String resolvedFontPath = fontPath;

        if (isProjectAsset) {
            resolvedFontPath = "project://" + fontPath;
        } else if (!fontPath.startsWith("engine://")) {
            resolvedFontPath = "engine://" + fontPath;
        }

        FontRequest request = new FontRequest(resolvedFontPath, fontSize, range, isProjectAsset);

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
            this.isDirty = true;
        }
    }

    private void calculateTextDimensions() {
        if (font == null || text.isEmpty()) {
            textDimensions.set(0, 0);
            return;
        }

        float scaledFontSie = font.getFontSize() * Settings.WORLD_SCALE_FACTOR;

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

            float advance = font.getCharInfo(c).advance() * Settings.WORLD_SCALE_FACTOR;
            lineWidth += advance;
            width += advance;
        }

        maxLineWidth = Math.max(maxLineWidth, lineWidth);
        height = scaledFontSie * lineCount;

        if (lineCount > 1) {
            width = maxLineWidth;
        }

        textDimensions.set(width, height);
    }

    @Override
    public void start() {
        super.start();
        requestLoadFont();
    }

    @Override
    public void update(float dt) {
        if (font == null
                || !fontPath.equals(currentRequest.fontPath())
                || fontSize != currentRequest.fontSize()
                || !glyphRangeName.equals(currentRequest.glyphRange().name())
        ) {
            requestLoadFont();
        }
    }

    @Override
    public void editorUpdate(float dt) {
        if (font == null
                || !fontPath.equals(currentRequest.fontPath())
                || fontSize != currentRequest.fontSize()
                || !glyphRangeName.equals(currentRequest.glyphRange().name())
        ) {
            requestLoadFont();
        }
    }

    @Override
    public void imgui() {
        String textInput = ImEditorGui.inputTextWithIME("Text", text, 1024);
        setText(textInput);

        String fontPathInput = ImEditorGui.inputText("Font Path", fontPath);
        if (!fontPathInput.equals(fontPath)) {

            String resolvedPath;
            if (isProjectAsset && CurrentProject != null && ProjectRoot != null) {
                resolvedPath = PathResolver.resolveToAbsolute(ProjectRoot, fontPathInput);
            } else {
                resolvedPath = new File(fontPathInput).getAbsolutePath();
            }

            if (new File(resolvedPath).exists()) {
                this.fontPath = fontPathInput;
                this.pendingRequest = false;
                requestLoadFont();
            }
        }

        int fontSizeInput = ImEditorGui.dragIntCtrl("Font Size", fontSize);
        if (fontSizeInput != fontSize) {
            this.fontSize = Math.abs(fontSizeInput);
            this.pendingRequest = false;
            requestLoadFont();
        }

        if (ImGui.beginCombo("Glyph Range", glyphRangeName)) {
            for (GlyphRange range : GlyphRange.values()) {
                if (ImGui.selectable(range.getDescription(), range.name().equals(glyphRangeName))) {
                    setGlyphRange(range);
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

        super.imgui();
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

    public int getFontSize() {
        return fontSize;
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

    public void setFontPath(String fontPathInput) {
        if (!fontPathInput.equals(fontPath)) {

            String resolvedPath;
            if (isProjectAsset && CurrentProject != null && ProjectRoot != null) {
                resolvedPath = PathResolver.resolveToAbsolute(ProjectRoot, fontPathInput);
            } else {
                resolvedPath = new File(fontPathInput).getAbsolutePath();
            }

            if (new File(resolvedPath).exists()) {
                this.fontPath = fontPathInput;
                this.pendingRequest = false;
                requestLoadFont();
            }
        }
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

    public String getGlyphRangeName() {
        return glyphRangeName;
    }

    public void setGlyphRange(GlyphRange glyphRange) {
        if (!Objects.equals(this.glyphRangeName, glyphRange.name())) {
            this.glyphRangeName = glyphRange.name();
            this.pendingRequest = false;
            requestLoadFont();
            this.isDirty = true;
        }
    }
}
