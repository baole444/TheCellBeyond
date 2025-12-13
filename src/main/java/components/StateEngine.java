package components;

import java.util.concurrent.ConcurrentHashMap;

public class StateEngine {
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    private State defaultState = null;
    private transient State currentState = null;

}
