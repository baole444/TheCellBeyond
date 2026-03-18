package TheCellBeyond.internal;

import utility.IdPool;

/**
 * ResourceID is a universal unique ID system for resources, which can be safely pass between different pipelines,
 * while not carrying any live object reference.
 * <p>
 * This can be referred as {@code RID} for short.
 */
public class ResourceID {
    private static final IdPool IdPool = new IdPool(1, false);

    /**
     * The unique ID for the resource, this is created from the shared ID Pool across pipelines.
     * @see IdPool
     */
    public final int id = IdPool.newId();

    /**
     * The type of the resource, its meaning depend on the pipeline that create and use it.
     */
    public final ResourceType type;

    /**
     * Create a new tracking {@link ResourceID} with the given type value.
     * @param type resource type provided by a pipeline
     */
    public ResourceID(ResourceType type) {
        this.type = type == null ? ResourceType.Undefined : type;
    }

    /**
     * Release the RID, this should be done when the resource that this RID pointed to is disposed.
     */
    public void release() {
        IdPool.releaseId(id);
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d, type=%s}", ResourceID.class.getSimpleName(), id, type);
    }
}
