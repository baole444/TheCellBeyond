package TheCellBeyond;

import java.util.List;
import java.util.Set;

public record InputAction(String name, List<Set<InputKey>> keys) {
    public InputAction {
        keys = keys.stream().map(Set::copyOf).toList();
    }
}
