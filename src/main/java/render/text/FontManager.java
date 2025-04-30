package render.text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FontManager {
    private static FontManager instance;

    private final AsyncFontManager asyncFontManager;

    private FontManager() {
        asyncFontManager = AsyncFontManager.get();
    }

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
        try {
            CompletableFuture<TCBFont> future = asyncFontManager.loadFontAsync(filepath, fontSize, isProjectAsset, glyphRange, null);

            return future.get();
        } catch (Exception e)  {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }

            throw new IOException("Failed to load font: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<TCBFont> loadFontAsync(String filepath, int fontSize, boolean isProjectAsset, GlyphRange glyphRange, Consumer<TCBFont> onComplete) {
        return asyncFontManager.loadFontAsync(filepath, fontSize, isProjectAsset, glyphRange, onComplete);
    }

    public TCBFont getFont(String filepath, int fontSize) {
        return getFont(filepath, fontSize, GlyphRange.ASCII);
    }

    public TCBFont getFont(String filepath, int fontSize, GlyphRange glyphRange) {
        return asyncFontManager.getFont(filepath, fontSize, glyphRange);
    }

    public boolean isFontLoaded(String filepath, int fontSize, GlyphRange glyphRange) {
        return asyncFontManager.isFontLoaded(filepath, fontSize, glyphRange);
    }

    public boolean isFontLoading(String filepath, int fontSize, GlyphRange glyphRange) {
        return asyncFontManager.isFontLoading(filepath, fontSize, glyphRange);
    }

    public void cleanup() {
        asyncFontManager.cleanup();
    }
}
