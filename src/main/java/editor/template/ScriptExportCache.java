package editor.template;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ScriptExportCache {
    private static final Map<Class<?>, Consumer<Object>> cache = new HashMap<>();
    private ScriptExportCache() {}

    public static void buildAndCache(Class<?> c) {
        if (c == null) return;
        Consumer<Object> block = ExportBlockBuilder.build(c);
        if (block != null) cache.put(c, block);
    }

    public static void clear() {
        cache.clear();
    }

    static void render(Object instance) {
        if (instance == null) return;
        Consumer<Object> block = cache.get(instance.getClass());
        if (block != null) block.accept(instance);
    }
}
