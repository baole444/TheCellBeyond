package TheCellBeyond.internal;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceRegistry<T> {
    private final ConcurrentHashMap<Integer, T> resourceEntries = new ConcurrentHashMap<>();

    public void register(ResourceID RID, T resource) {
        resourceEntries.put(RID.id, resource);
    }

    public T get(ResourceID RID) {
        return resourceEntries.get(RID.id);
    }

    public boolean contains(ResourceID RID) {
        return resourceEntries.containsKey(RID.id);
    }

    public void unregister(ResourceID RID) {
        resourceEntries.remove(RID.id);
    }

    public Collection<T> values() {
        return resourceEntries.values();
    }

    public void clear() {
        resourceEntries.clear();
    }

    public int size() {
        return resourceEntries.size();
    }
}
