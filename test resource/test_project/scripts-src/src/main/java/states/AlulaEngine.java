package states;

import character.Alula;
import components.NotSerializeComponent;
import components.StateEngine;

public class AlulaEngine extends StateEngine implements NotSerializeComponent {
    @Override
    protected void onStart() {
        if (!(gameObject instanceof Alula alula)) return;
        enableAutoStateTransition = true;
        addState("Idle", new Idle(alula));
        addState("Up", new Up(alula));
        addState("Down", new Down(alula));
        addState("Left", new Left(alula));
        addState("Right", new Right(alula));
        setDefaultState("Idle");
    }
}
