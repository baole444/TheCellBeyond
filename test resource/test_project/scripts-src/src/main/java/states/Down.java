package states;

import character.Alula;

public class Down extends AlulaState {
    public Down(Alula alula) {
        super(alula);
        transitionPriority = 2;
    }

    @Override
    public boolean isStateEnterConditionMet() {
        return alula.direction == Alula.Direction.Down && alula.velocity.y < 0.0f;
    }

    @Override
    public void onStateEnter() {
        if (alula.animation != null) alula.animation.play(alula.WalkDown);
    }
}
