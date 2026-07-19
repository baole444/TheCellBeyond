package TheCellBeyond.internal;

import scripting.API;

/**
 * ResourceID is a universal unique ID system for resources, which can be safely pass between different pipelines,
 * while not carrying any live object reference.
 * <p>
 * This can be referred as {@code RID} for short.
 * <p>
 * Once a resource is disposed, its associated RID will never be recycled. Such RID will resolve to nothing for the rest of the session and never point to a different resource.
 */
@API
public class ResourceID {
    private static final IdCounter IdCounter = new IdCounter(1);
    /**
     * The unique ID for the resource, this is created from the shared ID counter across pipelines.
     */
    public final int id = IdCounter.newId();
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
     * Compare this RID against another by their id.
     * <p>
     * Equality of RIDs are not affect by the states of the resources behind them, as they keep the same identities for the entire session.
     * @param obj the object to compare against
     * @return true if the target is a RID carrying the same id
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResourceID target)) return false;
        return id == target.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d, type=%s}", ResourceID.class.getSimpleName(), id, type);
    }
}
