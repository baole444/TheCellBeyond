package TheCellBeyond;

import com.google.gson.*;
import components.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GameObjDeSerializer implements JsonDeserializer<GameObject> {
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
