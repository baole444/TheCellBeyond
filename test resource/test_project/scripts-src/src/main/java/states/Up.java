package states;

import character.Alula;

public class Up extends AlulaState {
    public Up(Alula alula) {
        super(alula);
        transitionPriority = 3;
    }

    @Override
    public boolean isStateEnterConditionMet() {
        return alula.direction == Alula.Direction.Up && alula.velocity.y > 0.0f;
    }

    @Override
    public void onStateEnter() {
        if (alula.animation != null) alula.animation.play(alula.WalkUp);
    }
}
