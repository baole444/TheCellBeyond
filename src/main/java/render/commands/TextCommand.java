package render.commands;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.text.HorizontalAlignment;
import render.text.VerticalAlignment;

public class TextCommand extends RenderCommand {
    public String text = "";
    public ResourceID fontRID = null;
    public float points = 12.0f;
    public HorizontalAlignment horizontalAlignment = HorizontalAlignment.Left;
    public VerticalAlignment verticalAlignment = VerticalAlignment.Top;
    public final Vector4f modulate = new Vector4f(1.0f);

    public final Vector2f position = new Vector2f();
    public float rotationDegrees = 0.0f;
    public final Vector2f scale = new Vector2f(1.0f);

    TextCommand() {
        super(CommandType.Text);
    }

    public static TextCommand acquire() {
        return RenderCommandPool.get().acquire(TextCommand.class, TextCommand::new);
    }

    public static void release(TextCommand command) {
        RenderCommandPool.get().release(command);
    }

    @Override
    protected void reset() {
        super.reset();
        text = "";
        fontRID = null;
        points = 12.0f;
        horizontalAlignment = HorizontalAlignment.Left;
        verticalAlignment = VerticalAlignment.Top;
        modulate.set(1.0f);
        position.zero();
        rotationDegrees = 0.0f;
        scale.set(1.0f);
    }
}
