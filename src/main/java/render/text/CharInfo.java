package render.text;

/**
 * Store information for a character loaded for a specific font by TCBFont.
 * @param x0 bottom left x coordinate
 * @param y0 bottom left y coordinate
 * @param x1 top right x coordinate
 * @param y1 top right y coordinate
 * @param xOffset how far in pixel the glyph was translated from the left, subtract to compensate x position
 * @param yOffset how far in pixel the glyph was translated from the baseline, subtract to compensate y position
 * @param advance character's xAdvance value
 * @param fontSize the size of the font in pixel.
 */
public record CharInfo(float x0, float y0, float x1, float y1,
                       double xOffset, double yOffset,
                       float advance, float fontSize
) {}
