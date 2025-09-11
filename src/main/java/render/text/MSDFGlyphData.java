package render.text;

import org.joml.Vector2f;

import java.nio.ByteBuffer;

/**
 * Store glyph data generate by msddfgen from TCBFont.
 * This serves as a temporary data record.
 */
record MSDFGlyphData(ByteBuffer pixelData, float advance, Vector2f bearing, Vector2f size) {}
