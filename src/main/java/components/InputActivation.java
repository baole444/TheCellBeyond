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
        Hold,

        /**
         * Input is activated within the frame when it was pressed.
         */
        Tap
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

    /**
     * Tracking of tap action.
     */
    private transient boolean inputConsumed = false;

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
            reset();
            return false;
        }

        switch (activationMode) {
            case Immediate -> {
                return true;
            }
            case Hold -> {
                currentHoldTime += dt;
                return currentHoldTime >= requiredHoldTime;
            }
            case Tap -> {
                if (inputConsumed) return false;
                inputConsumed = true;
                return true;
            }
            default -> {
                return false;
            }
        }
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

        switch (activationMode) {
            case Immediate -> {
                return 1.0f;
            }
            case Hold -> {
                if (requiredHoldTime <= 0.0f) return 1.0f;
                return Math.min(1.0f, currentHoldTime / requiredHoldTime);
            }
            case Tap -> {
                return inputConsumed ? 0.0f : 1.0f;
            }
            default -> {
                return 0.0f;
            }
        }
    }

    /**
     * Reset tracking state for activation conditions.
     */
    public void reset() {
        currentHoldTime = 0.0f;
        inputConsumed = false;
    }
}
