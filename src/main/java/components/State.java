package components;

import java.util.HashSet;

public class State {
    public final HashSet<String> stateFilters = new HashSet<>();
    public boolean filterAsBlackList = true;

    public void onStateEnter() {}

    public void update(float dt) {}

    public void physicUpdate(float physicDt) {}

    public void onStateLeave() {}
}
