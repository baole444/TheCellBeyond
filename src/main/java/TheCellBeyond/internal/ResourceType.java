package TheCellBeyond.internal;

public interface ResourceType {
    /**
     * Get the unique id of the resource type.
     * @return the number use to identify the type of resource
     */
    int value();

    ResourceType Undefined = new ResourceType() {
        @Override
        public int value() {
            return -1;
        }

        @Override
        public String toString() {
            return "Undefined";
        }
    };
}
