package render;

import render.commands.*;

import java.util.ArrayList;
import java.util.List;

public class RenderCommandQueue {
    public final List<RectEntry> rectEntries = new ArrayList<>();
    public final List<MeshEntry> meshEntries = new ArrayList<>();
    public final List<TextEntry> textEntries = new ArrayList<>();

    public void collect(RenderCommand chainHead) {
        rectEntries.clear();
        meshEntries.clear();
        textEntries.clear();
        TransformCommand currentTransform = null;
        boolean visible = true;
        RenderCommand current = chainHead;
        while (current != null) {
            if (current instanceof TransformCommand transform) {
                currentTransform = transform;
                visible = transform.visible;
                current = current.next;
                continue;
            }
            if (visible && currentTransform != null) {
                switch (current) {
                    case RectCommand rect -> rectEntries.add(new RectEntry(rect, currentTransform));
                    case MeshCommand mesh -> meshEntries.add(new MeshEntry(mesh, currentTransform));
                    case TextCommand text -> textEntries.add(new TextEntry(text, currentTransform));
                    default -> {}
                }
            }
            current = current.next;
        }
    }

    public void clear() {
        rectEntries.clear();
        meshEntries.clear();
        textEntries.clear();
    }
}
