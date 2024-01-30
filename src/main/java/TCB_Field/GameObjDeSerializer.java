package TCB_Field;

import com.google.gson.*;
import components.Component;


import java.lang.reflect.Type;

public class GameObjDeSerializer implements JsonDeserializer<GameObject> {

    @Override
    public GameObject deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String name = jsonObject.get("name").getAsString();
        JsonArray components = jsonObject.getAsJsonArray("components");

        GameObject Obj = new GameObject(name);
        for (JsonElement e : components) {
            Component c = context.deserialize(e, Component.class);
            Obj.addComponent(c);
        }
        Obj.transform = Obj.getComponent(Transform.class);

        return Obj;
    }
}
