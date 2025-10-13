package components;

import TheCellBeyond.Window;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import render.texture.Sprite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AnimatedSpriteRenderer extends SpriteRenderer {
    public static final float DEFAULT_FPS = 5.0f;
    private final ConcurrentHashMap<String, Animation> animations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Float> animationFPS = new ConcurrentHashMap<>();
    private String defaultAnimation = null;

    private transient String currentAnimationName = null;
    private transient Animation currentAnimation = null;
    private transient boolean play = false;
    private transient boolean backward = false;

    public String defaultAnimation() {
        return defaultAnimation;
    }

    public void setDefaultAnimation(String name) {
        if (name == null) {
            defaultAnimation = null;
            return;
        }

        if (name.isBlank() || !animations.containsKey(name)) return;

        defaultAnimation = name;
    }

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

    public float getAnimationFPS(String name) {
        if (name == null || name.isBlank() || !animationFPS.containsKey(name)) return -1.0f;

        return animationFPS.get(name);
    }

    public void setFPS(float fps, String name) {
        if (name == null ||animations.isEmpty() || !animations.containsKey(name)) return;

        fps = Math.max(0.01f, fps);
        Animation animation = animations.get(name);
        if (animation == null) return;

        animation.setFPS(fps);
        animationFPS.put(name, fps);
    }

    public boolean isPlaying() {
        return play;
    }

    public boolean isBackward() {
        return backward;
    }

    public boolean isPlayingBackward() {
        return play && backward;
    }

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

    public void pause() {
        play = false;
    }

    public void resume() {
        if (currentAnimation != null) play = true;
    }

    public void stop() {
        play = false;
        backward = false;
        if (currentAnimation != null) {
            currentAnimation.reset();
            updateSpriteFromCurrentAnimation();
        }
    }

    public boolean isAnimationLoop(String name) {
        if (name == null || !animations.containsKey(name)) return false;

        Animation animation = animations.get(name);
        if (animation == null) return false;

        return animation.loop;
    }

    public void setAnimationLoop(boolean loop, String name) {
        if (name == null || !animations.containsKey(name)) return;

        Animation animation = animations.get(name);
        if (animation != null) animation.loop = loop;
    }

    public String currentAnimationName() {
        return currentAnimationName;
    }

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

    public Animation currentAnimation() {
        return currentAnimation;
    }

    public HashMap<String, Animation> animations() {
        return new HashMap<>(animations);
    }

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

    public boolean moveFrameLeft(String name, int index) {
        if (index <= 0) return false;
        return moveFrame(name, index, index - 1);
    }

    public boolean moveFrameRight(String name, int index) {
        if (name == null || !animations.containsKey(name)) return false;

        Animation animation = animations.get(name);
        if (animation == null) return false;

        int frameCount = animation.numberOfFrames();
        if (index >= frameCount - 1) return false;

        return moveFrame(name, index, index + 1);
    }

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
        List<String> animationList = animations.keySet().stream().toList();
        String selectedAni = currentAnimationName;
        if (ImGui.beginCombo("Animation##Select_Current_Animation", selectedAni == null ? "Select an animation..." : currentAnimationName)) {
            for (String name : animationList) {
                if (ImGui.selectable(name, Objects.equals(name, selectedAni))) setCurrentAnimation(name);
            }

            ImGui.endCombo();
        }

        ImGui.indent();
        ImBoolean flipHState = new ImBoolean(isFlipHorizontally());
        ImBoolean flipVState = new ImBoolean(isFlipVertically());
        String compositeID = "Flip axis##" + getUUID();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (open) {
            if (ImGui.checkbox("Horizontal##" + getUUID(), flipHState)) flipHorizontally(flipHState.get());
            if (ImGui.checkbox("Vertical##" + getUUID(), flipVState)) flipVertically(flipVState.get());
        }
        ImGui.unindent();
    }
}
