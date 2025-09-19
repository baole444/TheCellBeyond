package components;

import editor.SpriteFrameEditor;
import imgui.ImGui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AnimatedSpriteRenderer extends SpriteRenderer {
    private final Map<String, Animation> animations = new HashMap<>();
    private String defaultAnimation = null;
    private String currentAnimationName = null;
    private transient Animation currentAnimation = null;
    private transient boolean play = false;

    public void setAnimation(String name, Animation animation) {
        if (animation == null) return;
        animations.put(name, animation);
    }

    public void removeAnimation(String name) {
        if (name == null || !animations.containsKey(name)) return;
        if (Objects.equals(defaultAnimation, name)) defaultAnimation = null;

        Animation animation = animations.remove(name);
        if (animation == currentAnimation) currentAnimation = null;
    }

    public void setFPS(float fps, String name) {
        if (animations.isEmpty() || !animations.containsKey(name)) return;
        fps = Math.max(0.01f, fps);
        Animation animation = animations.get(name);
        if (animation == null) return;
        animation.setFPS(fps);
    }

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

    public void pause() {
        play = false;
    }

    public void resume() {
        if (currentAnimation != null) play = true;
    }

    public String currentAnimationName() {
        return currentAnimationName;
    }

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

    @Override
    public void imgui() {
        if (ImGui.isItemClicked()) SpriteFrameEditor.selectedAnimatedSpriteRenderer = this;
        super.imgui();
    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.textWrapped("Extra control later");
    }
}
