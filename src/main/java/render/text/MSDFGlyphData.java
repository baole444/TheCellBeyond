package render.text;

import org.joml.Vector2f;

import java.nio.ByteBuffer;

/**
 * Store glyph data generate by msddfgen from TCBFont.
 * This serves as a temporary data record before information is sent to CharInfo.
 * @param pixelData byte buffer of the msdf texture for a given glyph
 * @param advance xAdvance calculated by FreeType at this glyph's designated font size.
 * @param leftOffset this is the glyph's bearingX value as glyph is translated to xMin (left bound) when generated
 * @param bottomOffset this is the glyph's height - bearing y as glyph is translated to yMin (bottom bound) when generated
 */
record MSDFGlyphData(ByteBuffer pixelData, float advance, double leftOffset, double bottomOffset) {}
