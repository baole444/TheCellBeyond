package render.texture;

import utility.IdPool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TextureHandle {
    private static final IdPool ID_POOL = new IdPool(1, false);

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
        handleId = ID_POOL.newId();
        status = new AtomicReference<>(Status.WAITING);
        textureId = new AtomicInteger(-1);
        width = new AtomicInteger(-1);
        height = new AtomicInteger(-1);
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
        textureId.set(id);
    }

    protected void setSize(int width, int height) {
        this.width.set(width);
        this.height.set(height);
    }

    protected void setError(String message) {
        errorMsg = message;
        status.set(Status.FAILED);
    }

    protected void releaseId() {
        ID_POOL.releaseId(handleId);
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
