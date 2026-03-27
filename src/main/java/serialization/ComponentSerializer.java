package serialization;

import com.google.gson.*;
import components.Component;
import scripting.ScriptLoader;
import utility.log.EngineLog;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Data serializer and deserializer for {@link Component} and its subclasses.
 */
public class ComponentSerializer implements JsonSerializer<Component>, JsonDeserializer<Component> {
    private static final EngineLog Logger = new EngineLog(ComponentSerializer.class);
    private static final String TypeKey = "type";
    private static final String UUIDKey = "uuid";
    private static final String PropertiesKey = "properties";
    private static final String NameKey = "componentName";
    /**
     * Create the component serialization module.
     */
    public ComponentSerializer() {}

    @Override
    public Component deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String type = jsonObject.get(TypeKey).getAsString();
        JsonElement element = jsonObject.get(PropertiesKey);
        try {
            Component component = context.deserialize(element, Class.forName(type, true, ScriptLoader.classLoader()));
            if (jsonObject.has(UUIDKey)) {
                component.setUUID(context.deserialize(jsonObject.get(UUIDKey), UUID.class));
            }
            if (jsonObject.has(NameKey)) {
                component.name(jsonObject.get(NameKey).getAsString());
            }
            return component;
        } catch (ClassNotFoundException e) {
            Logger.error(String.format("Skipping missing component class %s", type));
            return null;
        }
    }

    @Override
    public JsonElement serialize(Component component, Type type, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add(TypeKey, new JsonPrimitive(component.getClass().getCanonicalName()));
        result.add(UUIDKey, context.serialize(component.getUUID()));
        result.add(NameKey, new JsonPrimitive(component.name()));
        result.add(PropertiesKey, context.serialize(component, component.getClass()));
        return result;
    }
}
