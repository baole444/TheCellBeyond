package components;

import render.texture.Sprite;

public class Frame {
    public Sprite sprite;
    public float frameTime;

    public Frame() {}

    public Frame(Sprite sprite, float time) {
        this.sprite = sprite;
        this.frameTime = time;
    }

    public Frame copy() {
        return new Frame(this.sprite, this.frameTime);
    }

    public void copyFrom(Frame target) {
        if (target == null) return;

        this.sprite = target.sprite;
        this.frameTime = target.frameTime;
    }
}
