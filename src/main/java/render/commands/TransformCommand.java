package render.commands;

import org.joml.Vector2f;
import org.joml.Vector4f;

public class TransformCommand extends RenderCommand {
    public final Vector2f position = new Vector2f();
    public float rotationDegrees = 0.0f;
    public final Vector2f scale = new Vector2f(1.0f);
    public int zIndex = 0;
    public boolean visible = true;
    public final Vector4f modulate = new Vector4f(1.0f);

    TransformCommand() {
        super(CommandType.Transform);
    }

    public static TransformCommand acquire() {
        return RenderCommandPool.get().acquire(TransformCommand.class, TransformCommand::new);
    }

    @Override
    protected void reset() {
        super.reset();
        position.zero();
        rotationDegrees = 0.0f;
        scale.set(1.0f);
        zIndex = 0;
        visible = true;
        modulate.set(1.0f);
    }
}
