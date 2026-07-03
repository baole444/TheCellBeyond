package states;

import character.Alula;

public class Right extends AlulaState {
    public Right(Alula alula) {
        super(alula);
        transitionPriority = 0;
    }

    @Override
    public boolean isStateEnterConditionMet() {
        return alula.direction == Alula.Direction.Right && alula.velocity.x > 0.0f;
    }

    @Override
    public void onStateEnter() {
        if (alula.animation != null) alula.animation.play(alula.WalkRight);
    }
}
