package render.text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class FontRequestEntry {
    final FontRequest request;
    final List<WeakReference<FontStatusCallback>> callbacks = new ArrayList<>();

    FontRequestEntry(FontRequest request, FontStatusCallback callback) {
        this.request = request;
        if (callback != null) {
            this.callbacks.add(new WeakReference<>(callback));
        }
    }

    void addCallback(FontStatusCallback callback) {
        if (callback != null) {
            callbacks.add(new WeakReference<>(callback));
        }
    }
}
