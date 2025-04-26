package render.text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static FontManager instance;
    private Map<String, TCBFont> loadedFonts = new HashMap<>();

    private FontManager() {}

    public static FontManager get() {
        if (instance == null) {
            instance = new FontManager();
        }

        return instance;
    }

    public TCBFont loadFont(String filepath, int fontSize, boolean isProjectAsset) throws IOException {
        return loadFont(filepath, fontSize, isProjectAsset, GlyphRange.ASCII);
    }

    public TCBFont loadFont(String filepath, int fontSize, boolean isProjectAsset, GlyphRange glyphRange) throws IOException {
        String key = filepath + "_" + fontSize + "_" + glyphRange.name();
        if (loadedFonts.containsKey(key)) {
            return loadedFonts.get(key);
        }

        TCBFont font = new TCBFont(filepath, fontSize, isProjectAsset, glyphRange);
        loadedFonts.put(key, font);
        return font;
    }

    public TCBFont getFont(String filepath, int fontSize) {
        return getFont(filepath, fontSize, GlyphRange.ASCII);
    }

    public TCBFont getFont(String filepath, int fontSize, GlyphRange glyphRange) {
        String key = filepath + "_" + fontSize + "_" + glyphRange.name();
        return loadedFonts.getOrDefault(key, null);
    }

    public void cleanup() {
        for (TCBFont font : loadedFonts.values()) font.cleanup();;

        loadedFonts.clear();
    }
}
