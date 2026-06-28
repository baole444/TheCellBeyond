package states;

import character.Alula;

public class Idle extends AlulaState {
    private Alula.Direction pastDirection;

    public Idle(Alula alula) {
        super(alula);
        transitionPriority = 4;
    }

    @Override
    public boolean isStateEnterConditionMet() {
        return alula.velocity.equals(0.0f, 0.0f);
    }

    @Override
    public void onStateEnter() {
        pastDirection = alula.direction;
        if (alula.animation != null) alula.animation.stop();
    }

    @Override
    public void update(float dt) {
        if (pastDirection == alula.direction) return;
        pastDirection = alula.direction;
        alula.updateAnimation();
    }
}
