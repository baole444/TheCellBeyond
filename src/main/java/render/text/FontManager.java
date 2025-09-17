package render.text;

import org.lwjgl.BufferUtils;
import utility.AssetReference;
import utility.PathResolver;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FontManager {
    private static final Logger LOGGER = Logger.getLogger(FontManager.class.getName());

    private static FontManager instance;
    private static Thread processor;
    private final ExecutorService executorService;

    private final Map<FontRequest, TCBFont> fontCache = new ConcurrentHashMap<>();
    private final BlockingQueue<FontRequestEntry> pendingRequests = new LinkedBlockingQueue<>();
    private final Map<FontRequest, FontRequestEntry> requests = new ConcurrentHashMap<>();
    private final Map<AssetReference, Map<GlyphRange, ByteBuffer>> atlases = new ConcurrentHashMap<>();

    private FontManager() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        startProcessingThread();
    }

    public static synchronized FontManager get() {
        if (instance == null) {
            instance = new FontManager();
        }

        return instance;
    }

    private void startProcessingThread() {
        processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    FontRequestEntry entry = pendingRequests.take();

                    do {
                        processFontRequest(entry);
                        requests.remove(entry.request);
                    } while ((entry = pendingRequests.poll()) != null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "FontProcessor");

        processor.setDaemon(true);
        processor.start();
    }

    private void processFontRequest(FontRequestEntry entry) {
        if (entry == null) return;

        FontRequest request = entry.request;
        TCBFont font = fontCache.get(request);
        if (font != null) {
            notifyCallbacks(font, request, entry.callbacks);
            return;
        }

        try {
            AssetReference assetRef = request.fontAsset();
            PathResolver resolver = PathResolver.get();

            try (InputStream stream = resolver.getAssetStream(assetRef.getResolvedPath())) {
                byte[] fontData = stream.readAllBytes();
                ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
                fontBuffer.put(fontData);
                fontBuffer.flip();

                font = new TCBFont(fontBuffer, assetRef, request.getPixelSize(), request.glyphRange());

                fontCache.put(request, font);

                notifyCallbacks(font, request, entry.callbacks);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load font: " + request, e);
        }
    }

    private void notifyCallbacks(TCBFont font, FontRequest request, List<WeakReference<FontStatusCallback>> callbacks) {
        List<WeakReference<FontStatusCallback>> expiredCallbacks = new ArrayList<>();

        for (WeakReference<FontStatusCallback> ref : callbacks) {
            FontStatusCallback callback = ref.get();

            if (callback != null) {
                try {
                    callback.onFontReady(font, request);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception in font load callback", e);
                }
            } else {
                expiredCallbacks.add(ref);
            }
        }

        callbacks.removeAll(expiredCallbacks);
    }

    void cacheFontAtlas(AssetReference assetReference, GlyphRange glyphRange, ByteBuffer atlasData) {
        if (assetReference == null || glyphRange == null || atlasData == null) return;

        atlases.computeIfAbsent(assetReference, k -> new ConcurrentHashMap<>()).put(glyphRange, atlasData);
    }

    public ByteBuffer getFontAtlas(AssetReference assetReference, GlyphRange glyphRange) {
        if (assetReference == null || glyphRange == null) return null;

        return atlases.getOrDefault(assetReference, Map.of()).get(glyphRange);
    }

    public void requestFont(FontRequest request, FontStatusCallback callback) {
        TCBFont existingFont = fontCache.get(request);

        if (existingFont != null) {
            if (callback != null) callback.onFontReady(existingFont, request);
            return;
        }

        FontRequestEntry existingEntry = requests.get(request);
        if (existingEntry != null) {
            existingEntry.addCallback(callback);
            return;
        }

        FontRequestEntry newEntry = new FontRequestEntry(request, callback);
        FontRequestEntry current = requests.putIfAbsent(request, newEntry);

        if (current != null) {
            current.addCallback(callback);
            return;
        }

        if (!pendingRequests.offer(newEntry)) {
            LOGGER.log(Level.WARNING, "Failed to queue font request: " + request);
            requests.remove(request);
        }
    }

    public TCBFont getFont(FontRequest request) {
        TCBFont font = fontCache.get(request);

        if (font != null) return font;

        requestFont(request, null);

        return null;
    }

    public boolean isFontLoaded(FontRequest request) {
        TCBFont font = fontCache.get(request);
        return font != null && font.isLoaded();
    }

    public void cleanup() {
        executorService.shutdown();
        fontCache.clear();
        pendingRequests.clear();
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
        LOGGER.log(Level.INFO, "Thread shutdown completed");
        instance = null;
    }
}
