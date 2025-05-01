package render.text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class FontRequestEntry {
    final FontRequest request;
    final List<WeakReference<FontLoadCallback>> callbacks = new ArrayList<>();

    FontRequestEntry(FontRequest request, FontLoadCallback callback) {
        this.request = request;
        if (callback != null) {
            this.callbacks.add(new WeakReference<>(callback));
        }
    }

    void addCallback(FontLoadCallback callback) {
        if (callback != null) {
            callbacks.add(new WeakReference<>(callback));
        }
    }
}
