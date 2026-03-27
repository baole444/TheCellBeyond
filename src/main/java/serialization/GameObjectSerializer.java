package serialization;

import TheCellBeyond.GameObject;
import com.google.gson.*;
import components.Component;
import components.NotSerializeComponent;
import scripting.ScriptLoader;
import signal.Signal;
import utility.log.EngineLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Data serializer and deserializer for {@link GameObject} and its subclasses.
 */
public class GameObjectSerializer implements JsonSerializer<GameObject>, JsonDeserializer<GameObject> {
    private static final EngineLog Logger = new EngineLog(GameObjectSerializer.class);
    private final String Type = "type";
    private final String Property = "properties";
    private final String Component = "components";

    /**
     * Create the game object serialization module.
     */
    public GameObjectSerializer() {}

    @Override
    public JsonElement serialize(GameObject gameObject, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty(Type, gameObject.getClass().getCanonicalName());
        JsonObject properties = new JsonObject();
        serializeField(gameObject, gameObject.getClass(), properties, context);
        result.add(Property, properties);
        return result;
    }

    private void serializeField(GameObject go, Class<?> goClass, JsonObject properties, JsonSerializationContext context) {
        if (goClass == null || goClass == Object.class) return;
        serializeField(go, goClass.getSuperclass(), properties, context);
        Field[] fields = goClass.getDeclaredFields();
        for (Field field : fields) {
            int modifier = field.getModifiers();
            if (Modifier.isTransient(modifier) || Modifier.isStatic(modifier)) continue;
            if (Signal.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object val = field.get(go);
                if (!field.getName().equals(Component) || !(val instanceof Collection<?> collection)) {
                    if (val != null) properties.add(field.getName(), context.serialize(val));
                    continue;
                }
                JsonArray components = new JsonArray();
                for (Object c : collection) {
                    if (c instanceof Component && !(c instanceof NotSerializeComponent)) {
                        components.add(context.serialize(c, Component.class));
                    }
                }
                properties.add(Component, components);
            } catch (IllegalAccessException e) {
                System.err.println("Failed to serialize field: " + field.getName());
            }
        }
    }

    @Override
    public GameObject deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String className = jsonObject.get(Type).getAsString();
        JsonObject properties = jsonObject.getAsJsonObject(Property);
        try {
            Class<?> goClass = Class.forName(className, true, ScriptLoader.classLoader());
            String name;
            if (properties.has("name")) name = properties.get("name").getAsString();
            else name = "Unnamed object";
            GameObject go = createInstance(goClass, name);
            deserializeField(go, goClass, properties, context);
            return go;
        } catch (ClassNotFoundException e) {
            Logger.error(String.format("Skipping missing game object class %s", className));
            return null;
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
            if (Signal.class.isAssignableFrom(field.getType())) continue;
            String fieldName = field.getName();
            if (!properties.has(fieldName)) continue;
            try {
                field.setAccessible(true);
                JsonElement element = properties.get(fieldName);
                if (element.isJsonNull()) continue;
                if (!fieldName.equals(Component) || !element.isJsonArray()) {
                    Object val = context.deserialize(element, field.getGenericType());
                    field.set(go, val);
                    continue;
                }
                JsonArray components = element.getAsJsonArray();
                for (JsonElement c : components) {
                    Component component = context.deserialize(c, Component.class);
                    if (component != null) go.addComponent(component);
                }
            } catch (IllegalAccessException e) {
                System.err.println("Failed to deserialize field: " + fieldName);
                System.err.println(e.getMessage());
            }
        }
    }
}
