package components;

import render.texture.Sprite;

import java.util.ArrayList;
import java.util.List;

/**
 * Animation hold the sequence of frames, the duration of the total sequence
 * and animation loop flag along with animation speed multiplier.
 */
public class Animation {
    private final List<Frame> frames = new ArrayList<>();
    private float speedMultiplier = 1.0f;
    public boolean loop = false;

    private transient int frameIndex = 0;
    private transient float elapsedTime = 0.0f;
    private transient float animationDuration = 0.0f;

    /**
     * Create a new animation with empty frame sequences.
     */
    public Animation() {}

    /**
     * Create a new animation with the given sequence of frames.
     * @param frames the list of frames
     */
    public Animation(List<Frame> frames) {
        if (frames.isEmpty()) return;
        this.frames.addAll(frames);
        updateDuration();
    }
    /**
     * Append a new frame into the animation.
     * @param sprite sprite used by that frame
     * @param seconds the frame time
     */
    public void addFrame(Sprite sprite, float seconds) {
        if (sprite == null || seconds <= 0.0f) return;
        Frame newFrame = new Frame(sprite, seconds);
        frames.add(newFrame);
    }

    /**
     * Insert a new frame into the animation.
     * @param sprite sprite used by that frame
     * @param seconds the frame time
     * @param frameIndex the index to insert
     */
    public void addFrameAt(Sprite sprite, float seconds, int frameIndex) {
        if (sprite == null || seconds <= 0.0f) return;
        if (frameIndex < 0 || frameIndex > frames.size()) return;
        Frame newFrame = new Frame(sprite, seconds);
        frames.add(frameIndex, newFrame);
        updateDurationFrom(frameIndex);
    }

    /**
     * Update a frame in the animation.
     * @param sprite The new sprite for that frame
     * @param seconds the new frame time
     * @param frameIndex the index to update
     */
    public void setFrameAt(Sprite sprite, float seconds, int frameIndex) {
        if (sprite == null || seconds <= 0.0f) return;
        if (frameIndex < 0 || frameIndex >= frames.size()) return;
        Frame newFrame = new Frame(sprite, seconds);
        frames.set(frameIndex, newFrame);
        updateDurationFrom(frameIndex);
    }

    /**
     * Remove a frame form the animation.
     * @param frameIndex the index to remove
     */
    public void removeFrame(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= frames.size()) return;
        frames.remove(frameIndex);
        if (frameIndex == 0) {
            updateDuration();
            return;
        }

        updateDurationFrom(frameIndex - 1);
    }

    /**
     * Set frame time for a frame in the animation.
     * @param frameIndex the index to update
     * @param frameTime the new frame time
     */
    public void setFrameTime(int frameIndex, float frameTime) {
        if (frameIndex < 0 || frameIndex >= frames.size()) return;
        frameTime = Math.max(0.01f, frameTime);
        frames.get(frameIndex).frameTime = frameTime;
        updateDurationFrom(frameIndex);
    }

    /**
     * Set the frame time for all frames in the animation base on the FPS and number of frames this animation has.
     * @param fps frame per second, must be greater than 0
     */
    public void setFPS(float fps) {
        if (fps <= 0) return;
        float frameTime = 1.0f / fps;

    }

    /**
     * Get the number of frame this animation has.
     * @return size of the frame list
     */
    public int numberOfFrames() {
        return frames.size();
    }

    /**
     * Get the index of frame this animation is currently at.
     * @return the current frame index.
     * @see Animation#currentFrame() Get current frame of the animation
     */
    public int currentFrameIndex() {
        return frameIndex;
    }

    /**
     * Get the speed of which this animation is steeping at.
     * @return update speed multiplier
     */
    public float speedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Set the update speed multiplier for the animation.
     * @param speed the multiplier that affect the update rate, minimum accepted value is 0.01 (1%)
     */
    public void setSpeed(float speed) {
        speedMultiplier = Math.max(0.01f, speed);
    }

    /**
     * Get the frame this animation is currently on.
     * @return the frame at the current frame index or null if there is no frame in the animation
     * @see Animation#currentFrameIndex() Get the current frame index of the animation
     */
    public Frame currentFrame() {
        if (frames.isEmpty()) return null;

        return frames.get(frameIndex);
    }

    /**
     * Get the frame at the given index in this animation.
     * @param index the index of the frame (index start at 0)
     * @return the frame at the index or null if index is out of bound
     */
    public Frame getFrameAt(int index) {
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }

    /**
     * Reset this animation to the first frame (index 0.)
     * @see Animation#start() initiate the animation
     */
    public void reset() {
        frameIndex = 0;
        elapsedTime = 0.0f;
    }

    /**
     * Initiate the animation. This simply invoke {@link Animation#reset()}
     * and sync the animation duration with frames' data.
     */
    public void start() {
        reset();
        updateDuration();
    }

    /**
     * Update the frame index of the animation base on elapsed time and animation speed.
     * @param dt delta time
     */
    public void update(float dt) {
        if (frames.isEmpty()) return;

        elapsedTime += dt * speedMultiplier;
        if (!loop && elapsedTime >= animationDuration) {
            frameIndex = Math.max(frames.size() - 1, 0);
            return;
        }

        float time = loop ? (elapsedTime % animationDuration) : elapsedTime;
        int low = 0;
        int high = frames.size() - 1;
        while (low < high) {
            int mid = (low + high) / 2;
            if (time < frames.get(mid).endTime) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        frameIndex = low;
    }

    private void updateDuration() {
        animationDuration = 0.0f;
        for (Frame frame : frames) {
            animationDuration += frame.frameTime;
            frame.endTime = animationDuration;
        }
    }

    private void updateDurationFrom(int startIndex) {
        if (frames.isEmpty()) return;
        startIndex = Math.max(0, startIndex);
        if (startIndex >= frames.size()) return;
        if (startIndex == 0) {
            updateDuration();
            return;
        }

        float duration = frames.get(startIndex - 1).endTime;
        for (int i = startIndex; i < frames.size(); i++) {
            duration += frames.get(i).frameTime;
            frames.get(i).endTime = duration;
        }

        animationDuration = frames.getLast().endTime;
    }
}
