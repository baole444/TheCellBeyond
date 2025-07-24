package components;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class StateEngine extends Component {
    // Use for trigger a state
    private static class StateCondition {
        public String state;
        public String condition;

        public StateCondition() {}
        public StateCondition(String state, String condition) {
            this.state = state;
            this.condition = condition;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != StateCondition.class) return false;
            StateCondition t2 = (StateCondition)o;
            return t2.condition.equals(this.condition) && t2.state.equals(this.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(condition, state);
        }
    }

    public HashMap<StateCondition, String> shiftState = new HashMap<>();
    private List<AnimationState> states = new ArrayList<>();
    private transient AnimationState instState = null;
    private String defaultTitle = "";

    public void reloadTexture() {
        for (AnimationState state : states) {
            state.reloadTexture();
        }
    }

    public void addStateCondition(String from, String to, String meetCondition) {
        this.shiftState.put(new StateCondition(from, meetCondition), to);
    }

    public void addState(AnimationState state) {
        this.states.add(state);
    }

    public void condition(String condition) {
        for (StateCondition state : shiftState.keySet()) {
            if (state.state.equals(instState.title) && state.condition.equals(condition)) {
                if (shiftState.get(state) != null) {
                    int stateIndex = getStateIndex(shiftState.get(state));
                    if (stateIndex > -1) {
                        instState = states.get(stateIndex);
                    }
                }
                return;
            }
        }

        System.out.println("Unknown condition " + condition + " !");
    }

    private int getStateIndex(String title) {
        int index = 0;
        for (AnimationState animationState : states) {
            if (animationState.title.equals(title)) {
                return index;
            }
            index++;
        }

        return -1;
    }

    public void setDefaultState(String title) {
        for (AnimationState state : states) {
            if (state.title.equals(title)) {
                defaultTitle = title;

                if (instState == null) {
                    instState = state;
                    return;
                }
            }
        }

        System.out.println("Failed to find state " + title);
    }

    @Override
    public void start() {
        for (AnimationState animationState : states) {
            if (animationState.title.equals(defaultTitle)) {
                instState = animationState;
                break;
            }
        }
    }

    @Override
    public void update(float dt) {
        if (instState != null) {
            instState.update(dt);
            SpriteRenderer spriteRenderer = gameObject.getFirstComponent(SpriteRenderer.class);
            if (spriteRenderer != null) {
                spriteRenderer.setSprite(instState.loadInstSprite());
            }
        }
    }

    @Override
    public void editorUpdate(float dt) {
        if (instState != null) {
            instState.update(dt);
            SpriteRenderer spriteRenderer = gameObject.getFirstComponent(SpriteRenderer.class);
            if (spriteRenderer != null) {
                spriteRenderer.setSprite(instState.loadInstSprite());
            }
        }
    }

    @Override
    public void imgui() {
        int index = 0;
        for (AnimationState state : states) {
            ImString title = new ImString(state.title);
            ImGui.inputText("State: ", title);
            state.title = title.get();

            ImBoolean isLoop = new ImBoolean(state.isLoop);
            ImGui.checkbox("Loop Animation", isLoop);
            state.setLoop(isLoop.get());

            for (Frame frame : state.animateFrame) {
                float[] tmp = new float[1];
                tmp[0] = frame.frameTime;
                ImGui.dragFloat("Frame( " + index + " ) Time: ", tmp, 0.01f);
                frame.frameTime = tmp[0];
                index++;
            }
        }
    }
}
