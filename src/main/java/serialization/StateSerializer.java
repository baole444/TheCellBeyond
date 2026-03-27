package serialization;

import com.google.gson.*;
import components.State;
import scripting.ScriptLoader;

import java.lang.reflect.Type;

public class StateSerializer implements JsonSerializer<State>, JsonDeserializer<State> {
    private static final String TypeKey = "type";
    private static final String PropertiesKey = "properties";

    @Override
    public JsonElement serialize(State state, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty(TypeKey, state.getClass().getCanonicalName());
        result.add(PropertiesKey, context.serialize(state, state.getClass()));
        return result;
    }

    @Override
    public State deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has(TypeKey)) return context.deserialize(jsonObject, State.class);
        String className = jsonObject.get(TypeKey).getAsString();
        JsonElement properties = jsonObject.get(PropertiesKey);
        try {
            Class<?> stateClass = Class.forName(className, true, ScriptLoader.classLoader());
            return context.deserialize(properties, stateClass);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Unknown State type: " + className, e);
        }
    }
}
