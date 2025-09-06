package render.texture;

import render.Texture;
import utility.AssetReference;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class TextureManager {
    static final Logger LOGGER = Logger.getLogger(TextureManager.class.getName());
    private static TextureManager instance;

    private final ConcurrentLinkedQueue<TextureCommand> commandQueue;
    private final ConcurrentHashMap<Integer, TextureHandle> activeHandles;
    private final ConcurrentHashMap<String, TextureHandle> handlesByPath;

    // Limit per cycle
    private int createdTextures = 0;
    private int disposedTextures = 0;
    private static final int TEXTURE_OPERATION_LIMIT = 8;

    private TextureManager() {
        commandQueue = new ConcurrentLinkedQueue<>();
        activeHandles = new ConcurrentHashMap<>();
        handlesByPath = new ConcurrentHashMap<>();
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

    void removeHandleByPath(String canonicalPath) {
        handlesByPath.remove(canonicalPath);
    }

    void removeActiveHandle(int handleId) {
        activeHandles.remove(handleId);
    }

    /**
     * Get or create a new texture handle for the asset.
     * This prevents creating multiple OpenGL textures on the same asset.
     */
    public TextureHandle getTextureHandle(ByteBuffer imageData, AssetReference assetReference) {
        String canonicalPath = assetReference.getCanonicalPath();

        TextureHandle currentHandle = handlesByPath.get(canonicalPath);
        if (currentHandle != null) return currentHandle;

        TextureHandle handle = new TextureHandle();
        handlesByPath.put(canonicalPath, handle);

        CreateTextureCommand command = new CreateTextureCommand(handle, imageData, assetReference);

        commandQueue.offer(command);

        activeHandles.put(handle.getHandleId(), handle);

        LOGGER.fine("Created new handle for: " + canonicalPath);
        return handle;
    }

    public void disposeTexture(TextureHandle handle, String canonicalPath) {
        if (handle != null && !handle.isDisposed()) {
            TextureHandle currentHandle = handlesByPath.get(canonicalPath);
            if (currentHandle == handle) {
                handlesByPath.remove(canonicalPath);
                LOGGER.fine("Removed path cache for: " + canonicalPath);
            }

            DisposeTextureCommand command = new DisposeTextureCommand(handle);
            commandQueue.offer(command);
        }
    }

    public void forceDisposeTexture(TextureHandle handle) {
        if (handle != null && !handle.isDisposed()) {
            DisposeTextureCommand command = new DisposeTextureCommand(handle);
            commandQueue.offer(command);
        }
    }

    public void processCommands() {
        createdTextures = 0;
        disposedTextures = 0;

        while (!commandQueue.isEmpty() && (createdTextures + disposedTextures) < TEXTURE_OPERATION_LIMIT) {
            TextureCommand command = commandQueue.poll();
            if (command != null) {
                try {
                    command.execute();
                } catch (Exception e) {
                    LOGGER.severe("Failed to execute texture command: " + command + e);
                    if (command instanceof CreateTextureCommand createTCmd) {
                        createTCmd.handle.setError("Command execution failed: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void clearPathCache() {
        handlesByPath.clear();
        LOGGER.info("Cleared texture path cache");
    }

    void cleanup() {
        for (TextureHandle handle : activeHandles.values()) {
            if (handle.isReady()) {
                glDeleteTextures(handle.getTextureId());
            }

            handle.setStatus(TextureHandle.Status.DISPOSED);
        }

        activeHandles.clear();
        handlesByPath.clear();
        commandQueue.clear();

        LOGGER.info("TextureManager cleanup completed");
    }

    public static void dispose() {
        get().cleanup();
        TextureStatusCallback.clear();
        instance = null;
    }
}
