package editor.template;

import components.Component;

/**
 * The Template interface provide common method {@link #editorUI(Component)}, of which can be implemented to take specific component type.
 * This is mainly used for extracting editor UI's rendering code out of the component class itself.
 * <p>
 * Implementation of this interface is intended to be singleton, with private constructor and static instance, for example:
 * {@snippet lang= java:
 * class CustomTemplate implements ComponentTemplate<CustomComponent> {
 *     private static CustomTemplate instance = new CustomTemplate();
 *     private CustomTemplate() {}
 *
 *     @Override
 *     public void editorUI(CustomComponent component) {
 *         // The render logic go here
 *     }
 *
 *     // static method for access
 *     static void render(CustomComponent customComponent) {
 *         instance.editorUI(customComponent);
 *     }
 * }
 * }
 * @param <T> Component type or its subclasses.
 */
interface ComponentTemplate<T extends Component> {
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     * @param component the context component
     */
    void editorUI(T component);
}
