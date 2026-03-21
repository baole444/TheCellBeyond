package render;

import render.commands.RenderCommand;

public interface Renderable {
    /**
     * Build rendering command to manipulate the rendering tree, this could be a single command or a chain of commands.
     * <p>
     * In case of chaining commands, the returning command should be
     * the chain's head with all subsequence commands attached using {@link RenderCommand#next}.
     * @return the command for rendering server to consume
     */
    RenderCommand buildRenderCommand();

    /**
     * Check if the rendering server need to update the rendering data or not.
     * <p>
     * Implementers are responsible for handling the internal state of their flag.
     * @return true if dirty
     */
    boolean renderDirty();

    /**
     * Set the render dirty flag state.
     * <p>
     * Implementer are responsible for handling the internal state of their flag.
     * @param dirty the flag state
     */
    void renderDirty(boolean dirty);
    int renderZIndex();

    /**
     * Should the rendering node for the implementation of this interface not cloneable or repeatable.
     * This prevents the node associated with this interface's implemented instances from cloning due to repeating.
     * <p>
     * By default, this will return false, meaning the implementation is cloneable or repeatable.
     * @return true if cloning is not desired
     */
    default boolean nonRepeatable() {
        return false;
    }


}
