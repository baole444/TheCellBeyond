package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import org.joml.Vector2f;
import scene.Scene;

public class Parallax2D extends GameObject2D {
    /**
     * The multiplier for the {@link Parallax2D}'s final offset.
     * This can be used to control how fast the parallax effect scroll,
     * thus create the perception of distance from the camera.
     * <p>
     * For example, a value of {@code 1.0} will make the effect scrolls at the same speed as the camera.
     * A value greater than {@code 1.0} will scroll faster, make object appear closer, while less than
     * {@code 1.0} will make the object appear further by scrolling slower.
     * Object will stop scrolling on an axis if the value is set to {@code 0.0} for that axis.
     * </p>
     * When this value is set to less than {@code 0.0}, objects will scroll along and faster than the camera's scroll.
     * This is opposed to positive values where objects scroll as the camera is passing them.
     */
    public final Vector2f scrollScale = new Vector2f(1.0f);
    /**
     * The persistence scroll offset of the parallax effect, in world units.
     * This is the manual offset value that can be used to shift the parallax effect.
     * <p>
     * Unlike {@link #position()} and {@link #screenOffset}, this value is not overridden
     */
    public final Vector2f scrollOffset = new Vector2f();
    /**
     * The velocity at which the offset for the parallax effect scrolls automatically, in world units per second.
     * <p>
     * Positive value scroll toward the right, while negative will scroll toward the left.
     */
    public final Vector2f autoScrollVelocity = new Vector2f();
    /**
     * The bottom left corner limit for scrolling to start, in world units.
     * If the viewport's position is smaller than this value, the parallax effect is stopped.
     * <p>
     * This value must be smaller than {@link #topRightLimit} - {@code viewport size} to work.
     */
    public final Vector2f bottomLeftLimit = new Vector2f(-10240.0f);
    /**
     * The top right corner limit for the scrolling to end, in world units.
     * If the viewport's position is larger than this value, the parallax effect is stopped.
     * <p>
     * This value must be greater than the {@link #bottomLeftLimit} + {@code viewport size} to work.
     */
    public final Vector2f topRightLimit = new Vector2f(10240.0f);
    /**
     * Should the calculation of the parallax effect uses viewport's position as the base offset value.
     * This will make the viewport to become the origin for this parallax effect.
     * <p>
     * Set this to {@code false} to make the parallax effect independent of the viewport's position.
     */
    public boolean followViewport = true;
    /**
     * Should the parallax effect stop updating in response to the viewport's movements.
     * The effect will hold the last calculated position before this is set to {@code true}.
     * <p>
     * This is useful for scrolling the parallax effect exclusively via auto scroll or manual scrolling script,
     * regardless of the viewport's movements.
     * @apiNote
     * This must be set to {@code true} to control the {@link #screenOffset}.
     */
    public boolean ignoreViewportScroll = false;
    /**
     * Offset used to scroll this parallax effect. This value is updated automatically,
     * unless {@link #ignoreViewportScroll} is {@code true}.
     */
    public transient final Vector2f screenOffset = new Vector2f();
    private final transient Vector2f accumulatedScroll = new Vector2f();

    /**
     * Create a new {@link Parallax2D}.
     */
    public Parallax2D() {
        String name = Parallax2D.class.getSimpleName();
        super(name);
    }

    /**
     * Create a new {@link Parallax2D} with the given name.
     * @param name the new name for the parallax 2D
     */
    public Parallax2D(String name) {
        if (invalidName(name)) name = Parallax2D.class.getSimpleName();
        super(name);
    }

    @Override
    protected void onEditorStart() {
        repeatSource = true;
    }

    @Override
    protected void onStart() {
        repeatSource = true;
    }

    @Override
    protected void onUpdate(float dt) {
        if (!LogicServer.runtimeMode()) return;
        accumulatedScroll.add(autoScrollVelocity.x * dt, autoScrollVelocity.y * dt);
        if (repeatSize.x != 0.0f) accumulatedScroll.x = mod(accumulatedScroll.x, repeatSize.x);
        if (repeatSize.y != 0.0f) accumulatedScroll.y = mod(accumulatedScroll.y, repeatSize.y);
        float viewX = 0.0f, viewY = 0.0f;
        if (!ignoreViewportScroll) {
            Scene scene = LogicServer.currentScene();
            if (scene != null) {
                Vector2f viewportPosition = scene.viewport().position;
                viewX = bottomLeftLimit.x < topRightLimit.x ? Math.clamp(viewportPosition.x, bottomLeftLimit.x, topRightLimit.x) : viewportPosition.x;
                viewY = bottomLeftLimit.y < topRightLimit.y ? Math.clamp(viewportPosition.y, bottomLeftLimit.y, topRightLimit.y) : viewportPosition.y;
            }
        }
        float offsetX = scrollOffset.x + accumulatedScroll.x;
        float offsetY = scrollOffset.y + accumulatedScroll.y;
        float posX, posY;
        if (repeatSize.x != 0.0f) posX = viewX - mod(viewX * scrollScale.x - offsetX, repeatSize.x);
        else posX = viewX + offsetX - viewX * scrollScale.x;
        if (repeatSize.y != 0.0f) posY = viewY - mod(viewY * scrollScale.y - offsetY, repeatSize.y);
        else posY = viewY + offsetY - viewY * scrollScale.y;
        if (!followViewport) {
            posX -= viewX;
            posY -= viewY;
        }
        if (!ignoreViewportScroll) screenOffset.set(posX, posY);
        position(screenOffset.x, screenOffset.y);
    }

    private static float mod(float value, float mod) {
        return ((value % mod) + mod) % mod;
    }
}
