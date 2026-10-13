package render.texture;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceRegistry;
import render.Texture;
import render.text.GlyphRange;
import utility.AssetReference;
import utility.log.EngineLog;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public final class TextureManager {
    private record AtlasKey(String canonicalPath, GlyphRange glyphRange) {}
    static final EngineLog Logger = new EngineLog(TextureManager.class);
    private static TextureManager instance;
    private final ConcurrentLinkedQueue<TextureCommand> commandQueue;
    private final ConcurrentHashMap<Integer, TextureHandle> activeHandles;
    private final ConcurrentHashMap<String, TextureHandle> textureHandles;
    private final ConcurrentHashMap<AtlasKey, TextureHandle> fontAtlasHandles;
    private final ConcurrentHashMap<String, ResourceID> textureIDs = new ConcurrentHashMap<>();
    private final ResourceRegistry<Texture> textureRegistry = new ResourceRegistry<>();

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
        if (instance == null) instance = new TextureManager();
        return instance;
    }

    /**
     * Get the texture native resource bound to the given RID.
     * @param RID the RID of the texture
     * @return the texture resource, or null if the RID point to no texture
     */
    public Texture texture(ResourceID RID) {
        return RID != null ? textureRegistry.get(RID) : null;
    }

    /**
     * Get the RID of the texture on the given path. The texture will be loaded and cached if it hasn't.
     * @param canonicalPath the canonical path to the image, with no metadata tag
     * @return the RID of the texture
     */
    public ResourceID loadTexture(String canonicalPath) {
        ResourceID RID = textureIDs.get(canonicalPath);
        if (RID != null) return RID;
        Texture texture = new Texture();
        texture.init(canonicalPath);
        RID = texture.RID;
        textureIDs.put(canonicalPath, RID);
        textureRegistry.register(RID, texture);
        return RID;
    }

    /**
     * Get the RID of the cached texture on the given path. This does not load the texture.
     * @param canonicalPath the canonical path to the image, with no metadata tag
     * @return the RID of the texture, or null when there is no texture loaded on the canonical path
     * @see #loadTexture(String) Load and get texture RID
     */
    public ResourceID textureRID(String canonicalPath) {
        return textureIDs.get(canonicalPath);
    }

    /**
     * Unload the texture native resource from the given path.
     * @param canonicalPath the canonical path to the image, with no metadata tag
     */
    public void unloadTexture(String canonicalPath) {
        ResourceID RID = textureIDs.remove(canonicalPath);
        if (RID == null) return;
        Texture texture = textureRegistry.get(RID);
        textureRegistry.unregister(RID);
        if (texture != null) texture.dispose();
    }

    /**
     * Clear the texture cache. This does not free the texture native resource.
     */
    public void clearTextures() {
        textureIDs.clear();
        textureRegistry.clear();
    }

    /**
     * Get or create a new texture handle for the asset.
     * This prevents creating multiple OpenGL textures on the same asset.
     */
    public TextureHandle getTextureHandle(ByteBuffer imageData, AssetReference assetReference, ResourceID RID) {
        String canonicalPath = assetReference.canonicalPath();
        TextureHandle handle = textureHandles.get(canonicalPath);
        if (handle != null) return handle;
        handle = new TextureHandle(RID);
        textureHandles.put(canonicalPath, handle);
        commandQueue.offer(new CreateTextureCommand(handle, imageData, assetReference));
        activeHandles.put(RID.id, handle);
        Logger.debug("Created new handle for " + canonicalPath);
        return handle;
    }

    public TextureHandle getFontAtlasHandle(ByteBuffer atlasData, AssetReference assetReference, GlyphRange glyphRange, int width, int height, int channels, ResourceID RID) {
        AtlasKey key = new AtlasKey(assetReference.canonicalPath(), glyphRange);
        TextureHandle handle = fontAtlasHandles.get(key);
        if (handle != null) return handle;
        handle = new TextureHandle(RID);
        fontAtlasHandles.put(key, handle);
        commandQueue.offer(new CreateFontAtlasTextureCommand(handle, atlasData, assetReference, width, height, channels));
        activeHandles.put(RID.id, handle);
        Logger.debug("Created new atlas handle for " + assetReference.canonicalPath() + " with glyph " + glyphRange.toString());
        return handle;
    }

    /**
     * Create a handle for a load that failed before it could reach the GPU stage.
     * <p>
     * This is an uncached handle, carry the terminal state of a single resource. It holds no OpenGL texture to clean up.
     * @param RID the RID of the resource that failed to load
     * @param message the failure reason
     * @return a handle with its status set to {@link TheCellBeyond.internal.ResourceStatus#Failed} state
     */
    public TextureHandle failedHandle(ResourceID RID, String message) {
        TextureHandle handle = new TextureHandle(RID);
        handle.markFailed(message);
        return handle;
    }

    public void disposeTexture(TextureHandle handle, String canonicalPath) {
        if (handle == null || handle.isDisposed()) return;
        TextureHandle currentHandle = textureHandles.get(canonicalPath);
        if (currentHandle == handle) {
            textureHandles.remove(canonicalPath);
            Logger.debug("Removed texture handle cache for " + canonicalPath);
        }
        commandQueue.offer(new DisposeTextureCommand(handle));
    }

    public void disposeFontAtlasTexture(TextureHandle handle, String canonicalPath, GlyphRange glyphRange) {
        if (handle == null || handle.isDisposed()) return;
        AtlasKey key = new AtlasKey(canonicalPath, glyphRange);
        TextureHandle currentHandle = fontAtlasHandles.get(key);
        if (currentHandle == handle) {
            fontAtlasHandles.remove(key);
            Logger.debug("Removed atlas handle cache for " + canonicalPath + " with glyph" + glyphRange.toString());
        }
        commandQueue.offer(new DisposeTextureCommand(handle));
    }

    public void forceDisposeTexture(TextureHandle handle) {
        if (handle == null || handle.isDisposed()) return;
        commandQueue.offer(new DisposeTextureCommand(handle));
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
                    textureCmd.handle.markFailed("Command execution failed: " + e.getMessage());
                    continue;
                }
                if (!(command instanceof CreateFontAtlasTextureCommand fontCmd)) continue;
                fontCmd.handle.markFailed("Command execution failed: " + e.getMessage());
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
            if (handle.isReady()) glDeleteTextures(handle.getTextureId());
            handle.markDisposed();
        }
        activeHandles.clear();
        textureHandles.clear();
        fontAtlasHandles.clear();
        commandQueue.clear();

        Logger.info("TextureManager cleanup completed");
    }

    void stepCreatedTexture() {
        this.createdTextures++;
    }

    void stepDisposedTexture() {
        this.disposedTextures++;
    }

    void removeActiveHandle(int RID) {
        activeHandles.remove(RID);
    }

    public static void dispose() {
        get().cleanup();
        instance = null;
    }
}
