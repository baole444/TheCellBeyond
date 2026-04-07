package render.commands;

public abstract class RenderCommand {
    public final CommandType type;
    public TransformCommand transform = null;
    public TransformCommand previousTransform = null;
    public RenderCommand next = null;
    public int submitterID = 0;
    public long version = 0;

    protected RenderCommand(CommandType type) {
        this.type = type;
    }

    public void markChanged() {
        version++;
    }

    public void release() {
        RenderCommandPool.get().release(this);
    }

    public void copyFrom(RenderCommand source) {
        submitterID = source.submitterID;
        transform = source.transform;
        markChanged();
    }

    public static void release(RenderCommand command) {
        if (command == null) return;
        RenderCommandPool.get().release(command);
    }

    public static RenderCommand acquireCopy(RenderCommand source) {
        return RenderCommandPool.get().acquireCopy(source);
    }

    protected abstract RenderCommand acquireInstance();

    protected void reset() {
        next = null;
        transform = null;
        previousTransform = null;
        submitterID = 0;
    }
}
