package TheCellBeyond;

import com.google.gson.*;
import components.Component;
import components.IsNotSerialized;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GameObject2DSerializer implements JsonSerializer<GameObject2D>, JsonDeserializer<GameObject2D> {
    @Override
    public JsonElement serialize(GameObject2D go2D, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        result.addProperty("name", go2D.name);
        result.addProperty("uuid", go2D.getUUID());

        if (go2D.getParentUUID() != null) {
            result.addProperty("parentUUID", go2D.getParentUUID());
        }

        if (go2D.getChildrenUUIDs() != null && !go2D.getChildrenUUIDs().isEmpty()) {
            JsonArray childrenArray = new JsonArray();
            for (String childUUID : go2D.getChildrenUUIDs()) {
                childrenArray.add(childUUID);
            }

            result.add("childrenUUIDS", childrenArray);
        }

        Transform localTransform = go2D.getTransForm();
        JsonElement localTransformJson = context.serialize(localTransform, Transform.class);
        result.add("localTransform", localTransformJson);

        JsonArray componentsArray = new JsonArray();
        for (Component component : go2D.getComponents()) {
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
    public GameObject2D deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String name = jsonObject.get("name").getAsString();
        JsonArray components = jsonObject.getAsJsonArray("components");

        GameObject2D go2D = new GameObject2D(name);

        if (jsonObject.has("uuid")) {
            go2D.setUUID(jsonObject.get("uuid").getAsString());
        }

        if (jsonObject.has("parentUUID")) {
            go2D.setParentUUID(jsonObject.get("parentUUID").getAsString());
        }

        if (jsonObject.has("childrenUUIDs")) {
            JsonArray childrenArray = jsonObject.getAsJsonArray("childrenUUIDs");
            List<String> childrenUUIDs = new ArrayList<>();

            for (JsonElement childUUID : childrenArray) {
                childrenUUIDs.add(childUUID.getAsString());
            }

            go2D.setChildrenUUIDs(childrenUUIDs);
        }

        if (jsonObject.has("localTransform")) {
            Transform localTransform = context.deserialize(jsonObject.get("localTransform"), Transform.class);
            go2D.setPosition(localTransform.position);
            go2D.setRotation(localTransform.rotation);
            go2D.setScale(localTransform.scale);
        }

        for (JsonElement e : components) {
            Component c = context.deserialize(e, Component.class);
            go2D.addComponent(c);
        }

        return go2D;
    }
}
