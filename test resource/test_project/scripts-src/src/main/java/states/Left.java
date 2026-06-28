package states;

import character.Alula;

public class Left extends AlulaState {
    public Left(Alula alula) {
        super(alula);
        transitionPriority = 1;
    }

    @Override
    public void onStateEnter() {
        if (alula.animation != null) alula.animation.play(alula.WalkLeft);
    }

    @Override
    public boolean isStateEnterConditionMet() {
        return alula.direction == Alula.Direction.Left && alula.velocity.x < 0.0f;
    }
}
