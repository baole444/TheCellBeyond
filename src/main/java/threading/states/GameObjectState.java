package threading.states;

import TCB_Field.GameObject;
import components.SpriteRender;
import components.TextComponent;
import org.joml.Vector2f;

public class GameObjectState {
    private int objectId;
    private String name;

    // Transform data
    private Vector2f position;
    private Vector2f scale;
    private float rotation;
    private int zIndex;

    // Component states
    private SpriteRenderState spriteRenderState;
    private TextComponentState textComponentState;
    private boolean isSerializable;
    private boolean isGone;

    public GameObjectState(GameObject object) {
        this.objectId = object.loadUid();
        this.name = object.name;

        // Get transform state
        this.position = new Vector2f(object.transform.position);
        this.scale = new Vector2f(object.transform.scale);
        this.rotation = object.transform.rotate;
        this.zIndex = object.transform.zIndex;

        // Get component states
        SpriteRender spriteRender = object.getComponent(SpriteRender.class);
        if (spriteRender != null) {
            this.spriteRenderState = new SpriteRenderState(spriteRender);
        }

        TextComponent textComponent = object.getComponent(TextComponent.class);
        if (textComponent != null) {
            this.textComponentState = new TextComponentState(textComponent);
        }

        this.isSerializable = object.isSerialize();
        this.isGone = object.isGone();
    }

    public int getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public Vector2f getPosition() {
        return position;
    }

    public Vector2f getScale() {
        return scale;
    }

    public float getRotation() {
        return rotation;
    }

    public int getzIndex() {
        return zIndex;
    }

    public SpriteRenderState getSpriteRenderState() {
        return spriteRenderState;
    }

    public TextComponentState getTextComponentState() {
        return textComponentState;
    }

    public boolean isSerializable() {
        return isSerializable;
    }

    public boolean isGone() {
        return isGone;
    }
}
