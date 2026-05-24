package components;

/**
 * This component entire purpose is to invoke a runtime exception to test runtime crash handling.
 */
public final class CrashComponent extends Component {
    private static final float waitTime = 3.0f;
    private transient float currentTime = 0.0f;

    /**
     * Create a new {@link CrashComponent} component.
     */
    public CrashComponent() {
        String name = CrashComponent.class.getSimpleName();
        super(name);
    }

    @Override
    protected void internalUpdate(float dt) {
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
