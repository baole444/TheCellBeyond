package TheCellBeyond;

import com.google.gson.*;
import components.Component;
import components.IsNotSerialized;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GameObjectSerializer implements JsonSerializer<GameObject>, JsonDeserializer<GameObject> {
    @Override
    public JsonElement serialize(GameObject gameObject, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        result.addProperty("name", gameObject.name);
        result.addProperty("uuid", gameObject.getUUID());

        if (gameObject.getParentUUID() != null) {
            result.addProperty("parentUUID", gameObject.getParentUUID());
        }

        if (gameObject.getChildrenUUIDs() != null && !gameObject.getChildrenUUIDs().isEmpty()) {
            JsonArray childrenArray = new JsonArray();
            for (String childUUID : gameObject.getChildrenUUIDs()) {
                childrenArray.add(childUUID);
            }

            result.add("childrenUUIDs", childrenArray);
        }

        JsonArray componentsArray = new JsonArray();
        for (Component component : gameObject.getComponents()) {
            if (!(component instanceof IsNotSerialized)) {
                JsonElement cJ = context.serialize(component, Component.class);
                if (cJ != null && !cJ.isJsonNull()) {
                    componentsArray.add(cJ);
                }
            }
        }

        result.add("components", componentsArray);

        return result;
    }

    @Override
    public GameObject deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String name = jsonObject.get("name").getAsString();
        JsonArray components = jsonObject.getAsJsonArray("components");

        GameObject go = new GameObject(name);

        if (jsonObject.has("uuid")) {
            go.setUUID(jsonObject.get("uuid").getAsString());
        }

        if (jsonObject.has("parentUUID")) {
            go.setParentUUID(jsonObject.get("parentUUID").getAsString());
        }

        if (jsonObject.has("childrenUUIDs")) {
            JsonArray childrenArray = jsonObject.getAsJsonArray("childrenUUIDs");
            List<String> childrenUUIDs = new ArrayList<>();

            for (JsonElement childUUID : childrenArray) {
                childrenUUIDs.add(childUUID.getAsString());
            }

            go.setChildrenUUIDs(childrenUUIDs);
        }

        for (JsonElement e : components) {
            Component c = context.deserialize(e, Component.class);
            go.addComponent(c);
        }

        go.transform = go.getComponent(Transform.class);

        return go;
    }
}
