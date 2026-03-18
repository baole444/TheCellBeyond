package TheCellBeyond.internal;

public interface ResourceType {
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
