package TheCellBeyond;

import scripting.API;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * InputAction is a collection of keybinds or key combos (include mouse and keyboard),
 * served under a common name given to this action.
 * @param name the name for this action
 * @param keys the collection of keycode or key combo that can be used to activate this action
 * @see Input Checking input with InputAction
 */
@API
public record InputAction(String name, List<Set<InputKey>> keys) {
    /**
     * Compact constructor, ensure valid name and valid key collection.
     * @param name the name for this action
     * @param keys the collection of keycode or key combo that can be used to activate this action
     */
    public InputAction {
        if (name == null || name.isBlank()) name = "Unnamed_InputAction_" + UUID.randomUUID();
        if (keys == null) keys = new ArrayList<>();
        keys = keys.stream().map(Set::copyOf).toList();
    }
}
