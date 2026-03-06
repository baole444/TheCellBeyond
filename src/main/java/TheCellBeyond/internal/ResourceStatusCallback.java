package TheCellBeyond.internal;

import java.util.concurrent.CopyOnWriteArrayList;

public class ResourceStatusCallback {
    private static final CopyOnWriteArrayList<ResourceStatusListener> resources = new CopyOnWriteArrayList<>();

    public static void register(ResourceStatusListener listener) {
        if (!resources.contains(listener)) resources.add(listener);
    }

    public static void unregister(ResourceStatusListener listener) {
        resources.remove(listener);
    }

    public static void emit(ResourceID RID, ResourceStatus status) {
        for (ResourceStatusListener listener : resources) listener.onResourceStatusChange(RID, status);
    }

    public static void clear() {
        resources.clear();
    }
}
