package render.texture;

import java.util.concurrent.CopyOnWriteArrayList;

public class TextureStatusCallback {
    private static final CopyOnWriteArrayList<TextureStatusListener> listeners = new CopyOnWriteArrayList<>();

    static void register(TextureStatusListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    static void unRegister(TextureStatusListener listener) {
        listeners.remove(listener);
    }

    static void emit(int handleId, TextureHandle.Status status) {
        for (TextureStatusListener listener : listeners) {
            listener.onTextureStatusChange(handleId, status);
        }
    }

    static void clear() {
        listeners.clear();
    }
}
