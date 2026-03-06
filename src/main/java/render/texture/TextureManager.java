package render.texture;

import render.text.GlyphRange;
import utility.AssetReference;
import utility.log.EngineLog;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class TextureManager {
    static final EngineLog Logger = new EngineLog(TextureManager.class);
    private static TextureManager instance;

    private record AtlasKey(String canonicalPath, GlyphRange glyphRange) {}

    private final ConcurrentLinkedQueue<TextureCommand> commandQueue;
    private final ConcurrentHashMap<Integer, TextureHandle> activeHandles;
    private final ConcurrentHashMap<String, TextureHandle> textureHandles;
    private final ConcurrentHashMap<AtlasKey, TextureHandle> fontAtlasHandles;

    // Limit per cycle
    private int createdTextures = 0;
    private int disposedTextures = 0;
    private static final int TEXTURE_OPERATION_LIMIT = 8;

    private TextureManager() {
        commandQueue = new ConcurrentLinkedQueue<>();
        activeHandles = new ConcurrentHashMap<>();
        textureHandles = new ConcurrentHashMap<>();
        fontAtlasHandles = new ConcurrentHashMap<>();
    }

    public static TextureManager get() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }

    void stepCreatedTexture() {
        this.createdTextures++;
    }

    void stepDisposedTexture() {
        this.disposedTextures++;
    }

    void removeTextureHandle(String canonicalPath) {
        textureHandles.remove(canonicalPath);
    }

    void removeActiveHandle(int handleId) {
        activeHandles.remove(handleId);
    }

    void removeFontAtlasHandle(String canonicalPath, GlyphRange glyphRange) {
        fontAtlasHandles.remove(new AtlasKey(canonicalPath, glyphRange));
    }

    /**
     * Get or create a new texture handle for the asset.
     * This prevents creating multiple OpenGL textures on the same asset.
     */
    public TextureHandle getTextureHandle(ByteBuffer imageData, AssetReference assetReference) {
        String canonicalPath = assetReference.canonicalPath();
        TextureHandle currentHandle = textureHandles.get(canonicalPath);
        if (currentHandle != null) return currentHandle;
        TextureHandle handle = new TextureHandle();
        textureHandles.put(canonicalPath, handle);
        CreateTextureCommand command = new CreateTextureCommand(handle, imageData, assetReference);
        commandQueue.offer(command);
        activeHandles.put(handle.getHandleId(), handle);
        Logger.debug("Created new handle for " + canonicalPath);
        return handle;
    }

    public TextureHandle getFontAtlasHandle(ByteBuffer atlasData, AssetReference assetReference, GlyphRange glyphRange, int width, int height, int channels) {
        AtlasKey key = new AtlasKey(assetReference.canonicalPath(), glyphRange);

        TextureHandle currentHandle = fontAtlasHandles.get(key);
        if (currentHandle != null) return currentHandle;

        TextureHandle handle = new TextureHandle();
        fontAtlasHandles.put(key, handle);

        CreateFontAtlasTextureCommand command = new CreateFontAtlasTextureCommand(handle, atlasData, assetReference, width, height, channels);

        commandQueue.offer(command);
        activeHandles.put(handle.getHandleId(), handle);

        Logger.debug("Created new atlas handle for " + assetReference.canonicalPath() + " with glyph " + glyphRange.toString());
        return handle;
    }

    public void disposeTexture(TextureHandle handle, String canonicalPath) {
        if (handle == null || handle.isDisposed()) return;

        TextureHandle currentHandle = textureHandles.get(canonicalPath);
        if (currentHandle == handle) {
            textureHandles.remove(canonicalPath);
            Logger.debug("Removed texture handle cache for " + canonicalPath);
        }

        DisposeTextureCommand command = new DisposeTextureCommand(handle);
        commandQueue.offer(command);
    }

    public void disposeFontAtlasTexture(TextureHandle handle, String canonicalPath, GlyphRange glyphRange) {
        if (handle == null || handle.isDisposed()) return;

        AtlasKey key = new AtlasKey(canonicalPath, glyphRange);
        TextureHandle currentHandle = fontAtlasHandles.get(key);
        if (currentHandle == handle) {
            fontAtlasHandles.remove(key);
            Logger.debug("Removed atlas handle cache for " + canonicalPath + " with glyph" + glyphRange.toString());
        }

        DisposeTextureCommand command = new DisposeTextureCommand(handle);
        commandQueue.offer(command);
    }

    public void forceDisposeTexture(TextureHandle handle) {
        if (handle == null || handle.isDisposed()) return;

        DisposeTextureCommand command = new DisposeTextureCommand(handle);
        commandQueue.offer(command);
    }

    public void processCommands() {
        createdTextures = 0;
        disposedTextures = 0;

        while (!commandQueue.isEmpty() && (createdTextures + disposedTextures) < TEXTURE_OPERATION_LIMIT) {
            TextureCommand command = commandQueue.poll();
            if (command == null) continue;
            try {
                command.execute();
            } catch (Exception e) {
                Logger.error("Failed to execute texture command: " + command + e);
                if (command instanceof CreateTextureCommand textureCmd) {
                    textureCmd.handle.setError("Command execution failed: " + e.getMessage());
                    continue;
                }
                if (command instanceof CreateFontAtlasTextureCommand fontCmd) {
                    fontCmd.handle.setError("Command execution failed: " + e.getMessage());
                }
            }
        }
    }

    public void clearPathCache() {
        textureHandles.clear();
        fontAtlasHandles.clear();
        Logger.info("Cleared texture path cache");
    }

    void cleanup() {
        for (TextureHandle handle : activeHandles.values()) {
            if (handle.isReady()) {
                glDeleteTextures(handle.getTextureId());
            }

            handle.setStatus(TextureHandle.Status.DISPOSED);
        }

        activeHandles.clear();
        textureHandles.clear();
        fontAtlasHandles.clear();
        commandQueue.clear();

        Logger.info("TextureManager cleanup completed");
    }

    public static void dispose() {
        get().cleanup();
        TextureStatusCallback.clear();
        instance = null;
    }
}
