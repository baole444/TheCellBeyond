package render.commands;

public abstract class RenderCommand {
    public final CommandType type;
    public RenderCommand next = null;
    public int submitterID = 0;

    protected RenderCommand(CommandType type) {
        this.type = type;
    }

    public void release() {
        RenderCommandPool.get().release(this);
    }

    public static void release(RenderCommand command) {
        if (command == null) return;
        RenderCommandPool.get().release(command);
    }

    protected void reset() {
        next = null;
        submitterID = 0;
    }
}
