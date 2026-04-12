package render.text;

/**
 * FontAtlasLayout contain static attributes for the layout of the font atlas texture,
 * and method to calculate the atlas width and height.
 */
public final class FontAtlasLayout {
    /**
     * The size of the glyph for msdfgen, in pixels.
     * This is the size of the glyph's bitmap, or tight bound bitmap.
     */
    public static final int MSDFSize = 32;
    /**
     * The multiplier for the per glyph texture frame.
     */
    public static final int TextureSizeMultiplier = 2;
    /**
     * The scaled atlas's bitmap size to place the glyph into.
     * This is larger that the glyph's tight bound bitmap to create space for msdf to work properly.
     * This is size allocated per glyph.
     */
    public static final int BitmapSize = MSDFSize * TextureSizeMultiplier;
    /**
     * Extra padding for the atlas bitmap, in pixels.
     */
    public static final int GlyphPadding = 2;
    /**
     * The scaled atlas's bitmap size with padding applied to avoid texture filter clipping.
     */
    public static final int PaddedBitmapSize = BitmapSize + GlyphPadding * 2;
    /**
     * The number of colour channels, always 33 for msdf.
     */
    public static final int Channels = 3;

    private FontAtlasLayout() {}

    /**
     * Calculate the width of the font atlas texture using the given glyph range.
     * @param glyphRange the glyph range to calculate for
     * @return the width of the font atlas texture, in pixels
     */
    public static int atlasWidth(GlyphRange glyphRange) {
        int count = glyphRange.totalGlyphCount();
        int glyphPerRow = (int) Math.ceil(Math.sqrt(count));
        return glyphPerRow * PaddedBitmapSize;
    }

    /**
     * Calculate the height of the font atlas texture using the given glyph range.
     * @param glyphRange the glyph range to calculate for
     * @return the height of the font atlas texture, in pixels
     */
    public static int atlasHeight(GlyphRange glyphRange) {
        int count = glyphRange.totalGlyphCount();
        int glyphPerRow = (int) Math.ceil(Math.sqrt(count));
        return ((count + glyphPerRow - 1) / glyphPerRow) * PaddedBitmapSize;
    }
}
