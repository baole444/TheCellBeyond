package threading.states;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import org.joml.Vector2f;
import scene.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * A completed snapshot of the game's state at a specific point in time.
 * This allows thread-save passing of game state.
 */
public class GameState {
    // Snapshot of all renderable objects
    private List<GameObjectState> gameObjectStates =  new ArrayList<>();

    // Frame identification
    private long frameNumber;
    private float deltaTime;

    // Snapshot of the physic world
    private PhysicWorldState physicWorldState;

    // Camera/viewport information
    private Vector2f viewportPosition;
    private float viewportZoom;

    public GameState() {
        this.frameNumber = 0;
        this.deltaTime = 0;
        this.viewportPosition = new Vector2f();
        this.viewportZoom = 1.0f;
    }

    /**
     * Create a depp copy of source game state.
     * @param source the game state to copy from.
     */
    public void copyFrom(GameState source) {
        this.gameObjectStates.clear();

        this.gameObjectStates.addAll(source.gameObjectStates);

        this.frameNumber = source.frameNumber;
        this.deltaTime = source.deltaTime;

        this.physicWorldState = source.physicWorldState;

        this.viewportPosition = new Vector2f(source.viewportPosition);
        this.viewportZoom = source.viewportZoom;
    }

    /**
     * Capture the current state of a scene.
     * @param scene the scene to capture from.
     */
    public void captureFrom(Scene scene) {
        this.gameObjectStates.clear();

        for (GameObject obj : scene.getGameObjects()) {
            if (!obj.isRemoved()) {
                this.gameObjectStates.add(new GameObjectState(obj));
            }
        }

        this.physicWorldState = new PhysicWorldState(scene.getFlatPhysic());

        Viewport viewport = scene.viewport();
        this.viewportPosition = new Vector2f(viewport.position);
        this.viewportZoom = viewport.getZoom();
    }

    public List<GameObjectState> getGameObjectStates() {
        return gameObjectStates;
    }

    public long getFrameNumber() {
        return frameNumber;
    }

    public GameState setFrameNumber(long frameNumber) {
        this.frameNumber = frameNumber;
        return this;
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public GameState setDeltaTime(float deltaTime) {
        this.deltaTime = deltaTime;
        return this;
    }

    public Vector2f getViewportPosition() {
        return viewportPosition;
    }

    public float getViewportZoom() {
        return viewportZoom;
    }
}
