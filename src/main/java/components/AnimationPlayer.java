package components;

import scripting.API;

/**
 * Placeholder for future AnimationPlayer
 */
@API
public class AnimationPlayer extends SpriteRenderer {
    /**
     * Create a new {@link AnimationPlayer} component.
     */
    public AnimationPlayer() {
        String name = AnimationPlayer.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link AnimationPlayer} component using the given name.
     * @param name the new name for the component
     */
    public AnimationPlayer(String name) {
        if (invalidName(name)) name = AnimationPlayer.class.getSimpleName();
        super(name);
    }
}
