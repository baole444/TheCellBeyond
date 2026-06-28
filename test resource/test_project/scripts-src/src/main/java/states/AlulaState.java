package states;

import character.Alula;
import components.State;

public abstract class AlulaState extends State {
    public final Alula alula;

    public AlulaState(Alula alula) {
        this.alula = alula;
    }
}
