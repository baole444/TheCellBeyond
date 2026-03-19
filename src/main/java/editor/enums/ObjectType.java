package editor.enums;

import TheCellBeyond.Camera2D;
import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.TileMap;
import physic2d.CharacterBody2D;
import physic2d.KinematicBody2D;
import physic2d.RigidBody2D;
import physic2d.StaticBody2D;

// TODO: Need to come up with better solution in the future
//  to be able to register potential user's custom object type.
public enum ObjectType {
    GameObject("GameObject", "A plain game object, the base of other object types. " +
            "It support data serialization, object hierarchy tree and mounting components."),
    GameObject2D("GameObject2D", "A 2D game object, the base of all 2D-related object types. " +
            "It exists in the logic spatial world and can supports transformation."),
    StaticBody2D("StaticBody2D", "A 2D static physic object. It exists in both logic spatial and physic world. The object is immovable."),
    RigidBody2D("RigidBody2D", "A 2D rigid physic object with full physic simulation. " +
            "It exists in both logic spatial and physic world. The transformation of the object is the result of physic simulation via applied forces."),
    KinematicBody2D("KinematicBody2D", "A 2D physic object suitable for scripted movement, control via velocity. It is not affected by physic at all."),
    CharacterBody2D("CharacterBody2D", "A specialized 2D physic object that is not affected by physics at all, but it affects other physic objects in its path. " +
            "It is used to provide API to move objects in a specific way, as is often the case with user-controlled characters or logic driven NPCs."),
    TileMap("TileMap", "A 2D tile map object. Tile map can have static or kinematic physic body and physic collision defined by tiles in the map's tile set."),
    Camera2D("Camera2D", "A 2D camera object, allow update its targeted viewport's transform");

    /**
     * Display label for menus and dialogues.
     */
    public final String label;

    /**
     * Display description for menus and dialogues.
     */
    public final String description;

    ObjectType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * Get an instance for a type of game object.
     * <p>
     * The created object's name will be the {@link ObjectType###label}'s value.
     * @param type the type to create instance with
     * @return a new {@link GameObject} of the given type
     */
    public static GameObject getObjectFromType(ObjectType type) {
        return getObjectFromType(type, null);
    }

    /**
     * Get an instance for a type of game object with the given custom name.
     * <p>
     * If the custom name is invalid, this will default to the {@link ObjectType#label}'s value.
     * @param type the type to create instance with
     * @param customName the custom name to give to the object
     * @return a new {@link GameObject} of the given type and name
     */
    public static GameObject getObjectFromType(ObjectType type, String customName) {
        if (type == null) return null;
        if (TheCellBeyond.GameObject.invalidName(customName)) customName = type.label;
        return switch (type) {
            case GameObject2D -> new GameObject2D(customName);
            case GameObject -> new GameObject(customName);
            case StaticBody2D -> new StaticBody2D(customName);
            case RigidBody2D -> new RigidBody2D(customName);
            case KinematicBody2D -> new KinematicBody2D(customName);
            case CharacterBody2D -> new CharacterBody2D(customName);
            case TileMap -> new TileMap(customName);
            case Camera2D -> new Camera2D(customName);
        };
    }

    public static Class<? extends GameObject> getClassFromType(ObjectType type) {
        return switch (type) {
            case GameObject -> GameObject.class;
            case GameObject2D -> GameObject2D.class;
            case StaticBody2D -> StaticBody2D.class;
            case RigidBody2D -> RigidBody2D.class;
            case KinematicBody2D -> KinematicBody2D.class;
            case CharacterBody2D -> CharacterBody2D.class;
            case TileMap -> TileMap.class;
            case Camera2D -> Camera2D.class;
        };
    }
}
