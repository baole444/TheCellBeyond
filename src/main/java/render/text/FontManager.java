package render.text;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import org.lwjgl.BufferUtils;
import utility.AssetReference;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * FontManager handle the process of loading font files and build the font metadata,
 * along with generate the bitmap of the font atlas.
 * Font manager process run on a separated thread, allow loading fonts async.
 */
public final class FontManager {
    private static final EngineLog Logger = new EngineLog(FontManager.class);
    private record FontLoadingJob(TCBFont font, ResourceID atlasRID) {}

    /**
     * The reusable atlas source, produced from building an atlas.
     * <p>
     * Each entry are stored as data than the font that built it, so the entry is bound by the atlas lifetime than the point size that is happened to load first.
     * @param charUVs the glyph UV map
     * @param atlasRID the RID for indexing the atlas
     */
    record AtlasSource(Map<Character, CharUV> charUVs, ResourceID atlasRID) {}

    private static FontManager instance;
    private static Thread processor;
    private final ConcurrentHashMap<AssetReference, Map<GlyphRange, AtlasSource>> atlasSources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AssetReference, ByteBuffer> fontDataCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AssetReference, Map<GlyphRange, ByteBuffer>> atlases = new ConcurrentHashMap<>();
    private final BlockingQueue<FontLoadingJob> pendingRequests = new LinkedBlockingQueue<>();

    private FontManager() {
        startProcessingThread();
    }

    /**
     * Get the font manager instance in sync.
     * @return the current manager instance or a new one.
     */
    public static synchronized FontManager get() {
        if (instance == null) instance = new FontManager();
        return instance;
    }

    public void processFont(TCBFont font, ResourceID atlasRID) {
        if (pendingRequests.offer(new FontLoadingJob(font, atlasRID))) return;
        Logger.warning(String.format("Failed to queue font job for '%s'", font.canonicalPath()));
        ResourceStatusCallback.emit(font.RID, ResourceStatus.Failed);
    }

    public ByteBuffer getFontAtlas(AssetReference assetReference, GlyphRange glyphRange) {
        if (assetReference == null || glyphRange == null) return null;
        return atlases.getOrDefault(assetReference, Map.of()).get(glyphRange);
    }

    /**
     * Remove the cached {@link AtlasSource}.
     * <p>
     * This should be call when the atlas is unloaded, as they are generated in pair.
     * @param assetReference the reference to the font whose atlas was unloaded
     * @param glyphRange the glyph range of that atlas
     */
    public void releaseAtlas(AssetReference assetReference, GlyphRange glyphRange) {
        if (assetReference == null || glyphRange == null) return;
        Map<GlyphRange, AtlasSource> sources = atlasSources.get(assetReference);
        if (sources != null) sources.remove(glyphRange);
        Map<GlyphRange, ByteBuffer> bitmaps = atlases.get(assetReference);
        if (bitmaps != null) bitmaps.remove(glyphRange);
    }

    public synchronized void dispose() {
        if (processor != null) {
            processor.interrupt();
            try {
                processor.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            processor = null;
        }
        cleanup();
        Logger.info("Thread shutdown completed");
        instance = null;
    }

    public void cleanup() {
        fontDataCache.clear();
        atlasSources.clear();
        atlases.clear();
        pendingRequests.clear();
    }

    void cacheFontAtlas(AssetReference assetReference, GlyphRange glyphRange, ByteBuffer atlasData) {
        if (assetReference == null || glyphRange == null || atlasData == null) return;
        atlases.computeIfAbsent(assetReference, k -> new ConcurrentHashMap<>()).put(glyphRange, atlasData);
    }

    private void startProcessingThread() {
        processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    FontLoadingJob job = pendingRequests.take();
                    do {
                        processJob(job);
                    } while ((job= pendingRequests.poll()) != null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "FontProcessor");
        processor.setDaemon(true);
        processor.start();
    }

    private void processJob(FontLoadingJob job) {
        TCBFont font = job.font;
        ResourceID atlasRID = job.atlasRID;
        AssetReference assetReference = font.assetReference;
        GlyphRange glyphRange = font.glyphRange;
        int fontSizePixels = font.fontSizePixels();
        try {
            AtlasSource source = getAtlasSource(assetReference, glyphRange);
            if (source != null) {
                ByteBuffer fontData = fontDataCache.get(assetReference);
                if (fontData != null) {
                    Map<Character, CharMetric> charMetrics = TCBFontLoader.computeMetrics(fontData, source.charUVs, fontSizePixels, assetReference);
                    font.populateExisting(source, charMetrics);
                    ResourceStatusCallback.emit(font.RID, ResourceStatus.Ready);
                    return;
                }
            }
            ByteBuffer fontData = loadFontData(assetReference);
            if (fontData == null) {
                Logger.error(String.format("Failed to load font file '%s'", assetReference.canonicalPath()));
                ResourceStatusCallback.emit(font.RID, ResourceStatus.Failed);
                return;
            }
            TCBFontLoader.LoadResult result = TCBFontLoader.generate(fontData, glyphRange, fontSizePixels);
            cacheFontAtlas(assetReference, glyphRange, result.atlasData());
            font.populate(result.charUVs(), result.charMetrics(), atlasRID);
            atlasSources.computeIfAbsent(assetReference, _ -> new ConcurrentHashMap<>()).put(glyphRange, new AtlasSource(result.charUVs(), atlasRID));
            ResourceStatusCallback.emit(font.RID, ResourceStatus.Ready);
        } catch (Exception e) {
            Logger.error(String.format("Failed to process font '%s': %s", font.canonicalPath(), e.getMessage()));
            ResourceStatusCallback.emit(font.RID, ResourceStatus.Failed);
        }
    }

    private AtlasSource getAtlasSource(AssetReference assetReference, GlyphRange glyphRange) {
        Map<GlyphRange, AtlasSource> ranges = atlasSources.get(assetReference);
        return ranges != null ? ranges.get(glyphRange) : null;
    }

    private ByteBuffer loadFontData(AssetReference assetReference) {
        ByteBuffer cached = fontDataCache.get(assetReference);
        if (cached != null) return cached;
        try (InputStream stream = UnifiedPaths.getAssetStream(assetReference.resolvedPath())) {
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            fontDataCache.put(assetReference, buffer);
            return buffer;
        } catch (IOException e) {
            Logger.error(String.format("Cannot read font file '%s': %s", assetReference.canonicalPath(), e.getMessage()));
            return null;
        }
    }
}
