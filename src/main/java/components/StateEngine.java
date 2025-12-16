package components;

import java.util.concurrent.ConcurrentHashMap;

public class StateEngine extends Component {
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private String defaultState = null;

    private transient String currentStateName = null;
    private transient State currentState = null;

}
