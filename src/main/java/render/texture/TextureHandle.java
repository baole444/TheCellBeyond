package render.texture;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import render.RenderResourceType;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TextureHandle {
    enum InternalStatus {
        WAITING,
        LOADING,
        READY,
        FAILED,
        DISPOSED
    }

    private final ResourceID RID;
    private final AtomicReference<InternalStatus> status = new AtomicReference<>(InternalStatus.WAITING);
    private final AtomicInteger textureId = new AtomicInteger(-1);
    private volatile String errorMsg;

    public TextureHandle(ResourceID RID) {
        this.RID = RID;
    }

    public ResourceID resourceID() {
        return RID;
    }

    public ResourceStatus getStatus() {
        return asResourceStatus(status.get());
    }

    public boolean isReady() {
        return status.get() == InternalStatus.READY;
    }

    public boolean isFailed() {
        return status.get() == InternalStatus.FAILED;
    }

    public boolean isDisposed() {
        return status.get() == InternalStatus.DISPOSED;
    }

    public int getTextureId() {
        return textureId.get();
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    void markLoading() {
        if (invalidTransition(InternalStatus.LOADING)) return;
        status.set(InternalStatus.LOADING);
    }

    void markReady() {
        if (invalidTransition(InternalStatus.READY)) return;
        status.set(InternalStatus.READY);
        ResourceStatusCallback.emit(RID, ResourceStatus.READY);
    }

    void markFailed(String message) {
        if (invalidTransition(InternalStatus.FAILED)) return;
        errorMsg = message;
        status.set(InternalStatus.FAILED);
        ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
    }

    void markDisposed() {
        if (invalidTransition(InternalStatus.DISPOSED)) return;
        status.set(InternalStatus.DISPOSED);
        ResourceStatusCallback.emit(RID, ResourceStatus.DISPOSED);
    }

    protected void setTextureId(int id) {
        textureId.set(id);
    }

    private static ResourceStatus asResourceStatus(InternalStatus internalStatus) {
        return switch (internalStatus) {
            case WAITING, LOADING -> ResourceStatus.WAITING;
            case READY -> ResourceStatus.READY;
            case FAILED -> ResourceStatus.FAILED;
            case DISPOSED -> ResourceStatus.DISPOSED;
        };
    }

    private boolean invalidTransition(InternalStatus nextStatus) {
        InternalStatus current = status.get();
        if (current == InternalStatus.DISPOSED) return true;
        if (nextStatus == InternalStatus.WAITING || nextStatus == InternalStatus.LOADING) {
            return current == InternalStatus.READY || current == InternalStatus.FAILED;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d, status=%s, textureId=%d}",
                TextureHandle.class.getSimpleName(), RID.id,
                status.get(), textureId.get()
        );
    }
}
