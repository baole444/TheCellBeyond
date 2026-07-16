package components;

import scripting.API;

import java.util.HashSet;

@API
public class State implements Comparable<State> {
    public final HashSet<String> stateFilters = new HashSet<>();

    public boolean filterAsBlackList = true;

    /**
     * Priority for state transition.
     * <p>
     * When multiple states in a {@link StateEngine} meet enter condition,
     * state with the highest priority value will be chosen.
     * </p>
     * Used for automatic state transition logic.
     */
    public int transitionPriority = 0;

    public boolean isStateEnterConditionMet() {
        return false;
    }

    public void onStateEnter() {}

    public void update(float dt) {}

    public void physicUpdate(float physicDt) {}

    public void onStateExit() {}

    /**
     * Compare this state to another based on their transition priorities.
     * <p>
     * If this state A has higher priority than other state B, then A is considered less than B.
     * This allows state A to be checked first when sorted.
     * @param other the other {@link State} to compare to
     * @return a negative integer, zero, or a positive integer as this state is less than,
     * equal to, or greater than the other state.
     */
    @Override
    public int compareTo(State other) {
        return Integer.compare(other.transitionPriority, this.transitionPriority);
    }
}
