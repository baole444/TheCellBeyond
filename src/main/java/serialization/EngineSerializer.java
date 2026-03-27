package serialization;

import TheCellBeyond.GameObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import components.Component;
import components.State;
import signal.SignalExclusionStrategy;

/**
 * Standard serialization of the engine, a wrapper with preconfigured gson.
 */
public class EngineSerializer {
    private final Gson gson;

    private EngineSerializer(boolean prettyPrint) {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .registerTypeHierarchyAdapter(State.class, new StateSerializer())
                .setExclusionStrategies(new SignalExclusionStrategy())
                .enableComplexMapKeySerialization();
        if (prettyPrint) builder.setPrettyPrinting();
        this.gson = builder.create();
    }

    public static EngineSerializer standard() {
        return new EngineSerializer(false);
    }

    public static EngineSerializer prettyPrint() {
        return new EngineSerializer(true);
    }

    public String serialize(Object source) {
        return gson.toJson(source);
    }

    public void serialize(Object source, Appendable writer) {
        gson.toJson(source, writer);
    }

    public <T> T deserialize(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public <T> T deserialize(JsonElement jsonElement, Class<T> type) {
        return gson.fromJson(jsonElement, type);
    }

    public JsonElement toJsonTree(Object source) {
        return gson.toJsonTree(source);
    }
}
