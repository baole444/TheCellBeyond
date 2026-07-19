package TheCellBeyond.internal;

import scripting.API;

/**
 * Resource status allow describing universal state of a native resource, free from the context of its specialized loading pipeline.
 * @apiNote Each resource pipeline may have finer states, but they should always be mapped to one of the 4 universal states here.
 */
@API
public enum ResourceStatus {
    /**
     * The resource is waiting to be loaded or is loading.
     */
    Waiting,
    /**
     * The resource is loaded and ready to be used.
     */
    Ready,
    /**
     * The resource had failed to load, terminal.
     */
    Failed,
    /**
     * The resource is discarded and cannot be use again, terminal.
     */
    Disposed
}
