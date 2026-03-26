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
    public final Vector2f textDimension = new Vector2f();

    TextCommand() {
        super(CommandType.Text);
    }

    public static TextCommand acquire() {
        return RenderCommandPool.get().acquire(TextCommand.class, TextCommand::new);
    }

    @Override
    protected RenderCommand acquireInstance() {
        return acquire();
    }

    @Override
    public void copyFrom(RenderCommand source) {
        super.copyFrom(source);
        if (!(source instanceof TextCommand t)) return;
        text = t.text;
        fontRID = t.fontRID;
        points = t.points;
        horizontalAlignment = t.horizontalAlignment;
        verticalAlignment = t.verticalAlignment;
        modulate.set(t.modulate);
        textDimension.set(t.textDimension);
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
        textDimension.zero();
    }
}
