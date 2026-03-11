package render.commands;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class RectCommand extends RenderCommand {
    public ResourceID textureRID = null;
    public final Vector2f[] uvCoordinates = {
            new Vector2f(1.0f),
            new Vector2f(1.0f, 0.0f),
            new Vector2f(),
            new Vector2f(0.0f, 1.0f)
    };
    public boolean flipHorizontally = false;
    public boolean flipVertically = false;
    public final Vector4f modulate = new Vector4f(1.0f);

    public final Vector2f position = new Vector2f();
    public float rotationDegrees = 0.0f;
    public final Vector2f scale = new Vector2f(1.0f);

    RectCommand() {
        super(CommandType.Rect);
    }

    public static RectCommand acquire() {
        return RenderCommandPool.get().acquire(RectCommand.class, RectCommand::new);
    }

    public static void release(RectCommand command) {
        RenderCommandPool.get().release(command);
    }

    @Override
    protected void reset() {
        super.reset();
        textureRID = null;
        uvCoordinates[0].set(1.0f);
        uvCoordinates[1].set(1.0f, 0.0f);
        uvCoordinates[2].zero();
        uvCoordinates[3].set(0.0f, 1.0f);
        flipHorizontally = false;
        flipVertically = false;
        modulate.set(1.0f);
        position.zero();
        rotationDegrees = 0.0f;
        scale.set(1.0f);
    }
}
