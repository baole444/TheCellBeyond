package components;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * A custom serializer and deserializer for {@link Component} objects,
 * enabling conversion between {@link Component} instances and their JSON representation.
 * <p>
 * This class integrates with Gson to support saving and loading {@code Component}
 * subclasses by embedding type information, UUID, and component name into JSON.
 * </p>
 */
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
                component.setUUID(jsonObject.get("uuid").getAsString());
            }

            if (jsonObject.has("componentName")) {
                component.setComponentName(jsonObject.get("componentName").getAsString());
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

        result.add("uuid", new JsonPrimitive(component.getUUID()));

        if (component.getComponentName() != null && !component.getComponentName().isEmpty()) {
            result.add("componentName", new JsonPrimitive(component.getComponentName()));
        }

        result.add("properties",
                context.serialize(component,
                        component.getClass()
                )
        );
        return result;
    }
}
