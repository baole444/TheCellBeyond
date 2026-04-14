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

    public void resetAccumulation() {
        visible = true;
        modulate.set(1.0f);
    }

    public void copyAccumulation(TransformCommand source) {
        visible = source.visible;
        modulate.set(source.modulate);
    }

    public static TransformCommand acquire() {
        return RenderCommandPool.get().acquire(TransformCommand.class, TransformCommand::new);
    }

    /**
     * Get a new {@link TransformCommand}, this one is created new, separated from pool acquired.
     * This is intended for long-lasting reference that is written to.
     * @return a new transform command
     */
    public static TransformCommand createOwned() {
        return new TransformCommand();
    }

    @Override
    protected RenderCommand acquireInstance() {
        return acquire();
    }

    @Override
    public void copyFrom(RenderCommand source) {
        super.copyFrom(source);
        if (!(source instanceof TransformCommand t)) return;
        position.set(t.position);
        rotationDegrees = t.rotationDegrees;
        scale.set(t.scale);
        zIndex = t.zIndex;
        visible = t.visible;
        modulate.set(t.modulate);
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
