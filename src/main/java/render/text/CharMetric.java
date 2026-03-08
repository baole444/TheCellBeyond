package render.text;

/**
 * FreeType's metric for a glyph, varies per point size for a font.
 * @param advance horizontal advance in pixels
 * @param fontSize the size of the glyph in pixel
 */
public record CharMetric(float advance, float fontSize) {}
