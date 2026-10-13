package utility;

import TheCellBeyond.Sound;
import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceRegistry;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import org.joml.Vector2i;
import render.FontAtlasTexture;
import render.Shader;
import render.Texture;
import render.text.FontManager;
import render.text.GlyphRange;
import render.text.TCBFont;
import render.texture.SingleSprite;
import render.texture.SpriteSheet;
import render.texture.TextureManager;
import scripting.API;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * AssetManager is a collection of static methods providing control for caching resource while the engine is running.
 */
@API
public final class AssetManager {
    private record AtlasKey(String canonicalPath, GlyphRange glyphRange) {}
    private record FontKey(String canonicalPath, GlyphRange glyphRange, float points) {}
    private static final Map<AtlasKey, ResourceID> fontAtlasIDs = new ConcurrentHashMap<>();
    private static final Map<FontKey, ResourceID> fontIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> shaderIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> spriteSheetIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> singleSpriteIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> soundIDs = new ConcurrentHashMap<>();
    private static final ResourceRegistry<FontAtlasTexture> fontAtlasRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<TCBFont> fontRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<Shader> shaderRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<SpriteSheet> spriteSheetRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<SingleSprite> singleSpriteRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<Sound> soundRegistry = new ResourceRegistry<>();

    private AssetManager() {}

    /**
     * Start tracking status for a resource using the given RID.
     * @param RID the RID to track status for
     * @param onChange callback to invoked for each status transition
     * @return a tracker bound to RID or null if the callback and RID given were null
     */
    public static ResourceTracker track(ResourceID RID, BiConsumer<ResourceID, ResourceStatus> onChange) {
        if (RID == null || onChange == null) return null;
        return new ResourceTracker(RID, onChange);
    }

    /**
     * Request to load a new texture into cache. This simply return the RID if the texture already exists.
     * @param path the path to the image
     * @return the {@link ResourceID} for the requested texture, or null if the path is null
     */
    public static ResourceID loadTexture(String path) {
        if (path == null) return null;
        String canonicalPath = UnifiedPaths.stripMetadata(asCanonicalPath(path));
        return TextureManager.get().loadTexture(canonicalPath);
    }

    /**
     * Request to load a new texture into cache. This simply return the RID if the texture already exists.
     * @param reference the reference to the image
     * @return the {@link ResourceID} for the requested texture, or null if the reference is null
     */
    public static ResourceID loadTexture(AssetReference reference) {
        if (reference == null) return null;
        ResourceID cached = reference.RID;
        if (cached != null && TextureManager.get().texture(cached) != null) return cached;
        cached = loadTexture(reference.canonicalPath());
        reference.RID = cached;
        return cached;
    }

    /**
     * Get the size of a loaded texture image.
     * @param RID the RID of the texture
     * @return the image's size in pixels, or null if there is no texture with the given RID
     */
    public static Vector2i textureSize(ResourceID RID) {
        Texture texture = TextureManager.get().texture(RID);
        if (texture == null || texture.width() <= 0 || texture.height() <= 0) return null;
        return new Vector2i(texture.width(), texture.height());
    }

    /**
     * Get the native ID for a texture on the GPU.
     * @param RID he RID of the texture
     * @return the texture's ID number, or -1 if there is no texture with the given RID loaded on the GPU
     */
    public static int textureID(ResourceID RID) {
        Texture texture = TextureManager.get().texture(RID);
        return texture != null ? texture.nativeID() : -1;
    }

    /**
     * Get the current status of a texture.
     * @param RID the RID of the texture
     * @return the texture's status, or null when there is no texture with the given RID
     */
    public static ResourceStatus textureStatus(ResourceID RID) {
        Texture texture = TextureManager.get().texture(RID);
        return texture != null ? texture.status() : null;
    }

    /**
     * Get the path of a load texture's image.
     * @param RID the RID of the texture
     * @return the canonical path of the image, or null when there is no texture with the given RID
     */
    public static String texturePath(ResourceID RID) {
        Texture texture = TextureManager.get().texture(RID);
        return texture != null ? texture.canonicalPath() : null;
    }

    /**
     * Load or get the RID of the {@link FontAtlasTexture} for the given font path and glyph range.
     * @param path the unified path or system file path to the font
     * @param glyphRange the glyph range used by that font
     * @return a {@link ResourceID} of an existing font atlas or from a new one
     */
    public static ResourceID loadFontAtlas(String path, GlyphRange glyphRange) {
        String canonicalPath = asCanonicalPath(path);
        AtlasKey key = new AtlasKey(canonicalPath, glyphRange);
        ResourceID RID = fontAtlasIDs.get(key);
        if (RID != null) return RID;
        FontAtlasTexture atlas = new FontAtlasTexture();
        atlas.init(canonicalPath, glyphRange);
        RID = atlas.RID;
        fontAtlasIDs.put(key, RID);
        fontAtlasRegistry.register(RID, atlas);
        return RID;
    }

    /**
     * Load or get the RID of the {@link TCBFont} for the given font path. glyph range and font point size.
     * @param path the unified path or system file path to the font
     * @param glyphRange the glyph range used by that font
     * @param point the size of the font in point ({@link FontPT})
     * @return a {@link ResourceID} of an existing font or from a new one
     */
    public static ResourceID loadFont(String path, GlyphRange glyphRange, float point) {
        String canonicalPath = asCanonicalPath(path);
        FontKey key = new FontKey(canonicalPath, glyphRange, point);
        ResourceID RID = fontIDs.get(key);
        if (RID != null) return RID;
        ResourceID atlasID = loadFontAtlas(canonicalPath, glyphRange);
        TCBFont font = new TCBFont(new AssetReference(canonicalPath), glyphRange, point);
        RID = font.RID;
        fontIDs.put(key, RID);
        fontRegistry.register(RID, font);
        FontManager.get().processFont(font, atlasID);
        return RID;
    }

    /**
     * Load or get the RID of the {@link Shader} for the given path. This will compile the shader if not loaded yet.
     * @param path the path to the shader file
     * @return a {@link ResourceID} of an existing shader or from a new one
     */
    public static ResourceID loadShader(String path) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = shaderIDs.get(canonicalPath);
        if (RID != null) return RID;
        Shader shader = new Shader(canonicalPath);
        shader.compile();
        RID = shader.RID;
        shaderIDs.put(canonicalPath, RID);
        shaderRegistry.register(RID, shader);
        return RID;
    }

    /**
     * Add a sprite sheet into cache, keyed by the given path, if it is not cached yet.
     * @param path the path to the sprite sheet's texture
     * @param spriteSheet the sprite sheet to cache
     * @return the {@link ResourceID} of the existing sprite sheet or from a new one
     */
    public static ResourceID addSpriteSheet(String path, SpriteSheet spriteSheet) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = spriteSheetIDs.get(canonicalPath);
        if (RID != null) return RID;
        RID = new ResourceID(AssetResourceType.SpriteSheet);
        spriteSheetIDs.put(canonicalPath, RID);
        spriteSheetRegistry.register(RID, spriteSheet);
        return RID;
    }

    /**
     * Add a single sprite into cache, keyed by the given path, if it is not cached yet.
     * @param path the path to the single sprite's texture
     * @param singleSprite the single sprite to cache
     * @return the {@link ResourceID} of the existing single sprite or from a new one
     */
    public static ResourceID addSingleSprite(String path, SingleSprite singleSprite) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = singleSpriteIDs.get(canonicalPath);
        if (RID != null) return RID;
        RID = new ResourceID(AssetResourceType.SingleSprite);
        singleSpriteIDs.put(canonicalPath, RID);
        singleSpriteRegistry.register(RID, singleSprite);
        return RID;
    }

    /**
     * Load or get the RID of the {@link Sound} clip for the given path.
     * @param path the path to the sound file
     * @param isLoop the sound clip loop status
     * @return a {@link ResourceID} of an existing sound or from a new one
     */
    public static ResourceID loadSound(String path, boolean isLoop) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = soundIDs.get(canonicalPath);
        if (RID != null) return RID;
        UnifiedPaths.AssetPath assetPath = UnifiedPaths.resolvePath(path);
        Sound sound = new Sound(assetPath.resolvedPath(), isLoop);
        RID = sound.RID;
        soundIDs.put(canonicalPath, RID);
        soundRegistry.register(RID, sound);
        return RID;
    }

    /**
     * Check if there is a shader cached for the given path or not.
     * @param path the path to the shader file
     * @return true if cached
     */
    public static boolean hasShader(String path) {
        return shaderIDs.containsKey(asCanonicalPath(path));
    }

    /**
     * Check if there is a sprite sheet cached for the given path or not.
     * @param path the path to the sprite sheet's texture file
     * @return true if cached
     */
    public static boolean hasSpriteSheet(String path) {
        return spriteSheetIDs.containsKey(asCanonicalPath(path));
    }

    /**
     * Check if there is a single sprite cached for the given path or not.
     * @param path the path to the single sprite's texture file
     * @return true if cached
     */
    public static boolean hasSingleSprite(String path) {
        return singleSpriteIDs.containsKey(asCanonicalPath(path));
    }

    /**
     * Get the shader program from cache.
     * @param path the path to the shader file
     * @return the shader program or null if there is none.
     */
    public static Shader getShader(String path) {
        ResourceID RID = shaderIDs.get(asCanonicalPath(path));
        return RID != null ? shaderRegistry.get(RID) : null;
    }

    /**
     * Get the sprite sheet from cache.
     * @param path the path to the sprite sheet's texture file
     * @return the sprite sheet or null if there is none
     */
    public static SpriteSheet getSpriteSheet(String path) {
        ResourceID RID = spriteSheetIDs.get(asCanonicalPath(path));
        return RID != null ? spriteSheetRegistry.get(RID) : null;
    }

    /**
     * Get the single sprite from cache.
     * @param path the path to the single sprite's texture file
     * @return the single sprite or null if there is none
     */
    public static SingleSprite getSingleSprite(String path) {
        ResourceID RID = singleSpriteIDs.get(asCanonicalPath(path));
        return RID != null ? singleSpriteRegistry.get(RID) : null;
    }

    /**
     * Reload the shader from cache using its RID.
     * @param RID the RID of the shader program
     */
    public static void reloadShader(ResourceID RID) {
        Shader shader = shaderRegistry.get(RID);
        if (shader != null) shader.reload();
    }

    /**
     * Reload all the cached shader.
     */
    public static void reloadAllShader() {
        for (Shader shader : shaderRegistry.values()) shader.reload();
    }

    /**
     * Get a font atlas from cache, using the given RID.
     * @param RID the RID of the atlas
     * @return the cached font atlas or null if there is none
     */
    public static FontAtlasTexture getFontAtlas(ResourceID RID) {
        return fontAtlasRegistry.get(RID);
    }

    /**
     * Get a font from cache, using the given RID.
     * @param RID the RID of the font
     * @return the cached font or null if there is none
     */
    public static TCBFont getFont(ResourceID RID) {
        return fontRegistry.get(RID);
    }

    /**
     * Get a shader program from cache, using the given RID.
     * @param RID the RID of the shader
     * @return the cached shader or null if there is none
     */
    public static Shader getShader(ResourceID RID) {
        return shaderRegistry.get(RID);
    }

    /**
     * Get a sprite sheet from cache, using the given RID.
     * @param RID the RID of the sprite sheet
     * @return the cached sprite sheet or null if there is none
     */
    public static SpriteSheet getSpriteSheet(ResourceID RID) {
        return spriteSheetRegistry.get(RID);
    }

    /**
     * Get a single sprite from cache, using the given RID.
     * @param RID the RID of the single sprite
     * @return the cached single sprite or null if there is none
     */
    public static SingleSprite getSingleSprite(ResourceID RID) {
        return singleSpriteRegistry.get(RID);
    }

    /**
     * Get a sound from cache, using the given RID.
     * @param RID the RID of the sound
     * @return the cached sound or null if there is none
     */
    public static Sound getSound(ResourceID RID) {
        return soundRegistry.get(RID);
    }

    public static void unloadTexture(String path) {
        if (path == null || path.isBlank()) return;
        String canonicalPath = UnifiedPaths.stripMetadata(asCanonicalPath(path));
        TextureManager.get().unloadTexture(canonicalPath);
    }

    public static void unloadSpriteSheet(String path) {
        if (path == null || path.isBlank()) return;
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = spriteSheetIDs.remove(canonicalPath);
        if (RID == null) return;
        SpriteSheet sheet = spriteSheetRegistry.get(RID);
        spriteSheetRegistry.unregister(RID);
        if (sheet != null) sheet.dispose();
    }

    public static void unloadSingleSprite(String path) {
        if (path == null || path.isBlank()) return;
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = singleSpriteIDs.remove(canonicalPath);
        if (RID == null) return;
        singleSpriteRegistry.unregister(RID);
    }

    /**
     * Unload a cached shader, disposing its program.
     * @param path the path to the shader file
     */
    public static void unloadShader(String path) {
        if (path == null || path.isBlank()) return;
        ResourceID RID = shaderIDs.remove(asCanonicalPath(path));
        if (RID == null) return;
        Shader shader = shaderRegistry.get(RID);
        shaderRegistry.unregister(RID);
        if (shader != null) shader.dispose();
    }

    /**
     * Unload a cached sound, disposing its native buffer and source.
     * @param path the path to the sound file
     */
    public static void unloadSound(String path) {
        if (path == null || path.isBlank()) return;
        ResourceID RID = soundIDs.remove(asCanonicalPath(path));
        if (RID == null) return;
        Sound sound = soundRegistry.get(RID);
        soundRegistry.unregister(RID);
        if (sound != null) sound.dispose();
    }

    /**
     * Unload a cached font, its font's atlas is disposed only when there is no other loaded font on the same path and glyph range remains.
     * @param path the unified path or system file path to the font
     * @param glyphRange the glyph range used by that font
     * @param point the size of the font in point ({@link FontPT})
     */
    public static void unloadFont(String path, GlyphRange glyphRange, float point) {
        if (path == null || path.isBlank()) return;
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = fontIDs.remove(new FontKey(canonicalPath, glyphRange, point));
        if (RID == null) return;
        fontRegistry.unregister(RID);
        ResourceStatusCallback.emit(RID, ResourceStatus.Disposed);
        unloadFontAtlas(canonicalPath, glyphRange);
    }

    /**
     * Unload the font atlas for the given path and glyph range, if no loaded font still reference it.
     * @param canonicalPath the canonical path to the font
     * @param glyphRange the glyph range used by that atlas
     */
    private static void unloadFontAtlas(String canonicalPath, GlyphRange glyphRange) {
        for (FontKey key : fontIDs.keySet()) if (key.canonicalPath.equals(canonicalPath) && key.glyphRange == glyphRange) return;
        FontManager.get().releaseAtlas(new AssetReference(canonicalPath), glyphRange);
        ResourceID atlasRID = fontAtlasIDs.remove(new AtlasKey(canonicalPath, glyphRange));
        if (atlasRID == null) return;
        FontAtlasTexture atlas = fontAtlasRegistry.get(atlasRID);
        fontAtlasRegistry.unregister(atlasRID);
        if (atlas != null) atlas.dispose();
    }

    /**
     * Clear all cached resource and clear their RID. This will also dispose all shader program and sound's native resource.
     */
    public static void clearCache() {
        for (Shader shader : shaderRegistry.values()) shader.dispose();
        for (Sound sound : soundRegistry.values()) sound.dispose();
        fontAtlasIDs.clear();
        fontIDs.clear();
        shaderIDs.clear();
        spriteSheetIDs.clear();
        singleSpriteIDs.clear();
        soundIDs.clear();
        fontAtlasRegistry.clear();
        fontRegistry.clear();
        shaderRegistry.clear();
        spriteSheetRegistry.clear();
        singleSpriteRegistry.clear();
        soundRegistry.clear();
        TextureManager.get().clearTextures();
        TextureManager.get().clearPathCache();
    }

    private static String asCanonicalPath(String path) {
        return UnifiedPaths.toCanonicalPath(path);
    }
}
