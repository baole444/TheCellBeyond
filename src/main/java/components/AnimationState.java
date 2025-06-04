package components;

import utility.AssetsPool;

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

    public void reloadTexture() {
        for (Frame frame : animateFrame) {
            frame.sprite.setTex(AssetsPool.loadTexture(frame.sprite.getTexture().getFilePath()));
        }
    }

    public AnimationState copy() {
        AnimationState copy = new AnimationState();

        copy.title = this.title;
        copy.isLoop = this.isLoop;

        for (Frame frame : this.animateFrame) {
            copy.animateFrame.add(frame.copy());
        }

        return copy;
    }

    public void copyFrom(AnimationState target) {
        if (target == null) return;

        this.title = target.title;
        this.isLoop = target.isLoop;

        this.animateFrame.clear();
        for (Frame frame : target.animateFrame) {
            this.animateFrame.add(frame.copy());
        }

        this.timeTrack = 0.0f;
        this.instSprite = 0;
    }
}
