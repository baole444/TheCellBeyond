package render.texture;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TextureHandle {
    private static final AtomicInteger HANDLE_COUNTER = new AtomicInteger(1);

    public enum Status {
        WAITING,
        LOADING,
        READY,
        FAILED,
        DISPOSED
    }

    private final int handleId;
    private final AtomicReference<Status> status;
    private final AtomicInteger textureId;
    private final AtomicInteger width;
    private final AtomicInteger height;
    private volatile String errorMsg;

    public TextureHandle() {
        this.handleId = HANDLE_COUNTER.getAndIncrement();
        this.status = new AtomicReference<>(Status.WAITING);
        this.textureId = new AtomicInteger(-1);
        this.width = new AtomicInteger(-1);
        this.height = new AtomicInteger(-1);
    }

    public int getHandleId() {
        return handleId;
    }

    public Status getStatus() {
        return status.get();
    }

    public boolean isReady() {
        return status.get() == Status.READY;
    }

    public boolean isFailed() {
        return status.get() == Status.FAILED;
    }

    public boolean isDisposed() {
        return status.get() == Status.DISPOSED;
    }

    public int getTextureId() {
        return textureId.get();
    }

    public int getWidth() {
        return width.get();
    }

    public int getHeight() {
        return height.get();
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    protected void setStatus(Status newStatus) {
        this.status.set(newStatus);

        TextureStatusCallback.emit(getHandleId(), getStatus());
    }

    protected void setTextureId(int id) {
        this.textureId.set(id);
    }

    protected void setSize(int width, int height) {
        this.width.set(width);
        this.height.set(height);
    }

    protected void setError(String message) {
        this.errorMsg = message;
        this.status.set(Status.FAILED);
    }

    @Override
    public String toString() {
        return "TextureHandle{" +
                "id=" + handleId +
                ", status=" + status.get() +
                ", textureId=" + textureId.get() +
                ", size=" + width.get() + "x" + height.get() +
                "}";
    }
}
