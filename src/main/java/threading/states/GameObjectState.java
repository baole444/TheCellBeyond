package threading.states;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform;
import components.SpriteRender;
import components.TextComponent;

public class GameObjectState {
    private final int objectId;
    private final String name;

    // Transform data
    private final Transform transform;

    // Component states
    private SpriteRenderState spriteRenderState;
    private TextComponentState textComponentState;
    private final boolean isSerializable;
    private final boolean isRemoved;

    public GameObjectState(GameObject object) {
        this.objectId = object.getUID();
        this.name = object.name;

        // Get transform state
        this.transform = new Transform(object.transform);

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
        this.isRemoved = object.isRemoved();
    }

    public int getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public Transform getTransform() {
        return transform;
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

    public boolean isRemoved() {
        return isRemoved;
    }
}
