package utility;

import TheCellBeyond.Sound;
import components.SpriteSheet;
import render.Shader;
import render.Texture;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AssetsPool {
    private static final Map<String, Shader> shaders = new ConcurrentHashMap<>();
    private static final Map<String, Texture> textures = new ConcurrentHashMap<>();
    private static final Map<String, SpriteSheet> spritesheets = new ConcurrentHashMap<>();
    private static final Map<String, Sound> sounds = new ConcurrentHashMap<>();

    public static Shader loadShader(String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        if (shaders.containsKey(canonicalPath)) {
            return shaders.get(canonicalPath);
        } else {
            Shader shader = new Shader(canonicalPath);
            shader.compile();
            shaders.put(canonicalPath, shader);

            return shader;
        }
    }

    public static Shader getShader (String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        return shaders.get(canonicalPath);
    }

    public static boolean hasShader(String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        return shaders.containsKey(canonicalPath);
    }

    public static void reloadShader(String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        Shader shader = shaders.get(canonicalPath);

        if (shader != null) shader.reload();
    }

    public static void reloadAllShader() {
        for (Shader shader : shaders.values()) {
            shader.reload();
        }
    }

    public static Texture loadTexture(String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        if (textures.containsKey(canonicalPath)) {
            return textures.get(canonicalPath);
        } else {
            Texture texture = new Texture();
            texture.init(canonicalPath);
            textures.put(canonicalPath, texture);

            return texture;
        }
    }

    public static void addSpriteSheet(String path, SpriteSheet spritesheet) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        if (!spritesheets.containsKey(canonicalPath)) {
            spritesheets.put(canonicalPath, spritesheet);
        }
    }

    public static SpriteSheet loadSpriteSheet(String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        if (!spritesheets.containsKey(canonicalPath)) {
            System.err.println("Failed to load '" + canonicalPath + "', no asset added.");
        }

        return spritesheets.getOrDefault(canonicalPath, null);
    }

    public static Sound addSound(String path, boolean isLoop) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        if (sounds.containsKey(canonicalPath)) {
            return sounds.get(canonicalPath);
        } else {
            PathResolver.AssetPath assetPath = resolver.resolvePath(path);

            Sound sound = new Sound(assetPath.resolvedPath(), isLoop);
            sounds.put(canonicalPath, sound);
            return sound;
        }
    }

    public static Sound loadSound(String path) {
        PathResolver resolver = PathResolver.get();
        String canonicalPath = resolver.toCanonicalPath(path);

        if (sounds.containsKey(canonicalPath)) {
            return sounds.get(canonicalPath);
        } else {
            System.err.println("Failed to load '" + canonicalPath + "'");
        }

        return null;
    }

    public static Collection<Sound> loadAllSound() {
        return sounds.values();
    }

    public static void clearCache() {
        for (Shader shader : shaders.values()) {
            shader.dispose();
        }

        shaders.clear();
        textures.clear();
        spritesheets.clear();
        sounds.clear();
    }

}
