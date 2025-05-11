package threading.states;

import components.TextComponent;
import org.joml.Vector4f;

public class TextComponentState {
    private String text;
    private String fontPath;
    private int fontSize;
    private Vector4f color;
    private String glyphRangeName;
    private TextComponent.HorizontalAlignment hAlign;
    private TextComponent.VerticalAlignment vAlign;

    public TextComponentState(TextComponent textComponent) {
        this.text = textComponent.getText();
        this.fontPath = textComponent.getFontPath();
        this.fontSize = textComponent.getFontSize();
        this.color = new Vector4f(textComponent.getColor());
        this.glyphRangeName = textComponent.getGlyphRangeName();
        this.hAlign = textComponent.getHorizontalAlignment();
        this.vAlign = textComponent.getVerticalAlignment();
    }

    public String getText() {
        return text;
    }

    public String getFontPath() {
        return fontPath;
    }

    public int getFontSize() {
        return fontSize;
    }

    public Vector4f getColor() {
        return color;
    }

    public String getGlyphRangeName() {
        return glyphRangeName;
    }

    public TextComponent.HorizontalAlignment gethAlign() {
        return hAlign;
    }

    public TextComponent.VerticalAlignment getvAlign() {
        return vAlign;
    }
}
