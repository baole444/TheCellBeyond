package components;

import java.util.ArrayList;
import java.util.List;

public class AnimationState {
    public String title;
    public List<Frame> animateFrame = new ArrayList<>();

    private static Sprite defaultSprite = new Sprite();
    private transient float timeTrack = 0.0f;
    private transient int instSprite = 0;
    public boolean isLoop = false;

    public void addFrame(Sprite sprite, float frameTime) {
        animateFrame.add(new Frame(sprite, frameTime));
    }

    public void setLoop(boolean isLoop) {
        this.isLoop = isLoop;
    }

    public void update(float dt) {
        if (instSprite < animateFrame.size()) {
            timeTrack -= dt;
            if (timeTrack <= 0) {
                if (instSprite != animateFrame.size() -1 || isLoop) {
                    instSprite = (instSprite + 1) % animateFrame.size();
                }

                timeTrack = animateFrame.get(instSprite).frameTime;
            }
        }
    }

    public Sprite loadInstSprite() {
        if (instSprite < animateFrame.size()) {
            return animateFrame.get(instSprite).sprite;
        }

        return defaultSprite;
    }
}
