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
        RenderCommand current = chainHead;
        while (current != null) {
            if (current.transform == null || !current.transform.visible) {
                current = current.next;
                continue;
            }
            switch (current) {
                case RectCommand rect -> rectEntries.add(new RectEntry(rect, current.transform));
                case MeshCommand mesh -> meshEntries.add(new MeshEntry(mesh, current.transform));
                case TextCommand text -> textEntries.add(new TextEntry(text, current.transform));
                default -> {}
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
