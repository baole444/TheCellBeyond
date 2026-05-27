package utility;

import TheCellBeyond.Sound;
import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceRegistry;
import TheCellBeyond.internal.ResourceStatus;
import render.FontAtlasTexture;
import render.Shader;
import render.Texture;
import render.text.FontManager;
import render.text.GlyphRange;
import render.text.TCBFont;
import render.texture.SpriteSheet;
import render.texture.TextureManager;
import render.texture.TextureUnit;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * AssetManager is a collection of static methods providing control for caching resource while the engine is running.
 */
public final class AssetManager {
    private record AtlasKey(String canonicalPath, GlyphRange glyphRange) {}
    private record FontKey(String canonicalPath, GlyphRange glyphRange, float points) {}
    private static final Map<String, ResourceID> textureIDs = new ConcurrentHashMap<>();
    private static final Map<AtlasKey, ResourceID> fontAtlasIDs = new ConcurrentHashMap<>();
    private static final Map<FontKey, ResourceID> fontIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> shaderIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> spriteSheetIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> textureUnitIDs = new ConcurrentHashMap<>();
    private static final Map<String, ResourceID> soundIDs = new ConcurrentHashMap<>();
    private static final ResourceRegistry<Texture> textureRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<FontAtlasTexture> fontAtlasRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<TCBFont> fontRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<Shader> shaderRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<SpriteSheet> spriteSheetRegistry = new ResourceRegistry<>();
    private static final ResourceRegistry<TextureUnit> textureUnitRegistry = new ResourceRegistry<>();
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
     * Request loading a new texture into cache.
     * @param path the path to the image
     * @return the {@link ResourceID} for the requested texture
     */
    public static ResourceID loadTexture(String path) {
        String canonicalPath = UnifiedPaths.stripMetadata(asCanonicalPath(path));
        ResourceID RID = textureIDs.get(canonicalPath);
        if (RID != null) return RID;
        Texture texture = new Texture();
        texture.init(canonicalPath);
        RID = texture.RID;
        textureIDs.put(canonicalPath, RID);
        textureRegistry.register(RID, texture);
        return RID;
    }

    public static ResourceID getTextureRID(String path) {
        return textureIDs.get(UnifiedPaths.stripMetadata(asCanonicalPath(path)));
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
     * Add a texture unit into cache, keyed by the given path, if it is not cached yet.
     * @param path the path to the texture unit's texture
     * @param textureUnit the texture unit to cache
     * @return the {@link ResourceID} of the existing texture unit or from a new one
     */
    public static ResourceID addTextureUnit(String path, TextureUnit textureUnit) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = textureUnitIDs.get(canonicalPath);
        if (RID != null) return RID;
        RID = new ResourceID(AssetResourceType.TextureUnit);
        textureUnitIDs.put(canonicalPath, RID);
        textureUnitRegistry.register(RID, textureUnit);
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
     * Check if there is a texture unit cached for the given path or not.
     * @param path the path to the texture unit's texture file
     * @return true if cached
     */
    public static boolean hasTextureUnit(String path) {
        return textureUnitIDs.containsKey(asCanonicalPath(path));
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
     * Get the texture unit from cache.
     * @param path the path to the texture unit's texture file
     * @return the texture unit or null if there is none
     */
    public static TextureUnit getTextureUnit(String path) {
        ResourceID RID = textureUnitIDs.get(asCanonicalPath(path));
        return RID != null ? textureUnitRegistry.get(RID) : null;
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
     * Get a texture from cache, using the given RID.
     * @param RID the RID of the texture
     * @return the cached texture or null if there is none
     */
    public static Texture getTexture(ResourceID RID) {
        return textureRegistry.get(RID);
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
     * Get a texture unit from cache, using the given RID.
     * @param RID the RID of the texture unit
     * @return the cached texture unit or null if there is none
     */
    public static TextureUnit getTextureUnit(ResourceID RID) {
        return textureUnitRegistry.get(RID);
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
        ResourceID RID = textureIDs.remove(canonicalPath);
        if (RID == null) return;
        Texture texture = textureRegistry.get(RID);
        textureRegistry.unregister(RID);
        if (texture != null) texture.dispose();
        RID.release();
    }

    public static void unloadSpriteSheet(String path) {
        if (path == null || path.isBlank()) return;
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = spriteSheetIDs.remove(canonicalPath);
        if (RID == null) return;
        SpriteSheet sheet = spriteSheetRegistry.get(RID);
        spriteSheetRegistry.unregister(RID);
        RID.release();
        if (sheet != null) sheet.dispose();
    }

    public static void unloadTextureUnit(String path) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = textureUnitIDs.remove(canonicalPath);
        if (RID == null) return;
        TextureUnit unit = textureUnitRegistry.get(RID);
        textureRegistry.unregister(RID);
        RID.release();
        if (unit != null) unit.dispose();
    }

    /**
     * Clear all cached resource and clear their RID. This will also dispose all shader program and sound's native resource.
     */
    public static void clearCache() {
        for (Shader shader : shaderRegistry.values()) shader.dispose();
        for (Sound sound : soundRegistry.values()) sound.dispose();
        releaseAll(textureIDs.values());
        releaseAll(fontAtlasIDs.values());
        releaseAll(fontIDs.values());
        releaseAll(shaderIDs.values());
        releaseAll(spriteSheetIDs.values());
        releaseAll(textureUnitIDs.values());
        releaseAll(soundIDs.values());
        textureIDs.clear();
        fontAtlasIDs.clear();
        fontIDs.clear();
        shaderIDs.clear();
        spriteSheetIDs.clear();
        textureUnitIDs.clear();
        soundIDs.clear();
        textureRegistry.clear();
        fontAtlasRegistry.clear();
        fontRegistry.clear();
        shaderRegistry.clear();
        spriteSheetRegistry.clear();
        textureUnitRegistry.clear();
        soundRegistry.clear();
        TextureManager.get().clearPathCache();
    }

    private static void releaseAll(Collection<ResourceID> RIDs) {
        for (ResourceID RID : RIDs) RID.release();
    }

    private static String asCanonicalPath(String path) {
        return UnifiedPaths.toCanonicalPath(path);
    }
}
