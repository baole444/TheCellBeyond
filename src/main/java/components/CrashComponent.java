package components;

public class CrashComponent extends Component {
    private static final float waitTime = 3.0f;
    private transient float currentTime = 0.0f;

    @Override
    protected void onUpdate(float dt) {
        currentTime += dt;
        if (currentTime < waitTime) return;
        float chance = (float) Math.random();
        if (chance < 0.75f) {
            currentTime = 0.0f;
            return;
        }

        throw new RuntimeException("Crash test triggered, if the editor did not crash, it is good");
    }
}
