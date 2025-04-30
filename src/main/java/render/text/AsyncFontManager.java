package render.text;

import org.lwjgl.BufferUtils;
import utility.PathResolver;
import utility.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;

public class AsyncFontManager {
    public static final Logger LOGGER = Logger.getLogger(AsyncFontManager.class.getName());
    private static AsyncFontManager instance;

    // Thread pool for font loading tasks
    private final ExecutorService executorService;

    // Cache loaded fonts
    private final Map<String, TCBFont> loadedFonts = new ConcurrentHashMap<>();

    // Tracking pending tasks
    private final Map<String, CompletableFuture<TCBFont>> pendingLoads = new ConcurrentHashMap<>();

    // List of fonts waiting for their texture to be generated.
    private final List<TCBFont> waitingForTexture = new ArrayList<>();

    // fallback font
    private TCBFont fallback;

    private AsyncFontManager() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        initFallback();
    }

    private void initFallback() {
        try {
            fallback = new TCBFont(Settings.PATH.CONSOLA, 12, false);
        } catch (IOException e) {
            LOGGER.warning("Fall back font file is missing, skipping...");
        }
    }

    public static AsyncFontManager get() {
        if (instance == null) {
            instance = new AsyncFontManager();
        }

        return instance;
    }

    public void updateFontTextures () {
        List<TCBFont> toProcess;
        synchronized (waitingForTexture) {
            toProcess = new ArrayList<>(waitingForTexture);
            waitingForTexture.clear();
        }

        for (TCBFont font : toProcess) {
            if (font.waitingTexture()) {
                try {
                    font.createTexture();
                } catch (Exception e) {
                    LOGGER.severe("Error creating font texture: " + e.getMessage());
                    synchronized (waitingForTexture) {
                        waitingForTexture.add(font);
                    }
                }
            }
        }
    }

    private void addWaitingFontTexture(TCBFont font) {
        synchronized (waitingForTexture) {
            waitingForTexture.add(font);
        }
    }

    public CompletableFuture<TCBFont> loadFontAsync(String filepath, int fontSize, boolean isProjectAsset, GlyphRange glyphRange, Consumer<TCBFont> onComplete) {
        String key = keyGen(filepath, fontSize, glyphRange);

        // Is the font loaded?
        TCBFont cache = loadedFonts.get(key);
        if (cache != null) {
            CompletableFuture<TCBFont> result = CompletableFuture.completedFuture(cache);
            if (onComplete != null) {
                onComplete.accept(cache);
            }

            return result;
        }

        // Is the font scheduled to load?
        CompletableFuture<TCBFont> pendingLoad = pendingLoads.get(key);
        if (pendingLoad != null) {
            if (onComplete != null) {
                pendingLoad.thenAccept(onComplete);
            }

            return pendingLoad;
        }

        // Start a new font load.
        CompletableFuture<TCBFont> future = new CompletableFuture<>();
        pendingLoads.put(key, future);

        String resolvedPath;
        if (isProjectAsset && CurrentProject != null && ProjectRoot != null) {
            resolvedPath = PathResolver.resolveToAbsolute(ProjectRoot, filepath);
        } else {
            resolvedPath = new File(filepath).getAbsolutePath();
        }

        final String finalPath = resolvedPath;

        executorService.submit(() -> {
            try {
                TCBFont font = loadFontBitmap(finalPath, fontSize, glyphRange);

                // Cache the font
                loadedFonts.put(key, font);
                pendingLoads.remove(key);

                // Schedule for texture gen
                addWaitingFontTexture(font);

                if (onComplete != null) {
                    onComplete.accept(font);
                }

                future.complete(font);
            } catch (Exception e) {
                LOGGER.severe("failed to load font: " + finalPath);
                pendingLoads.remove(key);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private TCBFont loadFontBitmap(String filepath, int fontSize, GlyphRange glyphRange) throws IOException {
        File toVerify = new File(filepath);
        if (!toVerify.exists()) {
            throw new IOException("Font file not found at: " + filepath);
        }

        byte[] fontData = Files.readAllBytes(Paths.get(filepath));
        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
        fontBuffer.put(fontData);
        fontBuffer.flip();

        return new TCBFont(fontBuffer, filepath, fontSize, glyphRange);
    }

    public TCBFont getFont(String filepath, int fontSize, GlyphRange glyphRange) {
        String key = keyGen(filepath, fontSize, glyphRange);
        TCBFont font = loadedFonts.get(key);

        if (font != null) {
            if (font.waitingTexture()) {
                addWaitingFontTexture(font);
            }

            return font;
        }

        CompletableFuture<TCBFont> pendingLoad = pendingLoads.get(key);
        if (pendingLoad != null && !pendingLoad.isDone()) {
            return fallback;
        }

        loadFontAsync(filepath, fontSize, false, glyphRange, null);
        return  fallback;
    }

    private String keyGen(String filepath, int fontSize, GlyphRange glyphRange) {
        return filepath + "_" + fontSize + "_" + glyphRange.name();
    }

    public boolean isFontLoaded(String filepath, int fontSize, GlyphRange glyphRange) {
        String key = keyGen(filepath, fontSize, glyphRange);
        TCBFont font = loadedFonts.get(key);
        return font != null && !font.waitingTexture();
    }

    public boolean isFontLoading(String filepath, int fontSize, GlyphRange glyphRange) {
        String key = keyGen(filepath, fontSize, glyphRange);
        return pendingLoads.containsKey(key) && !pendingLoads.get(key).isDone();
    }

    public void cleanup() {
        executorService.shutdown();

        for (TCBFont font : loadedFonts.values()) {
            font.cleanup();
        }

        loadedFonts.clear();
        pendingLoads.clear();
        waitingForTexture.clear();

        if (fallback != null) {
            fallback.cleanup();
        }
    }
}
