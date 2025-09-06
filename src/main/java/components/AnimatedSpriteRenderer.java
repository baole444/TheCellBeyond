package components;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AnimatedSpriteRenderer extends SpriteRenderer {
    private final Map<String, Animation> animations = new HashMap<>();
    private String defaultAnimation = null;
    private transient Animation currentAnimation = null;
    public transient boolean play = false;

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
