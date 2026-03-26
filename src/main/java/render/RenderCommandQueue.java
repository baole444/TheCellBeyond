package render;

import render.commands.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderCommandQueue {
    public enum BatchType {
        Tile,
        Sprite,
        Text
    }

    public record ZIndexGroup(BatchType batchType, int zIndex, int startIndex, int count) {}

    public final List<RectEntry> rectEntries = new ArrayList<>();
    public final List<MeshEntry> meshEntries = new ArrayList<>();
    public final List<TextEntry> textEntries = new ArrayList<>();
    public final List<BatchEntry> entries = new ArrayList<>();
    public final List<ZIndexGroup> zIndexGroups = new ArrayList<>();

    public void collect(RenderCommand chainHead) {
        rectEntries.clear();
        meshEntries.clear();
        textEntries.clear();
        entries.clear();
        zIndexGroups.clear();
        RenderCommand current = chainHead;
        while (current != null) {
            if (current.transform == null || !current.transform.visible) {
                current = current.next;
                continue;
            }
            switch (current) {
                case RectCommand rect -> {
                    RectEntry entry = new RectEntry(rect, current.transform);
                    rectEntries.add(entry);
                    entries.add(entry);
                }
                case MeshCommand mesh -> {
                    MeshEntry entry = new MeshEntry(mesh, current.transform);
                    meshEntries.add(entry);
                    entries.add(entry);
                }
                case TextCommand text -> {
                    TextEntry entry = new TextEntry(text, current.transform);
                    textEntries.add(entry);
                    entries.add(entry);
                }
                default -> {}
            }
            current = current.next;
        }
        entries.sort(Comparator.comparingInt(e -> e.transform().zIndex));
        computeZIndexGroups();
    }

    private void computeZIndexGroups() {
        if (entries.isEmpty()) return;
        BatchType currentType = getBatchType(entries.getFirst());
        int currentZ = entries.getFirst().transform().zIndex;
        int groupStart = 0;
        for (int i = 1; i < entries.size(); i++) {
            BatchType type = getBatchType(entries.get(i));
            int z = entries.get(i).transform().zIndex;
            if (type == currentType && z == currentZ) continue;
            zIndexGroups.add(new ZIndexGroup(currentType, currentZ, groupStart, i - groupStart));
            currentType = type;
            currentZ = z;
            groupStart = i;
        }
        zIndexGroups.add(new ZIndexGroup(currentType, currentZ, groupStart, entries.size() - groupStart));
    }

    private static BatchType getBatchType(BatchEntry entry) {
        return switch (entry) {
            case MeshEntry _ -> BatchType.Tile;
            case RectEntry _ -> BatchType.Sprite;
            case TextEntry _ -> BatchType.Text;
        };
    }

    public void clear() {
        rectEntries.clear();
        meshEntries.clear();
        textEntries.clear();
        entries.clear();
        zIndexGroups.clear();
    }
}
