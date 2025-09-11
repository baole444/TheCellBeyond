package components;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AnimatedSpriteRenderer extends SpriteRenderer {
    private final Map<String, Animation> animations = new HashMap<>();
    private String defaultAnimation = null;
    private String currentAnimationName = null;
    private transient Animation currentAnimation = null;
    private transient boolean play = false;
    
    /**
     * Adds or replaces an animation with the given name.
     *
     * <p>If an animation with the same name already exists, it will be overwritten.
     * Passing {@code null} as the animation does nothing.</p>
     *
     * <p>Example:</p>
     * <pre>
     * AnimatedSpriteRenderer renderer = new AnimatedSpriteRenderer();
     * renderer.setAnimation("walk", walkAnimation);
     * renderer.play("walk");
     * </pre>
     *
     * @param name The unique name of the animation.
     * @param animation The {@link Animation} to associate with this name.
     */
    public void setAnimation(String name, Animation animation) {
        if (animation == null) return;
        animations.put(name, animation);
    }

    /**
     * Removes the animation with the given name.
     *
     * <p>If the animation does not exist, or if {@code name} is {@code null}, nothing happens.
     * If the removed animation is the default or the currently playing animation,
     * those references are cleared.</p>
     *
     * @param name The name of the animation to remove.
     */
    public void removeAnimation(String name) {
        if (name == null || !animations.containsKey(name)) return;
        if (Objects.equals(defaultAnimation, name)) defaultAnimation = null;

        Animation animation = animations.remove(name);
        if (animation == currentAnimation) currentAnimation = null;
    }

        /**
     * Sets the frame rate (FPS) of the animation with the given name.
     *
     * <p>If the animation does not exist, this method does nothing.
     * The FPS value is clamped to a minimum of {@code 0.01}.</p>
     *
     * <p>Example:</p>
     * <pre>
     * renderer.setFPS(24.0f, "walk"); // Walk animation at 24 FPS
     * </pre>
     *
     * @param fps The frames per second to set (minimum {@code 0.01}).
     * @param name The name of the animation whose FPS should be updated.
     * @see Animation#setFPS(float)
     */
    public void setFPS(float fps, String name) {
        if (animations.isEmpty() || !animations.containsKey(name)) return;
        fps = Math.max(0.01f, fps);
        Animation animation = animations.get(name);
        if (animation == null) return;
        animation.setFPS(fps);
    }

     /**
     * Starts playing the animation with the given name.
     *
     * <p>If the animation is different from the currently playing one,
     * it will be reset to the first frame before playing.</p>
     *
     * <p>If {@code name} is {@code null} or no animation is found under this name,
     * this method does nothing.</p>
     *
     * <p>Example:</p>
     * <pre>
     * renderer.play("walk");
     * </pre>
     *
     * @param name The name of the animation to play.
     * @see #pause()
     * @see #resume()
     * @see #stop()
     */
    public void play(String name) {
        if (name == null || !animations.containsKey(name)) return;
        Animation animation = animations.get(name);
        if (animation == null) return;

        if (animation != currentAnimation) animation.reset();
        currentAnimationName = name;
        currentAnimation = animation;
        play = true;
        setSprite(currentAnimation.currentFrame().sprite);
    }

    /**
     * Pauses the currently playing animation.
     *
     * <p>This does not reset the animation. Calling {@link #resume()} will
     * continue playing from the paused frame.</p>
     *
     * @see #resume()
     */
    public void pause() {
        play = false;
    }

    /**
     * Resumes the currently paused animation.
     *
     * <p>If no animation is currently assigned, this method does nothing.</p>
     *
     * @see #pause()
     */
    public void resume() {
        if (currentAnimation != null) play = true;
    }

    /**
     * Returns the name of the currently playing animation.
     *
     * <p>If no animation is playing, this may return {@code null}.</p>
     *
     * @return The name of the current animation, or {@code null} if none is playing.
     */
    public String currentAnimationName() {
        return currentAnimationName;
    }

    /**
     * Stops the currently playing animation.
     *
     * <p>This resets the animation back to its first frame and updates the sprite.
     * After calling {@code stop()}, the animation is not considered playing.</p>
     *
     * <p>Example:</p>
     * <pre>
     * renderer.play("attack");
     * renderer.stop(); // Reset attack animation back to frame 0
     * </pre>
     *
     * @see #play(String)
     */
    public void stop() {
        play = false;
        if (currentAnimation != null) {
            currentAnimation.reset();
            setSprite(currentAnimation.currentFrame().sprite);
        }
    }

    @Override
    protected void additionalStartLogic() {
        for (Map.Entry<String, Animation> entry : animations.entrySet()) {
            entry.getValue().start();
        }
    }

    @Override
    protected void additionalUpdateLogic(float dt) {
        if (!play || currentAnimation == null) return;
        currentAnimation.update(dt);
        setSprite(currentAnimation.currentFrame().sprite);
    }
}
