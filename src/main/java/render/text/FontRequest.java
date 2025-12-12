package render.text;

import utility.AssetReference;
import utility.FontPT;

import java.util.Objects;

public record FontRequest(AssetReference fontAsset, float point, GlyphRange glyphRange) {
    public int getPixelSize() {
        return FontPT.pointToPixel(point);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FontRequest that = (FontRequest) o;
        return point == that.point()
                && Objects.equals(fontAsset, that.fontAsset)
                && glyphRange == that.glyphRange;
    }

    @Override
    public String toString() {
        return "FontRequest{" +
                "Path='" + (fontAsset != null ? fontAsset.canonicalPath() : "null") + "'" +
                ", Size=" + point +
                ", glyph=" + glyphRange.getDescription() +
                "}";
    }
}
