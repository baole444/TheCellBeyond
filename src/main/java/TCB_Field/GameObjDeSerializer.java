package TCB_Field;

import com.google.gson.*;


import java.lang.reflect.Type;

public class GameObjDeSerializer implements JsonDeserializer<GameObject> {

    @Override
    public GameObject deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String name = jsonObject.get("name").getAsString();
        JsonArray components = jsonObject.getAsJsonArray("components");
        Transform transform = context.deserialize(jsonObject.get("transform"), Transform.class);
        int zIndex = context.deserialize(jsonObject.get("zIndex"), int.class);

        GameObject Obj = new GameObject(name, transform, zIndex);
        for (JsonElement e : components) {
            Component c = context.deserialize(e, Component.class);
            Obj.addComponent(c);
        }
        return Obj;
    }
}
