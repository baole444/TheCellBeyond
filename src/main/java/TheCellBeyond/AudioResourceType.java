package TheCellBeyond;

import TheCellBeyond.internal.ResourceType;
import scripting.API;

/**
 * AudioResourceType enums contain various resource type under sound category
 */
@API
public enum AudioResourceType implements ResourceType {
    /**
     * Standard audio clip resource.
     */
    Clip(3);

    /**
     * Numeric ID for the resource type.
     */
    public final int value;

    AudioResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
