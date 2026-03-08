package render.text;

/**
 * CharUV hold the position and the MSDF offset in the atlas.
 * This is shared across all instance of {@link TCBFont} of the same font and glyph range.
 * <p>
 * Subtract {@link #xOffset} to compensate for the {@code x} position.
 * Subtract {@link #yOffset} to compensate for {@code y} position.
 * @param x0 left texture coordinate on the atlas
 * @param y0 bottom texture coordinate on the atlas
 * @param x1 right texture coordinate on the atlas
 * @param y1 top texture coordinate on the atlas
 * @param xOffset distance that the glyph was translated from the left, in pixels
 * @param yOffset distance that the glyph was translated from the baseline.
 */
public record CharUV(float x0, float y0, float x1, float y1, double xOffset, double yOffset) {}
