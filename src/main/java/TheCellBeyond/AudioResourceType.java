package TheCellBeyond;

import TheCellBeyond.internal.ResourceType;

public enum AudioResourceType implements ResourceType {
    Clip(3);

    public final int value;

    AudioResourceType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
