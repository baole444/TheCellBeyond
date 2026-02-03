package components;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.UUID;

public class ComponentSerializer implements JsonSerializer<Component>,
        JsonDeserializer<Component> {

    @Override
    public Component deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        JsonElement element = jsonObject.get("properties");

        try {
            Component component = context.deserialize(element, Class.forName(type));

            if (jsonObject.has("uuid")) {
                component.setUUID(context.deserialize(jsonObject.get("uuid"), UUID.class));
            }

            if (jsonObject.has("componentName")) {
                component.name(jsonObject.get("componentName").getAsString());
            }

            return component;
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Unknown element type: " + type, e);
        }
    }

    @Override
    public JsonElement serialize(Component component, Type type, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("type",
                new JsonPrimitive(component.
                        getClass().
                        getCanonicalName()
                )
        );

        result.add("uuid", context.serialize(component.getUUID()));
        result.add("componentName", new JsonPrimitive(component.name()));
        result.add("properties", context.serialize(component, component.getClass()));
        return result;
    }
}
