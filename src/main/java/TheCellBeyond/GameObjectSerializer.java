package TheCellBeyond;

import com.google.gson.*;
import components.Component;
import components.NotSerializeComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;

public class GameObjectSerializer implements JsonSerializer<GameObject>, JsonDeserializer<GameObject> {
    private final String TYPE = "type";
    private final String PROPERTY = "properties";
    private final String COMPONENT = "components";

    @Override
    public JsonElement serialize(GameObject gameObject, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        result.addProperty(TYPE, gameObject.getClass().getCanonicalName());

        JsonObject properties = new JsonObject();
        serializeField(gameObject, gameObject.getClass(), properties, context);

        result.add(PROPERTY, properties);
        return result;
    }

    private void serializeField(GameObject go, Class<?> goClass, JsonObject properties, JsonSerializationContext context) {
        if (goClass == null || goClass == Object.class) return;

        serializeField(go, goClass.getSuperclass(), properties, context);

        Field[] fields = goClass.getDeclaredFields();
        for (Field field : fields) {
            int modifier = field.getModifiers();

            if (Modifier.isTransient(modifier) || Modifier.isStatic(modifier)) continue;

            try {
                field.setAccessible(true);
                Object val = field.get(go);

                if (field.getName().equals(COMPONENT) && val instanceof Collection) {
                    JsonArray components = new JsonArray();
                    for (Object c : (Collection<?>) val) {
                        if (c instanceof Component && !(c instanceof NotSerializeComponent)) {
                            components.add(context.serialize(c, Component.class));
                        }
                    }
                    properties.add(COMPONENT, components);
                } else if (val != null) {
                    properties.add(field.getName(), context.serialize(val));
                }
            } catch (IllegalAccessException e) {
                System.err.println("Failed to serialize field: " + field.getName());
            }
        }
    }

    @Override
    public GameObject deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String className = jsonObject.get(TYPE).getAsString();
        JsonObject properties = jsonObject.getAsJsonObject(PROPERTY);

        try {
            Class<?> goClass = Class.forName(className);
            String name;

            if (properties.has("name")) {
                name = properties.get("name").getAsString();
            } else {
                name = "Unnamed object";
            }

            GameObject go = createInstance(goClass, name);

            deserializeField(go, goClass, properties, context);

            return go;
        } catch (Exception e) {
            throw new JsonParseException("Failed to deserialize GameObject", e);
        }
    }

    private GameObject createInstance(Class<?> goClass, String name) throws Exception {
        try {
            return (GameObject) goClass.getConstructor(String.class).newInstance(name);
        } catch (NoSuchMethodException e) {
            GameObject go = (GameObject) goClass.getConstructor().newInstance();
            go.name(name);
            return go;
        }
    }

    private void deserializeField(GameObject go, Class<?> goClass, JsonObject properties, JsonDeserializationContext context) {
        if (goClass == null || goClass == Object.class) return;

        deserializeField(go, goClass.getSuperclass(), properties, context);

        Field[] fields = goClass.getDeclaredFields();
        for (Field field : fields) {
            int modifier = field.getModifiers();

            if (Modifier.isTransient(modifier) || Modifier.isStatic(modifier)) continue;

            String fieldName = field.getName();
            if (!properties.has(fieldName)) continue;

            try {
                field.setAccessible(true);
                JsonElement element = properties.get(fieldName);

                if (!element.isJsonNull()) {
                    if (fieldName.equals(COMPONENT) && element.isJsonArray()) {
                        JsonArray components = element.getAsJsonArray();
                        for (JsonElement c : components) {
                            Component component = context.deserialize(c, Component.class);
                            go.addComponent(component);
                        }
                    } else {
                        Object val = context.deserialize(element, field.getGenericType());
                        field.set(go, val);
                    }
                }
            } catch (IllegalAccessException e) {
                System.err.println("Failed to deserialize field: " + fieldName);
            }
        }
    }
}
