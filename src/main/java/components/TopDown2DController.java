package components;

import TheCellBeyond.InputAction;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class TopDown2DController extends Component {
    private float velocity;
    private final ConcurrentHashMap<ControllerDirection, HashSet<InputAction>> directionActionBind = new ConcurrentHashMap<>();

    @Override
    protected void additionalUpdateLogic(float dt) {

    }
}
