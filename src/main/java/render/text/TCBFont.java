package render.text;

import TheCellBeyond.internal.ResourceID;
import render.RenderResourceType;
import utility.AssetReference;
import utility.FontPT;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Font meta data.
 */
public class TCBFont {
    public final ResourceID RID = new ResourceID(RenderResourceType.Font);
    final AssetReference assetReference;
    final GlyphRange glyphRange;
    final float points;
    public final int atlasWidth;
    public final int atlasHeight;
    private ResourceID atlasRID;
    Map<Character, CharUV> charUVs;
    private Map<Character, CharMetric> charMetrics;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public TCBFont(AssetReference assetReference, GlyphRange glyphRange, float points) {
        if (points <= 0.0f) points = 0.1f;
        this.assetReference = assetReference;
        this.glyphRange = glyphRange;
        this.points = points;
        this.atlasWidth = FontAtlasLayout.atlasWidth(glyphRange);
        this.atlasHeight = FontAtlasLayout.atlasHeight(glyphRange);
    }

    /**
     * Populate this {@link TCBFont} instance from a load result
     * @param charUVs the texture uv map
     * @param charMetrics the size metric
     * @param atlasRID the RID of the underlying atlas
     */
    void populate(Map<Character, CharUV> charUVs, Map<Character, CharMetric> charMetrics, ResourceID atlasRID) {
        this.charUVs = charUVs;
        this.charMetrics = charMetrics;
        this.atlasRID = atlasRID;
        loaded.set(true);
    }

    /**
     * Populate this {@link TCBFont} instance from an existing source. This allows reusing the UV map and the atlas RID.
     * Only new size metrics is required.
     * @param source the existing font meta
     * @param charMetrics the new size metric
     */
    void populateExisting(TCBFont source, Map<Character, CharMetric> charMetrics) {
        this.charUVs = source.charUVs;
        this.atlasRID = source.atlasRID;
        this.charMetrics = charMetrics;
        loaded.set(true);
    }

    /**
     * Get the texture coordinate and offset of the given character. If the character does not exist, fallback to space.
     * <p>
     * This always return {@code null} if the font is not fully loaded yet.
     * @param c the character to get the uv for
     * @return the {@link CharUV} of the requested char if exist or blank space, or null if font not loaded
     */
    public CharUV charUV(char c) {
        if (charUVs == null) return null;
        CharUV uv = charUVs.get(c);
        return uv != null ? uv : charUVs.get(' ');
    }

    /**
     * Get the metric of the given character. If the character does not exist, fallback to space.
     * <p>
     * This always return {@code null} if the font is not fully loaded yet.
     * @param c the character to get the metric for
     * @return the {@link CharMetric} of the requested char if exist or blank space, or null if font not loaded
     */
    public CharMetric charMetric(char c) {
        if (charMetrics == null) return null;
        CharMetric charMetric = charMetrics.get(c);
        return charMetric != null ? charMetric : charMetrics.get(' ');
    }

    public ResourceID atlasRID() {
        return atlasRID;
    }

    public float points() {
        return points;
    }

    public int fontSizePixels() {
        return FontPT.pointToPixel(points);
    }

    public String canonicalPath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public GlyphRange glyphRange() {
        return glyphRange;
    }

    public boolean loaded() {
        return loaded.get();
    }
}
