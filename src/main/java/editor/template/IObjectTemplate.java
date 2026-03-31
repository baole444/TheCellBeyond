package editor.template;

import TheCellBeyond.GameObject;

/**
 * The Template interface provide common method {@link #editorUI(GameObject)}, of which can be implemented to take specific game object type.
 * This is mainly used for extracting editor UI's rendering code out of the object class itself.
 * <p>
 * Implementation of this interface is intended to be singleton, with private constructor and static instance, for example:
 * {@snippet lang = java:
 * class CustomTemplate implements IObjectTemplate<CustomObject> {
 *     private static CustomTemplate instance = new CustomTemplate();
 *     private CustomTemplate() {}
 *
 *     @Override
 *     public void editorUI(CustomObject object) {
 *         // The render logic go here
 *     }
 *
 *     // static method for access
 *     static void render(CustomObject customObject) {
 *         instance.editorUI(customComponent);
 *     }
 * }
 *}
 * @param <T> GameObject type or its subclasses.
 */
interface IObjectTemplate<T extends GameObject> {
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     * @param object the context object
     */
    void editorUI(T object);
}
