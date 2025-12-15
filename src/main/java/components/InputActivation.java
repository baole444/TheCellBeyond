package components;

public class InputActivation {

    /**
     * Input activation mode
     */
    public enum ActivationMode {
        /**
         * Input is activated as long as it is pressed.
         */
        Immediate,

        /**
         * Input is activated after the required hold time is meet.
         * Interruption while holding will reset the activation process.
         */
        Hold
    }

    private ActivationMode activationMode = ActivationMode.Immediate;

    /**
     * The required time to press the input (in second) before it is activated.
     * <p>
     * Immediate activation mode is not affected by this.
     */
    private float requiredHoldTime = 0.0f;

    /**
     * Tracking of current hold action.
     */
    private transient float currentHoldTime = 0.0f;

    public ActivationMode activationMode() {
        return activationMode;
    }

    public void activationMode(ActivationMode mode) {
        if (mode == null) return;
        if (activationMode != mode) reset();
        activationMode = mode;
    }

    public float requiredHoldTime() {
        return requiredHoldTime;
    }

    public void requiredHoldTime(float holdTime) {
        requiredHoldTime = Math.max(0.0f, holdTime);
    }

    /**
     * Check if the input met activation conditions or not.
     * @param isPressed the press state of the input
     * @param dt delta time
     * @return true if the conditions are met
     */
    public boolean isActivated(boolean isPressed, float dt) {
        if (!isPressed) {
            currentHoldTime = 0.0f;
            return false;
        }

        if (activationMode == ActivationMode.Immediate) return true;

        currentHoldTime += dt;
        return currentHoldTime >= requiredHoldTime;
    }

    /**
     * Get the progress until input activation (0.0 -> 1.0).
     * <p>
     * Immediate activation mode will return 1.0 on pressed
     * @param isPressed the press state of the input
     * @return progress value normalized to range of 0.0 and 1.0
     */
    public float activationProgress(boolean isPressed) {
        if (!isPressed) return 0.0f;

        if (activationMode == ActivationMode.Immediate) return 1.0f;

        if (requiredHoldTime <= 0.0f) return 1.0f;
        return Math.min(1.0f, currentHoldTime / requiredHoldTime);
    }

    /**
     * Reset tracking state for activation conditions.
     */
    public void reset() {
        currentHoldTime = 0.0f;
    }
}
