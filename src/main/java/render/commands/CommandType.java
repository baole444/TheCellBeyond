package render.commands;

/**
 * CommandType enums are used to identity the type of render command.
 */
public enum CommandType {
    /**
     * Rect command type carrying texture coordinates and modulate data for the texture.
     */
    Rect,
    /**
     * To be implemented.
     */
    NinePatch,
    /**
     * To be implemented.
     */
    Polygon,
    /**
     * To be implemented.
     */
    Primitive,
    /**
     * Mesh command type carrying mesh coordinate for textures.
     */
    Mesh,
    /**
     * Text command type carrying font metadata and the text.
     */
    Text,
    /**
     * Transform command type carrying position, rotation and scale.
     */
    Transform
}
