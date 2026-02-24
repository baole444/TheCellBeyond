package utility;

import TheCellBeyond.Sound;
import render.FontAtlasTexture;
import render.text.GlyphRange;
import render.texture.SpriteSheet;
import render.Shader;
import render.Texture;
import render.texture.TextureManager;
import render.texture.TextureUnit;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AssetsPool caches assets that loaded into the Engine. These include:
 * <ul>
 *     <li> Shader: the shader programs for rendering</li>
 *     <li> Texture: the textures of imported images</li>
 *     <li> Font Atlas: the textures of imported fonts</li>
 *     <li> Sprite sheet: the sheet of sprites cut from textures</li>
 *     <li> Texture unit: standalone sprites</li>
 *     <li> Sound: the sound object of imported audio</li>
 * </ul>
 */
public class AssetsPool {
    private record AtlasKey(String canonicalPath, GlyphRange glyphRange) {}
    private static final Map<String, Shader> shaders = new ConcurrentHashMap<>();
    private static final Map<String, Texture> textures = new ConcurrentHashMap<>();
    private static final Map<AtlasKey, FontAtlasTexture> fontAtlasTextures = new ConcurrentHashMap<>();
    private static final Map<String, SpriteSheet> spritesheets = new ConcurrentHashMap<>();
    private static final Map<String, TextureUnit> textureUnits = new ConcurrentHashMap<>();
    private static final Map<String, Sound> sounds = new ConcurrentHashMap<>();

    private AssetsPool() {}

    /**
     * Load a shader program from the given path into cache.
     * @param path The path to shader program source
     * @return the {@link Shader} instance compiled from the given source
     */
    public static Shader loadShader(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (shaders.containsKey(canonicalPath)) return shaders.get(canonicalPath);
        Shader shader = new Shader(canonicalPath);
        shader.compile();
        shaders.put(canonicalPath, shader);
        return shader;
    }

    /**
     * Get a shader program from cache using the given path.
     * @param path the path to the shader program source
     * @return the {@link Shader} instance in cache or null if this shader was not loaded
     * @see #loadShader(String) Load and get a shader program
     */
    public static Shader getShader(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        return shaders.get(canonicalPath);
    }

    /**
     * Check if there is a shader program from the given path in cache or not.
     * @param path the path to the shader program source
     * @return true if present in cache
     */
    public static boolean hasShader(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        return shaders.containsKey(canonicalPath);
    }

    /**
     * Reload a shader in cache.
     * @param path the path to the shader program source
     */
    public static void reloadShader(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        Shader shader = shaders.get(canonicalPath);
        if (shader != null) shader.reload();
    }

    /**
     * Reload all the shaders cached.
     */
    public static void reloadAllShader() {
        for (Shader shader : shaders.values()) {
            shader.reload();
        }
    }

    /**
     * Load a texture from the given path into cache.
     * @param path the path to the texture source
     * @return the {@link Texture} instance loaded from the given source
     */
    public static Texture loadTexture(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        Texture currentTexture = textures.get(canonicalPath);
        if (currentTexture != null) return currentTexture;
        Texture texture = new Texture();
        texture.init(canonicalPath);
        textures.put(canonicalPath, texture);
        return texture;
    }

    /**
     * Load a Font atlas texture of given specifications into cache.
     * @param path the path to the texture source
     * @param glyphRange the glyph range of the font
     * @param width width of the atlas in pixels
     * @param height height of the atlas in pixels
     * @param channels the number of colour channels
     * @return the {@link FontAtlasTexture} instance loaded from the given source
     */
    public static FontAtlasTexture loadFontAtlasTexture(String path, GlyphRange glyphRange, int width, int height, int channels) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        AtlasKey key = new AtlasKey(canonicalPath, glyphRange);
        FontAtlasTexture currentTexture = fontAtlasTextures.get(key);
        if (currentTexture != null) return currentTexture;
        FontAtlasTexture texture = new FontAtlasTexture();
        texture.init(canonicalPath, glyphRange, width, height, channels);
        fontAtlasTextures.put(key, texture);
        return texture;
    }

    /**
     * Check if a texture unit from the given path is loaded in cache or not.
     * @param path the path to the texture source
     * @return true if present in cache
     */
    public static boolean hasTextureUnit(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        return textureUnits.containsKey(canonicalPath);
    }

    /**
     * Add a texture unit to cache if one does not exist for the given path yet.
     * @param path the path to the texture source
     * @param textureUnit the texture unit to cache
     */
    public static void addTextureUnit(String path, TextureUnit textureUnit) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (!textureUnits.containsKey(canonicalPath)) {
            textureUnits.put(canonicalPath, textureUnit);
        }
    }

    /**
     * Get a texture unit from cache using the given path.
     * @param path the path to the texture source
     * @return the {@link TextureUnit} instance loaded from the given source
     */
    public static TextureUnit getTextureUnit(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (!textureUnits.containsKey(canonicalPath)) System.err.println("Failed to load '" + canonicalPath + "', no asset added.");
        return textureUnits.getOrDefault(canonicalPath, null);
    }

    /**
     * Check if a sprite sheet from the given path in cache or not.
     * @param path the path to the sprite sheet source
     * @return true if present in cache
     */
    public static boolean hasSpriteSheet(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        return spritesheets.containsKey(canonicalPath);
    }

    /**
     * Add a sprite sheet to cache if one does not exist for the given path yet.
     * @param path the path to the sprite sheet source
     * @param spritesheet the sprite sheet to cache
     */
    public static void addSpriteSheet(String path, SpriteSheet spritesheet) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (!spritesheets.containsKey(canonicalPath)) {
            spritesheets.put(canonicalPath, spritesheet);
        }
    }

    /**
     * Get a sprite sheet from cache using the given path.
     * @param path the path to the sprite sheet source
     * @return the {@link SpriteSheet} instance loaded from the given source
     */
    public static SpriteSheet getSpriteSheet(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (!spritesheets.containsKey(canonicalPath)) {
            System.err.println("Failed to load '" + canonicalPath + "', no asset added.");
        }
        return spritesheets.getOrDefault(canonicalPath, null);
    }

    /**
     * Add a sound to cache if one does not exist for the given source yet.
     * @param path the path to the sourd source
     * @param isLoop is the sound playback looped
     * @return the {@link Sound} instance loaded from the given source
     */
    public static Sound addSound(String path, boolean isLoop) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (sounds.containsKey(canonicalPath)) return sounds.get(canonicalPath);
        UnifiedPaths.AssetPath assetPath = resolver.resolvePath(path);
        Sound sound = new Sound(assetPath.resolvedPath(), isLoop);
        sounds.put(canonicalPath, sound);
        return sound;
    }

    /**
     * Get a sound from cache using the given path.
     * @param path the path to the sound source
     * @return the {@link Sound} instance loaded from the given source
     */
    public static Sound getSound(String path) {
        UnifiedPaths resolver = UnifiedPaths.get();
        String canonicalPath = resolver.toCanonicalPath(path);
        if (sounds.containsKey(canonicalPath)) return sounds.get(canonicalPath);
        System.err.println("Failed to load '" + canonicalPath + "'");
        return null;
    }

    /**
     * Get all the cached sound.
     * @return a collection of {@link Sound} stored in cache
     */
    public static Collection<Sound> getAllSounds() {
        return sounds.values();
    }

    /**
     * Clear all the cache of {@link AssetsPool}.
     */
    public static void clearCache() {
        for (Shader shader : shaders.values()) shader.dispose();
        shaders.clear();
        textures.clear();
        spritesheets.clear();
        textureUnits.clear();
        sounds.clear();
        TextureManager.get().clearPathCache();
    }
}
