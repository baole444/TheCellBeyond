package utility;

import TheCellBeyond.Sound;
import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceRegistry;
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

/**
 * AssetManager is the centralized control for caching resource while the engine is running.
 */
public class AssetManager {
    private record AtlasKey(String canonicalPath, GlyphRange glyphRange) {}
    private record FontKey(String canonicalPath, GlyphRange glyphRange, float points) {}
    private static AssetManager instance;

    private final Map<String, ResourceID> textureIDs = new ConcurrentHashMap<>();
    private final Map<AtlasKey, ResourceID> fontAtlasIDs = new ConcurrentHashMap<>();
    private final Map<FontKey, ResourceID> fontIDs = new ConcurrentHashMap<>();
    private final Map<String, ResourceID> shaderIDs = new ConcurrentHashMap<>();
    private final Map<String, ResourceID> spriteSheetIDs = new ConcurrentHashMap<>();
    private final Map<String, ResourceID> textureUnitIDs = new ConcurrentHashMap<>();
    private final Map<String, ResourceID> soundIDs = new ConcurrentHashMap<>();

    private final ResourceRegistry<Texture> textureRegistry = new ResourceRegistry<>();
    private final ResourceRegistry<FontAtlasTexture> fontAtlasRegistry = new ResourceRegistry<>();
    private final ResourceRegistry<TCBFont> fontRegistry = new ResourceRegistry<>();
    private final ResourceRegistry<Shader> shaderRegistry = new ResourceRegistry<>();
    private final ResourceRegistry<SpriteSheet> spriteSheetRegistry = new ResourceRegistry<>();
    private final ResourceRegistry<TextureUnit> textureUnitRegistry = new ResourceRegistry<>();
    private final ResourceRegistry<Sound> soundRegistry = new ResourceRegistry<>();

    private AssetManager() {}

    /**
     * Get the asset manager. This will create a new instance if current one don't exist.
     * @return the asset manager instance
     */
    public static AssetManager get() {
        if (instance == null) instance = new AssetManager();
        return instance;
    }

    /**
     * Request loading a new texture into cache.
     * @param path the path to the image
     * @return the {@link ResourceID} for the requested texture
     */
    public ResourceID loadTexture(String path) {
        String canonicalPath = asCanonicalPath(path);
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
     * Load or get the RID of the {@link FontAtlasTexture} for the given font path and glyph range.
     * @param path the unified path or system file path to the font atlas
     * @param glyphRange the glyph range used by that font atlas
     * @return a {@link ResourceID} of an existing font atlas or from a new one
     */
    public ResourceID loadFontAtlas(String path, GlyphRange glyphRange) {
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


    public ResourceID loadFont(String path, GlyphRange glyphRange, float point) {
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

    public ResourceID loadShader(String path) {
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

    public ResourceID addSpriteSheet(String path, SpriteSheet spriteSheet) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = spriteSheetIDs.get(canonicalPath);
        if (RID != null) return RID;
        RID = new ResourceID(AssetResourceType.SpriteSheet);
        spriteSheetIDs.put(canonicalPath, RID);
        spriteSheetRegistry.register(RID, spriteSheet);
        return RID;
    }

    public ResourceID addTextureUnit(String path, TextureUnit textureUnit) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = textureUnitIDs.get(canonicalPath);
        if (RID != null) return RID;
        RID = new ResourceID(AssetResourceType.TextureUnit);
        textureUnitIDs.put(canonicalPath, RID);
        textureUnitRegistry.register(RID, textureUnit);
        return RID;
    }

    public ResourceID loadSound(String path, boolean isLoop) {
        String canonicalPath = asCanonicalPath(path);
        ResourceID RID = soundIDs.get(canonicalPath);
        if (RID != null) return RID;
        UnifiedPaths.AssetPath assetPath = UnifiedPaths.get().resolvePath(path);
        Sound sound = new Sound(assetPath.resolvedPath(), isLoop);
        RID = sound.RID;
        soundIDs.put(canonicalPath, RID);
        soundRegistry.register(RID, sound);
        return RID;
    }

    public boolean hasShader(String path) {
        return shaderIDs.containsKey(asCanonicalPath(path));
    }

    public boolean hasSpriteSheet(String path) {
        return spriteSheetIDs.containsKey(asCanonicalPath(path));
    }

    public boolean hasTextureUnit(String path) {
        return textureUnitIDs.containsKey(asCanonicalPath(path));
    }

    public Shader getShader(String path) {
        ResourceID RID = shaderIDs.get(asCanonicalPath(path));
        return RID != null ? shaderRegistry.get(RID) : null;
    }

    public SpriteSheet getSpriteSheet(String path) {
        ResourceID RID = spriteSheetIDs.get(asCanonicalPath(path));
        return RID != null ? spriteSheetRegistry.get(RID) : null;
    }

    public TextureUnit getTextureUnit(String path) {
        ResourceID RID = textureUnitIDs.get(asCanonicalPath(path));
        return RID != null ? textureUnitRegistry.get(RID) : null;
    }

    public void reloadShader(ResourceID RID) {
        Shader shader = shaderRegistry.get(RID);
        if (shader != null) shader.reload();
    }

    public void reloadAllShader() {
        for (Shader shader : shaderRegistry.values()) shader.reload();
    }

    public Texture getTexture(ResourceID RID) {
        return textureRegistry.get(RID);
    }

    public FontAtlasTexture getFontAtlas(ResourceID RID) {
        return fontAtlasRegistry.get(RID);
    }

    public TCBFont getFont(ResourceID RID) {
        return fontRegistry.get(RID);
    }

    public Shader getShader(ResourceID RID) {
        return shaderRegistry.get(RID);
    }

    public SpriteSheet getSpriteSheet(ResourceID RID) {
        return spriteSheetRegistry.get(RID);
    }

    public TextureUnit getTextureUnit(ResourceID RID) {
        return textureUnitRegistry.get(RID);
    }

    public Sound getSound(ResourceID RID) {
        return soundRegistry.get(RID);
    }

    public void clearCache() {
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

    private void releaseAll(Collection<ResourceID> RIDs) {
        for (ResourceID RID : RIDs) RID.release();
    }

    private String asCanonicalPath(String path) {
        return UnifiedPaths.get().toCanonicalPath(path);
    }
}
