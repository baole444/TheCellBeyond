package render.text;

public class FontAtlasLayout {
    public static final int MSDFSize = 32;
    public static final int TextureSizeMultiplier = 2;
    public static final int BitmapSize = MSDFSize * TextureSizeMultiplier;
    public static final int GlyphPadding = 2;
    public static final int PaddedBitmapSize = BitmapSize + GlyphPadding * 2;
    public static final int Channels = 3;

    public static int atlasWidth(GlyphRange glyphRange) {
        int count = glyphRange.totalGlyphCount();
        int glyphPerRow = (int) Math.ceil(Math.sqrt(count));
        return glyphPerRow * PaddedBitmapSize;
    }

    public static int atlasHeight(GlyphRange glyphRange) {
        int count = glyphRange.totalGlyphCount();
        int glyphPerRow = (int) Math.ceil(Math.sqrt(count));
        return ((count + glyphPerRow - 1) / glyphPerRow) * PaddedBitmapSize;
    }
}
