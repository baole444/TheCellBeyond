package render.text;

import java.util.Objects;

public record FontRequest(String fontPath, int fontSize, GlyphRange glyphRange, boolean isProjectAsset) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FontRequest that = (FontRequest) o;
        return fontSize == that.fontSize
                && isProjectAsset == that.isProjectAsset
                && Objects.equals(fontPath, that.fontPath)
                && glyphRange == that.glyphRange;
    }

    @Override
    public String toString() {
        return "FontRequest{" +
                "Path='" + fontPath + "'" +
                ", Size=" + fontSize +
                ", glyph=" + glyphRange.getDescription() +
                ", is project asset=" + isProjectAsset +
                "}";
    }
}
