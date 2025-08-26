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

    private final ExecutorService executorService;

    private final Map<FontRequest, TCBFont> fontCache = new ConcurrentHashMap<>();

    private final Queue<FontRequestEntry> pendingRequests = new ConcurrentLinkedDeque<>();

    private final Queue<TCBFont> fontsWaitingTexture = new ConcurrentLinkedDeque<>();

    private FontManager() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        startProcessingThread();
    }

    public static FontManager get() {
        if (instance == null) {
            instance = new FontManager();
        }

        return instance;
    }

    private void startProcessingThread() {
        Thread processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                processFontRequest();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "FontProcessor");

        processor.setDaemon(true);
        processor.start();
    }

    private void processFontRequest() {
        FontRequestEntry entry;
        while ((entry = pendingRequests.poll()) != null) {
            FontRequest request = entry.request;

            TCBFont font = fontCache.get(request);
            if (font != null) {
                notifyCallbacks(font, request, entry.callbacks);
                continue;
            }

            try {
                AssetReference assetRef = request.fontAsset();

                // TODO: if race condition happened, need to sync PathResolver init sequence.
                PathResolver resolver = PathResolver.get();

                try (InputStream stream = resolver.getAssetStream(assetRef.getResolvedPath())) {
                    byte[] fontData = stream.readAllBytes();
                    ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
                    fontBuffer.put(fontData);
                    fontBuffer.flip();

                    font = new TCBFont(fontBuffer, assetRef, request.getPixelSize(), request.glyphRange());

                    fontCache.put(request, font);
                    fontsWaitingTexture.add(font);

                    notifyCallbacks(font, request, entry.callbacks);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load font: " + request, e);
            }
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

    public void requestFont(FontRequest request, FontStatusCallback callback) {
        TCBFont existingFont = fontCache.get(request);

        if (existingFont != null) {
            if (callback != null) {
                callback.onFontReady(existingFont, request);
            }
            return;
        }

        boolean alreadyExisting = false;
        for (FontRequestEntry entry : pendingRequests) {
            if (entry.request.equals(request)) {
                entry.addCallback(callback);
                alreadyExisting = true;
                break;
            }
        }

        if (!alreadyExisting) {
            pendingRequests.add(new FontRequestEntry(request, callback));
        }
    }

    public void updateFontTextures() {
        TCBFont font;
        while ((font = fontsWaitingTexture.poll()) != null) {
            if (font.waitingTexture()) {
                try {
                    font.createTexture();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to create texture for font: " + font.getFilepath(), e);

                    fontsWaitingTexture.add(font);
                }
            }
        }
    }

    public TCBFont getFont(FontRequest request) {
        TCBFont font = fontCache.get(request);

        if (font != null) {
            if (font.waitingTexture()) {
                fontsWaitingTexture.add(font);
            }

            return font;
        }

        requestFont(request, null);

        return null;
    }

    public boolean isFontLoaded(FontRequest request) {
        TCBFont font = fontCache.get(request);
        return font != null && font.isLoaded() && !font.waitingTexture();
    }

    public void cleanup() {
        executorService.shutdown();

        for (TCBFont font : fontCache.values()) {
            font.cleanup();
        }

        fontCache.clear();

        pendingRequests.clear();

        fontsWaitingTexture.clear();
    }
}
