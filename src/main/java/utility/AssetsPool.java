package utility;

import TCB_Field.Sound;
import components.SpriteSheet;
import render.Shader;
import render.Texture;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AssetsPool {
    private static Map<String, Shader> shader = new HashMap<>();
    private static Map<String, Texture> texture = new HashMap<>();
    private static Map<String, SpriteSheet> spritesheet = new HashMap<>();
    private static Map<String, Sound> sounds = new HashMap<>();


    public static Shader loadShader(String rss) {
        File file = new File(rss);
        if (shader.containsKey(file.getAbsolutePath())) {
            return shader.get(file.getAbsolutePath());
        } else {
            Shader shader = new Shader(rss);
            shader.compile();
            AssetsPool.shader.put(file.getAbsolutePath(), shader);
            return shader;
        }
    }

    public static Texture loadTexture(String rss) {
        File file = new File(rss);
        if (AssetsPool.texture.containsKey(file.getAbsolutePath())) {
            return AssetsPool.texture.get(file.getAbsolutePath());
        } else {
            Texture texture = new Texture();
            texture.init(rss);
            AssetsPool.texture.put(file.getAbsolutePath(), texture);
            return texture;
        }
    }

    public static void addSpSheet(String rss, SpriteSheet spritesheet) {
        File file = new File(rss);
        if (!AssetsPool.spritesheet.containsKey(file.getAbsolutePath())) {
            AssetsPool.spritesheet.put(file.getAbsolutePath(), spritesheet);
        }
    }

    public static SpriteSheet loadSpSheet(String rss) {
        File file = new File(rss);
        if (!AssetsPool.spritesheet.containsKey(file.getAbsolutePath())) {
            assert false : "Error: failed to load '" + rss + "' , no assets added.";
        }
        return AssetsPool.spritesheet.getOrDefault(file.getAbsolutePath(), null);
    }

    public static Sound addSound(String audioFile, boolean isLoop) {
        File file = new File(audioFile);
        if (sounds.containsKey(file.getAbsolutePath())) {
            return sounds.get(file.getAbsolutePath());
        } else {
            Sound sound = new Sound(file.getAbsolutePath(), isLoop);
            AssetsPool.sounds.put(file.getAbsolutePath(), sound);
            return sound;
        }
    }

    public static Sound loadSound(String audioFile) {
        File file = new File(audioFile);
        if (sounds.containsKey(file.getAbsolutePath())) {
            return sounds.get(file.getAbsolutePath());
        } else {
            assert false : "Error: failed to load '" + audioFile + "'";
        }

        return null;
    }

    public static Collection<Sound> loadAllSound() {
        return sounds.values();
    }

}
