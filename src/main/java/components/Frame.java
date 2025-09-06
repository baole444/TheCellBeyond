package components;

import render.texture.Sprite;

public class Frame {
    public Sprite sprite;
    public float frameTime;
    public float endTime;

    public Frame(Sprite sprite, float second) {
        this.sprite = sprite;
        frameTime = second;
    }
}
