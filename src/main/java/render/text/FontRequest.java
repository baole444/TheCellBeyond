package render.text;

import utility.FontPT;

import java.util.Objects;

public record FontRequest(String fontPath, float point, GlyphRange glyphRange) {
    public int getPixelSize() {
        return FontPT.pointToPixel(point);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FontRequest that = (FontRequest) o;
        return point == that.point()
                && Objects.equals(fontPath, that.fontPath)
                && glyphRange == that.glyphRange;
    }

    @Override
    public String toString() {
        return "FontRequest{" +
                "Path='" + fontPath + "'" +
                ", Size=" + point +
                ", glyph=" + glyphRange.getDescription() +
                "}";
    }
}
