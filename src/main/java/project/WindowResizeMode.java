package project;

/**
 * Window resize mode allow the engine to handle rendering frame and field of view on resizing the game's window.
 */
public enum WindowResizeMode {
    /**
     * On resizing, scale the intended view, the entire camera area stay visible.
     * <p>
     * When aspect ratio is unlocked, the view is stretched to fill the surplus space.
     */
    Scale,
    /**
     * On resizing, change the visible area at a fixed scale. Growing the window reveal more of the world,
     * while shrinking it reveal less. Size of objects drawn on screen stay constant.
     * <p>
     * This mode does not affected by aspect ratio.
     */
    Expand
}
