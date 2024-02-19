package components;

public class Frame {
    public Sprite sprite;
    public float frameTime;

    public Frame() {}

    public Frame(Sprite sprite, float time) {
        this.sprite = sprite;
        this.frameTime = time;
    }
}
