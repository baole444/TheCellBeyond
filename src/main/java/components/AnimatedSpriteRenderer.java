package components;

import TheCellBeyond.Window;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import render.texture.Sprite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AnimatedSpriteRenderer hold the hash map of its various animations where their names are the key set.<br>
 * The frame times is even between frames of an animation and is managed via FPS value
 * ({@code frame time = 1 / fps}.)<br>
 * The component facilitates animation playback over time and default animation that autoplay on start.
 */
public class AnimatedSpriteRenderer extends SpriteRenderer {
    public static final float DEFAULT_FPS = 5.0f;
    private final ConcurrentHashMap<String, Animation> animations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Float> animationFPS = new ConcurrentHashMap<>();
    private String defaultAnimation = null;

    private transient String currentAnimationName = null;
    private transient Animation currentAnimation = null;
    private transient boolean play = false;
    private transient boolean backward = false;

    /**
     * Get the default animation's name in this AnimatedSpriteRenderer.
     * @return name of the default animation or null if there is none
     */
    public String defaultAnimation() {
        return defaultAnimation;
    }

    /**
     * Set the default animation for this AnimatedSpriteRenderer.<br>
     * Set name to {@code null} will set default animation to null for this component.
     * @param name the name of an animation to be made default
     */
    public void setDefaultAnimation(String name) {
        if (name == null) {
            defaultAnimation = null;
            return;
        }

        if (name.isBlank() || !animations.containsKey(name)) return;

        defaultAnimation = name;
    }

    /**
     * Create a new animation for this AnimatedSpriteRenderer.
     * This method ensures the uniqueness of the new animation's name.
     */
    public void newAnimation() {
        String newName = "animation";

        if (animations.isEmpty()) {
            animations.put(newName, new Animation());
            animationFPS.put(newName, DEFAULT_FPS);
            return;
        }

        String uniqueName = newName;
        int i = animations.size();
        while (animations.containsKey(uniqueName)) {
            uniqueName = newName + "_" + i;
            i++;
        }

        animations.put(uniqueName, new Animation());
        animationFPS.put(uniqueName, DEFAULT_FPS);
    }

    /**
     * Update name of an animation.
     * The new name must be unique to this AnimatedSpriteRenderer.
     * @param oldName the current name of the animation
     * @param newName the new name for the animation
     */
    public void renameAnimation(String oldName, String newName) {
        if (animations.isEmpty()) return;
        if (oldName == null || newName == null || oldName.isBlank() || newName.isBlank()) return;
        if (!animations.containsKey(oldName) || animations.containsKey(newName)) return;

        Animation animation = animations.remove(oldName);
        float fps = animationFPS.remove(oldName);
        if (animation == null) return;

        animations.put(newName, animation);
        animationFPS.put(newName, fps);

        if (Objects.equals(defaultAnimation, oldName)) defaultAnimation = newName;
        if (Objects.equals(currentAnimationName, oldName)) currentAnimationName = newName;
    }

    /**
     * Duplicate an existing animation in this AnimatedSpriteRenderer.
     * This method ensures the uniqueness of the new animation's name.
     * @param name the name of the animation to duplicate from
     */
    public void duplicateAnimation(String name) {
        if (name == null) return;
        if (animations.isEmpty()) return;
        if (!animations.containsKey(name)) return;

        Animation animation = animations.get(name);
        float fps = animationFPS.get(name);
        if (animation == null) return;

        String newName = name + "_copy";
        int i = 1;
        while (animations.containsKey(newName)) {
            newName = name + "_copy" + i;
            i++;
        }

        animations.put(newName, new Animation(animation));
        animationFPS.put(newName, fps);
    }

    /**
     * Update the content of an animation in this AnimatedSpriteRenderer.
     * @param name the name of the animation to be updated
     * @param animation reference of an animation instance
     */
    public void updateAnimation(String name, Animation animation) {
        if (name == null || name.isBlank() || animation == null) return;
        animation.start();
        animation.setFPS(animationFPS.get(name));
        animations.computeIfPresent(name, (k, v) -> animation);

        if (Objects.equals(name, currentAnimationName)) {
            stop();
            currentAnimation = animation;
            if (currentAnimation.frames().isEmpty()) return;
            setSprite(currentAnimation.currentFrame().sprite);
        }
    }

    /**
     * Remove an animation from this AnimatedSpriteRenderer.
     * @param name the name of the animation to be removed
     */
    public void removeAnimation(String name) {
        if (name == null || !animations.containsKey(name)) return;
        if (Objects.equals(defaultAnimation, name)) defaultAnimation = null;

        Animation animation = animations.remove(name);
        if (animation == currentAnimation) {
            currentAnimationName = null;
            currentAnimation = null;
            updateSpriteFromCurrentAnimation();
        }
    }

    /**
     * Get the frame rate of an animation in this AnimatedSpriteRenderer.
     * @param name the name of the animation
     * @return FPS value of that animation, negative value means animation does not exist
     */
    public float getAnimationFPS(String name) {
        if (name == null || name.isBlank() || !animationFPS.containsKey(name)) return -1.0f;

        return animationFPS.get(name);
    }

    /**
     * Set the frame rate for an animation in this AnimatedSpriteRenderer.<br>
     * FPS cannot be lower than {@code 0.01}.
     * @param fps the frame rate value for the animation
     * @param name the name of the animation to set the fps
     */
    public void setFPS(float fps, String name) {
        if (name == null ||animations.isEmpty() || !animations.containsKey(name)) return;

        fps = Math.max(0.01f, fps);
        Animation animation = animations.get(name);
        if (animation == null) return;

        animation.setFPS(fps);
        animationFPS.put(name, fps);
    }

    /**
     * Check the playback status of this AnimatedSpriteRenderer.
     * @return true if the component is playing an animation
     */
    public boolean isPlaying() {
        return play;
    }

    /**
     * Check the reverse playback status of this AnimatedSpriteRenderer.
     * @return true if the component is playing an animation backward
     */
    public boolean isBackward() {
        return backward;
    }

    /**
     * Play an animation in this AnimatedSpriteRenderer from the first frame forward.
     * This will set the specified animation as the current animation with reverse status set to {@code false}.
     * @param name the name of the animation to play
     * @see #playBackward(String name) play an animation in reverse
     */
    public void play(String name) {
        if (name == null || !animations.containsKey(name)) return;
        Animation animation = animations.get(name);
        if (animation == null) return;

        if (animation != currentAnimation) animation.reset();
        currentAnimationName = name;
        currentAnimation = animation;
        backward = false;
        play = true;
        updateSpriteFromCurrentAnimation();
    }

    /**
     * Play an animation in this AnimatedSpriteRenderer from the last frame backward.
     * This will set the specified animation as the current animation with reverse status set to {@code true}.
     * @param name the name of the animation to play
     * @see #play(String name) play an animation
     */
    public void playBackward(String name) {
        if (name == null || !animations.containsKey(name)) return;
        Animation animation = animations.get(name);
        if (animation == null) return;

        if (animation != currentAnimation) animation.resetBackward();
        currentAnimationName = name;
        currentAnimation = animation;
        backward = true;
        play = true;
        updateSpriteFromCurrentAnimation();
    }

    /**
     * Pause playback of the current animation in this AnimatedSpriteRenderer.<br>
     * This does not reset animation frame or reverse playback status.
     * @see #stop() stop the playback of the current animation
     */
    public void pause() {
        play = false;
    }

    /**
     * Continue playback of the current animation in this AnimatedSpriteRenderer.
     * This does not reset animation frame or reverse playback status.
     */
    public void resume() {
        if (currentAnimation != null) play = true;
    }

    /**
     * Stop the playback of the current animation in this AnimatedSpriteRenderer.
     * This will reset the animation frame and reverse playback status.
     * @see #pause() pause the playback of the current animation
     */
    public void stop() {
        play = false;
        backward = false;
        if (currentAnimation != null) {
            currentAnimation.reset();
            updateSpriteFromCurrentAnimation();
        }
    }

    /**
     * Check the loop status of an animation in this AnimatedSpriteRenderer.
     * @param name the name of the animation to check
     * @return true if the animation's loop flag is {@code true}
     */
    public boolean isAnimationLoop(String name) {
        if (name == null || !animations.containsKey(name)) return false;

        Animation animation = animations.get(name);
        if (animation == null) return false;

        return animation.loop;
    }

    /**
     * Set the loop status of an animation in this AnimatedSpriteRenderer.
     * @param loop true to enable looping, false to disable it
     * @param name the name of the animation to update
     */
    public void setAnimationLoop(boolean loop, String name) {
        if (name == null || !animations.containsKey(name)) return;

        Animation animation = animations.get(name);
        if (animation != null) animation.loop = loop;
    }

    /**
     * Get the name of the current animation.
     * @return the name of the animation, or null if there is none
     */
    public String currentAnimationName() {
        return currentAnimationName;
    }

    /**
     * Set the current animation for playback.
     * @param name the name of the animation to be made current.
     */
    public void setCurrentAnimation(String name) {
        if (name == null) {
            stop();
            currentAnimation = null;
            currentAnimationName = null;
            return;
        }

        if (Objects.equals(name, currentAnimationName)) return;

        stop();
        currentAnimationName = name;
        currentAnimation = animations.get(name);
        updateSpriteFromCurrentAnimation();
    }

    /**
     * Get the instance of the current animation in this AnimatedSpriteRenderer.
     * @return reference to the instance of the current animation, or null if there is none
     */
    public Animation currentAnimation() {
        return currentAnimation;
    }

    /**
     * Get the hash map of this AnimationSpriteRenderer's animations with their name as key set.
     * @return a new hash map instance of the animations hash map
     */
    public HashMap<String, Animation> animations() {
        return new HashMap<>(animations);
    }

    /**
     * Adjust a frame's index (order) of an animation in this AnimatedSpriteRenderer.<br>
     * The index must be within the bound of the animation's frame list ({@code 0 -> last index}).
     * @param name the name of the animation to move the frame from
     * @param currentIndex the index of the frame that need to be moved
     * @param targetIndex the target index where that frame will be moved to
     * @return true if frames reordered successfully
     */
    public boolean moveFrame(String name, int currentIndex, int targetIndex) {
        if (name == null || !animations.containsKey(name)) return false;
        if (Objects.equals(name, currentAnimationName)) stop();

        Animation animation = animations.get(name);
        if (animation == null) return false;

        int frameCount = animation.numberOfFrames();
        if (currentIndex < 0 || currentIndex >= frameCount || targetIndex < 0 || targetIndex >= frameCount || currentIndex == targetIndex) return false;
        Frame currentFrame = animation.getFrameAt(currentIndex);
        if (currentFrame == null) return false;

        int animationCurrentFrameIndex = animation.currentFrameIndex();
        animation.removeFrame(currentIndex);
        animation.addFrameAt(currentFrame.sprite, currentFrame.frameTime, targetIndex);

        if (animationCurrentFrameIndex == currentIndex) animation.setCurrentFrameIndex(targetIndex);
        if (Objects.equals(name, currentAnimationName) && !play) updateSpriteFromCurrentAnimation();
        return true;
    }

    /**
     * Move a frame of an animation in this AnimatedSpriteRenderer to the left of that frame's index.
     * @param name the name of the animation to move the frame from
     * @param index the index of the frame that need to be moved
     * @return true if frames reordered successfully
     * @see #moveFrame(String name, int currentIndex, int targetIndex) Reorder a frame in an animation
     */
    public boolean moveFrameLeft(String name, int index) {
        if (index <= 0) return false;
        return moveFrame(name, index, index - 1);
    }

    /**
     * Move a frame of an animation in this AnimatedSpriteRenderer to the right of that frame's index.
     * @param name the name of the animation to move the frame from
     * @param index the index of the frame that need to be moved
     * @return true if frames reordered successfully
     */
    public boolean moveFrameRight(String name, int index) {
        if (name == null || !animations.containsKey(name)) return false;

        Animation animation = animations.get(name);
        if (animation == null) return false;

        int frameCount = animation.numberOfFrames();
        if (index >= frameCount - 1) return false;

        return moveFrame(name, index, index + 1);
    }

    /**
     * Remove a frame from an animation in this AnimatedSpriteRenderer.
     * @param name the name of the animation to remove the frame from
     * @param index the index of the frame that need to be removed
     */
    public void removeFrame(String name, int index) {
        if (name == null || !animations.containsKey(name)) return;
        if (Objects.equals(name, currentAnimationName)) stop();

        Animation animation = animations.get(name);
        if (animation == null) return;

        int frameCount = animation.numberOfFrames();
        if (index < 0 || index >= frameCount) return;

        animation.removeFrame(index);

        if (Objects.equals(name, currentAnimationName) && !play) updateSpriteFromCurrentAnimation();
    }

    private void updateSpriteFromCurrentAnimation() {
        if (currentAnimation == null) {
            setSprite(null);
            return;
        }

        Frame f = currentAnimation.currentFrame();
        Sprite s = f == null ? null : f.sprite;
        setSprite(s);
    }

    @Override
    protected void additionalStartLogic() {
        if (currentAnimationName == null && defaultAnimation != null) {
            currentAnimationName = defaultAnimation;
            currentAnimation = animations.get(defaultAnimation);
        }

        for (Map.Entry<String, Animation> entry : animations.entrySet()) {
            entry.getValue().start();
        }

        if (Window.get().isRuntimeMode()) play(defaultAnimation);
    }

    @Override
    protected void additionalUpdateLogic(float dt) {
        if (currentAnimation == null) return;

        if (play && backward) {
            currentAnimation.updateBackward(dt);
        } else if (play) currentAnimation.update(dt);

        updateSpriteFromCurrentAnimation();
    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openAnimatedSprite = ImGui.collapsingHeader("AnimatedSpriteRenderer##Animated_Sprite_Renderer_Properties_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (!openAnimatedSprite) {
            return;
        }
        ImGui.indent();
        List<String> animationList = animations.keySet().stream().toList();
        String selectedAni = currentAnimationName;
        ImGui.text("Animation:");
        if (ImGui.beginCombo("##Select_Current_AnimatedSprite_Animation_Combo_" + getUUID(), selectedAni == null ? "Select an animation..." : currentAnimationName)) {
            for (String name : animationList) {
                String label = name + "##Select_" + name + "_AnimatedSprite_Selectable_" + getUUID();
                if (ImGui.selectable(label, Objects.equals(name, selectedAni))) setCurrentAnimation(name);
            }
            if (!animationList.isEmpty()) ImGui.separator();
            if (ImGui.selectable("New animation...##New_Animation_AnimatedSprite_Selectable_" + getUUID(), false)) newAnimation();

            ImGui.endCombo();
        }

        ImGui.spacing();
        ImBoolean flipHState = new ImBoolean(isFlipHorizontally());
        ImBoolean flipVState = new ImBoolean(isFlipVertically());
        String compositeID = "Flip axis##Flip_Axis_AnimatedSprite_Header" + getUUID();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openFlip = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (openFlip) {
            if (ImGui.checkbox("Horizontal##" + getUUID(), flipHState)) flipHorizontally(flipHState.get());
            if (ImGui.checkbox("Vertical##" + getUUID(), flipVState)) flipVertically(flipVState.get());
        }
        ImGui.unindent();
    }
}
