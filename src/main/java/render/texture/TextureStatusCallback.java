package render.texture;

import java.util.ArrayList;
import java.util.List;

public class TextureStatusCallback {
    private static final List<TextureStatusListener> listeners = new ArrayList<>();

    static void register(TextureStatusListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    static void emit(int handleId, TextureHandle.Status status) {
        for (TextureStatusListener listener : listeners) {
            listener.onTextureStatusChange(handleId, status);
        }
    }
}
