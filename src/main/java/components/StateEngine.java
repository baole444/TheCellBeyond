package components;

import TheCellBeyond.Window;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateEngine extends Component {
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private String defaultState = null;
    public boolean enableAutoStateTransition = false;

    private transient final LinkedHashMap<String, State> sortedStatesByPriority = new LinkedHashMap<>();
    private transient String currentStateName = null;
    private transient State currentState = null;
    private transient boolean isStatesSorted = false;
    private transient boolean initialized = false;

    @Override
    protected void onStarting() {
        if (!Window.get().isRuntimeMode()) return;

        initialize();
    }

    @Override
    protected void onUpdate(float dt) {
        if (!initialized) initialize();

        if (enableAutoStateTransition) autoStateTransition();

        if (currentState != null) currentState.update(dt);
    }

    private void initialize() {
        if (initialized) return;

        if (defaultState != null && states.containsKey(defaultState)) {
            switchState(defaultState);
        }

        initialized = true;
    }

    /**
     * Implement of a blank state for {@link #newState()}
     * @return a new {@link State} or any subclass of it
     */
    protected State newEmptyState() {
        return new State();
    }

    public void setDefaultState(String stateName) {
        if (stateName == null) {
            defaultState = null;
            return;
        }

        if (!states.containsKey(stateName)) {
            LOGGER.warning(String.format("Requested state '%s' does not exist in %s (%s)", stateName, this.getClass().getSimpleName(), getUUID()));
            return;
        }

        defaultState = stateName;
    }

    public String newState() {
        String newName = "state";

        if (states.isEmpty()) {
            states.put(newName, newEmptyState());
            isStatesSorted = false;
            return newName;
        }

        String uniqueName = newName;
        int i = states.size();
        while (states.containsKey(uniqueName)) {
            uniqueName = newName + "_" + i;
            i++;
        }

        states.put(uniqueName, newEmptyState());
        isStatesSorted = false;
        return uniqueName;
    }

    public boolean renameState(String oldName, String newName) {
        if (states.isEmpty()) return false;
        if (oldName == null || newName == null || oldName.isBlank() || newName.isBlank()) return false;
        newName = newName.trim();
        if (!states.containsKey(oldName) || states.containsKey(newName)) return false;

        State state = states.remove(oldName);
        if (state == null) return false;

        states.put(newName, state);

        for (Map.Entry<String, State> entry : states.entrySet()) {
            State s = entry.getValue();
            if (!s.stateFilters.contains(oldName)) continue;
            s.stateFilters.remove(oldName);
            s.stateFilters.add(newName);
        }

        if (oldName.equals(defaultState)) defaultState = newName;
        if (oldName.equals(currentStateName)) currentStateName = newName;

        return true;
    }

    public boolean addState(String stateName, State state) {
        if (stateName == null || stateName.isBlank() || state == null) return false;

        stateName = stateName.trim();
        if (states.containsKey(stateName)) return false;

        states.put(stateName, state);
        isStatesSorted = false;

        return true;
    }

    public boolean removeState(String stateName) {
        if (Window.get().isRuntimeMode()) {
            LOGGER.warning("State removal is forbidden while test running the scene!");
            return false;
        }

        if (stateName == null || stateName.isBlank() || !states.containsKey(stateName)) return false;

        if (stateName.equals(currentStateName)) {
            currentStateName = null;
            currentState = null;
        }

        if (stateName.equals(defaultState)) defaultState = null;

        states.remove(stateName);

        for (Map.Entry<String, State> entry : states.entrySet()) {
            State s = entry.getValue();
            s.stateFilters.remove(stateName);
        }

        isStatesSorted = false;

        return true;
    }

    public boolean switchState(String stateName) {
        return handleStateTransition(stateName, true);
    }

    public boolean forceSwitchState(String stateName) {
        return handleStateTransition(stateName, false);
    }

    public String getCurrentStateName() {
        return currentStateName;
    }

    public String getDefaultState() {
        return defaultState;
    }

    public State getCurrentState() {
        return currentState;
    }

    public Map<String, State> getStates() {
        return new HashMap<>(states);
    }

    private boolean handleStateTransition(String stateName, boolean checkFilter) {
        if (stateName == null || stateName.isBlank()) return false;
        if (!states.containsKey(stateName)) {
            LOGGER.warning(String.format("Requested state '%s' does not exist in %s (%s)", stateName, this.getClass().getSimpleName(), getUUID()));
            return false;
        }

        if (stateName.equals(currentStateName)) return false;
        if (checkFilter && !isTransitionAllowed(currentStateName, stateName)) return false;

        if (currentState != null) currentState.onStateExit();
        currentStateName = stateName;
        currentState = states.get(stateName);
        if (currentState != null) currentState.onStateEnter();

        return true;
    }

    private boolean isTransitionAllowed(String currentState, String targetState) {
        if (currentState == null) return true;

        State current = states.get(currentState);
        if (current == null) return true;

        boolean inFilter = current.stateFilters.contains(targetState);
        if (current.filterAsBlackList) return !inFilter;

        return inFilter;
    }

    private void autoStateTransition() {
        if (!isStatesSorted) sortStates();

        for (Map.Entry<String, State> entry : sortedStatesByPriority.entrySet()) {
            String stateName = entry.getKey();
            State state = entry.getValue();

            if (stateName.equals(currentStateName) || !state.isStateEnterConditionMet()) continue;
            if (!isTransitionAllowed(currentStateName, stateName)) continue;
            handleStateTransition(stateName, false);

            break;
        }
    }

    private void sortStates() {
        sortedStatesByPriority.clear();
        states.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(entry -> sortedStatesByPriority.put(entry.getKey(), entry.getValue()));

        isStatesSorted = true;
    }
}
