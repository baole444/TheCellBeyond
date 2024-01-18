package utility;

import render.Shader;
import render.Texture;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetsPool {
    private static Map<String, Shader> shader = new HashMap<>();
    private static Map<String, Texture> texture = new HashMap<>();

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
            Texture texture = new Texture(rss);
            AssetsPool.texture.put(file.getAbsolutePath(), texture);
            return texture;
        }
    }
}
