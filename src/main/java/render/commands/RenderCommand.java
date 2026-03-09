package render.commands;

public abstract class RenderCommand {
    public final CommandType type;
    public RenderCommand next = null;
    public int submitterID = 0;

    protected RenderCommand(CommandType type) {
        this.type = type;
    }

    protected void reset() {
        next = null;
        submitterID = 0;
    }
}
