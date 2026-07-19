package utility;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import TheCellBeyond.internal.ResourceStatusListener;
import scripting.API;

import java.util.function.BiConsumer;

/**
 * ResourceTracker allow observing status changes for a RID.
 * <p>
 * A tracker can be obtained via {@link AssetManager#track(ResourceID, BiConsumer)}.
 * Once created, a tracker is bounded to a single RID with callback triggered on matching RID.
 * </p>
 * Once a terminal status is reached ({@link ResourceStatus#Disposed} or {@link ResourceStatus#Failed}),
 * the tracker will cancel itself.
 * If the consumer no longer need updates (destroyed, or change resource),
 * the tracker should manually cancel using {@link #cancel()}.
 */
@API
public final class ResourceTracker {
    private final ResourceID RID;
    private final BiConsumer<ResourceID, ResourceStatus> onChange;
    private final ResourceStatusListener listener;
    private boolean active = true;

    ResourceTracker(ResourceID RID, BiConsumer<ResourceID, ResourceStatus> onChange) {
        this.RID = RID;
        this.onChange = onChange;
        this.listener = this::dispatch;
        ResourceStatusCallback.register(listener);
    }

    /**
     * Stop receiving status updates, the tracker become inactive.
     */
    public void cancel() {
        if (!active) return;
        active = false;
        ResourceStatusCallback.unregister(listener);
    }

    /**
     * Check if this tracker stills receiving status updates.
     * @return true if active
     */
    public boolean active() {
        return active;
    }

    /**
     * Get the {@link ResourceID} that is bound to this tracker.
     * @return the tracking RID
     */
    public ResourceID RID() {
        return RID;
    }

    private void dispatch(ResourceID changed, ResourceStatus status) {
        if (!active || !RID.equals(changed)) return;
        onChange.accept(changed, status);
        if (status == ResourceStatus.Disposed || status == ResourceStatus.Failed) cancel();
    }


}
