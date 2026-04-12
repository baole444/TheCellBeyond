package components;

import render.texture.Sprite;

/**
 * Frame contain the sprite to render and its duration in second.
 * The end of a frame is determined by the sequence it is in.
 */
public class Frame {
    /**
     * The sprite to render during this frame.
     */
    public Sprite sprite;
    /**
     * The time that this frame last for, in seconds.
     */
    public float frameTime;
    /**
     * The time that this frame end. This is calculated when the frame is inside a sequence (an animation.)
     */
    public float endTime;

    /**
     * Creeate a new frame with the given sprite and duration.
     * @param sprite the sprite for the frame
     * @param second the duration of the frame, in seconds
     */
    public Frame(Sprite sprite, float second) {
        this.sprite = sprite;
        frameTime = second;
    }
}
